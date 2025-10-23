package org.example.backend.service;

import csnd6.Csound;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class GranulatorService {

    private volatile boolean isRunning = false;

    public boolean isRunning() {
        return isRunning;
    }

    public synchronized String performGranulationOnce(Path orcPath, Path scoPath, boolean outputLive, Path outputPath) throws IOException {
        if (isRunning) {
            return "Granulation already running!";
        }

        isRunning = true;
        try {
            performGranulation(orcPath, scoPath, outputLive, outputPath);
        } finally {
            isRunning = false;
        }

        return "Granulation finished!";
    }

    protected void performGranulation(Path orcPath, Path scoPath, boolean outputLive, Path outputPath) throws IOException {
        // Determine secure output path
        Path effectiveOutput = outputPath != null
                ? outputPath.toAbsolutePath()
                : Files.createTempFile("granulated-", ".wav");

        System.out.println("Effective output path: " + effectiveOutput);

        if (effectiveOutput.getParent() != null) {
            Files.createDirectories(effectiveOutput.getParent());
        }

        Csound csound = new Csound();

        try {
            // Import files
            String orc = Files.readString(orcPath);
            String sco = Files.readString(scoPath);

            // Output mode
            if (outputLive) {
                csound.SetOption("-odac");
            } else {
                csound.SetOption("-o" + effectiveOutput);
            }

            System.out.println("--- ORC content ---");
            System.out.println(orc);
            System.out.println("--- SCO content ---");
            System.out.println(sco);


            // Prepare and run Csound
            csound.CompileOrc(orc);
            csound.ReadScore(sco);
            csound.Start();

            while (csound.PerformKsmps() == 0) {
                // Runs until end
            }

            System.out.println("Granulation complete.");

        } finally {
            csound.Stop();
            csound.Reset();
            csound.Cleanup();
        }
    }
}
