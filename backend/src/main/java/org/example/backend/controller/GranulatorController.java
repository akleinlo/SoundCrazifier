
package org.example.backend.controller;

import org.example.backend.service.AudioProcessor;
import org.example.backend.service.GranulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/crazifier")
public class GranulatorController {
    private static final Logger logger = LoggerFactory.getLogger(GranulatorController.class);
    private final GranulatorService granulatorService;
    private final AudioProcessor audioProcessor;

    // Maximum duration in seconds to support the grain opcode and avoid crashes
    private static final double MAX_DURATION_SECONDS = 60.0;

    @Autowired
    public GranulatorController(GranulatorService granulatorService, AudioProcessor audioProcessor) {
        this.granulatorService = granulatorService;
        this.audioProcessor = audioProcessor;
    }

    @PostMapping("/play")
    public ResponseEntity<String> playAudio(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("duration") double duration) throws IOException {

        // 1. duration limit check
        if (duration > MAX_DURATION_SECONDS) {
            String message = String.format("File is too long! Maximum duration is %.1f seconds to ensure stability (requested: %.1f s).",
                    MAX_DURATION_SECONDS, duration);
            logger.warn(message);
            // ResponseStatusException to return the HTTP 400 status code
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        // 2. concurrency check
        if (granulatorService.isRunning()) {
            logger.info("Crazification already running, rejecting new play request");
            return new ResponseEntity<>("Crazification already running!", HttpStatus.CONFLICT);
        }

        handleGranulationSync(audioFile, duration, true);
        return new ResponseEntity<>("Crazification started!", HttpStatus.OK);
    }

    @PostMapping("/save")
    public ResponseEntity<Resource> saveAudio(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("duration") double duration) throws IOException {

        if (duration > MAX_DURATION_SECONDS) {
            String message = String.format("File is too long! Maximum duration is %.1f seconds to ensure stability (requested: %.1f s).",
                    MAX_DURATION_SECONDS, duration);
            logger.warn(message);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        Path tempOutput = handleGranulationSync(audioFile, duration, false);

        if (tempOutput == null) {
            logger.error("Temporary output path was null during save process.");
            throw new IllegalStateException("tempOutput should never be null in /save");
        }

        Resource resource = new org.springframework.core.io.PathResource(tempOutput);
        String originalName = Optional.ofNullable(audioFile.getOriginalFilename()).orElse("granulated");
        String downloadName = originalName.replaceFirst("\\..*$", "-crazified.wav"); // Ersetzt die alte Endung durch .wav

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentLength(Files.size(tempOutput))
                .body(resource);
    }

    private Path handleGranulationSync(MultipartFile audioFile, double duration, boolean live) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String originalFilename = Optional.ofNullable(audioFile.getOriginalFilename()).orElse("audio-unknown");

        // Save temp file with original extension to make detection easier for SoX
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        Path tempAudio = Files.createTempFile(Path.of(tempDir), "audio-", fileExtension);
        Path tempSco = Files.createTempFile(Path.of(tempDir), "granular-", ".sco");

        // save Audio
        Files.write(tempAudio, audioFile.getBytes());

        Path audioForCsound;

        // When saving: Resample/Convert to 96 kHz WAV
        if (!live) {
            logger.info("Converting/Resampling audio for high-quality output...");
            // Using SoX to *always* convert to WAV and resample
            audioForCsound = audioProcessor.resampleWithSox(tempAudio, 96000);
            // Delete the original temp file, as SoX creates a new one.
            Files.deleteIfExists(tempAudio);
        } else {
            // For live playback, the file must be converted to a WAV file with 44.1 kHz
            logger.info("Converting/Resampling audio for live 44.1 kHz output...");
            audioForCsound = audioProcessor.resampleWithSox(tempAudio, 44100);
            Files.deleteIfExists(tempAudio);
        }

        // load Score
        Path scoResourcePath = new ClassPathResource("csound/granular.sco").getFile().toPath();
        String scoContent = Files.readString(scoResourcePath, StandardCharsets.UTF_8);

        // Replace REPLACE_ME with path of the *converted* WAV file
        scoContent = scoContent.replace("\"REPLACE_ME\"", "\"" + audioForCsound.toAbsolutePath() + "\"");

        // replace p3
        scoContent = scoContent.replaceFirst("(i1\\s+0\\.0\\s+)(\\d+\\.?\\d*)", "$1" + duration);

        // write new .sco
        Files.writeString(tempSco, scoContent, StandardCharsets.UTF_8);

        Path orcPath = new ClassPathResource("csound/granular.orc").getFile().toPath();
        Path tempOutput = live ? null : Files.createTempFile(Path.of(tempDir), "granulated-", ".wav");

        logger.info("Temporary Csound input audio file created at {}", audioForCsound);
        logger.info("Temporary SCO file created at {}", tempSco);
        logger.info("Using duration: {} seconds", duration);

        if (live) {
            // Service starts the background thread for live playback internally
            granulatorService.performGranulationOnce(orcPath, tempSco, true, null);
            return null;
        } else {
            granulatorService.performGranulationOnce(orcPath, tempSco, false, tempOutput);
            Files.deleteIfExists(audioForCsound);
            return tempOutput;
        }
    }

    @PostMapping("/stop")
    public String stopAudio() {
        granulatorService.stopGranulation();
        return "Crazification stopped!";
    }
}

