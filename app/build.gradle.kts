plugins {
    id("com.android.application")
    id("kotlin-android")
    kotlin("kapt")
    //id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "cn.archko.mupdf"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "cn.archko.mupdf"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        multiDexEnabled = true
        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        named("debug") {
            storeFile = rootProject.file("mupdf_release_key.jks")
            storePassword = "Abc123456"
            keyAlias = "mupdf_release_key"
            keyPassword = "Abc123456"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlin {
        jvmToolchain(8)
    }


    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("release") {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    buildFeatures {
        //compose true
        // Disable unused AGP features
        buildConfig = false
        aidl = false
        renderScript = false
        resValues = false
        shaders = false
        viewBinding = true
    }

    //composeOptions {
    //    kotlinCompilerExtensionVersion Libs.AndroidX.Compose.compiler_version
    //}

    packagingOptions {
        exclude("META-INF/*.kotlin_module")
        doNotStrip += "*/armeabi-v7a/*.so"
        doNotStrip += "*/arm64-v8a/*.so"
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    //implementation Libs . Coroutines . android
    //        implementation Libs . Coroutines . core

    //implementation Libs.AndroidX.Activity.activityCompose
    //implementation Libs.AndroidX.Lifecycle.viewModelCompose
    //implementation Libs.AndroidX.ConstraintLayout.constraintLayoutCompose

    implementation(libs.androidx.lifecycle.runtimeKtx)
    implementation(libs.androidx.lifecycle.viewmodelKtx)
    implementation(libs.androidx.lifecycle.livedata)
    //implementation Libs.AndroidX.Lifecycle.lifecycleRuntimeCompose

    /*implementation(Libs.AndroidX.Paging.pagingRuntime) {
        exclude group: "androidx.recyclerview"
    }
    implementation Libs.AndroidX.Paging.pagingCompose

    implementation platform(Libs.AndroidX.Compose.androidxComposeBom)

    implementation Libs.AndroidX.Compose.runtime
    implementation Libs.AndroidX.Compose.foundation
    implementation Libs.AndroidX.Compose.layout
    implementation Libs.AndroidX.Compose.ui
    implementation Libs.AndroidX.Compose.uiUtil
    implementation Libs.AndroidX.Compose.material
    implementation Libs.AndroidX.Compose.Material3.material3
    implementation Libs.AndroidX.Compose.Material3.material3WindowSizeClass
    implementation Libs.AndroidX.Compose.animation
    implementation Libs.AndroidX.Compose.tooling

    implementation Libs.flinger

    implementation Libs.Accompanist.pager
    implementation Libs.Accompanist.pagerIndicators
    implementation Libs.Accompanist.swiperefresh*/

    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation("io.github.jeremyliao:live-event-bus-x:1.8.0")

    //api(Libs.Umeng.asms) {
    //    exclude group: "androidx.lifecycle"
    //    exclude group: "androidx.activity"
    //    exclude group: "androidx.fragment"
    //}
    //api(Libs.Umeng.umengCommon) {
    //    exclude group: "androidx.lifecycle"
    //    exclude group: "androidx.activity"
    //    exclude group: "androidx.fragment"
    //}
    implementation(libs.multidex)
    //implementation(libs.constraintLayout)
    api ("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation(libs.material)
    implementation(libs.swiperefreshlayout)
    implementation(libs.colorPickerDialog)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    implementation(project(":reader"))
    //implementation project(":editor")
    //implementation project(":library")
    //implementation project(":core-ui")
    implementation(project(":paddle-ocr"))
    //implementation project(":ViewLib")
}