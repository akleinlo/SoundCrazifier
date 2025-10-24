import './App.css';
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import GranulatorPage from "./page/GranulatorPage";

export default function App() {
    return (
        <>
            <h1>Sound Crazifier</h1>
            <h2>Crazify your Soundfiles!</h2>
            <div className="min-h-screen bg-gradient-to-b from-gray-50 to-gray-100 flex flex-col items-center justify-center p-6">
                <Router>
                    <Routes>
                        <Route path="/" element={<GranulatorPage />} />
                    </Routes>
                </Router>

            </div>
        </>
    );
}
