plugins {
    id("com.android.application")
    id("kotlin-android")
    kotlin("kapt")
    //alias(libs.plugins.compose.compiler)
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

    sourceSets {
        named("main") {
            //java.setSrcDirs(listOf("src/main/java", "src/common/java"))
            java.srcDir("src/common/java")
            //java.srcDir("src/compose/java")
            java.srcDir("src/old/java")
            res.srcDir("src/old/res")
            //res.srcDir("src/compose/res")
            res.srcDir("src/common/res")
        }
    }

    buildFeatures {
        //compose = true
        // Disable unused AGP features
        buildConfig = false
        aidl = false
        renderScript = false
        resValues = false
        shaders = false
        viewBinding = true
    }

    packaging {
        exclude("META-INF/*.kotlin_module")
        jniLibs.keepDebugSymbols += "*/armeabi-v7a/*.so"
        jniLibs.keepDebugSymbols += "*/arm64-v8a/*.so"
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

    implementation(libs.androidx.lifecycle.runtimeKtx)
    implementation(libs.androidx.lifecycle.viewmodelKtx)
    implementation(libs.androidx.lifecycle.livedata)

    /*implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.material3.android)

    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.accompanist.swiperefresh)
    implementation(libs.coil.kt.compose)

    implementation(libs.flinger)
    implementation(project(":core-ui"))*/

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