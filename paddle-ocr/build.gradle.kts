plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "com.baidu.ai.edge.ui"
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
        freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    }
}
dependencies {
    //api fileTree(include: ["*.jar"], dir: "libs")
    api (files("libs/easyedge-sdk.jar"))
    api(project(":readercommon"))
    api("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation(libs.material)
}