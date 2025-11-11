package org.example.backend.service;

import csnd6.Csound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

@Component
public class CsoundConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(CsoundConfigurator.class);

    /**
     * Configures Csound for live playback or file rendering.
     */
    public void configureCsound(Csound csound, boolean outputLive, Path effectiveOutput) {
        if (outputLive) {
            csound.SetOption("-odac");
            csound.SetOption("-r44100");             // Live: 44.1 kHz
            csound.SetOption("--format=short");      // 16-bit for live
            logger.info("Configure Csound for live output (DAC) at 44.1 kHz, 16-bit");
        } else {
            csound.SetOption("-o" + effectiveOutput.toAbsolutePath());
            csound.SetOption("--format=float");      // 32-bit float
            csound.SetOption("-r96000");             // 96 kHz for rendering
            logger.info("Configure Csound for file output: {} at 96 kHz, 32-bit float", effectiveOutput);
        }

        csound.SetOption("-b1024"); // internal buffer size
        csound.SetOption("-B4096"); // audio buffer size
    }

    /**
     * Adjusts the sample rate in the ORC content.
     */
    public String adjustSampleRate(String orcContent, int sampleRate) {
        orcContent = orcContent.replaceFirst("sr\\s*=\\s*\\d+", "sr = " + sampleRate);
        logger.info("Sample rate adjusted to {} Hz in ORC", sampleRate);
        return orcContent;
    }

    /**
     * Result object containing the updated ORC and the temporary HRTF directory.
     */
    public record HrtfInjectionResult(String orcContent, Path tempDir) {
    }

    /**
     * Copies HRTF files to a temporary directory and injects their paths into the ORC.
     */
    public HrtfInjectionResult injectHrtfPaths(String orcContent, int sampleRate) throws IOException {
        Path tempDir = Files.createTempDirectory("csound-hrtf");

        try {
            Files.setPosixFilePermissions(tempDir,
                    PosixFilePermissions.fromString("rwx------"));
        } catch (UnsupportedOperationException e) {
            logger.warn("Cannot set POSIX permissions for temporary directory {}", tempDir, e);
        }

        String baseName = "hrtf-" + sampleRate + "-";
        Path leftTemp = tempDir.resolve(baseName + "left.dat");
        Path rightTemp = tempDir.resolve(baseName + "right.dat");

        try (InputStream leftIn = new ClassPathResource("csound/" + baseName + "left.dat").getInputStream();
             InputStream rightIn = new ClassPathResource("csound/" + baseName + "right.dat").getInputStream()) {

            Files.copy(leftIn, leftTemp);
            Files.copy(rightIn, rightTemp);

            logger.info("HRTF files for {} Hz copied to temporary directory {}", sampleRate, tempDir);
        } catch (IOException e) {
            throw new IOException("HRTF files not found for " + sampleRate + " Hz", e);
        }

        String replacement = String.format(
                "gS_HRTF_left  = \"%s\"%ngS_HRTF_right = \"%s\"",
                leftTemp.toAbsolutePath(), rightTemp.toAbsolutePath());

        orcContent = orcContent.replaceFirst(
                "gS_HRTF_left\\s*=\\s*\"[^\"]+\"\\s*\\ngS_HRTF_right\\s*=\\s*\"[^\"]+\"",
                replacement);

        logger.info("Injected HRTF paths for {} Hz: left={}, right={}", sampleRate, leftTemp, rightTemp);
        return new HrtfInjectionResult(orcContent, tempDir);
    }
}
