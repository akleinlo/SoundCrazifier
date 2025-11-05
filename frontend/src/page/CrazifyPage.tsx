import GranulatorPlayer from "../components/GranulatorPlayer";

export default function CrazifyPage() {
    return (
        <div className="page-wide" style={{ paddingTop: "3rem", paddingBottom: "4rem" }}>
            {/* Überschrift */}
            <h1 style={{
                textAlign: "center",
                marginBottom: "2.5rem",
                fontSize: "3rem"
            }}>
                Crazify Your Sound
            </h1>

            {/* GUI Block */}
            <div className="gui-section">
                <GranulatorPlayer />
            </div>

            {/* Info Bar direkt unter dem GUI Block */}
            <div style={{
                textAlign: "center",
                marginTop: "2.5rem",
                color: "#ccc",
                maxWidth: "600px",
                marginLeft: "auto",
                marginRight: "auto",
                lineHeight: 1.6
            }}>
                <p style={{ marginBottom: "1rem" }}>
                    Supported formats: <strong>MP3</strong>, <strong>WAV</strong>, and <strong>AIFF</strong>.
                </p>
                <p style={{ marginBottom: "1rem" }}>
                    You can save your <strong>crazified</strong> sound as MP3, WAV, or AIFF.
                </p>
                <p style={{ marginBottom: 0, fontSize: "0.85rem", color: "#aaa" }}>
                    The file extension determines the output format. Downloads will go to your default folder.
                </p>
            </div>
        </div>
    );
}
