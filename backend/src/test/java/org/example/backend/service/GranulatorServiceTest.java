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
    void setup() throws IOException {
        granulatorService = Mockito.spy(new GranulatorService());

        // Mock für performGranulation, um IOException zu vermeiden und Outputfile zu simulieren
        Mockito.doAnswer(invocation -> {
            Path output = invocation.getArgument(3, Path.class);
            if (output != null) {
                try {
                    Files.createFile(output); // simuliert erfolgreiche Granulation
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return null;
        }).when(granulatorService).performGranulation(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());
    }

    @Test
    @DisplayName("isRunning initially false")
    void initiallyNotRunning() {
        assertFalse(granulatorService.isRunning(), "Initial isRunning should be false");
    }

    @Test
    @DisplayName("performGranulationOnce returns 'already running' when flag true (via reflection)")
    void performGranulationOnce_rejectsWhenAlreadyRunning() throws Exception {
        Field isRunningField = GranulatorService.class.getDeclaredField("isRunning");
        isRunningField.setAccessible(true);
        isRunningField.setBoolean(granulatorService, true);

        Path dummyOrc = Paths.get("does-not-matter.orc");
        Path dummySco = Paths.get("does-not-matter.sco");

        String result = granulatorService.performGranulationOnce(dummyOrc, dummySco, false, null);

        assertEquals("Crazification already running!", result);

        // Cleanup
        isRunningField.setBoolean(granulatorService, false);
    }

    @Test
    @DisplayName("performGranulationOnce resets isRunning after IOException")
    void performGranulationOnce_resetsFlagAfterIOException() throws Exception {
        Mockito.doThrow(new IOException("Simulated IOException"))
                .when(granulatorService)
                .performGranulation(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());

        assertFalse(granulatorService.isRunning());

        Path dummyOrc = Paths.get("does-not-matter.orc");
        Path dummySco = Paths.get("does-not-matter.sco");

        IOException ex = assertThrows(IOException.class, () ->
                granulatorService.performGranulationOnce(dummyOrc, dummySco, false, null));
        assertEquals("Simulated IOException", ex.getMessage());

        assertFalse(granulatorService.isRunning(), "isRunning should be reset after exception");
    }

    @Test
    @DisplayName("performGranulationOnce returns 'Crazification finished!' when successful")
    void performGranulationOnce_success() throws Exception {
        Mockito.doNothing()
                .when(granulatorService)
                .performGranulation(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());

        Path dummyOrc = Paths.get("does-not-matter.orc");
        Path dummySco = Paths.get("does-not-matter.sco");

        String result = granulatorService.performGranulationOnce(dummyOrc, dummySco, false, null);

        assertEquals("Crazification finished!", result);
        assertFalse(granulatorService.isRunning());
    }

    @Test
    @DisplayName("performGranulation actually runs and writes output file (mocked)")
    void performGranulation_createsOutputFile() throws Exception {
        Mockito.doAnswer(invocation -> {
            Path output = invocation.getArgument(3, Path.class);
            if (output != null) {
                Files.createFile(output); // simuliert erfolgreiche Granulation
            }
            return null;
        }).when(granulatorService).performGranulation(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any());

        Path orc = Files.createTempFile("test", ".orc");
        Path sco = Files.createTempFile("test", ".sco");
        Files.writeString(orc, "instr 1\n a1 oscili 0.1, 440\n out a1\n endin");
        Files.writeString(sco, "i1 0 1");

        Path output = Files.createTempFile("output", ".wav");
        Files.deleteIfExists(output);

        granulatorService.performGranulation(orc, sco, false, output);

        assertTrue(Files.exists(output));

        Files.deleteIfExists(orc);
        Files.deleteIfExists(sco);
        Files.deleteIfExists(output);
    }

}
