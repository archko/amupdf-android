plugins {
    id("com.android.library")
    id("kotlin-android")
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
    //api("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")
    api(project(":subsampling-scale-image-view"))

    //docx to html
    api("org.zwobble.mammoth:mammoth:1.5.0")  //最新是1.7.0依赖问题导致崩溃
    //html to epub
    api("io.documentnode:epub4j-core:4.2.1") {
        exclude("xmlpull")
    }
}