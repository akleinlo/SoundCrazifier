package org.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AudioProcessorTest {

    @TempDir
    Path tempDir;

    private AudioProcessor audioProcessorSpy;
    private final String MOCK_SOX_PATH = "/usr/bin/sox";
    private Path inputPath;
    private Path expectedResampledPath;

    @BeforeEach
    void setUp() throws IOException {
        AudioProcessor realProcessor = new AudioProcessor(MOCK_SOX_PATH);
        audioProcessorSpy = Mockito.spy(realProcessor);

        inputPath = tempDir.resolve("original.mp3");
        Files.createFile(inputPath);

        expectedResampledPath = tempDir.resolve("resampled-test.wav");
    }

    private void mockProcessSuccess(String soxOutput) throws Exception {

        Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor()).thenReturn(0);

        InputStream outputStream = new ByteArrayInputStream(soxOutput.getBytes());
        when(mockProcess.getInputStream()).thenReturn(outputStream);

        ProcessBuilder mockPbInstance = mock(ProcessBuilder.class);
        when(mockPbInstance.start()).thenReturn(mockProcess);

        doReturn(mockPbInstance)
                .when(audioProcessorSpy)
                .createProcessBuilder(any(String[].class));

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createTempFile(
                            any(Path.class), anyString(), eq(".wav")))
                    .thenReturn(expectedResampledPath);

            Path result = audioProcessorSpy.resampleWithSox(inputPath, 96000);

            assertEquals(expectedResampledPath, result, "The returned path should be the expected temporary path.");

            ArgumentCaptor<String[]> commandCaptor = ArgumentCaptor.forClass(String[].class);

            verify(audioProcessorSpy).createProcessBuilder(commandCaptor.capture());

            List<String> actualCommand = Arrays.asList(commandCaptor.getValue());

            List<String> expectedCommand = List.of(
                    MOCK_SOX_PATH,
                    inputPath.toString(),
                    "-r", "96000",
                    expectedResampledPath.toString(),
                    "rate", "-v"
            );
            assertEquals(expectedCommand, actualCommand, "The SoX command must be constructed correctly..");

            verify(mockProcess, times(1)).waitFor();

            mockedFiles.verify(() -> Files.createTempFile(
                            eq(inputPath.getParent()), eq("resampled-"), eq(".wav")),
                    times(1));
        }
    }


    @Test
    @DisplayName("resampleWithSox: Successful execution with exit code 0")
    void resampleWithSox_success() throws Exception {
        mockProcessSuccess("");
    }

    @Test
    @DisplayName("resampleWithSox: Successful execution with output logging")
    void resampleWithSox_successWithOutput() throws Exception {
        mockProcessSuccess("Input file processed.\n");
    }

    @Test
    @DisplayName("resampleWithSox: SoX not found (ProcessBuilder throws IOException)")
    void resampleWithSox_soxNotFound() throws Exception {

        ProcessBuilder mockPbInstance = mock(ProcessBuilder.class);

        when(mockPbInstance.start()).thenThrow(new IOException("Cannot run program \"/usr/bin/sox\": No such file or directory"));

        doReturn(mockPbInstance)
                .when(audioProcessorSpy)
                .createProcessBuilder(any(String[].class));

        assertThrows(IOException.class,
                () -> audioProcessorSpy.resampleWithSox(inputPath, 44100),
                "Should throw an IOException if SoX is not found.");
    }

    @Test
    @DisplayName("resampleWithSox: SoX fails with non-zero exit code")
    void resampleWithSox_nonZeroExitCode() throws Exception {
        // GIVEN
        Process mockProcess = mock(Process.class);
        when(mockProcess.waitFor()).thenReturn(1);
        InputStream errorStream = new ByteArrayInputStream("sox FAIL formats: can't open original.mp3: Not a valid MP3 file".getBytes());
        when(mockProcess.getInputStream()).thenReturn(errorStream);

        ProcessBuilder mockPbInstance = mock(ProcessBuilder.class);
        when(mockPbInstance.start()).thenReturn(mockProcess);

        doReturn(mockPbInstance)
                .when(audioProcessorSpy)
                .createProcessBuilder(any(String[].class));

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createTempFile(
                            any(Path.class), anyString(), eq(".wav")))
                    .thenReturn(expectedResampledPath);

            // WHEN & THEN
            IOException ex = assertThrows(IOException.class,
                    () -> audioProcessorSpy.resampleWithSox(inputPath, 44100),
                    "Should throw an IOException if SoX fails.");

            assertTrue(ex.getMessage().contains("failed with exit code: 1"), "Error message should contain exit code.");
        }
    }

    // Füge diesen Test zur Klasse AudioProcessorTest hinzu

    @Test
    @DisplayName("createProcessBuilder: Ensures wrapper method returns a real ProcessBuilder")
    void createProcessBuilder_returnsRealInstance() {
        // GIVEN
        String[] expectedCommand = {
                "/usr/bin/sox", "input.wav"
        };

        // WHEN
        AudioProcessor realProcessor = new AudioProcessor("/dummy/sox");
        ProcessBuilder pb = realProcessor.createProcessBuilder(expectedCommand);

        // THEN
        assertNotNull(pb, "Should return a non-null ProcessBuilder instance.");
        assertEquals(List.of(expectedCommand), pb.command(),
                "The ProcessBuilder command list should match the input array.");
    }
}