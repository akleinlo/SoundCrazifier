package org.example.backend.service;

import csnd6.Csound;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class GranulatorService {

    /**
     * Performs granular synthesis for an ORC and SCO file.
     *
     * @param orcPath     Path to the ORC file
     * @param scoPath     PPath to the SCO file
     * @param outputLive  true = live playback, false = Render to WAV
     * @throws IOException
     */
    public void performGranulation(Path orcPath, Path scoPath, boolean outputLive) throws IOException {
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
                // läuft bis Ende des SCO
            }

        } finally {
            // Clean up, even if errors occur
            csound.Stop();
            csound.Reset();
            csound.Cleanup();
        }
    }
}
