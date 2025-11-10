<p align="center">
  <img src="frontend/src/assets/logo.png" alt="SoundCrazifier Logo" width="1000"/>
</p>

<h2 align="center">Audio Granulation Application with Csound</h2>

## 📊 Code Quality & CI

<p align="center">
  <a href="https://github.com/akleinlo/SoundCrazifier/actions">
    <img src="https://github.com/akleinlo/SoundCrazifier/actions/workflows/maven.yml/badge.svg" alt="Build Status" height="25"/>
  </a>&nbsp;
  <a href="https://sonarcloud.io/summary/new_code?id=akleinlo_SoundCrazifier_backend">
    <img src="https://sonarcloud.io/api/project_badges/measure?project=akleinlo_SoundCrazifier_backend&metric=coverage" alt="Coverage" height="25"/>
  </a>&nbsp;
  <a href="https://sonarcloud.io/summary/new_code?id=akleinlo_SoundCrazifier_backend">
    <img src="https://sonarcloud.io/api/project_badges/measure?project=akleinlo_SoundCrazifier_backend&metric=alert_status" alt="Quality Gate Status" height="25"/>
  </a>&nbsp;
  <a href="https://sonarcloud.io/summary/new_code?id=akleinlo_SoundCrazifier_backend">
    <img src="https://sonarcloud.io/api/project_badges/measure?project=akleinlo_SoundCrazifier_backend&metric=sqale_rating" alt="Maintainability Rating" height="25"/>
  </a>&nbsp;
  <a href="https://sonarcloud.io/summary/new_code?id=akleinlo_SoundCrazifier_backend">
    <img src="https://sonarcloud.io/api/project_badges/measure?project=akleinlo_SoundCrazifier_backend&metric=reliability_rating" alt="Reliability Rating" height="25"/>
  </a>&nbsp;
  <a href="https://sonarcloud.io/summary/new_code?id=akleinlo_SoundCrazifier_backend">
    <img src="https://sonarcloud.io/api/project_badges/measure?project=akleinlo_SoundCrazifier_backend&metric=security_rating" alt="Security Rating" height="25"/>
  </a>&nbsp;
  <a href="https://sonarcloud.io/summary/new_code?id=akleinlo_SoundCrazifier_backend">
    <img src="https://sonarcloud.io/api/project_badges/measure?project=akleinlo_SoundCrazifier_backend&metric=vulnerabilities" alt="Vulnerabilities" height="25"/>
  </a>
</p>

---

This repository contains the **SoundCrazifier application**, including a Spring Boot backend for audio processing and a React frontend for user interaction. The app processes uploaded audio files using **Csound** and **SoX** to create granular, "crazified" audio outputs.

It provides a robust REST API for both **live audio playback** and **offline file generation**, dynamically selecting Csound orchestrations based on the requested "crazify level."

## ⚙️ Technology Stack

| Component | Technology | Role |
|-----------|-----------|------|
| Backend Framework | Spring Boot 3.x (Java 17/21) | RESTful API, Service Management, Dependency Injection |
| Audio Processing Core | Csound 6.18+ | Primary synthesis and granulation engine (via `org.csound:csound` Java API) |
| File Utility | SoX (Sound eXchange) | External command-line utility for mandatory audio file resampling (to 44.1 kHz or 96 kHz) |
| Frontend | React 19+, TypeScript, Vite | User Interface, API interaction via axios, client-side file handling, and modern tooling |
| Build & Testing | Maven, JaCoCo, Mockito | Dependency management, testing, and coverage analysis |

## 💻 Frontend Overview: UI and Interaction

The client application is a modern, single-page application (SPA) built with React and TypeScript.

### Key Features

- **Client-Side Duration**: The duration of the uploaded file is calculated client-side upon file selection using the HTML5 Audio API.
- **Crazification Level**: Users select an intensity level (1-10) which dynamically selects the corresponding Csound Score file (`granularX.sco`) on the server.
- **Real-time Playback** (`/play`): Sends the audio file and parameters to the server for live processing, utilizing the server's audio device.
- **Offline Save** (`/save`): Triggers a high-quality (96 kHz) offline render of the granulated sound, returned as a `.wav` blob for direct download.
- **Process Management**: The Stop button allows graceful termination of any currently running Csound or SoX process on the server.

### Application Structure

The frontend utilizes `react-router-dom` for navigation between three main views:

- `/`: Home (Welcome page)
- `/info`: Info (About the project, developer, and usage notes)
- `/crazify`: Crazify (The main player component, GranulatorPlayer)

## 🛠️ System Prerequisites

