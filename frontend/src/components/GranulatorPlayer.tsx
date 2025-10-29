import React, {useState} from "react";
import axios from "axios";
import styles from "../css/GranulatorPlayer.module.css";


export default function GranulatorPlayer() {
    const [file, setFile] = useState<File | null>(null);
    const [filename, setFilename] = useState("");
    const [duration, setDuration] = useState(20); // default 20s
    const [newDuration, setNewDuration] = useState(duration);
    const [isCrazifying, setIsCrazifying] = useState(false);


    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const firstFile = e.target.files?.[0];
        if (firstFile) {
            setFile(firstFile);
            setFilename(firstFile.name.replace(".wav", "-crazified.wav"));

            // Dauer des Files ermitteln
            const audio = new Audio(URL.createObjectURL(firstFile));
            audio.addEventListener("loadedmetadata", () => {
                const fileDuration = parseFloat(audio.duration.toFixed(3));
                setDuration(fileDuration);
                setNewDuration(fileDuration);
            });
        }
    };

    const handlePlay = async () => {
        if (!file) return;
        const formData = new FormData();
        formData.append("audioFile", file);
        formData.append("duration", duration.toString());

        setIsCrazifying(true);
        try {
            await axios.post("http://localhost:8080/crazifier/play", formData, {
                headers: {"Content-Type": "multipart/form-data"},
            });
        } catch (err) {
            console.error("Error playing file:", err);
        } finally {
            setIsCrazifying(false);
        }
    };

    const handleSave = async () => {
        if (!file) return;
        const formData = new FormData();
        formData.append("audioFile", file);
        formData.append("duration", duration.toString());

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

        setIsCrazifying(false); // turn off spinner immediately
        try {
            await axios.post("http://localhost:8080/crazifier/stop");
            console.log("Crazification stopped.");
        } catch (err) {
            console.error("Error stopping granulation:", err);
        }
    };


    return (
        <div style={{
            padding: "2rem",
            fontFamily: "sans-serif",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: "1.5rem"
        }}>
            {/* Choose File */}
            <div>
                <label htmlFor="fileInput" style={{
                    cursor: "pointer",
                    padding: "0.5rem 1rem",
                    backgroundColor: "#000",
                    color: "#fff",
                    borderRadius: "4px",
                    fontWeight: "bold"
                }}>
                    Choose File
                </label>
                <input
                    id="fileInput"
                    type="file"
                    accept="audio/*"
                    onChange={handleFileChange}
                    style={{display: "none"}}
                />
                {file && <span style={{marginLeft: "1rem"}}>{file.name}</span>}
            </div>

            {/* Duration + Update Button */}
            {file && (
                <div style={{display: "flex", alignItems: "center", gap: "0.5rem"}}>
                    <label>
                        Duration (seconds):
                        <input
                            type="number"
                            min={1}
                            step={0.01}
                            value={newDuration}
                            onChange={(e) => setNewDuration(parseFloat(e.target.value.replace(",", ".")))}
                            style={{marginLeft: "0.5rem", padding: "0.25rem", width: "80px"}}
                        />
                    </label>
                    <button
                        onClick={() => setDuration(newDuration)}
                        style={{
                            padding: "0.25rem 0.5rem",
                            backgroundColor: "#333",
                            color: "#fff",
                            borderRadius: "4px",
                            cursor: "pointer"
                        }}
                    >
                        Update
                    </button>
                </div>
            )}

            {/* Play, Save, Stop + Spinner */}
            <div style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                <button
                    onClick={handlePlay}
                    style={{
                        padding: "0.5rem 1rem",
                        backgroundColor: "#000",
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: "pointer",
                        fontWeight: "bold"
                    }}
                >
                    Play
                </button>

                <button
                    onClick={handleSave}
                    style={{
                        padding: "0.5rem 1rem",
                        backgroundColor: "#000",
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: "pointer",
                        fontWeight: "bold"
                    }}
                >
                    Save
                </button>

                <button
                    onClick={handleStop}
                    style={{
                        padding: "0.5rem 1rem",
                        backgroundColor: "#900",
                        color: "#fff",
                        border: "none",
                        borderRadius: "4px",
                        cursor: "pointer",
                        fontWeight: "bold"
                    }}
                >
                    Stop
                </button>

                {isCrazifying && (
                    <div className={styles.crazifyingContainer}>
                        <div className={styles.crazifyingSpinner} />
                        <span>Crazifying...</span>
                    </div>
                )}
            </div>

            {/* Save as input */}
            {file && (
                <div>
                    <label>
                        Save as:{" "}
                        <input
                            type="text"
                            value={filename}
                            onChange={(e) => setFilename(e.target.value)}
                            style={{padding: "0.25rem", width: "250px"}}
                        />
                    </label>
                </div>
            )}

            {/* Browser info */}
            <div style={{
                fontSize: "0.9rem",
                color: "#555",
                textAlign: "justify",
                textAlignLast: "center",
                maxWidth: "400px",
                marginTop: "1rem"
            }}>
                <strong>Save as…</strong><br/>
                By default, the crazified file will be saved in your Downloads folder.<br/>
                You can configure your browser to ask for the save location before downloading:<br/>
                <em>Safari/Chrome:</em> Settings → Downloads → "Ask where to save each file before downloading"
            </div>
        </div>
    );

}
