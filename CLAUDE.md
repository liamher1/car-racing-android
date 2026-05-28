# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A single-screen Android car-dodging game built with Jetpack Compose. The player controls a car on a 3-lane road, using left/right buttons to avoid falling obstacles. 3 lives per game; vibrates and shows a Toast on collision; auto-resets after all lives are lost. The app bar label is "HW1".

## Build Commands

On Windows, use `gradlew.bat` (or just `gradlew` in PowerShell):

```powershell
# Build debug APK
.\gradlew assembleDebug

# Run unit tests (host JVM)
.\gradlew test

# Run instrumented tests (requires connected device/emulator)
.\gradlew connectedAndroidTest

# Lint
.\gradlew lint

# Build + install on connected device
.\gradlew installDebug

# Run a single unit test class
.\gradlew test --tests "com.example.myapplication.ExampleUnitTest"
```

## Tech Stack & Versions

- **AGP**: 9.2.1 | **Kotlin**: 2.2.10 | **Compose BOM**: 2026.02.01
- **minSdk / targetSdk / compileSdk**: 37 (Android 15)
- **UI**: Jetpack Compose + Material3 (dynamic color enabled)
- **Dependencies** are version-cataloged in `gradle/libs.versions.toml`

## Architecture

All game logic and UI live in a single file: `app/src/main/java/com/example/myapplication/MainActivity.kt`.

### Key pieces

| Symbol | Role |
|---|---|
| `Obstacle` | Data class: `id` (timestamp), `lane` (0–2), `yPosition` (normalized 0.0–1.0) |
| `CarGameApp()` | Root composable; owns all `remember` state (car lane, lives, obstacle list, game-active flag) |
| `LaunchedEffect(gameActive)` | 50 ms game loop: moves obstacles, spawns new ones (5 % chance per tick, max 3 at once), and runs collision detection |

### Coordinate system
Positions are **normalized** (0.0 – 1.0) relative to the `BoxWithConstraints` road area. The car sits at `yPosition = 0.85f`; collision fires when an obstacle's `yPosition` is between `0.80f` and `0.95f` in the same lane.

### Theme
`ui/theme/` contains the standard Material3 scaffolding (`Color.kt`, `Type.kt`, `Theme.kt`). `MyApplicationTheme` uses dynamic color on API 31+ and falls back to a purple/pink static scheme otherwise.

### Permissions
`VIBRATE` permission is declared in `AndroidManifest.xml`; vibration is triggered via `VibratorManager` (API 31+).
