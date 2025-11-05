package org.example.backend.service;

import csnd6.Csound;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Service
public class GranulatorService {

    // ===========================
    // THREAD-SAFE STATUS FIELDS
    // ===========================
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private volatile Csound currentCsound = null;

    // ===========================
    // TIMING / CONSTANTS
    // ===========================
    private long lastStopTime = 0;
    private static final long COOLDOWN_MS = 1000;
    private static final long CLEANUP_WAIT_MS = 1000;
    private static final long CLEANUP_FINALIZE_DELAY = 50;

    // ===========================
    // LOGGER
    // ===========================
    private static final Logger logger = LoggerFactory.getLogger(GranulatorService.class);

    // ===========================
    // TEMPORARY FILE PATHS
    // ===========================
    private Path currentLiveAudioPath = null;
    private Path currentLiveScoPath = null;
    private Path currentHrtfTempDir = null;

    // ===========================
    // DEPENDENCIES
    // ===========================
    private final CsoundConfigurator csoundConfigurator;
    private final TemporaryFileManager fileManager;

    public GranulatorService(CsoundConfigurator csoundConfigurator, TemporaryFileManager fileManager) {
        this.csoundConfigurator = csoundConfigurator;
        this.fileManager = fileManager;
    }

    // ===========================
    // PUBLIC API
    // ===========================

    public boolean isRunning() {
        return isRunning.get();
    }

    /**
     * Starts granulation (live or offline)
     */
    public synchronized String performGranulationOnce(
            Path orcPath,
            Path scoPath,
            boolean outputLive,
            Path outputPath) throws IOException {
        // Step 1: Cooldown check
        if (checkCooldown()) {
            logger.warn("Cooldown active – quick repeat rejected.");
            return "Please wait a second before restarting.";
        }

        // Step 2: Check whether it is already running
        if (isRunning.get()) {
            logger.warn("Crazification is already running, new request rejected.");
            return "Crazification is already running!";
        }

        // Step 3: Force cleanup if a stuck instance exists
        forceCleanupIfNeeded();

        // Step 4: Capture temporary live file paths if it is a live run
        if (outputLive) captureLiveFilePaths(scoPath);

        Object csoundObj = createCsoundInstance();

        if (csoundObj instanceof Csound csound)
            return handleRealCsound(csound, orcPath, scoPath, outputLive, outputPath);

        if (csoundObj instanceof FakeCsound fake)
            return handleFakeCsound(fake, orcPath, scoPath, outputPath);

        throw new IllegalStateException("Unknown Csound instance type: " + csoundObj.getClass());
    }

    /**
     * Stops the currently running granulation
     */
    public synchronized void stopGranulation() {
        if (currentCsound != null && isRunning.get()) {
            logger.info("Signal that the current crazification should be stopped...");
            isRunning.set(false);
            // lastStopTime is set in cleanupCsound()
        } else {
            logger.info("No Crazification is currently running.");
        }
    }

    // ===========================
    // PROTECTED CORE METHODS
    // ===========================

    /**
     * Performs granulation using a real Csound instance
     */
    protected void performGranulation(Csound csound, Path orcPath, Path scoPath,
                                      boolean outputLive, Path outputPath) throws IOException {
        Path tempDir = null;
        Path effectiveOutput = outputPath;

        try {
            int sampleRate = outputLive ? 44100 : 96000;

            // --- Step 1: Prepare output and inject HRTF ---
            PreparationResult prep = prepareOutputAndHrtf(orcPath, scoPath, effectiveOutput, sampleRate, outputLive);
            effectiveOutput = prep.effectiveOutput();
            currentHrtfTempDir = prep.hrtfTempDir();
            tempDir = prep.tempDir();

            // --- Step 2: Configure and start Csound ---
            configureAndCompileCsound(csound, prep.orc(), prep.sco(), effectiveOutput, outputLive, sampleRate);

            // --- Step 3: Performance loop ---
            performLoop(csound, sampleRate);

        } catch (Exception e) {
            throw new IOException("Crazification failed", e);
        } finally {
            // --- Step 4: Clean up temporary files ---
            fileManager.cleanupTempDirectory(tempDir);
        }
    }

    /**
     * Performs granulation with FakeCsound (for testing purposes)
     */
    protected void performGranulationFake(FakeCsound fake, Path orcPath, Path scoPath,
                                          Path outputPath) throws IOException {
        Path tempDir = Files.createTempDirectory("crazifier-");
        Path effectiveOutput = outputPath != null
                ? outputPath.toAbsolutePath()
                : Files.createTempFile(tempDir, "crazified-", ".wav");

        try {
            if (effectiveOutput.getParent() != null) Files.createDirectories(effectiveOutput.getParent());
            fileManager.ensureValidWav(effectiveOutput);

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
            fileManager.cleanupTempDirectory(tempDir);
        }
    }

    // ===========================
    // PRIVATE AUXILIARY METHODS
    // ===========================

    protected Object createCsoundInstance() {
        return new Csound();
    }

    protected boolean checkCooldown() {
        long now = System.currentTimeMillis();
        return now - lastStopTime < COOLDOWN_MS;
    }

