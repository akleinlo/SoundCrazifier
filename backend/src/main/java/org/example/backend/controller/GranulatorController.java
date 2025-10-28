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

import javax.sound.sampled.*;
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

    @PostMapping("/play")
    public String playAudio(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("duration") double duration) throws IOException {

        if (granulatorService.isRunning()) {
            logger.info("Crazification already running, rejecting new play request");
            return "Crazification already running!";
        }

        handleGranulationSync(audioFile, duration, true);
        return "Crazification started!";
    }

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

    private Path handleGranulationSync(MultipartFile audioFile, double duration, boolean live) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        Path tempAudio = Files.createTempFile(Path.of(tempDir), "audio-", ".wav");
        Path tempSco = Files.createTempFile(Path.of(tempDir), "granular-", ".sco");

        // save Audio
        Files.write(tempAudio, audioFile.getBytes());

        // When saving: resample to 96 kHz
        if (!live) {
            logger.info("Resampling audio for high-quality output...");
            tempAudio = resampleWithSox(tempAudio, 96000);
        }

        // load Score
        String scoContent = Files.readString(new ClassPathResource("csound/granular.sco").getFile().toPath(), StandardCharsets.UTF_8);

        // Replace REPLACE_ME with path
        scoContent = scoContent.replace("\"REPLACE_ME\"", "\"" + tempAudio.toAbsolutePath() + "\"");

        // replace p3
        scoContent = scoContent.replaceFirst("(i1\\s+0\\.0\\s+)(\\d+\\.?\\d*)", "$1" + duration);

        // write new .sco
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
                    logger.error("Crazification playback failed for {}", audioFile.getOriginalFilename(), e);
                }
            }).start();
            return null;
        } else {
            granulatorService.performGranulationOnce(orcPath, tempSco, false, tempOutput);
            return tempOutput;
        }
    }

    /**
     * Resamples audio file using SoX to target sample rate
     */
    private Path resampleWithSox(Path inputPath, int targetSampleRate) throws IOException {
        // Check input file sample rate
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(inputPath.toFile())) {
            float currentSampleRate = ais.getFormat().getSampleRate();
            logger.info("Input audio sample rate: {} Hz", currentSampleRate);

            // If already at target rate, no resampling needed
            if (Math.abs(currentSampleRate - targetSampleRate) < 1) {
                logger.info("Sample rate already matches target, skipping resample");
                return inputPath;
            }
        } catch (UnsupportedAudioFileException e) {
            logger.warn("Could not detect sample rate, proceeding with resample anyway", e);
        }

        Path resampledPath = Files.createTempFile(inputPath.getParent(), "resampled-", ".wav");

        logger.info("Resampling {} to {} Hz using SoX...", inputPath.getFileName(), targetSampleRate);

        ProcessBuilder pb = new ProcessBuilder(
                "sox",
                inputPath.toString(),
                "-r", String.valueOf(targetSampleRate),
                resampledPath.toString()
        );

        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            // Read output for debugging
            String output = new String(process.getInputStream().readAllBytes());
            if (!output.isEmpty()) {
                logger.debug("SoX output: {}", output);
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                logger.error("SoX failed with exit code: {}", exitCode);
                throw new IOException("SoX resampling failed with exit code: " + exitCode);
            }

            logger.info("Successfully resampled to {} Hz", targetSampleRate);

            // Delete original temp file to save space
            Files.deleteIfExists(inputPath);

            return resampledPath;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("SoX process interrupted", e);
        }
    }

    @PostMapping("/stop")
    public String stopAudio() {
        granulatorService.stopGranulation();
        return "Crazification stopped!";
    }
}