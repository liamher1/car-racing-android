# 🏎️ Car Dodger

A high-speed, arcade-style Android car dodging game built with modern Android development practices. Dodge obstacles, collect coins, and climb the high-score leaderboard!

## 🌟 Features

### 🕹️ Game Modes
- **Slow Mode (Buttons)**: Classic arcade controls with manageable speed.
- **Fast Mode (Buttons)**: For players seeking a high-octane challenge.
- **Sensors Mode**: Control the car by tilting your device! Uses the accelerometer for immersive steering and speed modulation.

### 🏆 High Scores & Persistence
- **Local Leaderboard**: Keeps track of your top 10 best runs using **Room Database**.
- **Location Tracking**: Records the physical location where you achieved your high score (requires GPS permissions).
- **Interactive Map**: View where your best records were set on a fully interactive **OpenStreetMap** (powered by OSMDroid).

### 🎨 Visuals & UX
- **Dynamic HUD**: Real-time tracking of score, distance (odometer), and speed.
- **Game Over Screen**: Instant feedback on your final score with quick options to "Play Again" or return to the menu.
- **Vibration Feedback**: Feel the impact of every collision (requires VIBRATE permission).

---

## 🛠️ Technical Stack

- **Language**: Kotlin
- **Architecture**: Multi-Activity with Fragments
- **UI System**: XML Layouts with Material3 styling
- **Database**: Room Persistence Library
- **Maps**: OSMDroid (Free, Open-Source alternative to Google Maps)
- **Concurrency**: Kotlin Coroutines & Flow
- **Dependency Management**: Version Catalogs (libs.versions.toml)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- Android SDK 35 (Target)
- Minimum SDK 26 (Android 8.0 Oreo)

### Installation
1. Clone the repository.
2. Open the project in Android Studio.
3. Sync Gradle and ensure all dependencies are downloaded.
4. Build and run on a physical device (recommended for Sensor Mode) or emulator.

### Permissions
The app requests the following permissions for the full experience:
- `VIBRATE`: For haptic collision feedback.
- `ACCESS_FINE_LOCATION`: To record where your high scores happen.
- `INTERNET`: To download map tiles for the high scores screen.

---

## 🎮 How to Play

1. **Select Mode**: Choose your preferred control style on the Main Menu.
2. **Start**: Press "START GAME". Your car (🚗) will appear in the center lane.
3. **Dodge**: Avoid obstacles (🔥, ⚡, 🪨, 🚛) by moving left and right.
4. **Collect**: Grab golden coins (🟡) to boost your score by 50 points!
5. **Survive**: You have 3 lives. Lose them all, and it's Game Over.
6. **Leaderboard**: Check the "HIGH SCORES" screen to see your rank and tap any score to see where you set it on the map!

---

## 💡 Developer Note
This project uses **OSMDroid** for map rendering, meaning **no API Keys** are required to run the map functionality. It works out of the box!

*To reset high scores for testing, long-press the "HIGH SCORES" button on the Main Menu.*
