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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/crazifier")
public class GranulatorController {
    private static final Logger logger = LoggerFactory.getLogger(GranulatorController.class);
    private final GranulatorService granulatorService;
    private final AudioProcessor audioProcessor;

    private static final Pattern FILE_EXTENSION_PATTERN = Pattern.compile("^\\.[a-zA-Z0-9]{1,5}$");

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
            @RequestParam("duration") double duration,
            @RequestParam("crazifyLevel") int crazifyLevel) throws IOException {

        if (duration > MAX_DURATION_SECONDS) {
            String message = String.format("File is too long! Maximum duration is %.1f seconds (requested: %.1f s).",
                    MAX_DURATION_SECONDS, duration);
            logger.warn(message);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        if (granulatorService.isRunning()) {
            logger.info("Crazification already running, rejecting new play request");
            return new ResponseEntity<>("Crazification already running!", HttpStatus.CONFLICT);
        }

        handleGranulationSync(audioFile, duration, crazifyLevel, true);
        return new ResponseEntity<>("Crazification started for level " + crazifyLevel, HttpStatus.OK);
    }

    @PostMapping("/save")
    public ResponseEntity<Resource> saveAudio(
            @RequestParam("audioFile") MultipartFile audioFile,
            @RequestParam("duration") double duration,
            @RequestParam("crazifyLevel") int crazifyLevel) throws IOException {

        if (duration > MAX_DURATION_SECONDS) {
            String message = String.format("File is too long! Maximum duration is %.1f seconds (requested: %.1f s).",
                    MAX_DURATION_SECONDS, duration);
            logger.warn(message);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        Path tempOutput = handleGranulationSync(audioFile, duration, crazifyLevel, false);

        if (tempOutput == null) {
            logger.error("Temporary output path was null during save process.");
            throw new IllegalStateException("tempOutput should never be null in /save");
        }

        Resource resource = new org.springframework.core.io.PathResource(tempOutput);
        String originalName = Optional.ofNullable(audioFile.getOriginalFilename()).orElse("granulated");
        String downloadName = originalName.replaceFirst("\\..*$", "-crazified.wav");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentLength(Files.size(tempOutput))
                .body(resource);
    }

    private Path handleGranulationSync(MultipartFile audioFile, double duration, int crazifyLevel, boolean live) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        String originalFilename = Optional.ofNullable(audioFile.getOriginalFilename()).orElse("audio-unknown");

        String fileExtension = getSafeFileExtension(originalFilename).orElse(".wav");

        Path tempAudio = Files.createTempFile(Path.of(tempDir), "audio-", fileExtension);
        Path tempSco = Files.createTempFile(Path.of(tempDir), "granular-", ".sco");

        Files.write(tempAudio, audioFile.getBytes());

        Path audioForCsound;

        if (!live) {
            logger.info("Converting/Resampling audio for high-quality output...");
            audioForCsound = audioProcessor.resampleWithSox(tempAudio, 96000);
            Files.deleteIfExists(tempAudio);
        } else {
            logger.info("Converting/Resampling audio for live 44.1 kHz output...");
            audioForCsound = audioProcessor.resampleWithSox(tempAudio, 44100);
            Files.deleteIfExists(tempAudio);
        }

        // Secure level (1–10)
        int level = Math.max(1, Math.min(crazifyLevel, 10));
        String scoFilename = "csound/granular" + level + ".sco";

        Path scoResourcePath = new ClassPathResource(scoFilename).getFile().toPath();
        String scoContent = Files.readString(scoResourcePath, StandardCharsets.UTF_8);

        // Replace placeholders with path of converted WAV
        scoContent = scoContent.replace("\"REPLACE_ME\"", "\"" + audioForCsound.toAbsolutePath() + "\"");

        // Replace duration placeholder REPLACE_ME_DURATION
        scoContent = scoContent.replace("REPLACE_ME_DURATION", String.valueOf(duration));

        Files.writeString(tempSco, scoContent, StandardCharsets.UTF_8);

        Path orcPath = new ClassPathResource("csound/granular.orc").getFile().toPath();
        Path tempOutput = live ? null : Files.createTempFile(Path.of(tempDir), "granulated-", ".wav");

        logger.info("Temporary Csound input audio file created at {}", audioForCsound);
        logger.info("Temporary SCO file created at {}", tempSco);
        logger.info("Using duration: {} seconds", duration);
        logger.info("Using crazify level: {}", level);

        if (live) {
            granulatorService.performGranulationOnce(orcPath, tempSco, true, null);
            return null;
        } else {
            granulatorService.performGranulationOnce(orcPath, tempSco, false, tempOutput);
            Files.deleteIfExists(audioForCsound);
            return tempOutput;
        }
    }

    @PostMapping("/getDuration")
    public ResponseEntity<Double> getDuration(@RequestParam("audioFile") MultipartFile audioFile) throws IOException {
        String originalFilename = Optional.ofNullable(audioFile.getOriginalFilename()).orElse("audio-unknown");
        String fileExtension = getSafeFileExtension(originalFilename).orElse(".wav");

        Path tempFile = Files.createTempFile("audio-duration-", fileExtension);

        try {
            Files.setPosixFilePermissions(tempFile,
                    PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignore) {
        }

        Files.write(tempFile, audioFile.getBytes());
        logger.info("Temporary file created for duration check: {}", tempFile);

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    audioProcessor.getSoxExecutablePath(),
                    "--i", "-D",
                    tempFile.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes()).trim();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String errorMessage = String.format("SoX exited with code %d while checking duration. Output: %s", exitCode, output);
                logger.error(errorMessage);
                throw new IOException(errorMessage);
            }

            double duration = Double.parseDouble(output);
            return ResponseEntity.ok(duration);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("SoX process interrupted", e);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    // ===========================
    // PRIVATE HELPER METHODS
    // ===========================

    private Optional<String> getSafeFileExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return Optional.empty();
        }

        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return Optional.empty();
        }

        String extension = filename.substring(dotIndex).toLowerCase();

        Matcher matcher = FILE_EXTENSION_PATTERN.matcher(extension);

        if (matcher.matches()) {
            return Optional.of(extension);
        }

        return Optional.empty();
    }

    @PostMapping("/stop")
    public String stopAudio() {
        granulatorService.stopGranulation();
        return "Crazification stopped!";
    }
}