    protected void forceCleanupIfNeeded() {
        if (currentCsound != null) {
            logger.warn("Clean up any lingering Csound instances before starting the new instance.");
            cleanupCsound();
            try {
                Thread.sleep(CLEANUP_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Waiting time interrupted after cleanup.", e);
            }
        }
    }

    protected void captureLiveFilePaths(Path scoPath) {
        String audioPath = fileManager.getAudioPathFromSco(scoPath);
        if (audioPath != null) {
            this.currentLiveAudioPath = Path.of(audioPath);
        }
        this.currentLiveScoPath = scoPath;
    }

    protected String handleRealCsound(Csound csound, Path orcPath, Path scoPath,
                                      boolean outputLive, Path outputPath) throws IOException {
        currentCsound = csound;
        isRunning.set(true);

        if (outputLive) {
            logger.info("Starting live Csound thread...");
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
            try {
                performGranulation(csound, orcPath, scoPath, false, outputPath);
                return "Crazification finished!";
            } finally {
                cleanupCsound();
                fileManager.cleanupSaveInputFiles(scoPath);
            }
        }
    }

    protected String handleFakeCsound(FakeCsound fake, Path orcPath, Path scoPath, Path outputPath) throws IOException {
        isRunning.set(true);
        try {
            performGranulationFake(fake, orcPath, scoPath, outputPath);
            return "Fake Crazification completed!";
        } finally {
            isRunning.set(false);
        }
    }

    protected record PreparationResult(
            Path effectiveOutput,
            Path tempDir,
            Path hrtfTempDir,
            String orc,
            String sco
    ) {
    }

    protected PreparationResult prepareOutputAndHrtf(Path orcPath, Path scoPath, Path outputPath, int sampleRate, boolean outputLive) throws IOException {
        Path tempDir = null;
        Path effectiveOutput = outputPath;

        if (!outputLive && effectiveOutput == null) {
            tempDir = Files.createTempDirectory("crazifier-");
            effectiveOutput = Files.createTempFile(tempDir, "crazified-", ".wav");
        }

        if (effectiveOutput != null && effectiveOutput.getParent() != null) {
            Files.createDirectories(effectiveOutput.getParent());
            fileManager.ensureValidWav(effectiveOutput);
        }

        String orc = Files.readString(orcPath);
        String sco = Files.readString(scoPath);

        // Adjust sample rate + inject HRTF
        orc = csoundConfigurator.adjustSampleRate(orc, sampleRate);
        CsoundConfigurator.HrtfInjectionResult hrtfResult = csoundConfigurator.injectHrtfPaths(orc, sampleRate);

        return new PreparationResult(
                effectiveOutput,
                tempDir,
                hrtfResult.tempDir(),
                hrtfResult.orcContent(),
                sco
        );
    }

    protected void configureAndCompileCsound(Csound csound, String orc, String sco,
                                             Path output, boolean outputLive, int sampleRate) throws IOException {
        csoundConfigurator.configureCsound(csound, outputLive, output);

        csound.SetGlobalEnv("RAWADDF", "1");
        logger.info("Set Csound environment variable RAWADDF=1 (large GEN01 support).");

        logger.debug("--- ORC content ---\n{}", orc);
        logger.debug("--- SCO content ---\n{}", sco);

        if (csound.CompileOrc(orc) != 0)
            throw new IOException("Error compiling orchestra");
        if (csound.ReadScore(sco) != 0)
            throw new IOException("Error reading score");
        if (csound.Start() != 0)
            throw new IOException("Error starting Csound");
    }

    protected void performLoop(Csound csound, int sampleRate) {
        logger.info("Start Crazification performance loop at {} Hz...", sampleRate);
        long iterationCount = 0;

        while (csound.PerformKsmps() == 0 && isRunning.get()) {
            iterationCount++;

            if (iterationCount % 50000 == 0)
                logger.debug("Performance Iteration: {}", iterationCount);

            if (iterationCount % 100 == 0)
                Thread.yield();
        }

        if (!isRunning.get())
            logger.info("Crazification stopped by user after {} iterations", iterationCount);
        else
            logger.info("Crazification completed after {} iterations", iterationCount);
    }

    protected synchronized void cleanupCsound() {
        try {
            if (currentCsound != null) {
                logger.info("Cleaning up Csound instance...");
                currentCsound.Stop();
                currentCsound.Reset();
                currentCsound.Cleanup();
                TimeUnit.MILLISECONDS.sleep(CLEANUP_FINALIZE_DELAY);
                logger.info("Csound instance successfully cleaned up.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Cleanup interrupted while waiting for Csound finalization.", e);
        } catch (Exception e) {
            logger.error("Error cleaning up the Csound instance", e);
        } finally {
            currentCsound = null;
            isRunning.set(false);
            lastStopTime = System.currentTimeMillis();

            fileManager.cleanupLiveInputFiles(currentLiveAudioPath, currentLiveScoPath);
            fileManager.cleanupTempDirectory(currentHrtfTempDir);

            currentLiveAudioPath = null;
            currentLiveScoPath = null;
            currentHrtfTempDir = null;
        }
    }


}


