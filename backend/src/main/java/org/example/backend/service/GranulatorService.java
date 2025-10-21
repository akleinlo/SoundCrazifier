package org.example.backend.service;

import csnd6.Csound;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class GranulatorService {

    /**
     * Führt die Granular-Synthese für eine ORC- und SCO-Datei durch.
     *
     * @param orcPath Pfad zur ORC-Datei
     * @param scoPath Pfad zur SCO-Datei
     * @param outputLive true = live playback, false = Render zu WAV
     * @throws IOException
     */
    public void performGranulation(Path orcPath, Path scoPath, boolean outputLive) throws IOException {
        Csound c = new Csound();

        // Dateien einlesen
        String orc = Files.readString(orcPath);
        String sco = Files.readString(scoPath);

        // DAC oder WAV Output
        if (outputLive) {
            c.SetOption("-odac2");          // live playback
        } else {
            c.SetOption("-o granulated.wav"); // als WAV rendern
        }

        // Csound vorbereiten
        c.CompileOrc(orc);
        c.ReadScore(sco);
        c.Start();

        while (c.PerformKsmps() == 0) {
            // läuft bis Ende des SCO
        }

        c.Stop();
        c.Cleanup();
    }
}
