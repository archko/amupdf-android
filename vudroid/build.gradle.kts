plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
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

    //docx to html
    api(libs.mammoth)
    //html to epub
    api("io.documentnode:epub4j-core:4.2.2") {
        exclude("xmlpull")
    }

    api(libs.penfeizhou.awebp)
    api(libs.android.gif.drawable)
    api(libs.tiff.loader)
}