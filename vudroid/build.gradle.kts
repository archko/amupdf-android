plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "org.vudroid"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
    }

    buildFeatures {
        viewBinding = true
    }
}
dependencies {
    api(project(":readercommon"))
    api("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")
}