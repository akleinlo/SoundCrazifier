package org.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class GranulatorServiceTest {

    private GranulatorService granulatorService;

    @BeforeEach
    void setup() {
        granulatorService = Mockito.spy(new GranulatorService());
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
        String result = granulatorService.performGranulationOnce(dummyOrc, dummySco, false);

        // THEN
        assertEquals("Granulation already running!", result);

        // Cleanup
        isRunningField.setBoolean(granulatorService, false);
    }


    @Test
    @DisplayName("performGranulationOnce resets isRunning after IOException")
    void performGranulationOnce_resetsFlagAfterIOException() throws Exception {
        // GIVEN
        Mockito.doAnswer(invocation -> {
            throw new IOException("Simulated IOException");
        }).when(granulatorService).performGranulation(Mockito.any(), Mockito.any(), Mockito.anyBoolean());

        assertFalse(granulatorService.isRunning());
        Path dummyOrc = Paths.get("does-not-matter.orc");
        Path dummySco = Paths.get("does-not-matter.sco");

        // WHEN / THEN
        assertThrows(IOException.class, () -> granulatorService.performGranulationOnce(dummyOrc, dummySco, false));
        assertFalse(granulatorService.isRunning());
    }

}
