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

    @BeforeEach
    void setup() {
        // Spy with FakeCsound
        granulatorService = Mockito.spy(new GranulatorService() {
            @Override
            protected Object createCsoundInstance() {
                return new FakeCsound();
            }
        });
    }


    @Test
    @DisplayName("isRunning initially false")
    void initiallyNotRunning() {
        assertFalse(granulatorService.isRunning(), "Initial isRunning should be false");
    }

    @Test
    @DisplayName("performGranulationOnce returns 'already running' when flag true (via reflection)")
    void performGranulationOnce_rejectsWhenAlreadyRunning() throws Exception {
        // GIVEN
        Field isRunningField = GranulatorService.class.getDeclaredField("isRunning");
        isRunningField.setAccessible(true);
        isRunningField.setBoolean(granulatorService, true);

        Path dummyOrc = Paths.get("does-not-matter.orc");
        Path dummySco = Paths.get("does-not-matter.sco");

        // WHEN
        String result = granulatorService.performGranulationOnce(dummyOrc, dummySco, false, null);

        // THEN
        assertEquals("Granulation already running!", result);

        // Cleanup
        isRunningField.setBoolean(granulatorService, false);
    }

    @Test
    @DisplayName("performGranulationOnce resets isRunning after IOException")
    void performGranulationOnce_resetsFlagAfterIOException() throws Exception {
        // GIVEN
        Mockito.doThrow(new IOException("Simulated IOException"))
                .when(granulatorService)
                .performGranulation(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());

        // WHEN
        assertFalse(granulatorService.isRunning());

        Path dummyOrc = Paths.get("does-not-matter.orc");
        Path dummySco = Paths.get("does-not-matter.sco");

        // THEN
        IOException ex = assertThrows(IOException.class, () ->
                granulatorService.performGranulationOnce(dummyOrc, dummySco, false, null));
        assertEquals("Simulated IOException", ex.getMessage());

        assertFalse(granulatorService.isRunning(), "isRunning should be reset after exception");
    }

    @Test
    @DisplayName("performGranulationOnce returns 'Granulation finished!' when successful")
    void performGranulationOnce_success() throws Exception {
        // GIVEN
        Mockito.doNothing()
                .when(granulatorService)
                .performGranulation(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());

        Path dummyOrc = Paths.get("does-not-matter.orc");
        Path dummySco = Paths.get("does-not-matter.sco");

        // WHEN
        String result = granulatorService.performGranulationOnce(dummyOrc, dummySco, false, null);

        // THEN
        assertEquals("Granulation finished!", result);
        assertFalse(granulatorService.isRunning());
    }

    @Test
    @DisplayName("performGranulation actually runs and writes output file (mocked)")
    void performGranulation_createsOutputFile() throws Exception {
        // GIVEN
        Mockito.doAnswer(invocation -> {
            Path output = invocation.getArgument(3, Path.class);
            Files.createFile(output);
            return null;
        }).when(granulatorService).performGranulation(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());

        Path orc = Files.createTempFile("test", ".orc");
        Path sco = Files.createTempFile("test", ".sco");
        Files.writeString(orc, "instr 1\n a1 oscili 0.1, 440\n out a1\n endin");
        Files.writeString(sco, "i1 0 1");

        Path output = Files.createTempFile("output", ".wav");
        Files.deleteIfExists(output);

        // WHEN
        granulatorService.performGranulation(orc, sco, false, output);

        // THEN
        assertTrue(Files.exists(output));

        // cleanup
        Files.deleteIfExists(orc);
        Files.deleteIfExists(sco);
        Files.deleteIfExists(output);
    }

    @Test
    void performGranulation_writesOutput_withFakeCsound() throws Exception {
        // GIVEN
        Path orc = Files.createTempFile("test", ".orc");
        Path sco = Files.createTempFile("test", ".sco");
        Files.writeString(orc, "instr 1\n a1 oscili 0.1, 440\n out a1\n endin");
        Files.writeString(sco, "i1 0 1");

        Path output = Files.createTempFile("output", ".wav");
        Files.deleteIfExists(output);

        // WHEN
        granulatorService.performGranulation(orc, sco, false, output);

        // THEN
        assertFalse(granulatorService.isRunning(), "isRunning sollte nach performGranulation false sein");

        // cleanup
        Files.deleteIfExists(orc);
        Files.deleteIfExists(sco);
        Files.deleteIfExists(output);
    }

    @Test
    void performGranulationOnce_withFakeCsound_returnsFinished() throws IOException {
        // GIVEN
        Path orc = Files.createTempFile("test", ".orc");
        Path sco = Files.createTempFile("test", ".sco");
        Files.writeString(orc, "instr 1\n a1 oscili 0.1, 440\n out a1\n endin");
        Files.writeString(sco, "i1 0 1");

        // WHEN
        String result = granulatorService.performGranulationOnce(orc, sco, false, null);

        // THEN
        assertEquals("Granulation finished!", result);
        assertFalse(granulatorService.isRunning());

        // cleanup
        Files.deleteIfExists(orc);
        Files.deleteIfExists(sco);
    }

}
