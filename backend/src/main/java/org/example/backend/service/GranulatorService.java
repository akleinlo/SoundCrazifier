package org.example.backend.service;

import csnd6.Csound;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class GranulatorService {

    // Flag indicating whether granulation is currently running
    private volatile boolean isRunning = false;

    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Performs granular synthesis for an ORC and SCO file.
     * Only allows one granulation at a time.
     *
     * @param orcPath    Path to the ORC file
     * @param scoPath    Path to the SCO file
     * @param outputLive true = live playback, false = Render to WAV
     * @return a message indicating whether granulation started or is already running
     * @throws IOException if reading the ORC or SCO file fails
     */

    public synchronized String performGranulationOnce(Path orcPath, Path scoPath, boolean outputLive) throws IOException {
        if (isRunning) {
            return "Granulation already running!";
        }

        isRunning = true;
        try {
            performGranulation(orcPath, scoPath, outputLive);
        } finally {
            isRunning = false;
        }

        return "Granulation finished!";
    }

    /**
     * Internal method that actually performs the granulation.
     */
    private void performGranulation(Path orcPath, Path scoPath, boolean outputLive) throws IOException {
        Csound csound = new Csound();

        try {
            // import files
            String orc = Files.readString(orcPath);
            String sco = Files.readString(scoPath);

            // DAC or WAV Output
            if (outputLive) {
                csound.SetOption("-odac2"); // Live playback
            } else {
                csound.SetOption("-o granulated.wav"); // Render to WAV
            }

            // prepare Csound
            csound.CompileOrc(orc);
            csound.ReadScore(sco);
            csound.Start();

            // play SCO
            while (csound.PerformKsmps() == 0) {
                // runs until the end of the SCO
            }

        } finally {
            // Clean up, even if errors occur
            csound.Stop();
            csound.Reset();
            csound.Cleanup();
        }
    }
}
