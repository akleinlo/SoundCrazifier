package org.example.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class GranulatorControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGranulateWithEmptyFile() throws Exception {
        Path filePath = Path.of("src/test/resources/testfiles/empty.wav");
        MockMultipartFile file = new MockMultipartFile(
                "audioFile",
                "empty.wav",
                "audio/wav",
                Files.readAllBytes(filePath)
        );

        mockMvc.perform(multipart("/granulator/upload")
                        .file(file))
                .andExpect(status().isOk());
    }

    @Test
    void testGranulateWithSmallFile() throws Exception {
        Path filePath = Path.of("src/test/resources/testfiles/small.wav");
        MockMultipartFile file = new MockMultipartFile(
                "audioFile",
                "small.wav",
                "audio/wav",
                Files.readAllBytes(filePath)
        );

        mockMvc.perform(multipart("/granulator/upload")
                        .file(file))
                .andExpect(status().isOk());
    }
}
