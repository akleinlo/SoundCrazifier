import logo from "../assets/logo.png";
import "../css/HomePage.css";

export default function HomePage() {
    return (
        <div className="homePageContainer">

            <img
                src={logo}
                alt="Sound Crazifier"
                className="homePageLogo"
            />
            <div className="homePageText">
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