package org.example.backend.controller;

import org.example.backend.service.GranulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/granulator")
public class GranulatorController {

    private static final Logger logger = LoggerFactory.getLogger(GranulatorController.class);
    private final GranulatorService granulatorService;

    public GranulatorController(GranulatorService granulatorService) {

        this.granulatorService = granulatorService;
    }

    /**
     * Endpoint: Nimmt eine Audio-Datei, ersetzt im Score den Pfad,
     * und startet die Granulation mit dem festen ORC.
     */
    @PostMapping("/upload")
    public String uploadAndGranulate(@RequestParam("audioFile") MultipartFile audioFile) throws IOException {

        if (granulatorService.isRunning()) {
            logger.info("Granulation request rejected: already running.");
            return "Granulation already running!";
        }

        // 🟢 Use system temp directory
        String tempDir = System.getProperty("java.io.tmpdir");

        // 🟢 Create temp files
        Path tempAudio = Files.createTempFile(Path.of(tempDir), "audio-", ".wav");
        Path tempSco = Files.createTempFile(Path.of(tempDir), "granular-", ".sco");

        // 🟢 Save uploaded audio file
        Files.write(tempAudio, audioFile.getBytes());
        logger.info("Temporary audio file created at {}", tempAudio);

        // 🟢 Load score template from resources
        var scoTemplate = new ClassPathResource("csound/granular.sco");
        String scoContent = Files.readString(scoTemplate.getFile().toPath(), StandardCharsets.UTF_8);

        // 🟢 Replace f-table line dynamically
        // Example in template: f1 0 1048576 1 "REPLACE_ME" 0 0 0
        String replaced = scoContent.replace("\"REPLACE_ME\"", "\"" + tempAudio.toAbsolutePath() + "\"");

        // 🟢 Write modified SCO
        Files.writeString(tempSco, replaced, StandardCharsets.UTF_8);
        logger.info("Temporary SCO file created at {}", tempSco);

        // 🟢 Fixed ORC file in resources
        var orcResource = new ClassPathResource("csound/granular.orc");
        Path orcPath = orcResource.getFile().toPath();

        // 🟢 Run asynchronously
        new Thread(() -> {
            try {
                String result = granulatorService.performGranulationOnce(orcPath, tempSco, true);
                logger.info("Granulation result: {}", result);
            } catch (IOException e) {
                logger.error("Granulation failed", e);
            } finally {
                try {
                    Files.deleteIfExists(tempAudio);
                    Files.deleteIfExists(tempSco);
                } catch (IOException e) {
                    logger.warn("Could not delete temp files", e);
                }
            }
        }).start();

        return "Granulation started!";
    }
}
