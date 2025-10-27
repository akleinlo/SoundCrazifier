package org.example.backend.service;

import csnd6.Csound;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Getter
@Service
public class GranulatorService {

    private volatile boolean isRunning = false;
    private volatile Csound currentCsound = null;
    private static final Logger logger = LoggerFactory.getLogger(GranulatorService.class);
    private long lastStopTime = 0;

    protected Object createCsoundInstance() {
        return new Csound();
    }

    public synchronized String performGranulationOnce(Path orcPath, Path scoPath, boolean outputLive, Path outputPath) throws IOException {
        long now = System.currentTimeMillis();
        if (now - lastStopTime < 1000) {
            logger.warn("Cooldown active – rejecting rapid restart.");
            return "Please wait a second before starting again.";
        }

        if (currentCsound != null && isRunning) {
            logger.warn("Old Csound instance still running — forcing cleanup");
            cleanupCsound();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }

        if (isRunning) return "Crazification already running!";

        isRunning = true;

        if (outputLive) {
            new Thread(() -> {
                Object csound = createCsoundInstance();
                currentCsound = (csound instanceof Csound c) ? c : null;
                try {
                    performGranulation(orcPath, scoPath, true, outputPath);
                } catch (IOException e) {
                    logger.error("Error during live Crazification", e);
                } finally {
                    cleanupCsound();
                }
            }).start();
            return "Crazification started!";
        } else {
            try {
                performGranulation(orcPath, scoPath, false, outputPath);
            } finally {
                isRunning = false;
            }
            return "Crazification finished!";
        }
    }

    protected void performGranulation(Path orcPath, Path scoPath, boolean outputLive, Path outputPath) throws IOException {
        Path tempDir = Files.createTempDirectory("granulator-");
        Path effectiveOutput = outputPath != null
                ? outputPath.toAbsolutePath()
                : Files.createTempFile(tempDir, "granulated-", ".wav");

        if (effectiveOutput.getParent() != null) Files.createDirectories(effectiveOutput.getParent());
        ensureValidWav(effectiveOutput);

        Object csound = createCsoundInstance();

        try {
            String orc = Files.readString(orcPath);
            String sco = Files.readString(scoPath);

            if (csound instanceof Csound real) {
                currentCsound = real;

                if (outputLive) {
                    real.SetOption("-odac");
                } else {
                    real.SetOption("-o" + effectiveOutput);
                    real.SetOption("--format=float");
                    real.SetOption("-r96000");
                    real.SetOption("-b512");
                    real.SetOption("-B2048");
                }

                logger.debug("--- ORC content ---\n" + orc);
                logger.debug("--- SCO content ---\n" + sco);

                real.CompileOrc(orc);
                real.ReadScore(sco);
                real.Start();

                while (real.PerformKsmps() == 0 && isRunning) {}

                logger.info("Crazification complete.");

            } else if (csound instanceof FakeCsound fake) {
                currentCsound = null;
                fake.SetOption("-o" + effectiveOutput);
                logger.debug("--- ORC content ---\n" + orc);
                logger.debug("--- SCO content ---\n" + sco);
                fake.CompileOrc(orc);
                fake.ReadScore(sco);
                fake.Start();
                fake.PerformKsmps();
                logger.info("Fake crazification complete.");
            }

        } catch (Exception e) {
            throw new IOException("Crazification failed", e);
        } finally {
            if (csound instanceof Csound) cleanupCsound();
        }
    }

    private void ensureValidWav(Path path) throws IOException {
        if (!Files.exists(path) || Files.size(path) == 0) {
            // 1 Sekunde Stille, 44100 Hz, 16-bit PCM, Mono
            byte[] silence = new byte[44100 * 2]; // 44100 Samples * 2 Bytes pro Sample
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false); // PCM signed, little-endian
            try (AudioInputStream ais = new AudioInputStream(
                    new java.io.ByteArrayInputStream(silence),
                    format,
                    44100)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, path.toFile());
            } catch (Exception e) {
                logger.warn("Failed to create placeholder WAV", e);
            }
        }
    }



    private synchronized void cleanupCsound() {
        if (currentCsound != null) {
            try {
                currentCsound.Stop();
                currentCsound.Reset();
                currentCsound.Cleanup();
            } catch (Exception e) {
                logger.error("Failed to cleanup Csound instance", e);
            } finally {
                currentCsound = null;
                isRunning = false;
                logger.info("Csound instance cleaned up");
            }
        }
    }

    public synchronized void stopGranulation() {
        if (currentCsound != null && isRunning) {
            logger.info("Signaling current crazification to stop...");
            isRunning = false;
            lastStopTime = System.currentTimeMillis();
        } else {
            logger.info("No crazification is currently running.");
        }
    }
}
