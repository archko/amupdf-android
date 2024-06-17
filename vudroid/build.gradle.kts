plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "org.vudroid"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
    }
}
dependencies {
    api(project(":readercommon"))
}