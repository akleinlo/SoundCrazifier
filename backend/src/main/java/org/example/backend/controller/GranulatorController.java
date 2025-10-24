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
    public String playAudio(@RequestParam("audioFile") MultipartFile audioFile) throws IOException {
        if (granulatorService.isRunning()) {
            logger.info("Granulation already running, rejecting new play request");
            return "Granulation already running!";
        }
        handleGranulationSync(audioFile, true);
        return "Granulation started!";
    }


    /**
     * Saves the granulated audio to a user-specified path
     */
    @PostMapping("/save")
    public ResponseEntity<Resource> saveAudio(@RequestParam("audioFile") MultipartFile audioFile) throws IOException {
        Path tempOutput = handleGranulationSync(audioFile, false);
        if (tempOutput == null) {
            throw new IllegalStateException("tempOutput should never be null in /save");
        }
        org.springframework.core.io.Resource resource = new org.springframework.core.io.PathResource(tempOutput);
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
    private Path handleGranulationSync(MultipartFile audioFile, boolean live) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path tempAudio = Files.createTempFile(Path.of(tempDir), "audio-", ".wav");
        Path tempSco = Files.createTempFile(Path.of(tempDir), "granular-", ".sco");
        Files.write(tempAudio, audioFile.getBytes());
        String scoContent = Files.readString(new ClassPathResource("csound/granular.sco").getFile().toPath(), StandardCharsets.UTF_8);
        Files.writeString(tempSco, scoContent.replace("\"REPLACE_ME\"", "\"" + tempAudio.toAbsolutePath() + "\""), StandardCharsets.UTF_8);
        Path orcPath = new ClassPathResource("csound/granular.orc").getFile().toPath();
        Path tempOutput = live ? null : Files.createTempFile(Path.of(tempDir), "granulated-", ".wav");
        logger.info("Temporary audio file created at {}", tempAudio);
        logger.info("Temporary SCO file created at {}", tempSco);
        if (live) {
            // async playback
            new Thread(() -> {
                try {
                    granulatorService.performGranulationOnce(orcPath, tempSco, true, null);
                } catch (IOException e) {
                    logger.error("Granulation playback failed for {}", audioFile.getOriginalFilename(), e);
                }
            }).start();
            return null;
        } else {
            // synchronous render
            granulatorService.performGranulationOnce(orcPath, tempSco, false, tempOutput);
            return tempOutput;
        }
    }
}