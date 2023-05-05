package cn.archko.mupdf.buildsrc

object Versions {
    const val ktlint = "0.45.2"
}

object BuildConfig {
    const val compileSdkVersion = 33
    const val buildToolsVersion = "33.0.0"
    const val minSdkVersion = 21
    const val targetSdkVersion = 31
    const val versionCode = 290
    const val versionName = "5.5.0"
}

object Libs {
    const val jdkDesugar = "com.android.tools:desugar_jdk_libs:1.2.2"
    const val flinger = "com.github.iamjosephmj:flinger:1.1.1"
    const val androidGradlePlugin = "com.android.tools.build:gradle:7.4.1"

    object Accompanist {
        const val version = "0.30.1"

        //const val insets = "com.google.accompanist:accompanist-insets:$version"
        const val pager = "com.google.accompanist:accompanist-pager:$version"
        const val pagerIndicators = "com.google.accompanist:accompanist-pager-indicators:$version"
        const val swiperefresh = "com.google.accompanist:accompanist-swiperefresh:$version"
        const val systemuicontroller =
            "com.google.accompanist:accompanist-systemuicontroller:$version"
        const val flowlayouts = "com.google.accompanist:accompanist-flowlayout:$version"
    }

    object Coil {
        const val coil = "io.coil-kt:coil:2.2.2"
        const val coilCompose = "io.coil-kt:coil-compose:2.2.2"
    }

    object Kotlin {
        private const val version = "1.8.10"
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
        const val extensions = "org.jetbrains.kotlin:kotlin-android-extensions:$version"


        const val ksp = "1.8.0-1.0.9"
        const val kspGradlePlugin = "com.google.devtools.ksp.gradle.plugin:$ksp"
    }

    object Coroutines {
        private const val version = "1.6.4"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
        const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
    }

    object OkHttp {
        private const val version = "4.10.0"
        const val okhttp = "com.squareup.okhttp3:okhttp:$version"
        const val logging = "com.squareup.okhttp3:logging-interceptor:$version"
    }

    object AndroidX {
        const val coreKtx = "androidx.core:core-ktx:1.9.0"
        const val navigation = "androidx.navigation:navigation-compose:2.5.3"

        object Compose {
            const val compiler_version = "1.4.4"
            const val composeBomVersion = "2023.04.01"

            const val runtime = "androidx.compose.runtime:runtime"
            const val runtimeLivedata = "androidx.compose.runtime:runtime-livedata"
            const val animation = "androidx.compose.animation:animation"
            const val foundation = "androidx.compose.foundation:foundation"
            const val layout = "androidx.compose.foundation:foundation-layout"
            const val androidxComposeBom = "androidx.compose:compose-bom:$composeBomVersion"

            const val ui = "androidx.compose.ui:ui"
            const val material = "androidx.compose.material:material"
            const val iconsExtended = "androidx.compose.material:material-icons-extended"

            const val tooling = "androidx.compose.ui:ui-tooling"
            const val toolingPreview = "androidx.compose.ui:ui-tooling-preview"

            const val uiUtil = "androidx.compose.ui:ui-util"
            const val uiTest = "androidx.compose.ui:ui-test-junit4"

            object Material3 {
                const val snapshot = ""
                const val version = "1.1.0-alpha06"

                const val material3 = "androidx.compose.material3:material3"
                const val material3WindowSizeClass =
                    "androidx.compose.material3:material3-window-size-class"
            }
        }

        object Activity {
            const val activityCompose = "androidx.activity:activity-compose:1.7.1"
            const val activityKtx = "androidx.activity:activity-ktx:1.6.1"
            const val appCompat = "androidx.appcompat:appcompat:1.6.1"
            const val annotations = "androidx.annotation:annotation:1.4.0"
        }

        object Lifecycle {
            const val lifecycle = "2.6.1"
            const val viewModelCompose =
                "androidx.lifecycle:lifecycle-viewmodel-compose:${lifecycle}"

            const val runtime = "androidx.lifecycle:lifecycle-runtime:${lifecycle}"
            const val java8 = "androidx.lifecycle:lifecycle-common-java8:${lifecycle}"
            const val compiler = "androidx.lifecycle:lifecycle-compiler:${lifecycle}"
            const val viewModel = "androidx.lifecycle:lifecycle-viewmodel:${lifecycle}"
            const val viewModelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:${lifecycle}"
            const val liveDataKtx = "androidx.lifecycle:lifecycle-livedata-ktx:${lifecycle}"

            const val extensions = "androidx.lifecycle:lifecycle-runtime-ktx:${lifecycle}"
        }

        object ConstraintLayout {
            const val constraintLayoutCompose =
                "androidx.constraintlayout:constraintlayout-compose:1.0.1"

            const val constraintLayout =
                "androidx.constraintlayout:constraintlayout:2.1.4"
        }

        object Navigation {
            private const val version = "2.5.3"
            const val fragment = "androidx.navigation:navigation-fragment-ktx:$version"
            const val uiKtx = "androidx.navigation:navigation-ui-ktx:$version"
            const val runtime = "androidx.navigation:navigation-runtime:$version"
        }

        object Paging {
            private const val version = "3.1.0"
            const val pagingRuntime = "androidx.paging:paging-runtime:$version"

            // Jetpack Compose Integration
            const val pagingCompose = "androidx.paging:paging-compose:1.0.0-alpha14"
        }

        object Room {
            private const val version = "2.5.0"
            const val runtime = "androidx.room:room-runtime:${version}"
            const val ktx = "androidx.room:room-ktx:${version}"
            const val compiler = "androidx.room:room-compiler:${version}"
        }

        object DataStore {
            private const val dataStoreVersion = "1.0.0"
            const val preference_version = "1.1.1"

            const val dataStore = "androidx.datastore:datastore-preferences:$dataStoreVersion"

            const val preference_ktx = "androidx.preference:preference-ktx:$preference_version"
        }
    }

    object Retrofit {
        private const val version = "2.9.0"
        const val retrofit = "com.squareup.retrofit2:retrofit:$version"
        const val converterGson = "com.squareup.retrofit2:converter-gson:2.9.0"
        const val gson = "com.google.code.gson:gson:2.8.9"
        const val mock = "com.squareup.retrofit2:retrofit-mock:${version}"
    }

    object Arch {
        const val version = "2.1.0"
        const val runtime = "androidx.arch.core:core-runtime:${version}"
        const val testing = "androidx.arch.core:core-testing:${version}"
    }

    object Fragment {
        const val version = "1.5.0"
        const val runtime = "androidx.fragment:fragment:${version}"
        const val runtimeKtx = "androidx.fragment:fragment-ktx:${version}"
        const val testing = "androidx.fragment:fragment-testing:${version}"
    }

    //object Umeng {
    //    const val umeng_common = "9.3.8"
    //    const val asms_version = "1.2.2"
    //    const val umengCommon = "com.umeng.umsdk:common:${umeng_common}"
    //    const val asms = "com.umeng.umsdk:asms:${asms_version}"// 必选
    //}

    object Dependency {
        const val multidex = "androidx.multidex:multidex:2.0.1"
        const val material = "com.google.android.material:material:1.9.0-rc01"
        const val liveEventBusX = "com.jeremyliao:live-event-bus-x:1.5.0"
        const val viewpager2 = "androidx.viewpager2:viewpager2:1.0.0"
        const val vectordrawable =
            "androidx.vectordrawable:vectordrawable:1.1.0"
        const val swiperefreshlayout =
            "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0"
        const val colorPickerDialog = "me.jfenn.ColorPickerDialog:base:0.2.2"
    }
}
