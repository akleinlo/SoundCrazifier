import './App.css'
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import GranulatorPage from "./page/GranulatorPage";


export default function App() {
  return (
    <>
        <Router>
            <Routes>
                <Route path="/" element={<GranulatorPage />} />
            </Routes>
        </Router>

    </>
  )
}

