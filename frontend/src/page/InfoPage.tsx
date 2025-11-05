export default function InfoPage() {
    return (
        <div
            style={{
                maxWidth: "800px",
                margin: "5rem auto",
                lineHeight: 1.7,
                textAlign: "justify",
                textAlignLast: "center",
                padding: "0 1.5rem",
            }}
        >
            {/* Titel */}
            <h1 style={{ textAlign: "center", marginBottom: "2.5rem" }}>About Sound Crazifier</h1>

            {/* Projektbeschreibung */}
            <p style={{ marginBottom: "1.5rem" }}>
                Sound Crazifier is an experimental tool for granular
                sound manipulation. Upload an audio file, select your
                crazification level, play the result live, or save
                it as a new file.
            </p>
            <p style={{ marginBottom: "2rem" }}>
                Designed for composers, sound researchers, and anyone
                who finds normal sound too boring.
            </p>

            {/* Usage Notes */}
            <h2 style={{ textAlign: "center", marginTop: "3rem", marginBottom: "1rem" }}>Usage Notes</h2>
            <ul style={{ paddingLeft: "1.2rem", marginBottom: "2.5rem" }}>
                <li>Only one user can play or save crazified sound files at a time.</li>
                <li>Sound files longer than 1 minute cannot be crazified.</li>
                <li>A cooldown mechanism prevents rapid consecutive clicks on “Play” and “Stop”.</li>
                <li>
                    Explore the project and source code on <a href="https://github.com/akleinlo/SoundCrazifier" target="_blank" rel="noopener noreferrer">GitHub</a>.
                </li>
            </ul>

            {/* Über dich */}
            <h2 style={{ textAlign: "center", marginTop: "2rem", marginBottom: "1rem" }}>About the Developer</h2>
            <p style={{ marginBottom: "1rem" }}>
                Developed by Adrian Kleinlosen, a composer and sound researcher
                passionate about experimental audio tools and granular sound manipulation.
            </p>
            <p style={{ marginBottom: "1rem" }}>
                Visit my <a href="https://adriankleinlosen.com" target="_blank" rel="noopener noreferrer">website</a> or follow me on <a href="https://www.instagram.com/adriankleinlosen/" target="_blank" rel="noopener noreferrer">Instagram</a>.
            </p>

            {/* Ko-fi Button */}
            <div style={{ textAlign: "center", marginTop: "2rem" }}>
                <a
                    href="https://ko-fi.com/adriankleinlosen"
                    target="_blank"
                    rel="noopener noreferrer"
                    style={{
                        display: "inline-block",
                        padding: "0.6em 1.2em",
                        fontSize: "1rem",
                        borderRadius: "8px",
                        backgroundColor: "#29abe0",
                        color: "#fff",
                        textDecoration: "none",
                        fontWeight: "bold",
                        transition: "transform 0.2s",
                    }}
                    onMouseOver={(e) => (e.currentTarget.style.transform = "scale(1.05)")}
                    onMouseOut={(e) => (e.currentTarget.style.transform = "scale(1)")}
                >
                    Support me on Ko-fi ☕
                </a>
            </div>
        </div>
    );
}
