package org.example.backend.controller;

import org.example.backend.service.GranulatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/granulator")
public class GranulatorController {

    @Autowired
    private GranulatorService granulatorService;

    /**
     * A simple endpoint for uploading an ORC and SCO file and starting granulation.
     */
    @PostMapping("/perform")
    public String performGranulation(
            @RequestParam("orc") MultipartFile orcFile,
            @RequestParam("sco") MultipartFile scoFile
    ) throws IOException {

        //  // Write data immediately to temporary files
        Path tempOrc = Files.createTempFile("temp", ".orc");
        Path tempSco = Files.createTempFile("temp", ".sco");

        Files.write(tempOrc, orcFile.getBytes());
        Files.write(tempSco, scoFile.getBytes());

        // Background thread for playback
        new Thread(() -> {
            try {
                granulatorService.performGranulation(tempOrc, tempSco, false);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    Files.deleteIfExists(tempOrc);
                    Files.deleteIfExists(tempSco);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return "Granulation started!";
    }








}
