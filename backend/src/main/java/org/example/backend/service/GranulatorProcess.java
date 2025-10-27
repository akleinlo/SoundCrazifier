package org.example.backend.service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GranulatorProcess {

    public static Process startCsound(Path orcPath, Path scoPath, Path outputPath, boolean live) throws IOException {
        List<String> command = new ArrayList<>();
        command.add("csound");

        if (live) {
            command.add("-odac");          // Live-Wiedergabe
        } else {
            command.add("-o" + outputPath.toAbsolutePath());  // Output-Datei
            command.add("--format=float");
            command.add("-r96000");
            command.add("-b512");          // kleiner Puffer
            command.add("-B2048");         // I/O-Puffer
        }

        command.add(orcPath.toAbsolutePath().toString());
        command.add(scoPath.toAbsolutePath().toString());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO(); // Optional: Csound-Output direkt in Console
        return pb.start();
    }
}

