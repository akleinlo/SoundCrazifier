package org.example.backend.controller;

import org.example.backend.service.GranulatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GranulatorControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GranulatorService granulatorService;

    @BeforeEach
    void setup() throws IOException {
        Mockito.doAnswer(invocation -> {
            System.out.println("Mock granulation called");
            return "mocked result";
        }).when(granulatorService).performGranulationOnce(
                Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any()
        );
    }

    private MockMultipartFile createMockFile() throws IOException {
        Path filePath = Path.of("src/test/resources/testfiles/small.wav");
        return new MockMultipartFile(
                "audioFile",
                "small.wav",
                "audio/wav",
                Files.readAllBytes(filePath)
        );
    }

    // =================================================================
    // PLAY ENDPOINT TESTS (/crazifier/play)
    // =================================================================
    @Test
    void testPlayEndpoint() throws Exception {
        Path filePath = Path.of("src/test/resources/testfiles/small.wav");
        MockMultipartFile file = new MockMultipartFile(
                "audioFile",
                "small.wav",
                "audio/wav",
                Files.readAllBytes(filePath)
        );

        mockMvc.perform(multipart("/crazifier/play")
                        .file(file)
                        .param("duration", "5.0")
                        .param("crazifyLevel", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void testPlayEndpoint_durationMax_ok() throws Exception {
        MockMultipartFile file = createMockFile();

        mockMvc.perform(multipart("/crazifier/play")
                        .file(file)
                        .param("duration", "60.0")
                        .param("crazifyLevel", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void testPlayEndpoint_crazifyLevelBounds() throws Exception {
        MockMultipartFile file = createMockFile();

        // Level 0 → should be coerced to 1
        mockMvc.perform(multipart("/crazifier/play")
                        .file(file)
                        .param("duration", "5.0")
                        .param("crazifyLevel", "0"))
                .andExpect(status().isOk());

        // Level 15 → should be coerced to 10
        mockMvc.perform(multipart("/crazifier/play")
                        .file(file)
                        .param("duration", "5.0")
                        .param("crazifyLevel", "15"))
                .andExpect(status().isOk());
    }

    @Test
    void testPlayEndpoint_durationTooLong_returnsBadRequest() throws Exception {
        MockMultipartFile file = createMockFile();

        mockMvc.perform(multipart("/crazifier/play")
                        .file(file)
                        .param("duration", "60.1")
                        .param("crazifyLevel", "5"))
                .andExpect(status().isBadRequest()); // Erwartet 400
    }

    @Test
    void testPlayEndpoint_alreadyRunning_returnsConflict() throws Exception {
        MockMultipartFile file = createMockFile();

        when(granulatorService.isRunning()).thenReturn(true);

        mockMvc.perform(multipart("/crazifier/play")
                        .file(file)
                        .param("duration", "5.0")
                        .param("crazifyLevel", "5"))
                .andExpect(status().isConflict()); // Erwartet 409
    }

    @Test
    void testPlayEndpoint_maliciousFilename_handlesSafely() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "audioFile",
                "../../../etc/passwd.wav",  // Path traversal attempt
                "audio/wav",
                Files.readAllBytes(Path.of("src/test/resources/testfiles/small.wav"))
        );

        mockMvc.perform(multipart("/crazifier/play")
                        .file(file)
                        .param("duration", "5.0")
                        .param("crazifyLevel", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void testPlayEndpoint_invalidExtension_stillWorks() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "audioFile",
                "audio.exe",
                "audio/wav",
                Files.readAllBytes(Path.of("src/test/resources/testfiles/small.wav"))
        );

        mockMvc.perform(multipart("/crazifier/play")
                        .file(file)
                        .param("duration", "5.0")
                        .param("crazifyLevel", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void testPlayEndpoint_durationZero() throws Exception {
        MockMultipartFile file = createMockFile();

        mockMvc.perform(multipart("/crazifier/play")
                        .file(file)
                        .param("duration", "0.0")
                        .param("crazifyLevel", "5"))
                .andExpect(status().isOk()); // oder isBadRequest(), je nach Logik
    }

    // =================================================================
    // SAVE ENDPOINT TESTS (/crazifier/save)
    // =================================================================
    @Test
    void testSaveEndpoint() throws Exception {
        Path filePath = Path.of("src/test/resources/testfiles/small.wav");
        MockMultipartFile file = new MockMultipartFile(
                "audioFile",
                "small.wav",
                "audio/wav",
                Files.readAllBytes(filePath)
        );

        mockMvc.perform(multipart("/crazifier/save")
                        .file(file)
                        .param("duration", "5.0")
                        .param("crazifyLevel", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void testSaveEndpoint_specialCharactersInFilename() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "audioFile",
                "audio file!@#$%^&*().wav",
                "audio/wav",
                Files.readAllBytes(Path.of("src/test/resources/testfiles/small.wav"))
        );

        mockMvc.perform(multipart("/crazifier/save")
                        .file(file)
                        .param("duration", "5.0")
                        .param("crazifyLevel", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void testSaveEndpoint_durationTooLong_returnsBadRequest() throws Exception {
        MockMultipartFile file = createMockFile();

        mockMvc.perform(multipart("/crazifier/save")
                        .file(file)
                        .param("duration", "61.0")
                        .param("crazifyLevel", "5"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testSaveEndpoint_checksDownloadHeaders() throws Exception {
        MockMultipartFile file = createMockFile();

        mockMvc.perform(multipart("/crazifier/save")
                        .file(file)
                        .param("duration", "5.0")
                        .param("crazifyLevel", "5"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("-crazified.wav")));
    }

    // =================================================================
    // GET DURATION ENDPOINT TESTS (/crazifier/getDuration)
    // =================================================================
    @Test
    void testGetDuration_happyPath_returnsOk() throws Exception {
        MockMultipartFile file = createMockFile();

        mockMvc.perform(multipart("/crazifier/getDuration")
                        .file(file))
                .andExpect(status().isOk());
    }

    @Test
    void testGetDuration_fileWithoutExtension_returnsOk() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "audioFile",
                "audiofile",
                "audio/wav",
                Files.readAllBytes(Path.of("src/test/resources/testfiles/small.wav"))
        );

        mockMvc.perform(multipart("/crazifier/getDuration")
                        .file(file))
                .andExpect(status().isOk());
    }

    // =================================================================
    // STOP ENDPOINT TESTS (/crazifier/stop)
    // =================================================================
    @Test
    void testStopEndpoint_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/crazifier/stop"))
                .andExpect(status().isOk());

        Mockito.verify(granulatorService, Mockito.times(1)).stopGranulation();
    }
}
