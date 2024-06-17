plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    namespace = "com.baidu.ai.edge.ui"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }
}
dependencies {
    //api fileTree(include: ["*.jar"], dir: "libs")
    api (files("libs/easyedge-sdk.jar"))
    api(project(":readercommon"))
    implementation(libs.material)
}