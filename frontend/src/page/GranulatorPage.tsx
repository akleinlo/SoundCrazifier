import GranulatorPlayer from "../components/GranulatorPlayer";

export default function GranulatorPage() {
    return (
        <div className="p-4 flex flex-col items-center">
            {/* Player + Duration Input */}
            <GranulatorPlayer />

            {/* Info bar */}
            <div className="mt-6 w-full max-w-lg backdrop-blur-sm bg-white/70 border border-white/40 shadow-md rounded-2xl p-5 text-center">
                <p className="text-gray-700 text-sm">
                    Supported formats: <strong>MP3</strong>, <strong>WAV</strong>, and <strong>AIFF</strong>.
                </p>
                <p className="text-gray-700 text-sm">
                    You can save your <strong>crazified</strong> sound as MP3, WAV, or AIFF.
                </p>
                <p className="text-gray-600 text-xs mt-2">
                    The file extension determines the output format.
                </p>
            </div>
        </div>
    );
}
