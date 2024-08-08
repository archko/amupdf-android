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
            storePassword = ""
            keyAlias = ""
            keyPassword = ""
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

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }

        getByName("release") {
            isMinifyEnabled = false
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

    android.applicationVariants.all {
        val buildType = this.buildType.name
        val flavorName = this.flavorName
        val variant = this
        outputs.all {
            if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                //修改apk名称
                if (buildType == "release") {
                    this.outputFileName = "Dragon Viewer-${variant.versionName}.apk"
                } else if (buildType == "debug") {
                    this.outputFileName = "Dragon Viewer-${buildType}-${variant.versionName}.apk"
                }
            }
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    //implementation Libs . Coroutines . android
    //implementation Libs . Coroutines . core

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
    implementation(libs.constraintLayout)

    implementation(libs.material)
    implementation(libs.swiperefreshlayout)
    implementation(libs.colorPickerDialog)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)

    implementation(project(":reader"))
    implementation(project(":paddle-ocr"))

    // https://mvnrepository.com/artifact/com.github.axet/k2pdfopt
    implementation(libs.k2pdfopt)
    implementation(libs.sardine)
}