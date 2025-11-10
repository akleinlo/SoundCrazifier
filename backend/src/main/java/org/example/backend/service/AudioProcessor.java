package org.example.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class AudioProcessor {

    private final String soxExecutablePath;

    public AudioProcessor(@Value("${audio.sox.path:sox}") String soxExecutablePath) {
        this.soxExecutablePath = soxExecutablePath;
    }

    private static final Logger logger = LoggerFactory.getLogger(AudioProcessor.class);

    protected ProcessBuilder createProcessBuilder(String... command) {
        return new ProcessBuilder(command);
    }


    /**
     * Resamples/converts any audio file to WAV format at the target sample rate using SoX.
     *
     * @param inputPath        The path to the original file (e.g., MP3, WAV, AIFF)
     * @param targetSampleRate The target sample rate
     * @return The path to the new temporary WAV file.
     */
    public Path resampleWithSox(Path inputPath, int targetSampleRate) throws IOException {
        Path resampledPath = Files.createTempFile(inputPath.getParent(), "resampled-", ".wav");

        logger.info("Resampling/Converting {} to {} Hz WAV using SoX...",
                inputPath.getFileName(), targetSampleRate);

        ProcessBuilder pb = createProcessBuilder(
                soxExecutablePath,
                inputPath.toString(),
                "-r", String.valueOf(targetSampleRate),
                resampledPath.toString(),
                "rate", "-v"
        );

        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes());
            if (!output.isEmpty()) {
                logger.debug("SoX output: {}", output);
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                logger.error("SoX failed with exit code: {}", exitCode);
                if (!output.isEmpty()) {
                    logger.error("SoX detailed output:\n{}", output);
                }
                throw new IOException("SoX conversion/resampling failed with exit code: " + exitCode);
            }

            logger.info("Successfully converted/resampled to {} Hz WAV at {}",
                    targetSampleRate, resampledPath.getFileName());
            return resampledPath;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("SoX process interrupted", e);
        }
    }

    public String getSoxExecutablePath() {
        return soxExecutablePath;
    }
}
