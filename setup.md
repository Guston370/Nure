# 🚀 Nure Health Scanner - Setup Guide

## Quick Setup for Developers

### 1. Prerequisites
- **Android Studio**: Arctic Fox or later
- **Java**: JDK 11 or later
- **Android SDK**: API 24-34
- **Git**: Latest version

### 2. Clone and Setup
```bash
# Clone the repository
git clone https://github.com/Guston370/Nure.git
cd Nure

# Open in Android Studio
# File -> Open -> Select the project folder
```

### 3. Firebase Configuration
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Create a new project or use existing
3. Add Android app with package name: `com.example.healthscanner`
4. Download `google-services.json`
5. Place it in `app/` directory

### 4. Enable Firebase Services
- **Authentication**: Enable Email/Password and Google Sign-In
- **Firestore Database**: Create in test mode
- **Storage**: Enable for user profile images (optional)

### 5. Build and Run
```bash
# Clean and build
./gradlew clean build

# Install on device/emulator
./gradlew installDebug

# Or use Android Studio's Run button
```

### 6. Troubleshooting

#### Common Issues:
- **Google Services**: Ensure `google-services.json` is in correct location
- **SDK Version**: Update to latest Android SDK if build fails
- **Dependencies**: Run `./gradlew --refresh-dependencies` if needed

#### Build Errors:
```bash
# Clean project
./gradlew clean

# Refresh dependencies
./gradlew --refresh-dependencies

# Rebuild
./gradlew build
```

### 7. Development Tips
- Use **Debug** build variant for development
- Enable **USB Debugging** on your device
- Use **Android Studio Profiler** for performance monitoring
- Run **Lint** checks before committing: `./gradlew lint`

### 8. Project Structure
```
Nure/
├── app/                    # Main application module
├── build.gradle.kts        # Project build configuration
├── gradle/                 # Gradle wrapper and dependencies
├── README.md              # Project documentation
└── setup.md               # This setup guide
```

### 9. Key Features to Test
- [ ] User registration and login
- [ ] Google Sign-In authentication
- [ ] Barcode scanning functionality
- [ ] Product information display
- [ ] User profile management
- [ ] Scan history tracking
- [ ] Dark/Light theme switching

### 10. Contributing
1. Fork the repository
2. Create feature branch: `git checkout -b feature/new-feature`
3. Make changes and test thoroughly
4. Commit: `git commit -m "Add new feature"`
5. Push: `git push origin feature/new-feature`
6. Create Pull Request

---

**Happy Coding!** 🎉

For issues or questions, please create an issue on GitHub or contact the development team.