plugins {
    id("com.android.library")
    id("kotlin-android")
    kotlin("kapt")
}

android {
    namespace = "cn.archko.pdf.common"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    /*externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
        }
    }*/
    //android.ndkVersion "25.2.9519653"

    packagingOptions {
        doNotStrip += "*/armeabi-v7a/*.so"
        doNotStrip += "*/arm64-v8a/*.so"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    api("io.github.jeremyliao:live-event-bus-x:1.8.0")
    api(libs.coil.kt)
    api(libs.recyclerview)
    api("com.tencent:mmkv:1.3.4")

    api(project(":pdf-recyclerview"))
    api(project(":mupdf-android-fitz"))
}