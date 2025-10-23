package org.example.backend.controller;

import org.example.backend.service.GranulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
        return handleGranulation(audioFile, true, null);
    }

    /**
     * Saves the granulated audio to a user-specified path
     */
    @PostMapping("/save")
    public ResponseEntity<Resource> saveAudio(@RequestParam("audioFile") MultipartFile audioFile) throws IOException {
        if (granulatorService.isRunning()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build(); // schon läuft
        }

        String tempDir = System.getProperty("java.io.tmpdir");
        Path tempAudio = Files.createTempFile(Path.of(tempDir), "audio-", ".wav");
        Path tempSco = Files.createTempFile(Path.of(tempDir), "granular-", ".sco");

        // Save upload file
        Files.write(tempAudio, audioFile.getBytes());
        logger.info("Temporary audio file created at {}", tempAudio);

        // Load SCO template and replace path
        var scoTemplate = new ClassPathResource("csound/granular.sco");
        String scoContent = Files.readString(scoTemplate.getFile().toPath(), StandardCharsets.UTF_8);
        String replaced = scoContent.replace("\"REPLACE_ME\"", "\"" + tempAudio.toAbsolutePath() + "\"");
        Files.writeString(tempSco, replaced, StandardCharsets.UTF_8);
        logger.info("Temporary SCO file created at {}", tempSco);

        // Fixed ORC file
        var orcResource = new ClassPathResource("csound/granular.orc");
        Path orcPath = orcResource.getFile().toPath();

        // Ausgabe-Pfad temporär auf Server
        Path tempOutput = Files.createTempFile(Path.of(tempDir), "granulated-", ".wav");

        // Execute granulation synchronously
        granulatorService.performGranulationOnce(orcPath, tempSco, false, tempOutput);

        // Return resource for download
        org.springframework.core.io.Resource resource = new org.springframework.core.io.PathResource(tempOutput);

        String originalName = audioFile.getOriginalFilename() != null
                ? audioFile.getOriginalFilename()
                : "granulated.wav";
        String downloadName = originalName.replace(".wav", "-granulated.wav");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentLength(Files.size(tempOutput))
                .body(resource);

    }


    /**
     * Internal helper to avoid code duplication
     */
    private String handleGranulation(MultipartFile audioFile, boolean live, Path outputPath) throws IOException {
        if (granulatorService.isRunning()) {
            logger.info("Granulation request rejected: already running.");
            return "Granulation already running!";
        }

        String tempDir = System.getProperty("java.io.tmpdir");
        Path tempAudio = Files.createTempFile(Path.of(tempDir), "audio-", ".wav");
        Path tempSco = Files.createTempFile(Path.of(tempDir), "granular-", ".sco");

        // Save uploaded audio file
        Files.write(tempAudio, audioFile.getBytes());
        logger.info("Temporary audio file created at {}", tempAudio);

        // Load and replace score template
        var scoTemplate = new ClassPathResource("csound/granular.sco");
        String scoContent = Files.readString(scoTemplate.getFile().toPath(), StandardCharsets.UTF_8);
        String replaced = scoContent.replace("\"REPLACE_ME\"", "\"" + tempAudio.toAbsolutePath() + "\"");
        Files.writeString(tempSco, replaced, StandardCharsets.UTF_8);
        logger.info("Temporary SCO file created at {}", tempSco);

        // Fixed ORC file in resources
        var orcResource = new ClassPathResource("csound/granular.orc");
        Path orcPath = orcResource.getFile().toPath();

        // Run asynchronously
        new Thread(() -> {
            try {
                granulatorService.performGranulationOnce(orcPath, tempSco, live, outputPath);
            } catch (IOException e) {
                logger.error("Granulation failed", e);
            } finally {
                try {
                    Files.deleteIfExists(tempAudio);
                    Files.deleteIfExists(tempSco);
                } catch (IOException e) {
                    logger.warn("Could not delete temp files", e);
                }
            }
        }).start();

        return live ? "Granulation playing!" : "Granulation saving to " + outputPath;
    }
}

