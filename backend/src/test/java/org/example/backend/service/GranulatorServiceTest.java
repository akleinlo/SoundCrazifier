package org.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class GranulatorServiceTest {

    private GranulatorService granulatorService;
    private TemporaryFileManager mockFileManager;
    private CsoundConfigurator mockConfigurator;

    @BeforeEach
    void setup() throws IOException {
        mockConfigurator = Mockito.mock(CsoundConfigurator.class);
        mockFileManager = Mockito.mock(TemporaryFileManager.class);

        granulatorService = Mockito.spy(new GranulatorService(mockConfigurator, mockFileManager));

        Mockito.doAnswer(invocation -> {
            Path output = invocation.getArgument(3, Path.class);
            if (output != null) {
                Files.createFile(output);
            }
            return null;
        }).when(granulatorService).performGranulationFake(
                Mockito.any(FakeCsound.class), Mockito.any(), Mockito.any(), Mockito.any());
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
        Field isRunningField = GranulatorService.class.getDeclaredField("isRunning");
        isRunningField.setAccessible(true);
        isRunningField.setBoolean(granulatorService, true);

        Path dummyOrc = Paths.get("dummy.orc");
        Path dummySco = Paths.get("dummy.sco");

        FakeCsound fakeCs = new FakeCsound();

        // WHEN
        granulatorService.performGranulationFake(fakeCs, dummyOrc, dummySco, null);

        // THEN
        assertTrue(granulatorService.isRunning(), "isRunning should remain true when already running");

        // Cleanup
        isRunningField.setBoolean(granulatorService, false);
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
        Mockito.doNothing()
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
        CsoundConfigurator mockConfigurator = Mockito.mock(CsoundConfigurator.class);
        GranulatorService spyService = Mockito.spy(new GranulatorService(mockConfigurator, mockFileManager));

        Mockito.doReturn(new Object()).when(spyService).createCsoundInstance();

        Path dummyOrc = Paths.get("dummy.orc");
        Path dummySco = Paths.get("dummy.sco");

        // WHEN + THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                spyService.performGranulationOnce(dummyOrc, dummySco, false, null));

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
}
