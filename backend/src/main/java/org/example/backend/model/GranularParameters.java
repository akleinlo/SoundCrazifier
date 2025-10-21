package org.example.backend.model;

public record GranularParameters(

        // Amplitude
        double iampMin,
        double iampMax,
        double kampCpsMin,
        double kampCpsMax,

        // Pitch
        double ipitchMin,
        double ipitchMax,
        double kpitchCpsMin,
        double kpitchCpsMax,

        // Density
        double idensMin,
        double idensMax,
        double kdensCpsMin,
        double kdensCpsMax,

        // Amplitude Offset
        double kampoffMin,
        double kampoffMax,
        double kampoffCpsMin,
        double kampoffCpsMax,

        // Pitch Offset
        double kpitchoffMin,
        double kpitchoffMax,
        double kpitchoffCpsMin,
        double kpitchoffCpsMax,

        // Grain Duration
        double kgdurMin,
        double kgdurMax,
        double kgdurCpsMin,
        double kgdurCpsMax,

        // File references and derived parameters
        String inputFile,   // Path or ID of the audio file (corresponds to igfn)
        String waveTable,   // e.g. "giSine"
        double imaxGrainDur // e.g. 2 (corresponds to imgdur = kgdur * 2)
) {}

