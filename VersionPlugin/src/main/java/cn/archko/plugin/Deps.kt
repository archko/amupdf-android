package cn.archko.plugin

/**
 *  @author: archko
 */
object BuildConfig {
    const val compileSdkVersion = 29
    const val buildToolsVersion = "30.0.3"
    const val minSdkVersion = 21
    const val targetSdkVersion = 29
    const val versionCode = 230
    const val versionName = "5.0.0"
}

object Versions {
    const val multidex = "2.0.1"
    const val androidGradlePlugin = "4.0.0"
    const val activity = "1.2.0-alpha08"
    const val annotations = "1.0.0"
    const val appcompat = "1.2.0"
    const val arch_core = "2.1.0"
    const val core_ktx = "1.3.0"
    const val atsl_core = "1.2.0"
    const val atsl_junit = "1.1.1"
    const val atsl_rules = "1.2.0"
    const val atsl_runner = "1.2.0"

    const val kotlin = "1.4.10"
    const val coroutines = "1.4.0"

    const val fragment = "1.3.0"
    const val lifecycle = "2.2.0"
    const val material = "1.2.0"

    const val umeng_analytics = "8.1.4"
    const val umeng_common = "2.1.8"
    const val liveEventBusX = "1.5.0"

    const val vectordrawable = "1.1.0"
    const val viewpager2 = "1.0.0"
    const val swiperefreshlayout = "1.1.0"
    const val cardview = "1.0.0"
    const val constraint_layout = "2.0.0-beta3"
    const val dexmaker = "2.2.0"
    const val recyclerview = "1.2.0-alpha04"

    const val glide = "4.10.0"
    const val dagger = "2.16"
    const val navigation = "2.3.0-alpha01"
    const val okhttpLoggingInterceptor = "3.9.0"
    const val paging = "3.0.0-alpha05"
    const val retrofit = "2.9.0"
    const val room = "2.3.0-alpha01"
    const val work = "2.2.0"
    const val apacheCommons = "2.5"

    //testing
    const val benchmark = "1.1.0-alpha01"
    const val mockito = "2.25.0"
    const val mockito_all = "1.10.19"
    const val mockito_android = "2.25.0"
    const val mockwebserver = "3.8.1"
    const val robolectric = "4.2"
    const val hamcrest = "1.3"
    const val espresso = "3.2.0"
    const val junit = "4.12"
    const val constraintlayout = "2.0.4"
    const val timber = "4.7.1"
    const val koin = "2.1.5"

    const val junitExt = "1.1.1"
    const val espressoCore = "3.2.0"
    const val jDatabinding = "1.0.1"
}

object AndroidX {
    const val activityKtx = "androidx.activity:activity-ktx:${Versions.activity}"
    const val appCompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
    const val coreKtx = "androidx.core:core-ktx:${Versions.core_ktx}"
    const val annotations = "androidx.annotation:annotation:${Versions.annotations}"
    const val multidex = "androidx.multidex:multidex:${Versions.multidex}"

    const val constraintLayout =
        "androidx.constraintlayout:constraintlayout:${Versions.constraintlayout}"
    const val constraintLayoutSolver =
        "androidx.constraintlayout:constraintlayout-solver:${Versions.constraint_layout}"
    const val pagingRuntime = "androidx.paging:paging-runtime:${Versions.paging}"

    const val workRuntime = "androidx.work:work-runtime:${Versions.work}"
    const val workTesting = "androidx.work:work-testing:${Versions.work}"
    const val cardview = "androidx.cardview:cardview:${Versions.cardview}"
    const val recyclerview = "androidx.recyclerview:recyclerview:${Versions.recyclerview}"

    const val viewpager2 = "androidx.viewpager2:viewpager2:${Versions.viewpager2}"
    const val vectordrawable = "androidx.vectordrawable:vectordrawable:${Versions.vectordrawable}"
    const val swiperefreshlayout =
        "androidx.swiperefreshlayout:swiperefreshlayout:${Versions.swiperefreshlayout}"
    const val dexmaker = "com.linkedin.dexmaker:dexmaker-mockito:${Versions.dexmaker}"
}

object Arch {
    const val runtime = "androidx.arch.core:core-runtime:${Versions.arch_core}"
    const val testing = "androidx.arch.core:core-testing:${Versions.arch_core}"
}

object Coroutines {
    const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
    const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"
}

