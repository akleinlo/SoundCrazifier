package org.example.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Diese Klasse kümmert sich ausschließlich um die Audio-Konvertierung
 * mithilfe von SoX. Ziel: einheitliche WAV-Dateien mit 96 kHz und 32-bit float.
 *
 * Beispielaufruf:
 * Path converted = soxConverter.convertToStandardFormat(originalPath);
 */
@Service
public class SoXConverter {

    private static final Logger logger = LoggerFactory.getLogger(SoXConverter.class);

    /**
     * Wandelt eine gegebene Audio-Datei in das Standardformat um:
     * WAV, 96 kHz, 32-bit float.
     *
     * @param inputPath Pfad zur Originaldatei (beliebiges Format, z. B. mp3, wav, aiff)
     * @return Pfad zur konvertierten Datei
     * @throws IOException wenn SoX fehlschlägt oder Datei nicht geschrieben werden kann
     */
    public Path convertToStandardFormat(Path inputPath) throws IOException {
        if (!Files.exists(inputPath)) {
            throw new IOException("Input-Datei existiert nicht: " + inputPath);
        }

        Path outputPath = inputPath.getParent().resolve(
                "converted_" + inputPath.getFileName().toString().replaceAll("\\..+$", "") + ".wav"
        );

        String[] command = {
                "sox",
                inputPath.toAbsolutePath().toString(),
                outputPath.toAbsolutePath().toString(),
                "-r", "96000",
                "-b", "32",
                "-e", "float"
        };

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("[SoX] {}", line);
            }
        }

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("SoX-Konvertierung fehlgeschlagen, Exit-Code: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("SoX-Prozess wurde unterbrochen", e);
        }

        logger.info("SoX-Konvertierung abgeschlossen: {}", outputPath);
        return outputPath;
    }

}
