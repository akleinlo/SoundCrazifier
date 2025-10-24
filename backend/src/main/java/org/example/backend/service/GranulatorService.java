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

    protected Object createCsoundInstance() {
        return new Csound();
    }

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
        Path tempDir = Files.createTempDirectory("granulator-");
        Path effectiveOutput = outputPath != null
                ? outputPath.toAbsolutePath()
                : Files.createTempFile(tempDir, "granulated-", ".wav");

        if (effectiveOutput.getParent() != null) {
            Files.createDirectories(effectiveOutput.getParent());
        }

        Object csound = createCsoundInstance(); // <-- Fake oder echt

        try {
            // Import files
            String orc = Files.readString(orcPath);
            String sco = Files.readString(scoPath);

            if (csound instanceof Csound real) {
                if (outputLive) real.SetOption("-odac");
                else real.SetOption("-o" + effectiveOutput);

                logger.debug("--- ORC content ---");
                logger.debug(orc);
                logger.debug("--- SCO content ---");
                logger.debug(sco);

                // Prepare and run Csound
                real.CompileOrc(orc);
                real.ReadScore(sco);
                real.Start();
                while (real.PerformKsmps() == 0) {
                }
                logger.info("Granulation complete.");

                real.Stop();
                real.Reset();
                real.Cleanup();

            } else if (csound instanceof FakeCsound fake) {
                // FakeCsound with Logging
                fake.SetOption("-o" + effectiveOutput);
                logger.debug("--- ORC content ---");
                logger.debug(orc);
                logger.debug("--- SCO content ---");
                logger.debug(sco);
                fake.CompileOrc(orc);
                fake.ReadScore(sco);
                fake.Start();
                fake.PerformKsmps();
                fake.Stop();
                fake.Reset();
                fake.Cleanup();
                logger.info("Fake granulation complete.");
            }

        } catch (Exception e) {
            throw new IOException("Granulation failed", e);
        }
    }
}
