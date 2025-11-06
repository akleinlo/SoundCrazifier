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

---

## ⚙️ Technology Stack

| Component | Technology | Role |
|-----------|-----------|------|
| Backend Framework | Spring Boot 3.x (Java 17/21) | RESTful API, Service Management, Dependency Injection |
| Audio Processing Core | Csound 6.18+ | Primary synthesis and granulation engine (`org.csound:csound`) |
| File Utility | SoX (Sound eXchange) | External command-line utility for mandatory audio file resampling (44.1 kHz / 96 kHz) |
| Frontend | React 19+, TypeScript, Vite | SPA, API interaction via axios, client-side file handling |
| Build & Testing | Maven, JaCoCo, Mockito | Dependency management, testing, and coverage analysis |

---

## 💻 Frontend Overview

The client application is a modern single-page application (SPA) built with React and TypeScript.

### Key Features

- **Client-Side Duration**: Duration of uploaded files calculated on the client using HTML5 Audio API.
- **Crazification Level**: Users select an intensity level (1-10) to choose the corresponding Csound Score file (`granularX.sco`).
- **Real-time Playback (`/play`)**: Streams audio through the server’s output device.
- **Offline Save (`/save`)**: Generates a high-quality `.wav` file for download (96 kHz).
- **Process Management**: Stop button terminates running Csound or SoX processes gracefully.

### Application Structure

- `/` → Home (Welcome page)
- `/info` → Info (About the project, developer, usage notes)
- `/crazify` → Crazify (Main player component: `GranulatorPlayer`)

---

## 🛠️ System Prerequisites

- **Csound 6.18+**: Native binaries required for audio processing.
- **SoX (Sound eXchange)**: Resamples user audio files to required sample rate (44.1 kHz live, 96 kHz offline).

The application's `application.properties` specifies a macOS-specific path for SoX:

```properties
audio.sox.path=/opt/homebrew/bin/sox
```

For correct execution in Linux environments (e.g., Ubuntu, Debian, CentOS), this setting must be overridden by the environment variable `AUDIO_SOX_PATH`:

```bash
export AUDIO_SOX_PATH="/usr/bin/sox"
```

### Java Dependency Note

The Csound Java API (`org.csound:csound`) is resolved via a custom Maven repository (specified in `pom.xml`), which requires GitHub package access for successful build.

## 🚀 Getting Started (Local Setup)

To run the full application, you need to start both the Backend and the Frontend simultaneously.

### 1. Requirements

- Java SDK (17 or higher)
- Apache Maven
- Csound (installed and runnable)
- SoX (installed and runnable)
- Node.js and npm (or yarn/pnpm)

### 2. Backend Setup (Spring Boot)

**Clone the Repository:**

```bash
git clone [repository-url]
cd backend
```

**Build the Project:**  
Ensure you have configured Maven access to the required GitHub package repository if necessary.

```bash
mvn clean install
```

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

### Csound Library Path Warning (macOS/Linux)

If you encounter issues initializing the Csound Java API (e.g., `UnsatisfiedLinkError`), you may need to explicitly set the path to your native Csound library files. This is often handled by setting the `DYLD_LIBRARY_PATH` (macOS) or `LD_LIBRARY_PATH` (Linux) environment variable before running the application.

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