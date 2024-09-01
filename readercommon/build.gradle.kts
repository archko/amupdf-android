plugins {
    id("com.android.library")
    id("kotlin-android")
    kotlin("kapt")
}

android {
    namespace = "cn.archko.pdf.common"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
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

    packaging {
        jniLibs.keepDebugSymbols += "*/armeabi-v7a/*.so"
        jniLibs.keepDebugSymbols += "*/arm64-v8a/*.so"
    }
}

dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    //implementation(libs.androidx.activity.compose)
    //implementation(libs.androidx.lifecycle.viewModelCompose)
    //implementation(libs.androidx.navigation.compose)
    api(libs.androidx.lifecycle.viewmodelKtx)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    api(libs.coil.kt)
    api(libs.recyclerview)
    api("com.tencent:mmkv:1.3.4")

    implementation("com.tencent.bugly:crashreport:4.1.9.3")
    implementation("com.github.albfernandez:juniversalchardet:2.4.0")
    api("com.artifex.mupdf:mupdf-fitz:1.24.3")

    api(project(":pdf-recyclerview"))
}