plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "cn.archko.pdf"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        encoding = "UTF-8"
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}
dependencies {
    api(libs.vectordrawable)
    //api (libs.swiperefreshlayout)
    api(libs.androidx.activity)
    api(libs.androidx.fragment)
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)

    implementation(libs.androidx.lifecycle.runtimeKtx)
    implementation(libs.androidx.lifecycle.viewmodelKtx)
    implementation(libs.androidx.lifecycle.livedata)

    //api(libs.datastore.preference_ktx)

    implementation(libs.material)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.colorPickerDialog)
    api(libs.recyclerview)

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
    implementation(libs.k2pdfopt)

    api(project(":vudroid"))
    api(project(":pdf-recyclerview"))
}