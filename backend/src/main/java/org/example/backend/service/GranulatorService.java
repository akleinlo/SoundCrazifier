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
    private volatile Csound currentCsound = null;
    private static final Logger logger = LoggerFactory.getLogger(GranulatorService.class);

    protected Object createCsoundInstance() {
        return new Csound();
    }

    public synchronized String performGranulationOnce(Path orcPath, Path scoPath, boolean outputLive, Path outputPath) throws IOException {
        if (isRunning) {
            return "Granulation already running!";
        }

        isRunning = true;

        if (outputLive) {
            // Live-Playback in eigenem Thread
            new Thread(() -> {
                Object csound = createCsoundInstance();
                currentCsound = (csound instanceof Csound c) ? c : null;
                try {
                    performGranulation(orcPath, scoPath, true, outputPath);
                } catch (IOException e) {
                    logger.error("Error during live granulation", e);
                } finally {
                    if (currentCsound != null) {
                        try {
                            currentCsound.Stop();
                            currentCsound.Reset();
                            currentCsound.Cleanup();
                        } catch (Exception e) {
                            logger.error("Failed to cleanup Csound instance", e);
                        }
                    }
                    currentCsound = null;
                    isRunning = false;
                    logger.info("Live granulation thread ended");
                }
            }).start();
            return "Granulation started!";
        } else {
            try {
                performGranulation(orcPath, scoPath, false, outputPath);
            } finally {
                isRunning = false;
            }
            return "Granulation finished!";
        }
    }


    protected void performGranulation(Path orcPath, Path scoPath, boolean outputLive, Path outputPath) throws IOException {
        Path tempDir = Files.createTempDirectory("granulator-");
        Path effectiveOutput = outputPath != null
                ? outputPath.toAbsolutePath()
                : Files.createTempFile(tempDir, "granulated-", ".wav");

        if (effectiveOutput.getParent() != null) {
            Files.createDirectories(effectiveOutput.getParent());
        }

        Object csound = createCsoundInstance(); // Fake or real

        try {
            String orc = Files.readString(orcPath);
            String sco = Files.readString(scoPath);

            if (csound instanceof Csound real) {
                currentCsound = real;
                if (outputLive) real.SetOption("-odac");
                else {
                    real.SetOption("-o" + effectiveOutput);
                    real.SetOption("--format=float");
                    real.SetOption("-r96000");
                }

                logger.debug("--- ORC content ---\n" + orc);
                logger.debug("--- SCO content ---\n" + sco);

                real.CompileOrc(orc);
                real.ReadScore(sco);
                real.Start();

                while (real.PerformKsmps() == 0 && isRunning) {
                }

                logger.info("Granulation complete.");

            } else if (csound instanceof FakeCsound fake) {
                currentCsound = null; // FakeCsound, keine echte Stop-Funktion
                fake.SetOption("-o" + effectiveOutput);
                logger.debug("--- ORC content ---\n" + orc);
                logger.debug("--- SCO content ---\n" + sco);
                fake.CompileOrc(orc);
                fake.ReadScore(sco);
                fake.Start();
                fake.PerformKsmps();
                logger.info("Fake granulation complete.");
            }

        } catch (Exception e) {
            throw new IOException("Granulation failed", e);
        } finally {
            if (csound instanceof Csound real) {
                try {
                    real.Stop();
                    real.Reset();
                    real.Cleanup();
                } catch (Exception e) {
                    logger.error("Failed to cleanup Csound instance", e);
                }
                currentCsound = null;
            }
            isRunning = false;
        }
    }

    public synchronized void stopGranulation() {
        if (currentCsound != null && isRunning) {
            logger.info("Signaling current granulation to stop...");
            isRunning = false;  // Live-Thread beendet sich dann selbst und macht Cleanup
        } else {
            logger.info("No granulation is currently running.");
        }
    }


}