object Fragment {
    const val runtime = "androidx.fragment:fragment:${Versions.fragment}"
    const val runtimeKtx = "androidx.fragment:fragment-ktx:${Versions.fragment}"
    const val testing = "androidx.fragment:fragment-testing:${Versions.fragment}"
}

object Lifecycle {
    const val runtime = "androidx.lifecycle:lifecycle-runtime:${Versions.lifecycle}"
    const val java8 = "androidx.lifecycle:lifecycle-common-java8:$[Versions.lifecycle]"
    const val compiler = "androidx.lifecycle:lifecycle-compiler:${Versions.lifecycle}"
    const val viewmodel = "androidx.lifecycle:lifecycle-viewmodel:${Versions.lifecycle}"
    const val viewmodelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}"
    const val livedataKtx = "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycle}"
    const val extensions = "androidx.lifecycle:lifecycle-extensions:${Versions.lifecycle}"
}

object Kotlin {
    const val stdlibJdk7 = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    const val stdlibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
    const val allopen = "org.jetbrains.kotlin:kotlin-allopen:${Versions.kotlin}"
    const val plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    const val test = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"
}

object Umeng {
    const val analytics = "com.umeng.umsdk:analytics:${Versions.umeng_analytics}"
    const val umengCommon = "com.umeng.umsdk:common:${Versions.umeng_common}"
}

object Retrofit {
    const val runtime = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    const val gson = "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"
    const val mock = "com.squareup.retrofit2:retrofit-mock:${Versions.retrofit}"
}

object Atsl {
    const val core = "androidx.test:core:${Versions.atsl_core}"
    const val ext_junit = "androidx.test.ext:junit:${Versions.atsl_junit}"
    const val runner = "androidx.test:runner:${Versions.atsl_runner}"
    const val rules = "androidx.test:rules:${Versions.atsl_rules}"
}

object Room {
    const val runtime = "androidx.room:room-runtime:${Versions.room}"
    const val compiler = "androidx.room:room-compiler:${Versions.room}"
    const val ktx = "androidx.room:room-ktx:${Versions.room}"
    const val rxjava2 = "androidx.room:room-rxjava2:${Versions.room}"
    const val testing = "androidx.room:room-testing:${Versions.room}"
}

object Navigation {
    const val runtime = "androidx.navigation:navigation-runtime:${Versions.navigation}"
    const val runtimeKtx = "androidx.navigation:navigation-runtime-ktx:${Versions.navigation}"
    const val fragment = "androidx.navigation:navigation-fragment:${Versions.navigation}"
    const val fragmentKtx = "androidx.navigation:navigation-fragment-ktx:${Versions.navigation}"
    const val testing = "androidx.navigation:navigation-testing:${Versions.navigation}"
    const val ui = "androidx.navigation:navigation-ui:${Versions.navigation}"
    const val ui_ktx = "androidx.navigation:navigation-ui-ktx:${Versions.navigation}"
    const val safe_args_plugin =
        "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.navigation}"
}

object Dependency {
    const val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"
    const val material = "com.google.android.material:material:${Versions.material}"
    const val liveEventBusX = "com.jeremyliao:live-event-bus-x:${Versions.liveEventBusX}"
    const val okhttpLoggingInterceptor =
        "com.squareup.okhttp3:logging-interceptor:${Versions.okhttpLoggingInterceptor}"
    const val pagingRuntime = "androidx.paging:paging-runtime:${Versions.paging}"

    const val junit = "junit:junit:${Versions.junit}"
    const val androidTestJunit = "androidx.test.ext:junit:${Versions.junitExt}"
    const val espressoCore = "androidx.test.espresso:espresso-core:${Versions.espressoCore}"
    const val jDatabinding = "com.hi-dhl:jdatabinding:${Versions.jDatabinding}"
    const val timber = "com.jakewharton.timber:timber:${Versions.timber}"
    const val benchmark = "androidx.benchmark:benchmark-junit4:${Versions.benchmark}"
    val benchmarkGradle = "androidx.benchmark:benchmark-gradle-plugin:${Versions.benchmark}"
}

object Koin {
    const val core = "org.koin:koin-core:${Versions.koin}"
    const val androidCore = "org.koin:koin-android:${Versions.koin}"
    const val viewmodel = "org.koin:koin-androidx-viewmodel:${Versions.koin}"
    const val androidScope = "org.koin:koin-android-scope:$${Versions.koin}"
}

