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

    // ML Kit Barcode Scanning & Text Recognition
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // CameraX
    val camerax_version = "1.3.4"
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
    
    // Firebase Storage (for binary image uploads)
    implementation("com.google.firebase:firebase-storage")
    
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

    // Room SQLite Offline Sync
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    
    // WorkManager
    val work_version = "2.9.0"
    implementation("androidx.work:work-runtime:$work_version")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}