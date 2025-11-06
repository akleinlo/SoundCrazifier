package org.example.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Component
public class TemporaryFileManager {

    private static final Logger logger = LoggerFactory.getLogger(TemporaryFileManager.class);

    public void cleanupTempDirectory(Path tempDir) {
        if (tempDir != null) {
            try (java.util.stream.Stream<Path> pathStream = Files.walk(tempDir)) {
                pathStream
                        .sorted(Comparator.reverseOrder()) // Delete files before directories
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                logger.warn("Error deleting temporary file: {}", path, e);
                            }
                        });
            } catch (IOException e) {
                logger.warn("Error cleaning up the temporary directory: {}", tempDir, e);
            }
        }
    }

    /**
     * Cleans up the temporary input file (audio and SCO) after a synchronous save operation.
     */
    public void cleanupSaveInputFiles(Path scoPath) {
        if (scoPath != null) {
            String audioPathStr = getAudioPathFromSco(scoPath);
            if (audioPathStr != null) {
                try {
                    Files.deleteIfExists(Path.of(audioPathStr));
                    logger.info("Deleted input audio file: {}", audioPathStr);
                } catch (IOException e) {
                    logger.warn("Could not delete input audio file: {}", e.getMessage());
                }
            }
            try {
                Files.deleteIfExists(scoPath);
                logger.info("Deleted SCO file: {}", scoPath.getFileName());
            } catch (IOException e) {
                logger.warn("Could not delete temporary SCO file: {}", e.getMessage());
            }
        }
    }

    public void cleanupLiveInputFiles(Path currentLiveAudioPath, Path currentLiveScoPath) {
        if (currentLiveAudioPath != null && currentLiveScoPath != null) {
            logger.info("Clean up Csound Live input files: {} and {}", currentLiveAudioPath.getFileName(), currentLiveScoPath.getFileName());
            try {
                Files.deleteIfExists(currentLiveAudioPath);
                Files.deleteIfExists(currentLiveScoPath);
                logger.info("Cleaned up live input files: {} and {}",
                        currentLiveAudioPath.getFileName(), currentLiveScoPath.getFileName());
            } catch (IOException e) {
                logger.warn("Could not delete live files: {}", e.getMessage());
            }
        }
    }

    public void ensureValidWav(Path path) throws IOException {
        if (!Files.exists(path) || Files.size(path) == 0) {
            // 1 second of silence, 44100 Hz, 16-bit PCM, Mono
            byte[] silence = new byte[44100 * 2];
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
            try (AudioInputStream ais = new AudioInputStream(
                    new java.io.ByteArrayInputStream(silence),
                    format,
                    44100)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, path.toFile());
            } catch (Exception e) {
                logger.warn("Error creating placeholder WAV file", e);
            }
        }
    }

    public String getAudioPathFromSco(Path scoPath) {
        try {
            String scoContent = Files.readString(scoPath);
            // Search for the line f1 0 0 1 “/path/to/file.wav” 0 0 0
            int start = scoContent.indexOf('"');
            int end = scoContent.lastIndexOf('"');
            if (start != -1 && end != -1 && end > start) {
                return scoContent.substring(start + 1, end);
            }
        } catch (IOException e) {
            logger.error("Could not read SCO file for audio path", e);
        }
        return null;
    }
}