This application relies on two crucial external command-line tools.

### ⚠️ Csound Version Compatibility Issue

**CRITICAL:** The included Java Csound API (`csound.jar`) has compatibility issues with Homebrew's Csound **6.18.1_12** (revision 12) due to a `RAWADDF` environment variable bug introduced in recent Homebrew builds.

**You MUST use Csound 6.18.1_11** (Homebrew revision 11) for this project to work correctly.

#### macOS (Homebrew) - Install Legacy Version

1. **Uninstall current version** (if already installed):
   ```bash
   brew uninstall csound
   ```

2. **Install from legacy tap**:
   ```bash
   brew tap akleinlo/csound-legacy
   brew install akleinlo/csound-legacy/csound
   ```

3. **Verify installation**:
   ```bash
   csound --version
   ```

   You should see:
   ```
   --Csound version 6.18 (double samples) Nov 23 2022
   [commit: a1580f9cdf331c35dceb486f4231871ce0b00266]
   ```

**Background:** The legacy tap repository is available at [github.com/akleinlo/homebrew-csound-legacy](https://github.com/akleinlo/homebrew-csound-legacy).

#### Linux (Ubuntu/Debian)

```bash
sudo apt-get update
sudo apt-get install csound libcsnd-dev
```

⚠️ Ensure the installed version is compatible with the Java API (version 6.18.x recommended).

#### Windows / Manual

Download from [csound.com/download](https://csound.com/download) and ensure you are using a compatible version (6.18.x) for Java API projects.

### SoX (Sound eXchange)

**Installation:**

- **macOS (Homebrew):**
  ```bash
  brew install sox
  ```
- **Linux (Ubuntu/Debian):**
  ```bash
  sudo apt-get install sox libsox-fmt-all
  ```
- **Windows:**  
  Download from [sox.sourceforge.net](http://sox.sourceforge.net/)

**Verify Installation:**
```bash
csound --version
sox --version
```

### ❗ Critical SoX Configuration Note

The application's `application.properties` specifies a macOS-specific path for SoX:

```properties
audio.sox.path=/opt/homebrew/bin/sox
```

For correct execution in Linux environments (e.g., Ubuntu, Debian, CentOS), this setting must be overridden by the environment variable `AUDIO_SOX_PATH`:

```bash
export AUDIO_SOX_PATH="/usr/bin/sox"
```

## 📦 Maven Dependency Setup

The Csound Java API (`org.csound:csound`) is not available in Maven Central and is included in `backend/lib/csound.jar`.

### Automatic Installation (Recommended)

The project's `pom.xml` is configured to automatically install `csound.jar` to your local Maven repository during the build process.

Simply run:

```bash
cd backend
mvn clean install
```

The JAR will be automatically installed to `~/.m2/repository/org/csound/csound/6.18.1/`.

### Manual Installation (If Needed)

If automatic installation fails or you prefer manual installation:

```bash
mvn install:install-file \
  -Dfile=backend/lib/csound.jar \
  -DgroupId=org.csound \
  -DartifactId=csound \
  -Dversion=6.18.1 \
  -Dpackaging=jar
```

### Verify Installation

```bash
ls ~/.m2/repository/org/csound/csound/6.18.1/
```

You should see `csound-6.18.1.jar` and `csound-6.18.1.pom`.

**Note:** This is a **one-time setup** step. Once installed, Maven will use the cached version for all subsequent builds.

## 🚀 Getting Started (Local Setup)

To run the full application, you need to start both the Backend and the Frontend simultaneously.

### 1. Requirements

- Java SDK (17 or higher)
- Apache Maven
- Csound 6.18.1_11 (installed and runnable)
- SoX (installed and runnable)
- Node.js and npm (or yarn/pnpm)

### 2. Backend Setup (Spring Boot)

**Clone the Repository:**

```bash
git clone https://github.com/akleinlo/SoundCrazifier.git
cd SoundCrazifier/backend
```

**Build the Project:**

```bash
mvn clean install
```

This will automatically install the Csound JAR to your local Maven repository.

**Run the Backend:**

The application runs on the default Spring Boot port (8080).

```bash
mvn spring-boot:run
```

### 3. Frontend Setup (React/Vite)

**Install Dependencies:**

Navigate to the frontend directory and install the Node modules.

```bash
cd ../frontend
npm install
```

**Run the Frontend:**

The development server runs on `http://localhost:5173` (as seen in the `application.properties` CORS setting).

```bash
npm run dev
```

### 🔧 Csound Library Path Configuration (macOS/Linux)

If you encounter issues initializing the Csound Java API (e.g., `UnsatisfiedLinkError`), you need to explicitly set the path to your native Csound library files.

#### 1. Locate Your Csound Library Path

**macOS (Homebrew):**

The recommended stable path (survives updates):
```bash
/opt/homebrew/opt/csound/libexec
```

Or the specific version path:
```bash
/opt/homebrew/Cellar/csound/6.18.1_11/libexec
```

**Linux (Ubuntu/Debian):**
```bash
/usr/lib/csound
# or
/usr/local/lib/csound
```

**To find your actual path:**

macOS (Homebrew):
```bash
# Check Csound installation
brew list csound | grep libexec

# Or check the stable symlink
ls -la /opt/homebrew/opt/csound/libexec
```

Linux:
```bash
# Find Csound libraries
find /usr -name "*csound*.so" 2>/dev/null

# Or use ldconfig
ldconfig -p | grep csound
```

#### 2. IntelliJ Run/Debug Configuration

To run the Spring Boot backend in IntelliJ IDEA:

- **Name**: `Backend` or `BackendApplication`
- **Build and run using**: Java `temurin-17`
- **VM Options**:
  ```
  -Djava.library.path=/opt/homebrew/Cellar/csound/6.18.1_11/libexec
  ```
- **Environment Variables** (macOS):
  ```
  DYLD_LIBRARY_PATH=/opt/homebrew/Cellar/csound/6.18.1_11/libexec;RAWADDF=1
  ```
- **Environment Variables** (Linux):
  ```
  LD_LIBRARY_PATH=/usr/local/lib/csound;RAWADDF=1
  ```

⚠️ **Important:** The `RAWADDF=1` environment variable is **required** to work around the Homebrew Csound 6.18.1_11 compatibility issue with large audio files.

#### 3. Running via Terminal / Maven

**macOS:**
```bash
export DYLD_LIBRARY_PATH=/opt/homebrew/Cellar/csound/6.18.1_11/libexec
export RAWADDF=1
mvn spring-boot:run
```

Or with VM options:
```bash
export RAWADDF=1
mvn spring-boot:run -Djava.library.path=/opt/homebrew/Cellar/csound/6.18.1_11/libexec
```

**Linux:**
```bash
export LD_LIBRARY_PATH=/usr/local/lib/csound
export RAWADDF=1
mvn spring-boot:run
```

⚠️ **Note**: Replace the paths above with the actual installation path of your Csound native libraries.

## 🔗 API Endpoints

The backend is configured to accept CORS requests from `http://localhost:5173` (the typical Frontend development environment). All endpoints are prefixed with `/crazifier`.

### 1. Start Live Playback

Initiates a Csound performance that plays the granulated audio directly through the server's audio output. Since the output is real-time, no file is returned.

- **Endpoint**: `POST /crazifier/play`
- **Media Type**: `multipart/form-data`

**Parameters:**

| Parameter | Type | Required | Description | Constraints |
|-----------|------|----------|-------------|-------------|
| `audioFile` | MultipartFile | Yes | The user-uploaded audio file (e.g., `.wav`, `.mp3`) | Must be less than 60 seconds long |
| `duration` | double | Yes | The duration of the granulation performance in seconds | Max 60.0 seconds |
| `crazifyLevel` | int | Yes | Level of intensity (1-10) corresponding to a specific Csound Score (`granularX.sco`) | 1 to 10 |

**Status Codes:**

- `200 OK`: Crazification process successfully started
- `400 BAD_REQUEST`: Duration exceeds 60 seconds
- `409 CONFLICT`: Granulation process is already running

### 2. Save Offline Audio File

Initiates a Csound performance that renders the output to a high-quality `.wav` file, which is then served as a download to the client.

- **Endpoint**: `POST /crazifier/save`
- **Media Type**: `multipart/form-data`
- **Parameters**: Same as `/play`
- **Response**: `ResponseEntity<Resource>` (Download stream)

**Status Codes:**

- `200 OK`: File successfully generated and returned
- `400 BAD_REQUEST`: Duration exceeds 60 seconds

### 3. Stop Running Process

Gracefully terminates any currently running Csound or SoX process.

- **Endpoint**: `POST /crazifier/stop`
- **Response**: String message confirming the stop

## 🧪 Testing

The project uses Maven with JUnit 5 and Mockito for unit testing, and includes integration tests with Spring Boot Test.

To run the full test suite:

```bash
mvn test
```

The test suite ensures 100% code coverage for all critical services, including robust error handling for native process execution (Csound, SoX) and filesystem operations.