# 🏥 Nure Health Scanner

A professional health-tech Android application for scanning and analyzing food products to provide comprehensive nutritional information and health insights.

## 🌟 Features

### 📱 Core Functionality
- **Barcode Scanning**: Advanced ML Kit barcode scanning with real-time detection
- **Product Analysis**: Comprehensive nutritional information and health scoring
- **User Authentication**: Secure Firebase authentication with Google Sign-In
- **Health Dashboard**: Personalized health insights and nutrition tracking
- **Scan History**: Track and manage previous scans with detailed analytics

### 🎨 Design & UX
- **Material 3 Design**: Modern, professional health-tech aesthetic
- **Mint-Teal Color Palette**: Evokes trust, freshness, and vitality
- **Dark/Light Theme**: Adaptive theming with seamless transitions
- **Responsive Layouts**: Optimized for various screen sizes and orientations
- **Smooth Animations**: Polished user experience with fluid transitions

### 🔧 Technical Features
- **Firebase Integration**: Real-time database and authentication
- **ML Kit Barcode Scanning**: Google's advanced computer vision
- **Material Components**: Latest Material 3 design system
- **Crash-Safe Architecture**: Robust error handling and recovery
- **Performance Optimized**: Efficient resource management and caching

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Firebase project setup
- Google Services configuration

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Guston370/Nuresync.git
   cd Nuresync
   ```

2. **Firebase Setup**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Enable Authentication and Firestore Database
   - Download `google-services.json` and place it in `app/` directory

3. **Build and Run**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

## 🏗️ Architecture

### Project Structure
```
app/
├── src/main/
│   ├── java/com/example/healthscanner/
│   │   ├── AuthManager.java          # Authentication management
│   │   ├── MainActivity.java         # Main dashboard
│   │   ├── LoginActivity.java        # User authentication
│   │   ├── ScannerActivity.java      # Barcode scanning
│   │   ├── ProfileActivity.java      # User profile management
│   │   └── HistoryActivity.java      # Scan history
│   └── res/
│       ├── layout/                   # UI layouts
│       ├── drawable/                 # Graphics and icons
│       ├── values/                   # Colors, themes, strings
│       └── anim/                     # Animations
└── build.gradle.kts                  # Build configuration
```

### Key Components
- **Authentication**: Firebase Auth with Google Sign-In integration
- **Scanning Engine**: ML Kit Barcode Scanner with custom UI
- **Data Layer**: Firebase Firestore for user data and scan history
- **UI Framework**: Material 3 components with custom health-tech theming

## 🎨 Design System

### Color Palette
- **Primary**: Mint Green → Teal gradient (#3CCF91 → #0FB8AD)
- **Secondary**: Soft Blue (#4DB6E3)
- **Accent**: Warm Orange (#FFB74D → #FFD166)
- **Success**: Health Green (#4CAF50)
- **Error**: Professional Red (#E53935)

### Typography
- **Headlines**: Sans-serif Medium (32sp, 24sp)
- **Body Text**: Sans-serif Regular (16sp, 14sp)
- **Captions**: Sans-serif Regular (12sp)

### Components
- **Buttons**: Gradient backgrounds with 12dp corner radius
- **Cards**: Elevated surfaces with 16dp corner radius
- **FAB**: Gradient scan button with health-themed colors
- **Text Inputs**: Outlined style with health accent colors

## 🔐 Security & Privacy

- **Data Encryption**: All user data encrypted in transit and at rest
- **Authentication**: Secure Firebase authentication with OAuth 2.0
- **Privacy First**: Minimal data collection with user consent
- **GDPR Compliant**: European privacy regulation compliance

## 📱 Supported Platforms

- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)
- **Architecture**: ARM64, ARMv7, x86_64
- **Screen Sizes**: Phone, Tablet, Foldable

## 🛠️ Development

### Build Variants
- **Debug**: Development build with debugging enabled
- **Release**: Production build with ProGuard optimization

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate test coverage report
./gradlew jacocoTestReport
```

### Code Quality
- **Lint**: Android lint checks enabled
- **ProGuard**: Code obfuscation and optimization
- **Crash Reporting**: Firebase Crashlytics integration

## 📊 Performance

- **App Size**: ~15MB (optimized with ProGuard)
- **Startup Time**: <2 seconds cold start
- **Memory Usage**: <100MB average runtime
- **Battery Efficient**: Optimized scanning algorithms

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow Material Design principles
- Maintain consistent code style
- Write comprehensive tests
- Update documentation for new features

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Material Design**: Google's design system
- **Firebase**: Backend infrastructure
- **ML Kit**: Barcode scanning technology
- **Community**: Open source contributors

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/Guston370/Nuresync/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Guston370/Nuresync/discussions)
- **Email**: support@nurehealth.com

---

**Nure Health Scanner** - Empowering healthier choices through technology 🌱

Made with ❤️ for better health and nutrition awareness.