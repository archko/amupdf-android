package cn.archko.mupdf.buildsrc

object Versions {
    const val ktlint = "0.40.0"
}

object BuildConfig {
    const val compileSdkVersion = 30
    const val buildToolsVersion = "30.0.3"
    const val minSdkVersion = 21
    const val targetSdkVersion = 29
    const val versionCode = 230
    const val versionName = "5.0.0"
}

object Libs {
    const val androidGradlePlugin = "com.android.tools.build:gradle:7.0.0-alpha14"
    const val jdkDesugar = "com.android.tools:desugar_jdk_libs:1.0.9"

    object Accompanist {
        const val version = "0.8.0"
        const val coil = "com.google.accompanist:accompanist-coil:$version"
        const val insets = "com.google.accompanist:accompanist-insets:$version"
        const val pager = "com.google.accompanist:accompanist-pager:$version"
        const val pagerIndicators = "com.google.accompanist:accompanist-pager-indicators:$version"
        const val swiperefresh = "com.google.accompanist:accompanist-swiperefresh:0.8.0"
    }

    object Kotlin {
        private const val version = "1.4.32"
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"
        const val extensions = "org.jetbrains.kotlin:kotlin-android-extensions:$version"
    }

    object Coroutines {
        private const val version = "1.4.2"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
        const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
    }

    object OkHttp {
        private const val version = "4.9.1"
        const val okhttp = "com.squareup.okhttp3:okhttp:$version"
        const val logging = "com.squareup.okhttp3:logging-interceptor:$version"
    }

    object AndroidX {
        const val coreKtx = "androidx.core:core-ktx:1.6.0-alpha01"
        const val navigation = "androidx.navigation:navigation-compose:1.0.0-alpha08"

        object Compose {
            private const val snapshot = ""
            private const val version = "1.0.0-beta05"

            @get:JvmStatic
            val snapshotUrl: String
                get() = "https://androidx.dev/snapshots/builds/$snapshot/artifacts/repository/"

            const val runtime = "androidx.compose.runtime:runtime:$version"
            const val animation = "androidx.compose.animation:animation:$version"
            const val foundation = "androidx.compose.foundation:foundation:$version"
            const val layout = "androidx.compose.foundation:foundation-layout:$version"

            const val ui = "androidx.compose.ui:ui:$version"
            const val material = "androidx.compose.material:material:$version"
            const val iconsExtended = "androidx.compose.material:material-icons-extended:$version"

            const val tooling = "androidx.compose.ui:ui-tooling:$version"

            const val uiUtil = "androidx.compose.ui:ui-util:$version"
            const val uiTest = "androidx.compose.ui:ui-test-junit4:$version"
        }

        object Activity {
            const val activityCompose = "androidx.activity:activity-compose:1.3.0-alpha07"
            const val activityKtx = "androidx.activity:activity-ktx:1.2.0-alpha08"
            const val appCompat = "androidx.appcompat:appcompat:1.3.0-rc01"
            const val annotations = "androidx.annotation:annotation:1.2.0"
        }

        object Lifecycle {
            const val viewModelCompose =
                "androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha04"

            const val lifecycle = "2.3.1"
            const val runtime = "androidx.lifecycle:lifecycle-runtime:${lifecycle}"
            const val java8 = "androidx.lifecycle:lifecycle-common-java8:${lifecycle}"
            const val compiler = "androidx.lifecycle:lifecycle-compiler:${lifecycle}"
            const val viewModel = "androidx.lifecycle:lifecycle-viewmodel:${lifecycle}"
            const val viewModelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:${lifecycle}"
            const val liveDataKtx = "androidx.lifecycle:lifecycle-livedata-ktx:${lifecycle}"

            const val extensions = "androidx.lifecycle:lifecycle-extensions:2.2.0"
        }

        object ConstraintLayout {
            const val constraintLayoutCompose =
                "androidx.constraintlayout:constraintlayout-compose:1.0.0-alpha05"

            const val constraintLayout =
                "androidx.constraintlayout:constraintlayout:2.0.0-beta3"
            const val constraintLayoutSolver =
                "androidx.constraintlayout:constraintlayout-solver:2.0.0-beta3"
        }

        object Navigation {
            private const val version = "2.3.4"
            const val fragment = "androidx.navigation:navigation-fragment-ktx:$version"
            const val uiKtx = "androidx.navigation:navigation-ui-ktx:$version"
            const val runtime = "androidx.navigation:navigation-runtime:$version"
        }

        object Paging {
            private const val version = "3.0.0-beta03"
            const val pagingRuntime = "androidx.paging:paging-runtime:$version"

            // Jetpack Compose Integration
            const val pagingCompose = "androidx.paging:paging-compose:1.0.0-alpha08"
        }

        object Room {
            private const val version = "2.2.6"
            const val runtime = "androidx.room:room-runtime:${version}"
            const val ktx = "androidx.room:room-ktx:${version}"
            const val compiler = "androidx.room:room-compiler:${version}"
        }
    }

    object Retrofit {
        private const val version = "2.9.0"
        const val retrofit = "com.squareup.retrofit2:retrofit:$version"
        const val converterGson = "com.squareup.retrofit2:converter-gson:2.9.0"
        const val gson = "com.google.code.gson:gson:2.8.5"
        const val mock = "com.squareup.retrofit2:retrofit-mock:${version}"
    }

    object Arch {
        const val version = "2.1.0"
        const val runtime = "androidx.arch.core:core-runtime:${version}"
        const val testing = "androidx.arch.core:core-testing:${version}"
    }

    object Fragment {
        const val version = "1.3.2"
        const val runtime = "androidx.fragment:fragment:${version}"
        const val runtimeKtx = "androidx.fragment:fragment-ktx:${version}"
        const val testing = "androidx.fragment:fragment-testing:${version}"
    }

    object Umeng {
        const val umeng_analytics = "8.1.4"
        const val umeng_common = "2.1.8"
        const val analytics = "com.umeng.umsdk:analytics:${umeng_analytics}"
        const val umengCommon = "com.umeng.umsdk:common:${umeng_common}"
    }

    object Dependency {
        const val multidex = "androidx.multidex:multidex:2.0.1"
        const val material = "com.google.android.material:material:1.3.0"
        const val liveEventBusX = "com.jeremyliao:live-event-bus-x:1.5.0"
        const val viewpager2 = "androidx.viewpager2:viewpager2:1.0.0"
        const val vectordrawable =
            "androidx.vectordrawable:vectordrawable:1.1.0"
        const val swiperefreshlayout =
            "androidx.swiperefreshlayout:swiperefreshlayout:1.1.0"
    }
}
