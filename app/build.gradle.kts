plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.healthscanner"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.example.healthscanner"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        jniLibs {
            // Store native libraries uncompressed and page-aligned. Required alongside
            // 16 KB-aligned dependencies for Google Play's Android 15+ requirement.
            useLegacyPackaging = false
        }
    }
}

// Remove Java toolchain configuration to use system default

dependencies {
    // AndroidX Core + Material 3
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // CardView
    implementation("androidx.cardview:cardview:1.0.0")

    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Volley (for API requests)
    implementation("com.android.volley:volley:1.2.1")

    // Use version catalog for these dependencies
    implementation(libs.gson)
    implementation(libs.okhttp)

    // ZXing Barcode Scanner
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.4.1")

    // ML Kit via Google Play Services (unbundled).
    //
    // The bundled com.google.mlkit:* artifacts package native pipelines whose LOAD segments
    // are not 16 KB aligned, which Google Play rejects for apps targeting Android 15+. The
    // bundled text-recognition artifact has no newer release, so there is no version bump
    // that fixes it. The unbundled variants ship no native libraries at all - Play Services
    // provides the pipeline - which resolves the alignment requirement and removes ~90 MB of
    // .so files from the APK.
    //
    // Trade-off: requires Google Play Services, and models download on first use.
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    // On-device general image labelling: names the food in a photo. Replaces the previously
    // bundled food_classifier.onnx, which shipped untrained weights.
    implementation("com.google.android.gms:play-services-mlkit-image-labeling:16.0.8")
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // CameraX — 1.4.x ships 16 KB-aligned libimage_processing_util_jni.so
    val camerax_version = "1.4.2"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    // Guava (for ListenableFuture used in CameraX)
    implementation("com.google.guava:guava:32.1.2-android")

    // Firebase BOM - manages all Firebase library versions
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    
    // MPAndroidChart for advanced charts and graphs
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    
    // Firebase Authentication
    implementation("com.google.firebase:firebase-auth")
    
    // Firebase Firestore (for database storage)
    implementation("com.google.firebase:firebase-firestore")
    
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Firebase Analytics (optional but recommended)
    implementation("com.google.firebase:firebase-analytics")

    // Material Design Components
    implementation("com.google.android.material:material:1.11.0")
    
    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // ViewPager2 for smooth transitions
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    
    // Lottie for animations
    implementation("com.airbnb.android:lottie:6.2.0")

    // Testing
    testImplementation(libs.junit)
    // Real org.json implementation: the android.jar on the unit test classpath only has
    // stubs that throw, so JSON serialisation tests need this on the classpath first.
    testImplementation("org.json:json:20231013")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}