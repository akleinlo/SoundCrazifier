package org.example.backend.controller;

import org.example.backend.service.GranulatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
    void testStopEndpoint_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/crazifier/stop")) // POST Request zu /stop
                .andExpect(status().isOk());

        Mockito.verify(granulatorService, Mockito.times(1)).stopGranulation();
    }
}
