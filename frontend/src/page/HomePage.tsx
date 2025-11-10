import logo from "../assets/logo.png";

export default function HomePage() {
    return (
        <div
            style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "center",
                height: "100vh",
                padding: "1.25rem",
                textAlign: "center",
                gap: "1.1rem",
                boxSizing: "border-box",
                background: "linear-gradient(180deg, #1a1a2e, #3b3b5e)",
                color: "#ffffff",
            }}
        >
            <img
                src={logo}
                alt="Sound Crazifier"
                style={{maxWidth: "42.5%", height: "auto"}}
            />

            <div
                style={{
                    maxWidth: "650px",
                    fontSize: "1.1rem",
                    lineHeight: 1.5,
                }}
            >
                <p>
                    Welcome to <strong>Sound Crazifier</strong> – your tool for experimental granular sound
                    manipulation.
                </p>
                <p>
                    Upload an audio file, adjust the crazification level, and play or save your new sound!
                </p>
            </div>
        </div>
    );
}
