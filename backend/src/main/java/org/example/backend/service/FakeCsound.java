package org.example.backend.service;

/**
 * Mock class for Csound, used exclusively for testing purposes
 * (e.g., in GranulatorService). Methods are implemented as no-ops
 * or simple returns to simulate success without running the Csound engine.
 */
public class FakeCsound {
    public void setOption(String option) {
        // Mock implementation: Does nothing.
    }

    public void compileOrc(String orc) {
        // Mock implementation: Does nothing.
    }

    public void readScore(String sco) {
        // Mock implementation: Does nothing.
    }

    public void start() {
        // Mock implementation: Start is a no-op, as no actual audio engine is running.
    }

    public int performKsmps() {
        // Mock implementation: Immediately returns 1 to simulate completion/stop for tests.
        return 1;
    }

    public void stop() {
        // Mock implementation: Does nothing.
    }

    public void reset() {
        // Mock implementation: Does nothing.
    }

    public void cleanup() {
        // Mock implementation: Does nothing.
    }
}