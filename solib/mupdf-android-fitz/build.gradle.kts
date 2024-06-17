plugins {
    id("com.android.library")
}

android {
    namespace = "com.artifex.mupdf.fitz.m"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
    }
}

dependencies {
    api("com.artifex.mupdf:mupdf-fitz:1.0-SNAPSHOT")
}
