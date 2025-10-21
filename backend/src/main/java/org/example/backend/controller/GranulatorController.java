package org.example.backend.controller;

import org.example.backend.service.GranulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
     * Endpoint for uploading an ORC and SCO file and starting a granulation process.
     * Both files are written to secure temporary files and processed asynchronously.
     *
     * @param orcFile uploaded Csound orchestra file
     * @param scoFile uploaded Csound score file
     * @return simple status message ("Granulation started!")
     * @throws IOException if file creation or writing fails
     */

    @PostMapping("/perform")
    public String performGranulation(
            @RequestParam("orc") MultipartFile orcFile,
            @RequestParam("sco") MultipartFile scoFile
    ) throws IOException {

        // Checking whether granulation is currently running
        if (granulatorService.isRunning()) {
            logger.info("Granulation request rejected: already running.");
            return "Granulation already running!";
        }

        // 🟢 Use system temp directory, which is OS-specific (e.g. /tmp on Unix, AppData on Windows)
        String tempDir = System.getProperty("java.io.tmpdir");

        // 🟢 Create temporary files with safe, unique names
        Path tempOrc = Files.createTempFile(Path.of(tempDir), "granular-", ".orc");
        Path tempSco = Files.createTempFile(Path.of(tempDir), "granular-", ".sco");

        // 🟢 Log file creation for easier debugging and traceability
        logger.info("Temporary ORC file created at {}", tempOrc);
        logger.info("Temporary SCO file created at {}", tempSco);

        // 🟢 Write uploaded bytes directly to the temporary files
        //    This avoids keeping large files in memory unnecessarily
        Files.write(tempOrc, orcFile.getBytes());
        Files.write(tempSco, scoFile.getBytes());

        // 🟢 Run the Csound process asynchronously to avoid blocking the HTTP request thread
        new Thread(() -> {
            try {
                String result = granulatorService.performGranulationOnce(tempOrc, tempSco, true);
                logger.info("Granulation result: {}", result);
            } catch (IOException e) {
                // 🟢 Use proper logging instead of printStackTrace for better observability
                logger.error("Granulation failed", e);
            } finally {
                // 🟢 Always clean up temporary files
                try {
                    Files.deleteIfExists(tempOrc);
                    Files.deleteIfExists(tempSco);
                } catch (IOException e) {
                    logger.warn("Could not delete temp files", e);
                }
            }

        }).start();

        return "Granulation started!";
    }
}
