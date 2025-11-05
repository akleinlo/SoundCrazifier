import { Link } from "react-router-dom";
import "../css/Navbar.css";

export default function Navbar() {
    return (
        <nav className="navbar">
            <Link to="/">Home</Link>
            <Link to="/info">Info</Link>
            <Link to="/crazify">Crazify</Link>
        </nav>
    );
}
