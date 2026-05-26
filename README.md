# HealthScanner (Nure)

> A lightweight Android app for health profile scanning and basic user health history tracking. This repository contains the HealthScanner (Nure) Android app built with Material 3 and Firebase integration.

## Features
- Material 3 UI with DayNight theming
- Firebase Authentication integration (Google services present)
- Profile screen (enhanced and legacy layouts)
- History and stats views with adaptive cards

## Tech Stack
- Android (Java)
- Material 3
- Firebase Auth
- Gradle (Kotlin DSL)

## Requirements
- Android Studio (Arctic Fox or later recommended)
- JDK 11 or newer
- Gradle (wrapper included)
- An Android device or emulator

## Quick Setup
1. Clone the repo:

```bash
git clone <repo-url>
cd Nure
```

2. Open the project in Android Studio.
3. Ensure `google-services.json` is present in `app/` (already included here for local dev). If you replace Firebase projects, update that file accordingly.

## Build & Run
From the project root you can use the Gradle wrapper:

```bash
./gradlew assembleDebug      # build debug APK (use gradlew.bat on Windows)
./gradlew installDebug       # build and install to connected device
```

On Windows (PowerShell):

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

## Project Structure Highlights
- `app/src/main/java/com/example/healthscanner/` - application activities and adapters
- `app/src/main/res/layout/activity_profile_enhanced.xml` - enhanced profile UI (preferred for edits)
- `app/src/main/res/values/` - themes, colors, dimens, styles

Follow the repository guidance: UI/layout changes only (do not modify Firebase paths or models unless you know what you're doing).

## Linting & Tests
- Run Android Lint via Gradle:

```bash
./gradlew lint
```

## Contributing
- Please follow existing Material 3 patterns and style tokens in `res/values/colors.xml` and `styles.xml`.
- When editing UI, prefer `activity_profile_enhanced.xml` and keep IDs used by code (e.g., `user_name`, `user_email`, `logout_button`).

## Notes / TODOs
- Profile UI enhancements live in `app/src/main/res/layout/activity_profile_enhanced.xml`.
- See `.github/copilot-instructions.md` for internal contributor guidance.

## License
This repository does not include a license file. Add one if you intend to change the license terms.

## Contact
For questions about the repo layout or design conventions, open an issue or contact the maintainer.
