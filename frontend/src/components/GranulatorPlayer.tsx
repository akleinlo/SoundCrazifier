import React, {useState} from "react";

import axios from "axios";

export default function GranulatorPlayer() {
    const [file, setFile] = useState<File | null>(null);
    const [, setAudioUrl] = useState<string | null>(null);

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            setFile(e.target.files[0]);
        }
    };

    const handleUpload = async () => {
        if (!file) return;

        const formData = new FormData();
        formData.append("audioFile", file);

        try {
            const response = await axios.post("http://localhost:8080/granulator/upload", formData, {
                headers: {"Content-Type": "multipart/form-data"},
                responseType: "blob",
            });

            setAudioUrl(URL.createObjectURL(response.data));
        } catch (err) {
            console.error("Error uploading file:", err);
        }
    };

    return (
        <div
            style={{
                padding: "2rem",
                fontFamily: "sans-serif",
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
            }}
        >
            <h2>Play your crazified Soundfile!</h2>

            {/* File chooser */}
            <div style={{marginBottom: "1rem"}}>
                <label
                    htmlFor="fileInput"
                    style={{
                        cursor: "pointer",
                        padding: "0.5rem 1rem",
                        backgroundColor: "#000",
                        color: "#fff",
                        borderRadius: "4px",
                        fontWeight: "bold",
                    }}
                >
                    Choose File
                </label>
                <input
                    id="fileInput"
                    type="file"
                    accept="audio/wav"
                    onChange={handleFileChange}
                    style={{display: "none"}}
                />
                {file && <span style={{marginLeft: "1rem"}}>{file.name}</span>}
            </div>

            {/* Play button */}
            <button
                onClick={handleUpload}
                style={{
                    padding: "0.5rem 1rem",
                    backgroundColor: "#000",
                    color: "#fff",
                    border: "none",
                    borderRadius: "4px",
                    cursor: "pointer",
                    fontWeight: "bold",
                }}
            >
                Play
            </button>
        </div>
    );
}
