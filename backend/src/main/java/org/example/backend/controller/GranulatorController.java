package org.example.backend.controller;

import org.example.backend.service.GranulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/granulator")
public class GranulatorController {
    private static final Logger logger = LoggerFactory.getLogger(GranulatorController.class);
    private final GranulatorService granulatorService;

    public GranulatorController(GranulatorService granulatorService) {
        this.granulatorService = granulatorService;
    }

    /**
     * Plays the granulated audio live
     */
    @PostMapping("/play")
    public String playAudio(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("duration") double duration) throws IOException {

        // 1. Temporäre Datei speichern, nur um die Dauer zu prüfen
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
        Path tempAudio = Files.createTempFile(tempDir, "audio-", ".wav");
        Files.write(tempAudio, audioFile.getBytes());

        // 2. Dauer prüfen
        double audioDurationSec = getAudioDuration(tempAudio);
        if (audioDurationSec > 60.0) {
            Files.deleteIfExists(tempAudio);
            return "Audio too long! Maximum allowed duration is 60 seconds.";
        }

        // 3. Nur wenn OK -> Granulation starten
        handleGranulationSync(audioFile, duration, true);

        // 4. Temp-Audio kann ggf. gelöscht werden, wenn handleGranulationSync eigene Temp-Dateien erstellt
        Files.deleteIfExists(tempAudio);

        return "Granulation started!";
    }


    public double getAudioDuration(Path audioPath) throws IOException {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioPath.toFile())) {
            AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength();
            return frames / format.getFrameRate();  // Dauer in Sekunden
        } catch (Exception e) {
            throw new IOException("Failed to determine audio duration", e);
        }
    }



    /**
     * Saves the granulated audio to a user-specified path
     */
    @PostMapping("/save")
    public ResponseEntity<Resource> saveAudio(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("duration") double duration) throws IOException {

        Path tempOutput = handleGranulationSync(audioFile, duration, false);
        if (tempOutput == null) {
            throw new IllegalStateException("tempOutput should never be null in /save");
        }

        Resource resource = new org.springframework.core.io.PathResource(tempOutput);
        String originalName = audioFile.getOriginalFilename() != null ? audioFile.getOriginalFilename() : "granulated.wav";
        String downloadName = originalName.replace(".wav", "-granulated.wav");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentLength(Files.size(tempOutput))
                .body(resource);
    }

    /**
     * Internal helper to avoid code duplication
     */
    private Path handleGranulationSync(MultipartFile audioFile, double duration, boolean live) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path tempAudio = Files.createTempFile(Path.of(tempDir), "audio-", ".wav");
        Path tempSco = Files.createTempFile(Path.of(tempDir), "granular-", ".sco");

        // Audio speichern
        Files.write(tempAudio, audioFile.getBytes());

        // Score laden
        String scoContent = Files.readString(new ClassPathResource("csound/granular.sco").getFile().toPath(), StandardCharsets.UTF_8);

        // REPLACE_ME durch Pfad ersetzen
        scoContent = scoContent.replace("\"REPLACE_ME\"", "\"" + tempAudio.toAbsolutePath() + "\"");

        // p3 ersetzen (nur das erste Vorkommen der 20.0 nach i1)
        scoContent = scoContent.replaceFirst("(i1\\s+0\\.0\\s+)(\\d+\\.?\\d*)", "$1" + duration);

        // Neues .sco schreiben
        Files.writeString(tempSco, scoContent, StandardCharsets.UTF_8);

        Path orcPath = new ClassPathResource("csound/granular.orc").getFile().toPath();
        Path tempOutput = live ? null : Files.createTempFile(Path.of(tempDir), "granulated-", ".wav");

        logger.info("Temporary audio file created at {}", tempAudio);
        logger.info("Temporary SCO file created at {}", tempSco);
        logger.info("Using duration: {} seconds", duration);

        if (live) {
            new Thread(() -> {
                try {
                    granulatorService.performGranulationOnce(orcPath, tempSco, true, null);
                } catch (IOException e) {
                    logger.error("Granulation playback failed for {}", audioFile.getOriginalFilename(), e);
                }
            }).start();
            return null;
        } else {
            granulatorService.performGranulationOnce(orcPath, tempSco, false, tempOutput);
            return tempOutput;
        }
    }

    @PostMapping("/stop")
    public String stopAudio() {
        granulatorService.stopGranulation();
        return "Granulation stopped!";
    }



}