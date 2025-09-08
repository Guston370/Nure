plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.healthscanner"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.healthscanner"
        minSdk = 24
        targetSdk = 34
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
}

dependencies {
    // AndroidX Core + Material 3
    implementation(libs.appcompat)
    implementation(libs.material)

    // CardView
    implementation("androidx.cardview:cardview:1.0.0")

    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Volley (for API requests)
    implementation("com.android.volley:volley:1.2.1")

    // Use version catalog for these dependencies
    implementation(libs.gson)
    implementation(libs.okhttp)

    // ZXing Barcode Scanner - use version catalog
    implementation(libs.zxing) {
        isTransitive = false // avoid duplicate AndroidX libs
    }
    implementation("com.google.zxing:core:3.4.1")

    // ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // CameraX
    val camerax_version = "1.3.4"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    // Guava (for ListenableFuture used in CameraX)
    implementation("com.google.guava:guava:32.1.2-android")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}