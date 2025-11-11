import GranulatorPlayer from "../components/GranulatorPlayer";
import "../css/CrazifyPage.css";

export default function CrazifyPage() {
    return (
        <div className="page-wide">
            <h1 className="pageTitle">
                CRAZIFY YOUR SOUND
            </h1>

            <div className="gui-section">
                <GranulatorPlayer/>
            </div>

            <div className="supportInfoBox">
                <p className="supportInfoText">
                    Supported formats: <strong>MP3</strong>, <strong>WAV</strong>, and <strong>AIFF</strong>.
                </p>
                <p className="supportInfoDetails">
                    The file extension determines the output format. Downloads will go to your default folder.
                </p>
            </div>
        </div>
    );
}