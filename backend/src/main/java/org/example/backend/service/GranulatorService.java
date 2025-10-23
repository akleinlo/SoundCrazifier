package org.example.backend.service;

import csnd6.Csound;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
@Service
public class GranulatorService {

    private volatile boolean isRunning = false;
    private static final Logger logger = LoggerFactory.getLogger(GranulatorService.class);

    public synchronized String performGranulationOnce(Path orcPath, Path scoPath, boolean outputLive, Path outputPath) throws IOException {
        if (isRunning) {
            return "Granulation already running!";
        }

        isRunning = true;
        try {
            performGranulation(orcPath, scoPath, outputLive, outputPath);
        } finally {
            isRunning = false;
        }

        return "Granulation finished!";
    }

    protected void performGranulation(Path orcPath, Path scoPath, boolean outputLive, Path outputPath) throws IOException {
        // Optional: isolated temporary directory for granulation
        Path tempDir = Files.createTempDirectory("granulator-");

        // Determine secure output path
        Path effectiveOutput = outputPath != null
                ? outputPath.toAbsolutePath()
                : Files.createTempFile(tempDir, "granulated-", ".wav");

        if (effectiveOutput.getParent() != null) {
            Files.createDirectories(effectiveOutput.getParent());
        }

        Csound csound = new Csound();

        try {
            // Import files
            String orc = Files.readString(orcPath);
            String sco = Files.readString(scoPath);

            // Output mode
            if (outputLive) {
                csound.SetOption("-odac");
            } else {
                csound.SetOption("-o" + effectiveOutput);
            }

            logger.debug("--- ORC content ---");
            logger.debug(orc);
            logger.debug("--- SCO content ---");
            logger.debug(sco);


            // Prepare and run Csound
            csound.CompileOrc(orc);
            csound.ReadScore(sco);
            csound.Start();

            while (csound.PerformKsmps() == 0) {
                // Runs until end
            }

            logger.info("Granulation complete.");

        } finally {
            csound.Stop();
            csound.Reset();
            csound.Cleanup();
        }
    }
}
