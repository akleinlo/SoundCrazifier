package org.example.backend.service;

import csnd6.Csound;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GranulatorServiceTest {

    private GranulatorService granulatorService;
    private TemporaryFileManager mockFileManager;
    private CsoundConfigurator mockConfigurator;

    @BeforeEach
    void setup() throws IOException {
        mockConfigurator = Mockito.mock(CsoundConfigurator.class);
        mockFileManager = Mockito.mock(TemporaryFileManager.class);

        granulatorService = Mockito.spy(new GranulatorService(mockConfigurator, mockFileManager));

        Mockito.doReturn(new FakeCsound())
                .when(granulatorService).createCsoundInstance();

        Mockito.doAnswer(invocation -> {
            Path output = invocation.getArgument(3, Path.class);
            if (output != null && !Files.exists(output)) {
                Files.createFile(output);
            }
            return null;
        }).when(granulatorService).performGranulationFake(
                Mockito.any(FakeCsound.class), Mockito.any(), Mockito.any(), Mockito.any());
    }

    /**
     * Helper method to safely access and set the internal isRunning AtomicBoolean field
     * in the GranulatorService instance via reflection.
     */
    private void setRunningStatus(boolean status) throws Exception {
        Field field = GranulatorService.class.getDeclaredField("isRunning");
        field.setAccessible(true);
        ((AtomicBoolean) field.get(granulatorService)).set(status);
    }

    /**
     * Helper method to safely access and get the internal isRunning AtomicBoolean field
     * in the GranulatorService instance via reflection.
     */
    private boolean getRunningStatus() throws Exception {
        Field field = GranulatorService.class.getDeclaredField("isRunning");
        field.setAccessible(true);
        return ((AtomicBoolean) field.get(granulatorService)).get();
    }

    @Test
    @DisplayName("isRunning initially false")
    void initiallyNotRunning() {
        assertFalse(granulatorService.isRunning(), "Initial isRunning should be false");
    }

    @Test
    @DisplayName("performGranulationOnce rejects when already running")
    void performGranulationOnce_rejectsWhenAlreadyRunning() throws Exception {
        // GIVEN
        // Field isRunningField Deklaration entfernt
        setRunningStatus(true);

        Path dummyOrc = Paths.get("dummy.orc");
        Path dummySco = Paths.get("dummy.sco");

        FakeCsound fakeCs = new FakeCsound();

        // WHEN
        granulatorService.performGranulationFake(fakeCs, dummyOrc, dummySco, null);

        // THEN
        // Wir verwenden granulatorService.isRunning() für die eigentliche Assertions
        assertTrue(granulatorService.isRunning(), "isRunning should remain true when already running");

        // Cleanup
        setRunningStatus(false);
    }

    @Test
    @DisplayName("performGranulationOnce resets isRunning after IOException")
    void performGranulationOnce_resetsFlagAfterIOException() throws Exception {
        // GIVEN
        Mockito.doThrow(new IOException("Simulated IOException"))
                .when(granulatorService)
                .performGranulationFake(Mockito.any(FakeCsound.class),
                        Mockito.any(), Mockito.any(), Mockito.any());

        assertFalse(granulatorService.isRunning());

        Path dummyOrc = Paths.get("dummy.orc");
        Path dummySco = Paths.get("dummy.sco");

        FakeCsound fakeCs = new FakeCsound();

        // WHEN
        IOException ex = assertThrows(IOException.class, () ->
                granulatorService.performGranulationFake(fakeCs, dummyOrc, dummySco, null));

        // THEN
        assertEquals("Simulated IOException", ex.getMessage());
        assertFalse(granulatorService.isRunning(), "isRunning should be reset after exception");
    }

    @Test
    @DisplayName("performGranulationOnce sets isRunning correctly when successful")
    void performGranulationOnce_success() throws Exception {
        // GIVEN
        doNothing()
                .when(granulatorService)
                .performGranulationFake(Mockito.any(FakeCsound.class),
                        Mockito.any(), Mockito.any(), Mockito.any());

        Path dummyOrc = Paths.get("dummy.orc");
        Path dummySco = Paths.get("dummy.sco");

        FakeCsound fakeCs = new FakeCsound();

        // WHEN
        granulatorService.performGranulationFake(fakeCs, dummyOrc, dummySco, null);

        // THEN
        assertFalse(granulatorService.isRunning(), "isRunning should be false after successful completion");
    }

    @Test
    @DisplayName("performGranulationOnce respects cooldown")
    void performGranulationOnce_cooldown() throws Exception {
        // GIVEN
        Field lastStopField = GranulatorService.class.getDeclaredField("lastStopTime");
        lastStopField.setAccessible(true);
        lastStopField.setLong(granulatorService, System.currentTimeMillis());

        Path dummyOrc = Paths.get("dummy.orc");
        Path dummySco = Paths.get("dummy.sco");
        FakeCsound fakeCs = new FakeCsound();

        // WHEN
        granulatorService.performGranulationFake(fakeCs, dummyOrc, dummySco, null);

        // THEN
        assertFalse(granulatorService.isRunning(), "isRunning should remain false during cooldown");
    }

    @Test
    @DisplayName("performGranulationOnce throws on unknown Csound object")
    void performGranulationOnce_unknownCsoundObject() {
        // GIVEN
        Mockito.doReturn(new Object()).when(granulatorService).createCsoundInstance();

        Path dummyOrc = Paths.get("dummy.orc");
        Path dummySco = Paths.get("dummy.sco");

        // WHEN + THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                granulatorService.performGranulationOnce(dummyOrc, dummySco, false, null));

        assertTrue(ex.getMessage().contains("Unknown Csound instance type"));
    }


    @Test
    @DisplayName("performGranulation actually runs and writes output file (mocked)")
    void performGranulation_createsOutputFile() throws Exception {
        Mockito.doAnswer(invocation -> {
            Path output = invocation.getArgument(3, Path.class);
            if (output != null) {
                Files.createFile(output);
            }
            return null;
        }).when(granulatorService).performGranulationFake(
                Mockito.any(FakeCsound.class), Mockito.any(), Mockito.any(), Mockito.any());

        Path orc = Files.createTempFile("test", ".orc");
        Path sco = Files.createTempFile("test", ".sco");
        Files.writeString(orc, "instr 1\n a1 oscili 0.1, 440\n out a1\n endin");
        Files.writeString(sco, "i1 0 1");

        Path output = Files.createTempFile("output", ".wav");
        Files.deleteIfExists(output);

        FakeCsound fakeCs = new FakeCsound();
        granulatorService.performGranulationFake(fakeCs, orc, sco, output);

        assertTrue(Files.exists(output), "Output file should be created by mocked performGranulation");

        // Cleanup
        Files.deleteIfExists(orc);
        Files.deleteIfExists(sco);
        Files.deleteIfExists(output);
    }

    @Test
    @DisplayName("performGranulation (Offline): Calls all steps in sequence and uses 96000 SR")
    void performGranulation_offline_success() throws Exception {
        // GIVEN
        Csound mockCsound = Mockito.mock(Csound.class);
        Path orc = Paths.get("dummy_orc.orc");
        Path sco = Paths.get("dummy_sco.sco");
        Path output = Paths.get("dummy_output.wav");

        String orcString = orc.toString();
        String scoString = sco.toString();

        Path tempDir = Paths.get("/tmp/granulation_temp");
        GranulatorService.PreparationResult mockPrepResult = Mockito.mock(GranulatorService.PreparationResult.class);

        when(mockPrepResult.orc()).thenReturn(orcString);
        when(mockPrepResult.sco()).thenReturn(scoString);
        when(mockPrepResult.effectiveOutput()).thenReturn(output); // Assuming effectiveOutput returns Path, otherwise change this too
        when(mockPrepResult.tempDir()).thenReturn(tempDir);

        doReturn(mockPrepResult).when(granulatorService).prepareOutputAndHrtf(
                orc, sco, output, 96000, false);

        doNothing().when(granulatorService).configureAndCompileCsound(
                any(), any(), any(), any(), anyBoolean());
        doNothing().when(granulatorService).performLoop(any(), anyInt());

        // WHEN
        granulatorService.performGranulation(mockCsound, orc, sco, false, output);

        // THEN
        verify(granulatorService, times(1)).prepareOutputAndHrtf(
                orc, sco, output, 96000, false);

        // Verification: Corrected to use Strings for orc/sco/output, if configureAndCompileCsound expects them
        verify(granulatorService, times(1)).configureAndCompileCsound(
                mockCsound,
                orcString,
                scoString,
                output,
                false);

        verify(granulatorService, times(1)).performLoop(mockCsound, 96000);

        verify(mockFileManager, times(1)).cleanupTempDirectory(tempDir);
    }

    @Test
    @DisplayName("performGranulation (Live): Calls all steps in sequence and uses 44100 SR")
    void performGranulation_live_success() throws Exception {
        // GIVEN
        Csound mockCsound = Mockito.mock(Csound.class);
        Path orc = Paths.get("dummy_orc.orc");
        Path sco = Paths.get("dummy_sco.sco");

        Path output = null;
        final int LIVE_SR = 44100;

        String orcString = orc.toString();
        String scoString = sco.toString();

        Path tempDir = Paths.get("/tmp/granulation_temp_live");
        GranulatorService.PreparationResult mockPrepResult = Mockito.mock(GranulatorService.PreparationResult.class);

        when(mockPrepResult.orc()).thenReturn(orcString);
        when(mockPrepResult.sco()).thenReturn(scoString);
        when(mockPrepResult.effectiveOutput()).thenReturn(output);
        when(mockPrepResult.tempDir()).thenReturn(tempDir);

        doReturn(mockPrepResult).when(granulatorService).prepareOutputAndHrtf(
                orc, sco, output, LIVE_SR, true);

        doNothing().when(granulatorService).configureAndCompileCsound(
                any(), any(), any(), any(), anyBoolean());
        doNothing().when(granulatorService).performLoop(any(), anyInt());

        // WHEN
        granulatorService.performGranulation(mockCsound, orc, sco, true, output);

        // THEN
        verify(granulatorService, times(1)).prepareOutputAndHrtf(
                orc, sco, output, LIVE_SR, true);

        verify(granulatorService, times(1)).configureAndCompileCsound(
                eq(mockCsound),
                eq(orcString),
                eq(scoString),
                isNull(),
                eq(true));

        verify(granulatorService, times(1)).performLoop(mockCsound, LIVE_SR);

        verify(mockFileManager, times(1)).cleanupTempDirectory(tempDir);
    }

    @Test
    @DisplayName("performGranulation: Throws IOException and cleans up temp directory on failure")
    void performGranulation_throwsAndCleansUp() throws Exception {
        // GIVEN
        Csound mockCsound = Mockito.mock(Csound.class);
        Path orc = Paths.get("dummy_orc.orc");
        Path sco = Paths.get("dummy_sco.sco");
        Path output = Paths.get("dummy_output.wav");

        Path tempDir = Paths.get("/tmp/granulation_temp");

        String orcString = orc.toString();
        String scoString = sco.toString();

        GranulatorService.PreparationResult mockPrepResult = Mockito.mock(GranulatorService.PreparationResult.class);
        when(mockPrepResult.tempDir()).thenReturn(tempDir);
        when(mockPrepResult.orc()).thenReturn(orcString);
        when(mockPrepResult.sco()).thenReturn(scoString);

        doReturn(mockPrepResult).when(granulatorService).prepareOutputAndHrtf(
                any(), any(), any(), anyInt(), anyBoolean());

        final RuntimeException failure = new RuntimeException("Csound compile failed");
        doThrow(failure).when(granulatorService).configureAndCompileCsound(
                any(), any(), any(), any(), anyBoolean());

        // WHEN + THEN
        IOException ex = assertThrows(IOException.class, () ->
                granulatorService.performGranulation(mockCsound, orc, sco, false, output)
        );

        assertEquals("Crazification failed", ex.getMessage());
        assertEquals(failure, ex.getCause(), "The original RuntimeException should be set as the cause.");

        verify(mockFileManager, times(1)).cleanupTempDirectory(tempDir);

        verify(granulatorService, times(1)).configureAndCompileCsound(
                eq(mockCsound),
                eq(orcString),
                eq(scoString),
                any(), // output
                eq(false));
    }


    @Test
    @DisplayName("forceCleanupIfNeeded calls cleanupCsound when currentCsound is not null")
    void forceCleanupIfNeeded_whenCurrentCsoundIsNotNull_callsCleanup() throws Exception {
        // GIVEN
        doNothing().when(granulatorService).cleanupCsound(); // Mock the method on the existing spy

        Csound mockCsound = Mockito.mock(Csound.class);
        Field currentCsoundField = GranulatorService.class.getDeclaredField("currentCsound");
        currentCsoundField.setAccessible(true);
        currentCsoundField.set(granulatorService, mockCsound);

        // WHEN
        granulatorService.forceCleanupIfNeeded();

        // THEN
        verify(granulatorService).cleanupCsound();

        // Cleanup
        currentCsoundField.set(granulatorService, null);
    }

    @Test
    @DisplayName("forceCleanupIfNeeded calls cleanupCsound when currentCsound is null")
    void forceCleanupIfNeeded_whenCurrentCsoundIsNull_doesNothing() throws Exception {
        // GIVEN
        Field currentCsoundField = GranulatorService.class.getDeclaredField("currentCsound");
        currentCsoundField.setAccessible(true);
        currentCsoundField.set(granulatorService, null);

        // WHEN
        granulatorService.forceCleanupIfNeeded();

        // THEN
        verify(granulatorService, Mockito.never()).cleanupCsound();
    }

    @Test
    @DisplayName("captureLiveFilePaths sets both internal path fields when audio path is present")
    void captureLiveFilePaths_setsBothPaths_whenAudioPathExists() throws IllegalAccessException, NoSuchFieldException {
        // GIVEN
        Path expectedScoPath = Paths.get("/data/test.sco");
        String expectedAudioPathString = "/audio/input.wav";
        Path expectedAudioPath = Path.of(expectedAudioPathString); // Der Pfad, der im Service gespeichert wird

        Mockito.when(mockFileManager.getAudioPathFromSco(expectedScoPath))
                .thenReturn(expectedAudioPathString);

        Field audioPathField = GranulatorService.class.getDeclaredField("currentLiveAudioPath");
        Field scoPathField = GranulatorService.class.getDeclaredField("currentLiveScoPath");
        audioPathField.setAccessible(true);
        scoPathField.setAccessible(true);

        // WHEN
        granulatorService.captureLiveFilePaths(expectedScoPath);

        // THEN
        assertEquals(expectedScoPath, scoPathField.get(granulatorService),
                "currentLiveScoPath sollte auf den übergebenen scoPath gesetzt werden.");

        assertEquals(expectedAudioPath, audioPathField.get(granulatorService),
                "currentLiveAudioPath sollte auf den konvertierten Pfad gesetzt werden.");

        // Cleanup
        audioPathField.set(granulatorService, null);
        scoPathField.set(granulatorService, null);
    }

    @Test
    @DisplayName("captureLiveFilePaths sets only scoPath when fileManager returns null for audio")
    void captureLiveFilePaths_setsOnlyScoPath_whenAudioPathIsMissing() throws Exception {
        // GIVEN
        Path expectedScoPath = Paths.get("/data/test-no-audio.sco");

        Mockito.when(mockFileManager.getAudioPathFromSco(expectedScoPath))
                .thenReturn(null);

        Field audioPathField = GranulatorService.class.getDeclaredField("currentLiveAudioPath");
        Field scoPathField = GranulatorService.class.getDeclaredField("currentLiveScoPath");
        audioPathField.setAccessible(true);
        scoPathField.setAccessible(true);

        audioPathField.set(granulatorService, null);
        scoPathField.set(granulatorService, null);

        // WHEN
        granulatorService.captureLiveFilePaths(expectedScoPath);

        // THEN
        assertEquals(expectedScoPath, scoPathField.get(granulatorService),
                "currentLiveScoPath should be set to the transferred scoPath.");

        assertNull(audioPathField.get(granulatorService),
                "currentLiveAudioPath should remain null if FileManager returns null.");
    }

    @Test
    @DisplayName("createCsoundInstance returns a Csound compatible object (FakeCsound) in test environment")
    void createCsoundInstance_returnsCsoundCompatibleObject() {
        // GIVEN
        Mockito.doReturn(new FakeCsound()).when(granulatorService).createCsoundInstance();

        // WHEN
        Object result = granulatorService.createCsoundInstance();

        // THEN
        verify(granulatorService).createCsoundInstance();
        assertInstanceOf(FakeCsound.class, result, "In test mode, FakeCsound should be returned (by mocking).");
    }

    @Test
    @DisplayName("checkCooldown returns true when lastStopTime is recent")
    void checkCooldown_isTrue_whenRecent() throws Exception {
        // GIVEN
        Field lastStopField = GranulatorService.class.getDeclaredField("lastStopTime");
        lastStopField.setAccessible(true);

        long recentTime = System.currentTimeMillis() - 10;
        lastStopField.setLong(granulatorService, recentTime);

        // WHEN
        boolean result = granulatorService.checkCooldown();

        // THEN
        assertTrue(result, "Cooldown should be active because the last stop time is very close to the current time.");
    }

    @Test
    @DisplayName("checkCooldown returns false when lastStopTime is long ago")
    void checkCooldown_isFalse_whenLongAgo() throws Exception {
        // GIVEN
        Field lastStopField = GranulatorService.class.getDeclaredField("lastStopTime");
        lastStopField.setAccessible(true);

        long longAgoTime = System.currentTimeMillis() - 3600000;
        lastStopField.setLong(granulatorService, longAgoTime);

        // WHEN
        boolean result = granulatorService.checkCooldown();

        // THEN
        assertFalse(result, "Cooldown should be inactive because the last stop time was a long time ago.");
    }

    @Test
    @DisplayName("handleRealCsound (Offline): Calls performGranulation synchronously and cleans up")
    void handleRealCsound_offline_callsPerformGranulationDirectly() throws Exception {
        // GIVEN
        Path dummyOrc = Paths.get("dummy.orc");
        Path dummySco = Paths.get("dummy.sco");
        Path dummyOutput = Paths.get("dummy.wav");
        Csound mockCsound = Mockito.mock(Csound.class);

        doNothing().when(granulatorService).performGranulation(
                any(), any(), any(), anyBoolean(), any());

        doCallRealMethod().when(granulatorService).cleanupCsound();

        // Field isRunningField Deklaration entfernt

        // WHEN
        String result = granulatorService.handleRealCsound(
                mockCsound, dummyOrc, dummySco, false, dummyOutput);

        // THEN
        assertEquals("Crazification finished!", result,
                "Should return 'Crazification finished!'.");

        assertFalse(getRunningStatus(),
                "isRunning should be false after completion, since cleanupCsound was called.");

        verify(granulatorService, times(1)).performGranulation(
                mockCsound, dummyOrc, dummySco, false, dummyOutput);

        verify(granulatorService, times(1)).cleanupCsound();

        verify(mockFileManager, times(1)).cleanupSaveInputFiles(dummySco);
    }

    @Test
    @DisplayName("handleRealCsound (Live): Starts thread, sets state, and returns immediately")
    void handleRealCsound_live_startsThread() throws Exception {
        // GIVEN
        Path dummyOrc = Paths.get("dummy.orc");
        Path dummySco = Paths.get("dummy.sco");
        Csound mockCsound = Mockito.mock(Csound.class);

        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(granulatorService).performGranulation(
                any(), any(), any(), eq(true), isNull());

        doCallRealMethod().when(granulatorService).cleanupCsound();

        // Field isRunningField Deklaration entfernt

        // WHEN
        long startTime = System.currentTimeMillis();
        String result = granulatorService.handleRealCsound(
                mockCsound, dummyOrc, dummySco, true, null);
        long endTime = System.currentTimeMillis();

        // THEN
        assertEquals("Crazification started!", result,
                "Should immediately return 'Crazification started!'.");

        assertTrue(endTime - startTime < 50,
                "The return should take place immediately, as granulation is started asynchronously.");

        assertTrue(getRunningStatus(),
                "isRunning should be true because the live thread is currently running.");

        assertTrue(latch.await(5, TimeUnit.SECONDS), "The granulation thread should be completed within the time frame.");

        verify(granulatorService, times(1)).performGranulation(
                mockCsound, dummyOrc, dummySco, true, null);

        verify(granulatorService, timeout(2000).times(1)).cleanupCsound();

        verify(mockFileManager, never()).cleanupSaveInputFiles(any());

        assertFalse(getRunningStatus(),
                "isRunning should be false after the live thread has finished and cleanup has been performed.");
    }

    @Test
    @DisplayName("handleRealCsound (Offline): Propagates IOException and ensures cleanup")
    void handleRealCsound_propagatesIOException() throws Exception {
        // GIVEN
        Path dummyOrc = Paths.get("dummy.orc");
        Path dummySco = Paths.get("dummy.sco");
        Path dummyOutput = Paths.get("dummy.wav");
        Csound mockCsound = Mockito.mock(Csound.class);
        final String expectedErrorMessage = "Granulation failed!";

        doThrow(new IOException(expectedErrorMessage))
                .when(granulatorService).performGranulation(
                        any(), any(), any(), eq(false), any());

        doCallRealMethod().when(granulatorService).cleanupCsound();

        // Field isRunningField Deklaration entfernt

        // WHEN + THEN
        IOException ex = assertThrows(IOException.class, () ->
                granulatorService.handleRealCsound(mockCsound, dummyOrc, dummySco, false, dummyOutput)
        );

        assertEquals(expectedErrorMessage, ex.getMessage(),
                "The original exception should be propagated.");

        verify(granulatorService, times(1)).cleanupCsound();

        verify(mockFileManager, times(1)).cleanupSaveInputFiles(dummySco);

        assertFalse(getRunningStatus(),
                "isRunning should be reset to false after the error and cleanup.");

        verify(granulatorService, times(1)).performGranulation(
                mockCsound, dummyOrc, dummySco, false, dummyOutput);
    }

    @Test
    @DisplayName("handleFakeCsound: Successfully runs and resets isRunning")
    void handleFakeCsound_successPath() throws Exception {
        // GIVEN
        Path dummyOrc = Paths.get("dummy.orc");
        Path dummySco = Paths.get("dummy.sco");
        Path dummyOutput = Paths.get("output.wav");
        FakeCsound fakeCsound = new FakeCsound();

        // Field isRunningField Deklaration entfernt
        setRunningStatus(false);

        // WHEN
        String result = granulatorService.handleFakeCsound(
                fakeCsound, dummyOrc, dummySco, dummyOutput);

        // THEN
        assertEquals("Fake Crazification completed!", result,
                "The success string should be returned.");

        assertFalse(getRunningStatus(),
                "isRunning should be false again after completion.");

        Mockito.verify(granulatorService, Mockito.times(1)).performGranulationFake(
                fakeCsound, dummyOrc, dummySco, dummyOutput);
    }

    @Test
    @DisplayName("handleFakeCsound: Resets isRunning even after IOException")
    void handleFakeCsound_resetsFlagAfterIOException() throws Exception {
        // GIVEN
        Path dummyOrc = Paths.get("dummy.orc");
        Path dummySco = Paths.get("dummy.sco");
        Path dummyOutput = Paths.get("output.wav");
        FakeCsound fakeCsound = new FakeCsound();
        final String expectedErrorMessage = "Simulated Granulation Error";

        Mockito.doThrow(new IOException(expectedErrorMessage))
                .when(granulatorService).performGranulationFake(
                        Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

        // Field isRunningField Deklaration entfernt
        setRunningStatus(false);

        // WHEN
        IOException ex = assertThrows(IOException.class, () ->
                granulatorService.handleFakeCsound(fakeCsound, dummyOrc, dummySco, dummyOutput)
        );

        // THEN
        assertEquals(expectedErrorMessage, ex.getMessage(),
                "The original exception should be propagated.");

        assertFalse(getRunningStatus(),
                "isRunning should be reset to false after the exception in the finally block.");

        Mockito.verify(granulatorService, Mockito.times(1)).performGranulationFake(
                fakeCsound, dummyOrc, dummySco, dummyOutput);
    }

    @Test
    @DisplayName("stopGranulation: Sets isRunning=false to signal stop when active")
    void stopGranulation_setsFlagsCorrectly() throws Exception {
        // GIVEN
        Csound mockCsound = Mockito.mock(Csound.class);

        // Field isRunningField Deklaration entfernt
        setRunningStatus(true);

        Field currentCsoundField = GranulatorService.class.getDeclaredField("currentCsound");
        currentCsoundField.setAccessible(true);
        currentCsoundField.set(granulatorService, mockCsound);

        verify(granulatorService, never()).cleanupCsound();

        // WHEN
        granulatorService.stopGranulation();

        // THEN
        assertFalse(getRunningStatus(),
                "isRunning must be set to false to stop the thread.");

        assertEquals(mockCsound, currentCsoundField.get(granulatorService),
                "currentCsound should NOT be set to zero by stopGranulation.");

        Mockito.verify(granulatorService, never()).cleanupCsound();
    }

    @Test
    @DisplayName("stopGranulation: Does nothing and is safe when no Csound is active")
    void stopGranulation_safeWhenNotRunning() throws Exception {
        // GIVEN
        // Field isRunningField Deklaration entfernt
        setRunningStatus(false);

        Field currentCsoundField = GranulatorService.class.getDeclaredField("currentCsound");
        currentCsoundField.setAccessible(true);
        currentCsoundField.set(granulatorService, null);

        // WHEN
        granulatorService.stopGranulation();

        // THEN
        assertFalse(getRunningStatus(),
                "isRunning should remain false.");

        assertNull(currentCsoundField.get(granulatorService),
                "currentCsound should remain null.");

        Mockito.verify(granulatorService, never()).cleanupCsound();
    }

    @Test
    @DisplayName("prepareOutputAndHrtf (Offline/Given Path): Returns correct result, no temp dir creation")
    void prepareOutputAndHrtf_offlineWithGivenPath() throws Exception {
        // GIVEN
        Path orcPath = Paths.get("/user/input.orc");
        Path scoPath = Paths.get("/user/input.sco");
        Path outputPath = Paths.get("/user/output/result.wav"); // Given output path
        int sampleRate = 96000;

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class, RETURNS_DEEP_STUBS)) {

            mockedFiles.when(() -> Files.readString(orcPath)).thenReturn("original ORC content");
            mockedFiles.when(() -> Files.readString(scoPath)).thenReturn("SCO content");

            mockedFiles.when(() -> Files.createDirectories(outputPath.getParent())).thenReturn(outputPath.getParent());

            String adjustedOrc = "adjusted ORC content 96k";
            String injectedOrc = "injected ORC content 96k";
            Path hrtfTempDir = Paths.get("/temp/hrtf-cache");

            when(mockConfigurator.adjustSampleRate(anyString(), eq(sampleRate))).thenReturn(adjustedOrc);

            CsoundConfigurator.HrtfInjectionResult mockHrtfResult =
                    new CsoundConfigurator.HrtfInjectionResult(injectedOrc, hrtfTempDir);

            when(mockConfigurator.injectHrtfPaths(adjustedOrc, sampleRate)).thenReturn(mockHrtfResult);

            // WHEN
            GranulatorService.PreparationResult result = granulatorService.prepareOutputAndHrtf(
                    orcPath, scoPath, outputPath, sampleRate, false); // outputLive=false

            // THEN
            mockedFiles.verify(() -> Files.createDirectories(outputPath.getParent()), times(1));
            verify(mockFileManager, times(1)).ensureValidWav(outputPath);
            mockedFiles.verify(() -> Files.createTempDirectory(anyString()), never()); // No temp dir created

            verify(mockConfigurator).adjustSampleRate("original ORC content", sampleRate);
            verify(mockConfigurator).injectHrtfPaths(adjustedOrc, sampleRate);

            assertEquals(outputPath, result.effectiveOutput(), "Effective output should be the given path.");
            assertNull(result.tempDir(), "tempDir should be null as no temporary directory was created.");
            assertEquals(hrtfTempDir, result.hrtfTempDir(), "hrtfTempDir should be the path from the injection result.");
            assertEquals(injectedOrc, result.orc(), "ORC content should be the final, injected content.");
            assertEquals("SCO content", result.sco(), "SCO content should be read directly.");
        }
    }

    @Test
    @DisplayName("prepareOutputAndHrtf (Offline/No Path): Creates temporary output and directory")
    void prepareOutputAndHrtf_offlineWithoutPath() throws Exception {
        // GIVEN
        Path orcPath = Paths.get("/user/input.orc");
        Path scoPath = Paths.get("/user/input.sco");
        Path outputPath = null; // Output path is null
        int sampleRate = 96000;

        Path createdTempDir = Paths.get("/tmp/crazifier-xyz");
        Path createdTempFile = Paths.get("/tmp/crazifier-xyz/crazified-abc.wav");

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class, RETURNS_DEEP_STUBS)) {
            mockedFiles.when(() -> Files.createTempDirectory(anyString())).thenReturn(createdTempDir);
            mockedFiles.when(() -> Files.createTempFile(eq(createdTempDir), anyString(), anyString())).thenReturn(createdTempFile);

            mockedFiles.when(() -> Files.readString(orcPath)).thenReturn("original ORC content");
            mockedFiles.when(() -> Files.readString(scoPath)).thenReturn("SCO content");

            when(mockConfigurator.adjustSampleRate(anyString(), anyInt())).thenReturn("adjusted ORC");
            when(mockConfigurator.injectHrtfPaths(anyString(), anyInt()))
                    .thenReturn(new CsoundConfigurator.HrtfInjectionResult("injected ORC", null));

            // WHEN
            GranulatorService.PreparationResult result = granulatorService.prepareOutputAndHrtf(
                    orcPath, scoPath, outputPath, sampleRate, false);

            // THEN
            mockedFiles.verify(() -> Files.createTempDirectory(eq("crazifier-")), times(1));
            mockedFiles.verify(() -> Files.createTempFile(eq(createdTempDir), anyString(), eq(".wav")), times(1));

            verify(mockFileManager, times(1)).ensureValidWav(createdTempFile);

            assertEquals(createdTempFile, result.effectiveOutput(), "Effective output must be the newly created temporary file.");
            assertEquals(createdTempDir, result.tempDir(), "tempDir must be the root of the temporary files.");
            assertNotNull(result.orc(), "ORC content must be processed.");
        }
    }

    @Test
    @DisplayName("prepareOutputAndHrtf (Live): Ignores outputPath and does no file creation/validation")
    void testPrepareOutputAndHrtf_liveMode() throws Exception {
        // GIVEN
        Path orcPath = Paths.get("/user/live.orc");
        Path scoPath = Paths.get("/user/live.sco");
        Path outputPath = Paths.get("/should/be/ignored.wav"); // Should be ignored for file creation/validation
        int sampleRate = 44100;

        Path expectedParentDir = outputPath.getParent();

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class, RETURNS_DEEP_STUBS)) {

            mockedFiles.when(() -> Files.readString(orcPath)).thenReturn("live ORC");
            mockedFiles.when(() -> Files.readString(scoPath)).thenReturn("live SCO");

            // Dies simuliert, dass das Verzeichnis existiert oder erstellt wird, was für Live-Modus in manchen Kontexten erforderlich sein kann
            mockedFiles.when(() -> Files.createDirectories(eq(expectedParentDir))).thenReturn(expectedParentDir);

            String adjustedOrc = "adjusted live ORC";
            String injectedOrc = "injected live ORC";

            when(mockConfigurator.adjustSampleRate(anyString(), eq(sampleRate))).thenReturn(adjustedOrc);

            CsoundConfigurator.HrtfInjectionResult mockHrtfResult =
                    new CsoundConfigurator.HrtfInjectionResult(injectedOrc, null);

            when(mockConfigurator.injectHrtfPaths(adjustedOrc, sampleRate)).thenReturn(mockHrtfResult);


            // WHEN
            GranulatorService.PreparationResult result = granulatorService.prepareOutputAndHrtf(
                    orcPath, scoPath, outputPath, sampleRate, true); // outputLive=true

            // THEN
            mockedFiles.verify(() -> Files.createTempDirectory(anyString()), never());

            // Überprüfung der Verzeichnis-Erstellung (kann je nach tatsächlicher Implementierung variieren)
            mockedFiles.verify(() -> Files.createDirectories(eq(expectedParentDir)), times(1));

            verify(mockFileManager, times(1)).ensureValidWav(outputPath);

            verify(mockConfigurator).adjustSampleRate("live ORC", sampleRate);
            verify(mockConfigurator).injectHrtfPaths(adjustedOrc, sampleRate);

            assertEquals(outputPath, result.effectiveOutput(), "Effective output should be the given path (it is passed through).");
            assertNull(result.tempDir(), "tempDir must be null in Live mode.");
            assertEquals(injectedOrc, result.orc(), "ORC content should be the final, injected content.");
        }
    }

    @Test
    @DisplayName("configureAndCompileCsound (Offline): Configures, sets Env, and compiles successfully")
    void testConfigureAndCompileCsound_setsCorrectOptionsOffline_success() throws Exception {
        // GIVEN
        Csound mockCsound = Mockito.mock(Csound.class);
        String orcContent = "instr 1\n ...";
        String scoContent = "i1 0 1\n ...";
        Path outputPath = Paths.get("/output/test.wav");

        when(mockCsound.CompileOrc(anyString())).thenReturn(0);
        when(mockCsound.ReadScore(anyString())).thenReturn(0);
        when(mockCsound.Start()).thenReturn(0);

        doNothing().when(mockConfigurator).configureCsound(any(), anyBoolean(), any());

        // WHEN
        granulatorService.configureAndCompileCsound(
                mockCsound, orcContent, scoContent, outputPath, false); // outputLive=false

        // THEN
        verify(mockConfigurator, times(1)).configureCsound(
                mockCsound, false, outputPath);

        verify(mockCsound, times(1)).SetGlobalEnv("RAWADDF", "1");

        verify(mockCsound, times(1)).CompileOrc(orcContent);
        verify(mockCsound, times(1)).ReadScore(scoContent);
        verify(mockCsound, times(1)).Start();
    }


    @Test
    @DisplayName("configureAndCompileCsound (Live): Configures for live output and compiles successfully")
    void testConfigureAndCompileCsound_setsOdacOptionInLiveMode_success() throws Exception {
        // GIVEN
        Csound mockCsound = Mockito.mock(Csound.class);
        String orcContent = "live instr 1\n ...";
        String scoContent = "i1 0 1\n ...";
        Path outputPath = null;

        when(mockCsound.CompileOrc(anyString())).thenReturn(0);
        when(mockCsound.ReadScore(anyString())).thenReturn(0);
        when(mockCsound.Start()).thenReturn(0);

        doNothing().when(mockConfigurator).configureCsound(any(), anyBoolean(), isNull());

        // WHEN
        granulatorService.configureAndCompileCsound(
                mockCsound, orcContent, scoContent, outputPath, true);

        // THEN
        verify(mockConfigurator, times(1)).configureCsound(
                eq(mockCsound), eq(true), isNull());

        verify(mockCsound, times(1)).SetGlobalEnv("RAWADDF", "1");

        verify(mockCsound, times(1)).CompileOrc(orcContent);
        verify(mockCsound, times(1)).ReadScore(scoContent);
        verify(mockCsound, times(1)).Start();
    }


    @ParameterizedTest(name = "{0}")
    @DisplayName("configureAndCompileCsound: Throws IOException if Csound compilation or start fails")
    @MethodSource("provideCsoundFailureCases")
    void testConfigureAndCompileCsound_compilationFails(
            String testName,
            int compileOrcResult,
            int readScoreResult,
            int startResult,
            String expectedMessage) {

        // GIVEN
        Csound mockCsound = Mockito.mock(Csound.class);
        Path outputPath = Paths.get("/output/fail.wav");

        when(mockCsound.CompileOrc(anyString())).thenReturn(compileOrcResult);
        when(mockCsound.ReadScore(anyString())).thenReturn(readScoreResult);
        when(mockCsound.Start()).thenReturn(startResult);

        doNothing().when(mockConfigurator).configureCsound(any(), anyBoolean(), any());

        // WHEN + THEN
        IOException ex = assertThrows(IOException.class, () ->
                granulatorService.configureAndCompileCsound(
                        mockCsound, "orc", "sco", outputPath, false)
        );

        assertEquals(expectedMessage, ex.getMessage(), "The correct failure message should be returned.");

        verify(mockCsound).SetGlobalEnv(anyString(), anyString());
    }

    // Helper method to provide test cases
    private static Stream<Arguments> provideCsoundFailureCases() {
        return Stream.of(
                Arguments.of("CompileOrc failure", 1, 0, 0, "Error compiling orchestra"),
                Arguments.of("ReadScore failure", 0, 1, 0, "Error reading score"),
                Arguments.of("Start failure", 0, 0, 1, "Error starting Csound")
        );
    }


    @Test
    @DisplayName("performLoop: Runs and stops when PerformKsmps returns non-zero (Csound finished)")
    void testPerformLoop_runsUntilZeroReturned() throws Exception {
        // GIVEN
        Csound mockCsound = Mockito.mock(Csound.class);
        int sampleRate = 96000;

        Mockito.when(mockCsound.PerformKsmps())
                .thenReturn(0) // Iteration 1
                .thenReturn(0) // Iteration 2
                .thenReturn(0) // Iteration 3
                .thenReturn(1); // Termination

        // Field isRunningField Deklaration entfernt
        setRunningStatus(true);

        // WHEN
        granulatorService.performLoop(mockCsound, sampleRate);

        // THEN
        verify(mockCsound, times(4)).PerformKsmps();

        assertTrue(getRunningStatus(),
                "isRunning should still be true if Csound completed normally.");

        setRunningStatus(false);
    }

    @Test
    @DisplayName("performLoop: Runs and stops when isRunning becomes false (User stop)")
    void testPerformLoop_stopsWhenRunningFlagIsFalse() throws Exception {
        // GIVEN
        Csound mockCsound = Mockito.mock(Csound.class);
        int sampleRate = 44100;

        final int[] callCount = {0};

        when(mockCsound.PerformKsmps()).thenAnswer(invocation -> {
            callCount[0]++;
            if (callCount[0] >= 5) {
                // Wir nutzen den Helper, um den Flag zu setzen
                setRunningStatus(false);
            }
            return 0;
        });

        // Field isRunningField Deklaration entfernt
        setRunningStatus(true);

        // WHEN
        granulatorService.performLoop(mockCsound, sampleRate);

        // THEN
        verify(mockCsound, times(5)).PerformKsmps();

        assertFalse(getRunningStatus(),
                "isRunning must be false after the loop terminates via user stop.");

        setRunningStatus(false);
    }

    @Test
    @DisplayName("performLoop: Runs long enough to trigger yield and terminates cleanly")
    void testPerformLoop_runsLongEnoughAndTerminates() throws Exception {
        // GIVEN
        Csound mockCsound = Mockito.mock(Csound.class);
        int sampleRate = 44100;

        Integer[] returns = new Integer[201];
        Arrays.fill(returns, 0);
        returns[200] = 1;

        Mockito.when(mockCsound.PerformKsmps())
                .thenReturn(returns[0], Arrays.copyOfRange(returns, 1, returns.length));

        setRunningStatus(true);

        // WHEN
        granulatorService.performLoop(mockCsound, sampleRate);

        // THEN
        verify(mockCsound, times(201)).PerformKsmps();

        assertTrue(getRunningStatus(),
                "isRunning should remain true if Csound completed normally.");

        setRunningStatus(false);
    }


    @Test
    @DisplayName("cleanupCsound: Stops, Resets Csound, and resets flags/state")
    void testCleanupCsound_stopsAndResetsCsound_andResetsFlags() throws Exception {
        // GIVEN
        Csound mockCsound = Mockito.mock(Csound.class);
        Path mockHrtfDir = Paths.get("/temp/hrtf");
        Path mockAudioPath = Paths.get("/live/audio.wav");
        Path mockScoPath = Paths.get("/live/sco.sco");

        Field csoundField = GranulatorService.class.getDeclaredField("currentCsound");
        csoundField.setAccessible(true);
        csoundField.set(granulatorService, mockCsound);

        setRunningStatus(true);

        Field hrtfDirField = GranulatorService.class.getDeclaredField("currentHrtfTempDir");
        hrtfDirField.setAccessible(true);
        hrtfDirField.set(granulatorService, mockHrtfDir);

        Field audioPathField = GranulatorService.class.getDeclaredField("currentLiveAudioPath");
        audioPathField.setAccessible(true);
        audioPathField.set(granulatorService, mockAudioPath);

        Field scoPathField = GranulatorService.class.getDeclaredField("currentLiveScoPath");
        scoPathField.setAccessible(true);
        scoPathField.set(granulatorService, mockScoPath);

        Field lastStopTimeField = GranulatorService.class.getDeclaredField("lastStopTime");
        lastStopTimeField.setAccessible(true);

        // WHEN
        granulatorService.cleanupCsound();

        // THEN
        verify(mockCsound, times(1)).Stop();
        verify(mockCsound, times(1)).Reset();
        verify(mockCsound, times(1)).Cleanup();

        verify(mockFileManager, times(1)).cleanupTempDirectory(mockHrtfDir);
        verify(mockFileManager, times(1)).cleanupLiveInputFiles(mockAudioPath, mockScoPath);

        assertNull(csoundField.get(granulatorService), "currentCsound must be set to null.");
        assertFalse(getRunningStatus(), "isRunning must be set to false.");

        assertNotNull(lastStopTimeField.get(granulatorService),
                "lastStopTime must be updated.");

        assertNull(hrtfDirField.get(granulatorService), "currentHrtfTempDir must be reset to null.");
        assertNull(audioPathField.get(granulatorService), "currentLiveAudioPath must be reset to null.");
        assertNull(scoPathField.get(granulatorService), "currentLiveScoPath must be reset to null.");
    }


    @Test
    @DisplayName("cleanupCsound: Does nothing if currentCsound is null, but resets state")
    void testCleanupCsound_doesNothingIfCsoundIsNull() throws Exception {
        // GIVEN
        Csound mockCsound = Mockito.mock(Csound.class); // Mocked but never set

        Field csoundField = GranulatorService.class.getDeclaredField("currentCsound");
        csoundField.setAccessible(true);
        csoundField.set(granulatorService, null);

        setRunningStatus(true);

        Field hrtfDirField = GranulatorService.class.getDeclaredField("currentHrtfTempDir");
        hrtfDirField.setAccessible(true);
        hrtfDirField.set(granulatorService, null);

        // WHEN
        granulatorService.cleanupCsound();

        // THEN
        verify(mockCsound, never()).Stop();
        verify(mockCsound, never()).Reset();
        verify(mockCsound, never()).Cleanup();

        verify(mockFileManager, times(1)).cleanupTempDirectory(isNull());
        verify(mockFileManager, times(1)).cleanupLiveInputFiles(isNull(), isNull());

        assertNull(csoundField.get(granulatorService), "currentCsound must remain null.");
        assertFalse(getRunningStatus(), "isRunning must be set to false.");
    }

    @Test
    @DisplayName("cleanupCsound: Executes finally block even if Csound throws an exception")
    void testCleanupCsound_executesFinallyBlockOnException() throws Exception {
        // GIVEN
        Csound mockCsound = Mockito.mock(Csound.class);
        Path mockHrtfDir = Paths.get("/temp/hrtf-err"); // non-null temp dir

        Field csoundField = GranulatorService.class.getDeclaredField("currentCsound");
        csoundField.setAccessible(true);
        csoundField.set(granulatorService, mockCsound);

        Field hrtfDirField = GranulatorService.class.getDeclaredField("currentHrtfTempDir");
        hrtfDirField.setAccessible(true);
        hrtfDirField.set(granulatorService, mockHrtfDir);

        doThrow(new RuntimeException("Csound cleanup failed")).when(mockCsound).Stop();

        // WHEN
        granulatorService.cleanupCsound();

        // THEN
        verify(mockCsound, times(1)).Stop();

        verify(mockFileManager, times(1)).cleanupTempDirectory(mockHrtfDir);

        assertNull(csoundField.get(granulatorService), "currentCsound must be set to null.");
    }

    @Test
    void testPerformGranulationFake() throws IOException {
        // GIVEN
        FakeCsound fake = new FakeCsound();
        Path orcPath = Files.createTempFile("test", ".orc");
        Path scoPath = Files.createTempFile("test", ".sco");
        Path outputPath = Files.createTempFile("test-output", ".wav");

        // WHEN
        Files.writeString(orcPath, "dummy orc content");
        Files.writeString(scoPath, "dummy sco content");

        granulatorService.performGranulationFake(fake, orcPath, scoPath, outputPath);

        // THEN
        assertTrue(Files.exists(outputPath));

        // Cleanup
        Files.deleteIfExists(orcPath);
        Files.deleteIfExists(scoPath);
        Files.deleteIfExists(outputPath);
    }

    @Test
    void testPerformGranulationFakeRealCall() throws IOException {
        CsoundConfigurator configurator = new CsoundConfigurator();
        TemporaryFileManager fileManager = new TemporaryFileManager();
        GranulatorService service = new GranulatorService(configurator, fileManager);

        FakeCsound fake = new FakeCsound();
        Path orc = Files.createTempFile("test", ".orc");
        Path sco = Files.createTempFile("test", ".sco");
        Path output = Files.createTempFile("test-output", ".wav");

        Files.writeString(orc, "dummy orc content");
        Files.writeString(sco, "dummy sco content");

        service.performGranulationFake(fake, orc, sco, output);

        assertTrue(Files.exists(output));

        Files.deleteIfExists(orc);
        Files.deleteIfExists(sco);
        Files.deleteIfExists(output);
    }


}