plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "com.davemorrissey.labs.subscaleview"
    compileSdk = 35

    defaultConfig {
        minSdk = 14
        targetSdk = 34
    }
}

dependencies {
    api("androidx.annotation:annotation:1.8.0")
    api("androidx.exifinterface:exifinterface:1.3.7")
}
