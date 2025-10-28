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
import java.util.Comparator;
import java.util.Objects;

@Getter
@Service
public class GranulatorService {

    private volatile boolean isRunning = false;
    private volatile Csound currentCsound = null;
    private static final Logger logger = LoggerFactory.getLogger(GranulatorService.class);
    private long lastStopTime = 0;
    private static final long COOLDOWN_MS = 1000;
    private static final long CLEANUP_WAIT_MS = 1000;

    protected Object createCsoundInstance() {
        return new Csound();
    }

    public synchronized void performGranulationOnce(Path orcPath, Path scoPath, boolean outputLive, Path outputPath) throws IOException {
        // 1. Cooldown check
        long now = System.currentTimeMillis();
        if (now - lastStopTime < COOLDOWN_MS) {
            logger.warn("Cooldown active – rejecting rapid restart.");
            logger.info("Please wait a second before starting again.");
        }

        // 2. Check if already running
        if (isRunning) {
            logger.warn("Crazification already running, rejecting new request");
            logger.info("Crazification already running!");
        }

        // 3. Force cleanup if stuck instance exists
        if (currentCsound != null) {
            logger.warn("Cleaning up stuck Csound instance before starting new one");
            cleanupCsound();
            try {
                Thread.sleep(CLEANUP_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Cleanup wait interrupted", e);
            }
        }

        // 4. Create Csound instance BEFORE starting thread
        Object csoundObj = createCsoundInstance();

        if (csoundObj instanceof Csound csound) {
            currentCsound = csound;
            isRunning = true;

            if (outputLive) {
                // Live playback in separate thread
                new Thread(() -> {
                    try {
                        performGranulation(csound, orcPath, scoPath, true, null);
                    } catch (IOException e) {
                        logger.error("Error during live Crazification", e);
                    } finally {
                        cleanupCsound();
                    }
                }, "Csound-LiveThread").start();

                logger.info("Crazification started!");
            } else {
                // Synchronous file rendering
                try {
                    performGranulation(csound, orcPath, scoPath, false, outputPath);
                    logger.info("Crazification finished!");
                } finally {
                    cleanupCsound();
                }
            }
        } else if (csoundObj instanceof FakeCsound fake) {
            // For testing
            isRunning = true;
            try {
                performGranulationFake(fake, orcPath, scoPath, outputPath);
                logger.info("Fake crazification finished!");
            } finally {
                isRunning = false;
            }
        } else {
            throw new IllegalStateException("Unknown Csound instance type: " + csoundObj.getClass());
        }
    }

    /**
     * Performs granulation with a real Csound instance (passed explicitly)
     */
    protected void performGranulation(Csound csound, Path orcPath, Path scoPath,
                                      boolean outputLive, Path outputPath) throws IOException {
        Path tempDir = null;
        Path effectiveOutput = outputPath;

        try {
            // Prepare output path
            if (!outputLive && effectiveOutput == null) {
                tempDir = Files.createTempDirectory("crazifier-");
                effectiveOutput = Files.createTempFile(tempDir, "crazified-", ".wav");
            }

            if (effectiveOutput != null) {
                if (effectiveOutput.getParent() != null) {
                    Files.createDirectories(effectiveOutput.getParent());
                }
                ensureValidWav(effectiveOutput);
            }

            // Read orchestration and score
            String orc = Files.readString(orcPath);
            String sco = Files.readString(scoPath);

            // Configure Csound
            if (outputLive) {
                csound.SetOption("-odac");
                logger.info("Configuring Csound for live output (DAC)");
            } else {
                Objects.requireNonNull(effectiveOutput, "Effective output path must not be null");
                csound.SetOption("-o" + effectiveOutput.toAbsolutePath());
                csound.SetOption("--format=float");
                csound.SetOption("-r96000");
                logger.info("Configuring Csound for file output: {}", effectiveOutput);
            }

            // Buffer settings - increased for stability with long files
            csound.SetOption("-b1024");
            csound.SetOption("-B4096");

            logger.debug("--- ORC content ---\n{}", orc);
            logger.debug("--- SCO content ---\n{}", sco);

            // Compile and start
            int compileResult = csound.CompileOrc(orc);
            if (compileResult != 0) {
                throw new IOException("Failed to compile orchestra, error code: " + compileResult);
            }

            int scoreResult = csound.ReadScore(sco);
            if (scoreResult != 0) {
                throw new IOException("Failed to read score, error code: " + scoreResult);
            }

            int startResult = csound.Start();
            if (startResult != 0) {
                throw new IOException("Failed to start Csound, error code: " + startResult);
            }

            // Performance loop
            logger.info("Starting Crazification performance loop...");
            //int performResult;
            long iterationCount = 0;

            while (csound.PerformKsmps() == 0 && isRunning) {
                iterationCount++;
                if (iterationCount % 1000 == 0) logger.debug("Performance iteration: {}", iterationCount);
                if (iterationCount % 100 == 0) Thread.yield();
            }


            if (!isRunning) {
                logger.info("Crazification stopped by user after {} iterations", iterationCount);
            } else {
                logger.info("Crazification complete after {} iterations", iterationCount);
            }

        } catch (Exception e) {
            logger.error("Error during granulation performance", e);
            throw new IOException("Crazification failed", e);
        } finally {
            // Cleanup temp directory if created
            cleanupTempDirectory(tempDir);
        }
    }

    /**
     * Performs granulation with FakeCsound (for testing)
     */
    protected void performGranulationFake(FakeCsound fake, Path orcPath, Path scoPath,
                                          Path outputPath) throws IOException {
        Path tempDir = Files.createTempDirectory("crazifier-");
        Path effectiveOutput = outputPath != null
                ? outputPath.toAbsolutePath()
                : Files.createTempFile(tempDir, "crazified-", ".wav");

        try {
            if (effectiveOutput.getParent() != null) {
                Files.createDirectories(effectiveOutput.getParent());
            }
            ensureValidWav(effectiveOutput);

            String orc = Files.readString(orcPath);
            String sco = Files.readString(scoPath);

            fake.SetOption("-o" + effectiveOutput);
            logger.debug("--- ORC content ---\n{}", orc);
            logger.debug("--- SCO content ---\n{}", sco);

            fake.CompileOrc(orc);
            fake.ReadScore(sco);
            fake.Start();
            fake.PerformKsmps();

            logger.info("Fake crazification complete.");
        } finally {
            cleanupTempDirectory(tempDir);
        }
    }

    private void cleanupTempDirectory(Path tempDir) {
        if (tempDir != null) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                logger.warn("Failed to delete temp file: {}", path, e);
                            }
                        });
            } catch (IOException e) {
                logger.warn("Failed to cleanup temp directory: {}", tempDir, e);
            }
        }
    }


    private void ensureValidWav(Path path) throws IOException {
        if (!Files.exists(path) || Files.size(path) == 0) {
            // 1 second silence, 44100 Hz, 16-bit PCM, Mono
            byte[] silence = new byte[44100 * 2];
            AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
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
                logger.info("Cleaning up Csound instance...");
                currentCsound.Stop();
                currentCsound.Reset();
                currentCsound.Cleanup();
                logger.info("Csound instance cleaned up successfully");
            } catch (Exception e) {
                logger.error("Failed to cleanup Csound instance", e);
            } finally {
                currentCsound = null;
                isRunning = false;
                lastStopTime = System.currentTimeMillis();
            }
        } else {
            isRunning = false;
            lastStopTime = System.currentTimeMillis();
        }
    }

    public synchronized void stopGranulation() {
        if (currentCsound != null && isRunning) {
            logger.info("Signaling current crazification to stop...");
            isRunning = false;
            // lastStopTime will be set in cleanupCsound()
        } else {
            logger.info("No crazification is currently running.");
        }
    }
}