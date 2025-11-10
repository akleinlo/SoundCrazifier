import GranulatorPlayer from "../components/GranulatorPlayer";

export default function CrazifyPage() {
    return (
        <div className="page-wide" style={{paddingTop: "3rem", paddingBottom: "4rem"}}>
            <h1 style={{textAlign: "center", marginBottom: "3rem", fontSize: "3rem", color: "#61dafb"}}>
                CRAZIFY YOUR SOUND
            </h1>

            <div className="gui-section" style={{maxWidth: '700px', margin: '0 auto'}}>
                <GranulatorPlayer/>
            </div>

            <div style={{
                textAlign: "center",
                marginTop: "4rem",
                color: "#aaa",
                maxWidth: "600px",
                marginLeft: "auto",
                marginRight: "auto",
                lineHeight: 1.6,
                padding: '1.5rem',
                border: '1px solid rgba(255, 255, 255, 0.1)',
                borderRadius: '8px'
            }}>
                <p style={{marginBottom: "1rem", color: '#fff'}}>
                    Supported formats: <strong>MP3</strong>, <strong>WAV</strong>, and <strong>AIFF</strong>.
                </p>
                <p style={{marginBottom: 0, fontSize: "0.85rem", color: "#aaa"}}>
                    The file extension determines the output format. Downloads will go to your default folder.
                </p>
            </div>
        </div>
    );
}