plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
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

    //externalNativeBuild {
    //    cmake {
    //        path = File("src/main/cpp/CMakeLists.txt")
    //    }
    //}
    //android.ndkVersion = "26.1.10909125"

    packaging {
        jniLibs.keepDebugSymbols += "*/armeabi-v7a/*.so"
        jniLibs.keepDebugSymbols += "*/arm64-v8a/*.so"
    }
}

dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.androidx.lifecycle.viewmodelKtx)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    add("ksp", libs.room.compiler)

    api(libs.coil.kt)
    api(libs.recyclerview)
    api(libs.mmkv)

    api(libs.crashreport)
    api(libs.juniversalchardet)
    api("com.artifex.mupdf:mupdf-fitz:1.26.0")
    api(libs.sardine) {
        exclude("stax")
        exclude("stax-api")
        exclude("xpp3")
    }

    api(project(":pdf-recyclerview"))
}