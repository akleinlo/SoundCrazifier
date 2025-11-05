import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Navbar from "./components/Navbar";
import HomePage from "./page/HomePage";
import InfoPage from "./page/InfoPage";
import CrazifyPage from "./page/CrazifyPage";

export default function App() {
    return (
        <Router>
            <Navbar />
            <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/info" element={<InfoPage />} />
                <Route path="/crazify" element={<CrazifyPage />} />
            </Routes>
        </Router>
    );
}
