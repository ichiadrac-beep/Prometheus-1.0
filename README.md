# Prometheus — 100% Offline AI

A minimal Android app that runs large language models entirely on-device using llama.cpp (via the prebuilt `llama-android` AAR). After the model is loaded, **no internet is required**.

## Features
- Fully offline inference
- Loads any GGUF model the user selects
- Simple chat interface
- System prompt oriented toward practical / survival / DIY advice

## Build the APK

1. Open the project in **Android Studio** (Hedgehog or newer recommended).
2. Let Gradle sync (it will download the `dev.ffmpegkit-maintained:llama-android:0.1.1` dependency).
3. Connect an Android device or start an emulator (arm64-v8a).
4. Build → Build Bundle(s) / APK(s) → Build APK(s).

The debug APK will be at:  
`app/build/outputs/apk/debug/app-debug.apk`

## Usage

1. Install the APK.
2. Download a small GGUF model while you still have internet (examples):
   - Qwen2.5-1.5B-Instruct-Q4_K_M.gguf (~1 GB)
   - Phi-3-mini-4k-instruct-q4.gguf
   - Gemma-2-2b-it-Q4_K_M.gguf
3. Tap **Load Model** and select the `.gguf` file.
4. Chat. Everything runs on the phone.

## Notes
- Minimum Android 7.0 (API 24).
- arm64-v8a only (most modern phones).
- Free version of the AAR does not stream tokens; responses appear after generation completes.
- Model must fit in available RAM (prefer 1–3 B models on 6–8 GB phones).
- For production, consider adding proper model management, streaming (Pro AAR), and a knowledge base.

## License
App code: MIT.  
llama.cpp and the AAR: MIT.
