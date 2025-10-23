import React, { useState } from "react";
import axios from "axios";

export default function GranulatorPlayer() {
    const [file, setFile] = useState<File | null>(null);
    const [filename, setFilename] = useState(""); // Name für den Download

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const firstFile = e.target.files?.[0];
        if (firstFile) {
            setFile(firstFile);
            setFilename(firstFile.name.replace(".wav", "-granulated.wav"));
        }
    };

    const handlePlay = async () => {
        if (!file) return;
        const formData = new FormData();
        formData.append("audioFile", file);

        try {
            await axios.post("http://localhost:8080/granulator/play", formData, {
                headers: { "Content-Type": "multipart/form-data" },
            });
        } catch (err) {
            console.error("Error playing file:", err);
        }
    };

    const handleSave = async () => {
        if (!file) return;
        const formData = new FormData();
        formData.append("audioFile", file);

        try {
            const response = await axios.post(
                "http://localhost:8080/granulator/save",
                formData,
                {
                    headers: { "Content-Type": "multipart/form-data" },
                    responseType: "blob",
                    params: { outputPath: filename }, // Backend nimmt den Dateinamen
                }
            );

            const url = URL.createObjectURL(response.data);
            const link = document.createElement("a");
            link.href = url;
            link.download = filename; // hier wird der eingegebene Name verwendet
            link.click();
        } catch (err) {
            console.error("Error saving file:", err);
        }
    };

    return (
        <div style={{ padding: "2rem", fontFamily: "sans-serif", display: "flex", flexDirection: "column", alignItems: "center" }}>
            <h2>Play your crazified Soundfile!</h2>

            {/* Choose File */}
            <div style={{ marginBottom: "1rem" }}>
                <label htmlFor="fileInput" style={{ cursor: "pointer", padding: "0.5rem 1rem", backgroundColor: "#000", color: "#fff", borderRadius: "4px", fontWeight: "bold" }}>
                    Choose File
                </label>
                <input
                    id="fileInput"
                    type="file"
                    accept="audio/wav"
                    onChange={handleFileChange}
                    style={{ display: "none" }}
                />
                {file && <span style={{ marginLeft: "1rem" }}>{file.name}</span>}
            </div>

            {/* Play Button */}
            <button onClick={handlePlay} style={{ padding: "0.5rem 1rem", backgroundColor: "#000", color: "#fff", border: "none", borderRadius: "4px", cursor: "pointer", fontWeight: "bold", marginBottom: "1rem" }}>
                Play
            </button>

            {/* Save Button */}
            <button onClick={handleSave} style={{ padding: "0.5rem 1rem", backgroundColor: "#000", color: "#fff", border: "none", borderRadius: "4px", cursor: "pointer", fontWeight: "bold", marginBottom: "1rem" }}>
                Save
            </button>

            {/* Save as: input field */}
            {file && (
                <div style={{ marginBottom: "1rem" }}>
                    <label>
                        Save as:{" "}
                        <input
                            type="text"
                            value={filename}
                            onChange={(e) => setFilename(e.target.value)}
                            style={{ padding: "0.25rem", width: "250px" }}
                        />
                    </label>
                </div>
            )}

            {/* Save-as note */}
            <div style={{
                fontSize: "0.9rem",
                color: "#555",
                textAlign: "justify",
                textAlignLast: "center",
                maxWidth: "400px",
                marginTop: "1rem"
            }}>
                <strong>Save as…</strong><br />
                By default, the granulated file will be saved in your Downloads folder.
                You can configure your browser to ask for the save location before downloading:
                <br />
                <em>Safari/Chrome:</em> Settings → Downloads → "Ask for each download"
            </div>
        </div>
    );

}
