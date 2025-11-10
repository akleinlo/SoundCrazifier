package org.example.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TemporaryFileManagerTest {

    private TemporaryFileManager fileManager;

    @BeforeEach
    void setUp() {
        fileManager = new TemporaryFileManager();
    }

    @Test
    @DisplayName("getAudioPathFromSco: Extracts path correctly from standard f-statement")
    void getAudioPathFromSco_success() {
        // GIVEN
        Path mockScoPath = Paths.get("/dummy/score.sco");
        String scoContent = """
                i1 0 1
                f1 0 0 1 "/temp/audio_file.wav" 0 0 0
                """;

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.readString(mockScoPath)).thenReturn(scoContent);

            // WHEN
            String result = fileManager.getAudioPathFromSco(mockScoPath);

            // THEN
            assertEquals("/temp/audio_file.wav", result,
                    "The path should be extracted correctly between the quotation marks.");
            mockedFiles.verify(() -> Files.readString(mockScoPath), times(1));
        }
    }

    @Test
    @DisplayName("getAudioPathFromSco: Returns null if audio path is missing")
    void getAudioPathFromSco_noPath() {
        // GIVEN
        Path mockScoPath = Paths.get("/dummy/score.sco");
        String scoContent = """
                i1 0 1
                f1 0 0 1 1 0 0 0
                """;

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.readString(mockScoPath)).thenReturn(scoContent);

            // WHEN
            String result = fileManager.getAudioPathFromSco(mockScoPath);

            // THEN
            assertNull(result, "Should return null if no quotation marks are found.");
        }
    }

    @Test
    @DisplayName("getAudioPathFromSco: Returns null on IOException during read")
    void getAudioPathFromSco_ioException() {
        // GIVEN
        Path mockScoPath = Paths.get("/dummy/score.sco");

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.readString(mockScoPath)).thenThrow(new IOException("Read failed"));

            // WHEN
            String result = fileManager.getAudioPathFromSco(mockScoPath);

            // THEN
            assertNull(result, "Should return null if an error occurs during reading..");
        }
    }

    @Test
    @DisplayName("cleanupTempDirectory: Recursively deletes all files in the directory")
    @SuppressWarnings("resource")
    void cleanupTempDirectory_success() {
        // GIVEN
        Path tempDir = Paths.get("/tmp/crazifier-test-dir");
        Path fileA = tempDir.resolve("fileA.txt");
        Path fileB = tempDir.resolve("fileB.txt");

        try (Stream<Path> pathStream = Stream.of(fileB, fileA, tempDir)) {

            try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
                mockedFiles.when(() -> Files.walk(tempDir)).thenReturn(pathStream);

                mockedFiles.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

                // WHEN
                fileManager.cleanupTempDirectory(tempDir);

                mockedFiles.verify(() -> Files.deleteIfExists(fileB), times(1));
                mockedFiles.verify(() -> Files.deleteIfExists(fileA), times(1));
                mockedFiles.verify(() -> Files.deleteIfExists(tempDir), times(1));
            }
        }
    }

    @Test
    @DisplayName("cleanupTempDirectory: Ignores null input")
    @SuppressWarnings("resource")
    void cleanupTempDirectory_nullInput() {
        // GIVEN / WHEN
        fileManager.cleanupTempDirectory(null);

        // THEN
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.verify(() -> Files.walk(any()), never());
        }
    }

    @Test
    @DisplayName("cleanupTempDirectory: Logs error if Files.walk throws IOException")
    @SuppressWarnings("resource")
    void cleanupTempDirectory_filesWalkFails() {
        // GIVEN
        Path tempDir = Paths.get("/tmp/crazifier-test-dir");

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.walk(tempDir)).thenThrow(new IOException("Stream failed"));

            // WHEN
            fileManager.cleanupTempDirectory(tempDir);

            // THEN
            mockedFiles.verify(() -> Files.walk(tempDir), times(1));
            mockedFiles.verify(() -> Files.deleteIfExists(any()), never());
        }
    }

    @Test
    @DisplayName("cleanupSaveInputFiles: Deletes audio and SCO files after save")
    void cleanupSaveInputFiles_success() {
        // GIVEN
        Path mockScoPath = Paths.get("/tmp/temp.sco");
        String mockAudioPathString = "/tmp/converted.wav";
        Path mockAudioPath = Paths.get(mockAudioPathString);

        TemporaryFileManager spyFileManager = Mockito.spy(fileManager);
        doReturn(mockAudioPathString).when(spyFileManager).getAudioPathFromSco(mockScoPath);

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

            // WHEN
            spyFileManager.cleanupSaveInputFiles(mockScoPath);

            // THEN
            mockedFiles.verify(() -> Files.deleteIfExists(mockAudioPath), times(1));
            mockedFiles.verify(() -> Files.deleteIfExists(mockScoPath), times(1));

            verify(spyFileManager, times(1)).getAudioPathFromSco(mockScoPath);
        }
    }

    @Test
    @DisplayName("cleanupSaveInputFiles: Handles missing audio path gracefully")
    void cleanupSaveInputFiles_onlyScoDeleted() {
        // GIVEN
        Path mockScoPath = Paths.get("/tmp/temp.sco");

        TemporaryFileManager spyFileManager = Mockito.spy(fileManager);
        doReturn(null).when(spyFileManager).getAudioPathFromSco(mockScoPath);

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

            // WHEN
            spyFileManager.cleanupSaveInputFiles(mockScoPath);

            // THEN
            mockedFiles.verify(() -> Files.deleteIfExists(any(Path.class)), times(1)); // Nur für SCO
            mockedFiles.verify(() -> Files.deleteIfExists(mockScoPath), times(1));
        }
    }


    @Test
    @DisplayName("cleanupLiveInputFiles: Deletes audio and SCO files after live run")
    void cleanupLiveInputFiles_success() {
        // GIVEN
        Path mockAudioPath = Paths.get("/live/audio.wav");
        Path mockScoPath = Paths.get("/live/sco.sco");

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.deleteIfExists(any(Path.class))).thenReturn(true);

            // WHEN
            fileManager.cleanupLiveInputFiles(mockAudioPath, mockScoPath);

            // THEN
            mockedFiles.verify(() -> Files.deleteIfExists(mockAudioPath), times(1));
            mockedFiles.verify(() -> Files.deleteIfExists(mockScoPath), times(1));
        }
    }

    @Test
    @DisplayName("cleanupLiveInputFiles: Ignores null paths")
    void cleanupLiveInputFiles_nullPaths() {
        // GIVEN / WHEN
        fileManager.cleanupLiveInputFiles(null, Paths.get("/live/sco.sco"));

        // THEN
        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.verify(() -> Files.deleteIfExists(any()), never());
        }
    }


    @Test
    @DisplayName("ensureValidWav: Does nothing if file exists and is not empty")
    void ensureValidWav_fileExistsAndNotEmpty() throws IOException {
        // GIVEN
        Path mockPath = Paths.get("/output/existing.wav");

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class)) {
            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            mockedFiles.when(() -> Files.size(mockPath)).thenReturn(100L);

            // WHEN
            fileManager.ensureValidWav(mockPath);

            // THEN
            mockedFiles.verify(() -> Files.readString(any()), never());
            mockedFiles.verify(() -> Files.writeString(any(), any()), never());
        }
    }

    @Test
    @DisplayName("ensureValidWav: Creates placeholder WAV if file is missing (using AudioSystem)")
    void ensureValidWav_createsPlaceholder_ifMissing() throws Exception {
        // GIVEN
        Path mockPath = Paths.get("/output/missing.wav");

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<AudioSystem> mockedAudioSystem = Mockito.mockStatic(AudioSystem.class)) {

            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(false);
            mockedFiles.when(() -> Files.size(mockPath)).thenReturn(0L);

            mockedAudioSystem.when(() -> AudioSystem.write(
                    any(javax.sound.sampled.AudioInputStream.class),
                    any(javax.sound.sampled.AudioFileFormat.Type.class),
                    any(java.io.File.class)
            )).thenReturn(44100 * 2);

            // WHEN
            fileManager.ensureValidWav(mockPath);

            // THEN
            mockedFiles.verify(() -> Files.exists(mockPath), times(1));

            mockedAudioSystem.verify(() -> AudioSystem.write(
                    any(), eq(javax.sound.sampled.AudioFileFormat.Type.WAVE), eq(mockPath.toFile())
            ), times(1));
        }
    }

    @Test
    @DisplayName("ensureValidWav: Creates placeholder WAV if file is empty (using AudioSystem)")
    void ensureValidWav_createsPlaceholder_ifEmpty() throws Exception {
        // GIVEN
        Path mockPath = Paths.get("/output/empty.wav");

        try (MockedStatic<Files> mockedFiles = Mockito.mockStatic(Files.class);
             MockedStatic<AudioSystem> mockedAudioSystem = Mockito.mockStatic(AudioSystem.class)) {

            mockedFiles.when(() -> Files.exists(mockPath)).thenReturn(true);
            mockedFiles.when(() -> Files.size(mockPath)).thenReturn(0L);

            mockedAudioSystem.when(() -> AudioSystem.write(
                    any(javax.sound.sampled.AudioInputStream.class),
                    any(javax.sound.sampled.AudioFileFormat.Type.class),
                    any(java.io.File.class)
            )).thenReturn(44100 * 2);

            // WHEN
            fileManager.ensureValidWav(mockPath);

            // THEN
            mockedAudioSystem.verify(() -> AudioSystem.write(
                    any(), eq(javax.sound.sampled.AudioFileFormat.Type.WAVE), eq(mockPath.toFile())
            ), times(1));
        }
    }
}