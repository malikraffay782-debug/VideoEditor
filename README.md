# Video Editor — Professional AI 3D Video Editor for Android

Lightweight, professional video editing application optimized for low-end Android devices (2 GB RAM target).

## Current Status

**Phase 1 complete (foundation)**

- Multi-module Gradle project
- Jetpack Compose + dark professional theme
- Adaptive PerformanceMode detection (Performance / Balanced / High Quality)
- GitHub Actions APK build workflow
- Core architecture modules scaffolded

## Requirements

- Android Studio Ladybug or newer (or CI)
- JDK 17
- Android SDK 35
- minSdk 26 (Android 8.0)

## Build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/`

## Modules

| Module            | Responsibility                          |
|-------------------|-----------------------------------------|
| app               | Entry point, navigation, DI             |
| core              | Models, PerformanceDetector             |
| core-ui           | Theme, shared Compose components        |
| media-engine      | Import, proxy, Media3 playback          |
| timeline          | Multitrack timeline                     |
| effects           | Filters, transitions, keyframes         |
| audio-engine      | Audio tracks, volume, voice-over        |
| 3d-engine         | Lightweight 3D + 2.5D fallback          |
| ai-engine         | Editing plan validation & application   |
| ai-providers      | OpenRouter, Groq, Gemini, OpenAI-compat |
| project-manager   | Room, autosave, non-destructive state   |
| export-engine     | Media3 Transformer export               |

## Development Phases

1. Foundation + GitHub Actions ← **current**
2. Media import, playback, basic timeline
3. Effects, audio, transitions, export
4. Low-end optimization & proxy system
5. 3D Studio
6. AI providers + editing assistant
7. Security, tests, final verification

## License

Private project.
