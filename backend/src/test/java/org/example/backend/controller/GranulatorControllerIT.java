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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

    @Test
    void testPlayEndpoint() throws Exception {
        Path filePath = Path.of("src/test/resources/testfiles/small.wav");
        MockMultipartFile file = new MockMultipartFile(
                "audioFile",
                "small.wav",
                "audio/wav",
                Files.readAllBytes(filePath)
        );

        mockMvc.perform(multipart("/granulator/play")
                        .file(file)
                        .param("duration", "5.0")) // hier Dauer mitgeben
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

        mockMvc.perform(multipart("/granulator/save")
                        .file(file)
                        .param("duration", "5.0")) // hier Dauer mitgeben
                .andExpect(status().isOk());
    }
}
