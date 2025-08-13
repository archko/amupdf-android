plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
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
        val variant = this
        outputs.all {
            if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                //修改apk名称
                this.outputFileName = "Dragon Viewer-${variant.versionName}.apk"
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
    add("ksp", libs.room.compiler)

    implementation(project(":reader"))
    implementation(project(":paddle-ocr"))

    // https://mvnrepository.com/artifact/com.github.axet/k2pdfopt
    implementation(libs.k2pdfopt)
}