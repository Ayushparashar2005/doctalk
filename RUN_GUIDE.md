# DocTalk Run Guide

## Android App

1. Open the project in Android Studio.
2. Sync Gradle.
3. Start an Android emulator or connect a device.
4. Run the app.

## Local Backend

1. Copy `backend/.env.example` to `backend/.env`.
2. Add your Groq API key.
3. Install backend dependencies with `pip install -r requirements.txt`.
4. Start the backend with `uvicorn main:app --reload --host 0.0.0.0 --port 8000`.

## Emulator Connectivity

- The Android app uses `http://10.0.2.2:8000/api/` so the emulator can reach the local backend.
- If you use a physical device, replace that URL with your machine's LAN IP.

## Quick Check

1. Verify the backend at `http://localhost:8000/health`.
2. Launch the app on the emulator.
3. Upload a document and start a chat.
