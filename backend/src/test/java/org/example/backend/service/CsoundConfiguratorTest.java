package org.example.backend.service;

import csnd6.Csound;
import org.example.backend.service.CsoundConfigurator.HrtfInjectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsoundConfiguratorTest {

    private CsoundConfigurator configurator;
    private Csound mockCsound;

    @BeforeEach
    void setUp() {
        configurator = new CsoundConfigurator();
        mockCsound = mock(Csound.class);
    }

    @Test
    @DisplayName("configureCsound (Live): Sets 44100 Hz, 16-bit, and DAC output")
    void configureCsound_live() {
        // GIVEN
        Path ignoredOutput = Paths.get("/ignored/output.wav");

        // WHEN
        configurator.configureCsound(mockCsound, true, ignoredOutput);

        // THEN
        verify(mockCsound).SetOption("-odac");
        verify(mockCsound).SetOption("-r44100");
        verify(mockCsound).SetOption("--format=short");

        verify(mockCsound).SetOption("-b1024");
        verify(mockCsound).SetOption("-B4096");
    }

    @Test
    @DisplayName("configureCsound (Offline): Sets 96000 Hz, 32-bit float, and file output path")
    void configureCsound_offline() {
        // GIVEN
        Path output = Paths.get("/render/output.wav");
        String expectedOutputOption = "-o" + output.toAbsolutePath();

        // WHEN
        configurator.configureCsound(mockCsound, false, output);

        // THEN
        verify(mockCsound).SetOption(expectedOutputOption);
        verify(mockCsound).SetOption("--format=float");
        verify(mockCsound).SetOption("-r96000");

        verify(mockCsound).SetOption("-b1024");
        verify(mockCsound).SetOption("-B4096");
    }


    @Test
    @DisplayName("adjustSampleRate: Replaces 'sr' value correctly")
    void adjustSampleRate_success() {
        // GIVEN
        String originalOrc = "sr    = 44100\nksmps = 1\n";
        int newSampleRate = 96000;

        // WHEN
        String result = configurator.adjustSampleRate(originalOrc, newSampleRate);

        // THEN
        assertEquals("sr = 96000\nksmps = 1\n", result,
                "The sample rate should be replaced correctly, even with irregular spacing.");
    }

    @Test
    @DisplayName("adjustSampleRate: Handles different spacing around the equals sign")
    void adjustSampleRate_differentSpacing() {
        // GIVEN
        String originalOrc = "sr=12345\n";
        int newSampleRate = 1;

        // WHEN
        String result = configurator.adjustSampleRate(originalOrc, newSampleRate);

        // THEN
        assertEquals("sr = 1\n", result,
                "Should correctly replace the value when there are no spaces.");
    }


    @Test
    @DisplayName("injectHrtfPaths: Creates temp dir, copies resources, and injects paths into ORC")
    @SuppressWarnings("unused")
    void injectHrtfPaths_success() throws IOException {
        // GIVEN
        int sampleRate = 44100;
        String originalOrc = """
                gS_HRTF_left  = "OLD_LEFT.dat"
                gS_HRTF_right = "OLD_RIGHT.dat"
                instr 1 ...
                """;

        Path mockTempDir = Paths.get("/mock/temp/csound-hrtf-123");

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createTempDirectory(anyString())).thenReturn(mockTempDir);
            mockedFiles.when(() -> Files.copy(any(InputStream.class), any(Path.class))).thenReturn(100L);

            try (var mockedConstruction = mockConstruction(ClassPathResource.class,
                    (mock, ctx) -> {
                        if (ctx.arguments().get(0).toString().contains("left")) {
                            when(mock.getInputStream()).thenReturn(new ByteArrayInputStream("left data".getBytes()));
                        } else {
                            when(mock.getInputStream()).thenReturn(new ByteArrayInputStream("right data".getBytes()));
                        }
                    })) {

                // WHEN
                HrtfInjectionResult result = configurator.injectHrtfPaths(originalOrc, sampleRate);

                // THEN
                Path expectedLeftPath = mockTempDir.resolve("hrtf-44100-left.dat");
                Path expectedRightPath = mockTempDir.resolve("hrtf-44100-right.dat");

                mockedFiles.verify(() -> Files.createTempDirectory(eq("csound-hrtf")), times(1));
                mockedFiles.verify(() -> Files.copy(any(InputStream.class), eq(expectedLeftPath)), times(1));
                mockedFiles.verify(() -> Files.copy(any(InputStream.class), eq(expectedRightPath)), times(1));

                assertEquals(mockTempDir, result.tempDir());
                assertTrue(result.orcContent().contains(expectedLeftPath.toString()));
                assertTrue(result.orcContent().contains(expectedRightPath.toString()));
                assertFalse(result.orcContent().contains("OLD_LEFT.dat"));
            }
        }
    }


    @Test
    @DisplayName("injectHrtfPaths: Throws IOException if HRTF resource is missing")
    @SuppressWarnings("unused")
    void injectHrtfPaths_resourceMissing() {
        int sampleRate = 48000;
        String originalOrc = "gS_HRTF_left  = \"...\"\n" +
                "gS_HRTF_right = \"...\"";

        Path mockTempDir = Paths.get("/mock/temp/csound-hrtf-48k");

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.createTempDirectory(anyString())).thenReturn(mockTempDir);

            try (var mockedConstruction = mockConstruction(ClassPathResource.class,
                    (mock, ctx) -> when(mock.getInputStream()).thenThrow(new IOException("Resource not found")))) {

                IOException ex = assertThrows(IOException.class, () ->
                        configurator.injectHrtfPaths(originalOrc, sampleRate)
                );

                assertTrue(ex.getMessage().contains("HRTF files not found for 48000 Hz"));

                mockedFiles.verify(() -> Files.copy(any(InputStream.class), any(Path.class)), never());
                mockedFiles.verify(() -> Files.createTempDirectory(eq("csound-hrtf")), times(1));
            }
        }
    }

}