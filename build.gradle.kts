// BitChord - Clean Flexible Configuration
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.bitchord.player"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.bitchord.player"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
