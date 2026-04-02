# Aeris

> Making sound visible.

Aeris is a real-time assistive Android application for deaf and hard-of-hearing individuals. It continuously listens to the environment using on-device AI and converts critical sounds into haptic alerts, visual notifications, and live captions - privately, offline, and instantly.

---

## What It Does

### Always-On Sound Awareness
Aeris runs persistently in the background, detecting five critical sound categories in real time:
- Alarms (fire alarm, smoke detector)
- Sirens (ambulance, police, civil defense)
- Horns (car horn, air horn)
- Doorbells and knocks
- Human voice and speech

Every detection triggers an immediate haptic pattern and an on-screen notification - even when the phone is locked.

### Intelligent Haptic Alerts
Each sound type has a distinct vibration signature. A siren fires a rapid triple pulse. A doorbell triggers a gentle double tap. Users know what they're being alerted to without looking at the screen.

### Adjustable Sensitivity
Detection thresholds can be tuned independently per sound category - reducing false positives in noisy environments while staying sensitive to what matters.

### AI Conversation Co-pilot
A dedicated screen for two-way assisted communication.
- Incoming speech is transcribed in real time using on-device speech-to-text
- The on-device LLM reads conversation context and suggests natural replies
- Users tap a suggestion or type their own
- Aeris speaks the response aloud via text-to-speech

### Sleep Mode
Aeris stays active overnight. It wakes the user the moment a critical sound is detected - alarm, siren, baby cry or knock - without any manual setup.

### Fully On-Device, Zero Cloud
Every model - sound classifier, STT, LLM, TTS - runs locally on the device. No audio, no text, no personal data ever leaves the phone.

---

## Tech Stack

| Component | Technology |
|---|---|
| Sound Classification | YAMNet via TensorFlow Lite |
| Speech-to-Text | Whisper Tiny via Sherpa-ONNX |
| Reply Suggestions | SmolLM2 via LlamaCPP |
| Text-to-Speech | Piper TTS via Sherpa-ONNX |
| Platform | Android (Kotlin) |
| ML Runtimes | ONNX Runtime, LlamaCPP, TFLite |
| Haptics | Android Vibrator / VibrationEffect API |

---

## Architecture
```
Microphone Input
      ↓
Audio Pipeline (16kHz, mono, sliding window)
      ↓
YAMNet TFLite Model (on-device)
      ↓
Sound Classification + Confidence Score
      ↓
Alert Engine → Haptic Pattern + Visual Notification
      ↓
(If voice detected) → STT → Transcript
      ↓
LLM → Reply Suggestions
      ↓
User Response → TTS → Spoken Aloud
```

---

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android device running API 26 (Oreo) or above
- Minimum 4GB RAM recommended for on-device LLM

### Installation
```bash
git clone https://github.com/yourusername/aeris.git
cd aeris
```

Open in Android Studio, sync Gradle, and run on a physical device.

> Note: Sound classification models are bundled in `assets/`. Interaction models (STT, TTS, LLM) are downloaded on first launch via the Home screen.

---

## Project Structure
```
app/src/main/
├── assets/                          # Bundled YAMNet models
└── java/com/runanywhere/kotlin_starter_example/
    ├── data/                        # Repositories and Data models
    ├── services/                    # Background Service, Audio & AI engines
    ├── ui/                          # Screens and Theme
    │   └── screens/
    └── viewmodel/                   # State management
```

---

## Known Limitations

- Multi-speaker separation in noisy environments is not yet reliable
- Accuracy drops with heavy accents on the STT model
- Real-time sign language recognition is not yet supported
- On-device LLM requires sufficient device RAM to run smoothly

---

## Roadmap

- [ ] Tone and emotion detection alongside captions
- [ ] Speaker identification in group conversations
- [ ] Medical appointment mode - clinic, classroom, workplace
- [ ] Context-specific modes - clinic, classroom, workplace
- [ ] Smartwatch and wearable integration
- [ ] Custom sound training - teach Aeris new sounds from your environment

---

## Why Aeris

430 million people live with disabling hearing loss globally. Existing solutions solve one piece - caption apps ignore environmental sounds, smart home alerters don't travel with you, hearing aids cost thousands and don't help everyone.

Aeris is the first tool that combines real-time environmental sound detection, live captions, and two-way AI-assisted communication in a single offline app on a phone people already carry.

The people who need it most should never have to pay for it. Aeris is free for end users. Always.
