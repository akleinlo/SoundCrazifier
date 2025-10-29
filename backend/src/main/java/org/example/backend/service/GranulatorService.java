package org.example.backend.service;

import csnd6.Csound;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

@Getter
@Service
public class GranulatorService {

    private volatile boolean isRunning = false;
    private volatile Csound currentCsound = null;
    private static final Logger logger = LoggerFactory.getLogger(GranulatorService.class);
    private long lastStopTime = 0;
    private static final long COOLDOWN_MS = 1000;
    private static final long CLEANUP_WAIT_MS = 1000;
    private static final long CLEANUP_FINALIZE_DELAY = 50;

    // Temporary paths for files that need to be deleted in live mode (asynchronously)
    private Path currentLiveAudioPath = null;
    private Path currentLiveScoPath = null;

    // Path to the temporary HRTF directory (for Head-Related Transfer Function data)
    private Path currentHrtfTempDir = null;

    protected Object createCsoundInstance() {
        return new Csound();
    }

    public synchronized String performGranulationOnce(Path orcPath, Path scoPath, boolean outputLive, Path outputPath) throws IOException {
        // 1. cooldown check
        long now = System.currentTimeMillis();

        if (now - lastStopTime < COOLDOWN_MS) {
            logger.warn("Cooldown active – quick repeat rejected.");
            return "Please wait a second before restarting.";
        }

        // 2. Check whether it is already running
        if (isRunning) {
            logger.warn("Crazification is already running, new request rejected.");
            return "Crazification is already running!";
        }

        // 3. Force cleanup if a stuck instance exists
        if (currentCsound != null) {
            logger.warn("Clean up any lingering Csound instances before starting the new instance.");
            cleanupCsound();
            try {
                // Short wait time after cleanup
                Thread.sleep(CLEANUP_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Waiting time interrupted after cleanup.", e);
            }
        }

        // Capture temporary live file paths if it is a live run
        if (outputLive) {
            String audioPath = getAudioPathFromSco(scoPath);
            if (audioPath != null) {
                this.currentLiveAudioPath = Path.of(audioPath);
            }
            this.currentLiveScoPath = scoPath;
        }


        // 4. Create Csound instance BEFORE starting the thread
        Object csoundObj = createCsoundInstance();

        if (csoundObj instanceof Csound csound) {
            currentCsound = csound;
            isRunning = true;

            if (outputLive) {
                // Live playback in a separate thread (asynchronous)
                new Thread(() -> {
                    try {
                        performGranulation(csound, orcPath, scoPath, true, null);
                    } catch (IOException e) {
                        logger.error("Errors during live crazification", e);
                    } finally {
                        cleanupCsound();
                    }
                }, "Csound-LiveThread").start();

                return "Crazification started!";
            } else {
                // Synchronous file creation (storage mode)
                try {
                    performGranulation(csound, orcPath, scoPath, false, outputPath);
                    return "Crazification finished!";
                } finally {
                    cleanupCsound();
                    // Additional cleaning of input files after saving
                    cleanupSaveInputFiles(scoPath);
                }
            }
        } else if (csoundObj instanceof FakeCsound fake) {
            // for tests (optional)
            isRunning = true;
            try {
                performGranulationFake(fake, orcPath, scoPath, outputPath);
                return "Fake Crazification completed!";
            } finally {
                isRunning = false;
            }
        } else {
            throw new IllegalStateException("Unknown Csound instance type: " + csoundObj.getClass());
        }
    }

    /**
     * Performs granulation using a real Csound instance
     */
    protected void performGranulation(Csound csound, Path orcPath, Path scoPath,
                                      boolean outputLive, Path outputPath) throws IOException {
        Path tempDir = null;
        Path effectiveOutput = outputPath;

        try {
            // Determine sample rate based on output mode
            int sampleRate = outputLive ? 44100 : 96000;

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

            // Reset temporary HRTF directory
            currentHrtfTempDir = null;

            // Orchestration and score reading
            String orc = Files.readString(orcPath);
            String sco = Files.readString(scoPath);

            // Adjust sample rate and dynamically inject HRTF paths
            orc = adjustSampleRate(orc, sampleRate);
            orc = injectHrtfPaths(orc, sampleRate); // currentHrtfTempDir wird hier gesetzt

            // Configure Csound with the appropriate settings
            configureCsound(csound, outputLive, effectiveOutput, sampleRate);

            //  Set RAWADDF=1 to enable aggressive memory allocation for large GEN01 tables
            csound.SetGlobalEnv("RAWADDF", "1");
            logger.info("Csound environment variable RAWADDF=1 set for large file support.");

            logger.debug("--- ORC content ---\n{}", orc);
            logger.debug("--- SCO content ---\n{}", sco);

            // Compile and start
            int compileResult = csound.CompileOrc(orc);
            if (compileResult != 0) {
                throw new IOException("Error compiling the orchestra, error code: " + compileResult);
            }

            int scoreResult = csound.ReadScore(sco);
            if (scoreResult != 0) {
                throw new IOException("Error compiling the score, error code: " + scoreResult);
            }

            int startResult = csound.Start();
            if (startResult != 0) {
                throw new IOException("Error starting Csound, error code: " + startResult);
            }

            // performance loop
            logger.info("Start Crazification performance loop at {} Hz...", sampleRate);
            long iterationCount = 0;

            while (csound.PerformKsmps() == 0 && isRunning) {
                iterationCount++;

                // Log progress (for long files)
                if (iterationCount % 1000 == 0) {
                    logger.debug("Performance Iteration: {}", iterationCount);
                }

                // Give other threads a chance (prevents CPU utilization)
                if (iterationCount % 100 == 0) {
                    Thread.yield();
                }
            }

            if (!isRunning) {
                logger.info("Crazification stopped by user after {} iterations", iterationCount);
            } else {
                logger.info("Crazification completed after {} iterations", iterationCount);
            }

        } catch (Exception e) {
            logger.error("Error during granulation performance", e);
            throw new IOException("Crazification failed", e);
        } finally {
            // Clean up temporary directory if created for temporary output
            cleanupTempDirectory(tempDir);
        }
    }

    /**
     * Configures a Csound instance for live or file output.
     */
    private void configureCsound(Csound csound, boolean outputLive, Path effectiveOutput, int sampleRate) {
        if (outputLive) {
            csound.SetOption("-odac");
            csound.SetOption("-r44100");             // Live: 44.1 kHz für Performance
            csound.SetOption("--format=short");      // 16-bit für Live-Wiedergabe
            logger.info("Configure Csound for live output (DAC) at 44.1 kHz, 16-bit");
        } else {
            csound.SetOption("-o" + effectiveOutput.toAbsolutePath());
            csound.SetOption("--format=float");      // 32-bit float für Speichern
            csound.SetOption("-r96000");             // Speichern: 96 kHz für Qualität
            logger.info("Configure Csound for file output: {} at 96 kHz, 32-bit float", effectiveOutput);
        }

        // Buffer settings - increased for stability with long files
        csound.SetOption("-b1024");
        csound.SetOption("-B4096");
    }

    /**
     * Adjusts the sample rate in the orchestration content.
     */
    private String adjustSampleRate(String orcContent, int sampleRate) {
        orcContent = orcContent.replaceFirst("sr\\s*=\\s*\\d+", "sr = " + sampleRate);
        logger.info("Sample rate adjusted to {} Hz in ORC", sampleRate);
        return orcContent;
    }

    /**
     * Injects HRTF paths for the specified sample rate.
     */
    private String injectHrtfPaths(String orcContent, int sampleRate) throws IOException {
        // Dynamic HRTF file names based on the desired sample rate
        String baseName = "hrtf-" + sampleRate + "-";
        String leftFile = baseName + "left.dat";
        String rightFile = baseName + "right.dat";

        // Save the path for later cleanup
        Path tempDir = Files.createTempDirectory("csound-hrtf");
        this.currentHrtfTempDir = tempDir;

        Path leftTemp = tempDir.resolve(leftFile);
        Path rightTemp = tempDir.resolve(rightFile);

        try (InputStream leftIn = new ClassPathResource("csound/" + leftFile).getInputStream();
             InputStream rightIn = new ClassPathResource("csound/" + rightFile).getInputStream()) {
            Files.copy(leftIn, leftTemp);
            Files.copy(rightIn, rightTemp);
            logger.info("HRTF files for {} Hz copied to temporary directory", sampleRate);
        } catch (IOException e) {
            logger.error("Error loading HRTF files for {} Hz - check if the files exist under resources/csound/.", sampleRate);
            throw new IOException("HRTF files not found for sample rate " + sampleRate + " Hz", e);
        }

        // Replace global string definitions with absolute paths
        String replacement = String.format("gS_HRTF_left  = \"%s\"\ngS_HRTF_right = \"%s\"",
                leftTemp.toAbsolutePath(), rightTemp.toAbsolutePath());

        orcContent = orcContent.replaceFirst(
                "gS_HRTF_left\\s*=\\s*\"[^\"]+\"\\s*\\ngS_HRTF_right\\s*=\\s*\"[^\"]+\"",
                replacement
        );

        logger.info("HRTF paths injected for {} Hz: left={}, right={}", sampleRate, leftTemp, rightTemp);
        return orcContent;
    }

    /**
     * Performs granulation with FakeCsound (for testing purposes).
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

            logger.info("Fake Crazification finished.");
        } finally {
            cleanupTempDirectory(tempDir);
        }
    }

    /**
     * Deletes a temporary directory recursively.
     */
    private void cleanupTempDirectory(Path tempDir) {
        if (tempDir != null) {
            try {
                Files.walk(tempDir)
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
    private void cleanupSaveInputFiles(Path scoPath) {
        if (scoPath != null) {
            String audioPathStr = getAudioPathFromSco(scoPath);
            if (audioPathStr != null) {
                try {
                    logger.info("Clean up input audio file (storage mode): {}", audioPathStr);
                    Files.deleteIfExists(Path.of(audioPathStr));
                } catch (IOException e) {
                    logger.warn("Could not delete input audio file (storage mode): {}", e.getMessage());
                }
            }
            try {
                logger.info("Clean up SCO file (storage mode): {}", scoPath.getFileName());
                Files.deleteIfExists(scoPath);
            } catch (IOException e) {
                logger.warn("Could not delete temporary SCO file: {}", e.getMessage());
            }
        }
    }

    /**
     * Ensures that a valid WAV file exists in the output path (as a fallback).
     */
    private void ensureValidWav(Path path) throws IOException {
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

    /**
     * Performs cleanup of the Csound instance.
     * Must be synchronized to avoid race conditions.
     */
    private synchronized void cleanupCsound() {
        if (currentCsound != null) {
            Csound csoundToCleanup = currentCsound; // Referenz sichern
            try {
                logger.info("Clean up Csound instance...");
                csoundToCleanup.Stop();
                csoundToCleanup.Reset();
                csoundToCleanup.Cleanup();

                Thread.sleep(CLEANUP_FINALIZE_DELAY);

                logger.info("Csound instance successfully cleaned up.");
            } catch (Exception e) {
                logger.error("Error cleaning up the Csound instance", e);
            } finally {
                // Reset the states after the cleanup attempt is complete.
                currentCsound = null;
                isRunning = false;
                lastStopTime = System.currentTimeMillis();
            }
        } else {
            isRunning = false;
            lastStopTime = System.currentTimeMillis();
        }

        // Cleaning up temporary files (live input & HRTF)
        cleanupLiveInputFiles();
        cleanupTempDirectory(currentHrtfTempDir); // deletes HRTF directory
        currentHrtfTempDir = null; // Reset the path
    }

    // Method for cleaning up live input files
    private void cleanupLiveInputFiles() {
        if (currentLiveAudioPath != null && currentLiveScoPath != null) {
            logger.info("Clean up Csound Live input files: {} and {}", currentLiveAudioPath.getFileName(), currentLiveScoPath.getFileName());
            try {
                Files.deleteIfExists(currentLiveAudioPath);
                Files.deleteIfExists(currentLiveScoPath);
            } catch (IOException e) {
                logger.warn("Could not delete temporary live files: {}", e.getMessage());
            }
        }
        currentLiveAudioPath = null;
        currentLiveScoPath = null;
    }

    /**
     * Auxiliary method to extract the path to the audio file from the SCO file.
     */
    private String getAudioPathFromSco(Path scoPath) {
        try {
            String scoContent = Files.readString(scoPath);
            // Search for the line f 1 0.00 0.00 1.00 “/path/to/file.wav” ...
            int start = scoContent.indexOf('"');
            int end = scoContent.lastIndexOf('"');
            if (start != -1 && end != -1 && end > start) {
                return scoContent.substring(start + 1, end);
            }
        } catch (IOException e) {
            logger.error("Could not read SCO file to obtain audio path", e);
        }
        return null;
    }

    public synchronized void stopGranulation() {
        if (currentCsound != null && isRunning) {
            logger.info("Signal that the current crazification should be stopped...");
            isRunning = false;
            // lastStopTime is set in cleanupCsound()
        } else {
            logger.info("No Crazification is currently running.");
        }
    }
}
