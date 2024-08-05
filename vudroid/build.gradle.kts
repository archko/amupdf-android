plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "org.vudroid"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    }
}
dependencies {
    api(project(":readercommon"))
    //api("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")
    api(project(":subsampling-scale-image-view"))
}