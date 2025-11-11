import "../css/InfoPage.css";

export default function InfoPage() {
    return (
        <div className="infoPageContainer">
            <h1 className="infoTitle">About Sound Crazifier</h1>
            <p className="infoParagraph">
                Sound Crazifier is an experimental tool for granular
                sound manipulation. Upload an audio file, select your
                crazification level, play the result live, or save
                it as a new file.
            </p>
            <p className="infoParagraph">
                Designed for composers, sound researchers, and anyone
                who finds normal sound too boring.
            </p>

            <h2 className="infoSubtitle">Usage Notes</h2>
            <ul className="infoList">
                <li>Only one user can play or save crazified sound files at a time.</li>
                <li>Sound files longer than 1 minute cannot be crazified.</li>
                <li>A cooldown mechanism prevents rapid consecutive clicks on “Play” and “Stop”.</li>
                <li>
                    Explore the project and source code on{" "}
                    <a
                        href="https://github.com/akleinlo/SoundCrazifier"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="infoLink"
                    >
                        GitHub
                    </a>.
                </li>
            </ul>

            <h2 className="infoSubtitle">About the Developer</h2>
            <p className="infoParagraph">
                Developed by Adrian Kleinlosen, a composer and sound researcher
                passionate about experimental audio tools and granular sound manipulation.
            </p>
            <p className="infoParagraph">
                Listen to my{" "}
                <a
                    href="https://soundcloud.com/adriankleinlosen"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="infoLink"
                >
                    music
                </a>{" "}
                or follow me on{" "}
                <a
                    href="https://www.instagram.com/adriankleinlosen/"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="infoLink"
                >
                    Instagram
                </a>.
            </p>

            <div className="infoKofiButtonContainer">
                <a
                    href="https://ko-fi.com/adriankleinlosen"
                    target="_blank"
                    rel="noopener noreferrer"
                    className="infoKofiButton"
                >
                    Support me on Ko-fi ☕
                </a>
            </div>
        </div>
    );
}