import React, {useState, useEffect, useRef} from "react";
import axios from "axios";
import styles from "../css/GranulatorPlayer.module.css";

const MAX_ABSOLUTE_DURATION = 60;

const FileIcon = () => <span>📁</span>;
const PlayIcon = () => <span>▶️</span>;
const SaveIcon = () => <span>💾</span>;
const StopIcon = () => <span>🛑</span>;

export default function GranulatorPlayer() {
    const [file, setFile] = useState<File | null>(null);
    const [filename, setFilename] = useState("");

    const [fileDuration, setFileDuration] = useState(0);
    const [newDurationInput, setNewDurationInput] = useState(0);
    const [cSoundDuration, setCSoundDuration] = useState(0);

    const [isCrazifying, setIsCrazifying] = useState(false);
    const [crazifyLevel, setCrazifyLevel] = useState(5);
    const [durationError, setDurationError] = useState<string | null>(null);

    const playTimeoutRef = useRef<number | null>(null);

    const maxAllowedInput = MAX_ABSOLUTE_DURATION;

    const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const firstFile = e.target.files?.[0];
        if (!firstFile) return;

        if (e.target.files) {
            e.target.value = '';
        }

        setFile(firstFile);
        setFilename(firstFile.name.replace(/\.[^/.]+$/, "") + "-crazified.wav");

        setFileDuration(0);
        setCSoundDuration(0);
        setNewDurationInput(0);
        setDurationError(null);

        const formData = new FormData();
        formData.append("audioFile", firstFile);

        try {
            const response = await axios.post("http://localhost:8080/crazifier/getDuration", formData);
            const durationFromBackend = response.data as number;
            const fileDuration = parseFloat(durationFromBackend.toFixed(3));

            if (fileDuration > MAX_ABSOLUTE_DURATION) {
                setDurationError(
                    `File too long (${fileDuration.toFixed(2)}s). Max. ${MAX_ABSOLUTE_DURATION}s allowed.`
                );
                setFileDuration(fileDuration);
                setCSoundDuration(0);
                setNewDurationInput(fileDuration);
                return;
            }

            if (fileDuration <= 0) {
                setDurationError("Error: Could not read a valid duration from the file.");
                setFileDuration(0);
                setCSoundDuration(0);
                setNewDurationInput(0);
                return;
            }

            setDurationError(null);
            setFileDuration(fileDuration);
            setCSoundDuration(fileDuration);
            setNewDurationInput(fileDuration);

        } catch (err: unknown) {
            console.error("Error getting file duration:", err);

            let errorMessage = "Unknown error reading duration (network or backend).";

            if (axios.isAxiosError(err) && err.response) {
                const data = err.response.data;
                if (typeof data === 'string') {
                    errorMessage = `Duration reading error: ${data}`;
                } else if (err.response.status) {
                    errorMessage = `Duration reading error (Status: ${err.response.status})`;
                }
            } else if (err instanceof Error) {
                errorMessage = `Network/connection error: ${err.message}`;
            }

            setDurationError(errorMessage);
            setFileDuration(0);
            setCSoundDuration(0);
            setNewDurationInput(0);
        }
    };

    const handleUpdateDuration = () => {
        const desiredInput = parseFloat(newDurationInput.toFixed(3));
        const validatedDuration = Math.min(Math.max(1, desiredInput), maxAllowedInput);

        setDurationError(null);

        setNewDurationInput(validatedDuration);
        setCSoundDuration(validatedDuration);
    };

    const handlePlay = async () => {
        if (!file || cSoundDuration <= 0 || durationError !== null) return;

        if (playTimeoutRef.current) {
            clearTimeout(playTimeoutRef.current);
            playTimeoutRef.current = null;
        }

        const formData = new FormData();
        formData.append("audioFile", file);
        formData.append("duration", cSoundDuration.toString());
        formData.append("crazifyLevel", crazifyLevel.toString());

        setIsCrazifying(true);
        try {
            await axios.post("http://localhost:8080/crazifier/play", formData, {
                headers: {"Content-Type": "multipart/form-data"},
            });

            playTimeoutRef.current = window.setTimeout(() => {
                setIsCrazifying(false);
                playTimeoutRef.current = null;
            }, cSoundDuration * 1000) as unknown as number;

        } catch (err) {
            console.error("Error playing file:", err);
            setIsCrazifying(false);
            playTimeoutRef.current = null;
        }
    };

    const handleSave = async () => {
        if (!file || cSoundDuration <= 0 || durationError !== null) return;

        const formData = new FormData();
        formData.append("audioFile", file);
        formData.append("duration", cSoundDuration.toString());
        formData.append("crazifyLevel", crazifyLevel.toString());

        setIsCrazifying(true);
        try {
            const response = await axios.post(
                "http://localhost:8080/crazifier/save",
                formData,
                {
                    headers: {"Content-Type": "multipart/form-data"},
                    responseType: "blob",
                    params: {outputPath: filename},
                }
            );
            const url = URL.createObjectURL(response.data);
            const link = document.createElement("a");
            link.href = url;
            link.download = filename;
            link.click();
        } catch (err) {
            console.error("Error saving file:", err);
        } finally {
            setIsCrazifying(false);
        }
    };

    const handleStop = async () => {
        if (!file) return;

        if (playTimeoutRef.current) {
            clearTimeout(playTimeoutRef.current);
            playTimeoutRef.current = null;
        }

        try {
            await axios.post("http://localhost:8080/crazifier/stop");
            console.log("Crazification stopped.");
        } catch (err) {
            console.error("Error stopping granulation:", err);
        } finally {
            setIsCrazifying(false);
        }
    };

    const isInputInvalid = newDurationInput <= 0 || newDurationInput > maxAllowedInput;

    useEffect(() => {
        return () => {
            if (playTimeoutRef.current) {
                clearTimeout(playTimeoutRef.current);
            }
            if (file) {
                axios.post("http://localhost:8080/crazifier/stop").catch(err => {
                    console.error("Error stopping granulation on unmount:", err);
                });
            }
        };
    }, [file]);

    return (
        <div className={styles.playerContainer}>
            <div className={styles.fileSection}>
                <label htmlFor="fileInput" className={styles.fileLabel}>
                    <FileIcon/> {file ? "Change File" : "Upload Audio File"}
                </label>
                <input
                    id="fileInput"
                    type="file"
                    accept="audio/*"
                    onChange={handleFileChange}
                    style={{display: "none"}}
                />
                <span className={styles.fileStatus}>
                    {file ? `${file.name} (Length: ${fileDuration.toFixed(2)}s)` : "No file selected."}
                </span>
            </div>

            <div className={styles.controlsGrid}>
                <div className={styles.controlGroup}>
                    <label className={styles.controlLabel}>
                        Crazify Level: <span>{crazifyLevel}</span>
                    </label>
                    <input
                        type="range"
                        min="1"
                        max="10"
                        value={crazifyLevel}
                        onChange={(e) =>
                            setCrazifyLevel(parseInt(e.target.value))
                        }
                        className={styles.levelSlider}
                    />
                </div>

                {file && (
                    <div className={styles.controlGroup}>
                        <label className={styles.controlLabel}>
                            Duration (Max Input {maxAllowedInput}s):
                        </label>
                        <div className={styles.durationInputGroup}>
                            <input
                                type="number"
                                min={1}
                                max={maxAllowedInput}
                                step={0.01}
                                value={newDurationInput}
                                onChange={(e) => {
                                    const val = parseFloat(e.target.value);
                                    setNewDurationInput(isNaN(val) ? 0 : val);
                                }}
                                className={styles.durationInput}
                                disabled={durationError !== null}
                            />
                            <button
                                onClick={handleUpdateDuration}
                                disabled={isInputInvalid || cSoundDuration === newDurationInput || durationError !== null}
                                className={styles.updateButton}
                            >
                                SET
                            </button>
                        </div>
                        {durationError && (
                            <span className={`${styles.warningText} ${styles.errorText}`}>
                                **Error:** {durationError}
                            </span>
                        )}
                        {isInputInvalid && !durationError && (
                            <span className={styles.warningText}>
                                **Error:** Max. duration is {maxAllowedInput}s.
                            </span>
                        )}

                    </div>
                )}
            </div>

            <div className={styles.actionButtons}>
                <button
                    onClick={handlePlay}
                    className={styles.playButton}
                    disabled={!file || isCrazifying || cSoundDuration <= 0 || durationError !== null}
                >
                    <PlayIcon/> PLAY LIVE
                </button>
                <button
                    onClick={handleSave}
                    className={styles.saveButton}
                    disabled={!file || isCrazifying || cSoundDuration <= 0 || durationError !== null}
                >
                    <SaveIcon/> SAVE OFFLINE
                </button>
                <button
                    onClick={handleStop}
                    className={styles.stopButton}
                    disabled={!isCrazifying}
                >
                    <StopIcon/> STOP
                </button>
            </div>

            <div className={styles.outputSection}>
                {isCrazifying ? (
                    <div className={styles.crazifyingContainer}>
                        <div className={styles.crazifyingSpinner}/>
                        <span>CRAZIFYING: {cSoundDuration.toFixed(2)}s</span>
                    </div>
                ) : (
                    file && (
                        <div className={styles.filenameGroup}>
                            <label className={styles.controlLabel}>
                                Output File:
                            </label>
                            <input
                                type="text"
                                value={filename}
                                onChange={(e) =>
                                    setFilename(e.target.value)
                                }
                                className={styles.filenameInput}
                            />
                        </div>
                    )
                )}
            </div>

            {file && (
                <div className={styles.infoBox}>
                    <p>
                        **Current Performance Duration:**{" "}
                        **{cSoundDuration.toFixed(3)}s** (File Length: {fileDuration.toFixed(
                        3
                    )}s / Absolute Max: {MAX_ABSOLUTE_DURATION}s)
                        <br/>
                    </p>
                </div>
            )}
        </div>
    );
}