package cn.archko.plugin

/**
 *  @author: archko
 */
object BuildConfig {
    val compileSdkVersion = 29
    val buildToolsVersion = "30.0.3"
    val minSdkVersion = 21
    val targetSdkVersion = 29
    val versionCode = 230
    val versionName = "5.0.0"
}

object Versions {
    val multidex = "2.0.1"
    val androidGradlePlugin = "4.0.0"
    val activity = "1.2.0-alpha08"
    val annotations = "1.0.0"
    val appcompat = "1.2.0"
    val arch_core = "2.1.0"
    val core_ktx = "1.3.0"
    val atsl_core = "1.2.0"
    val atsl_junit = "1.1.1"
    val atsl_rules = "1.2.0"
    val atsl_runner = "1.2.0"

    val kotlin = "1.4.10"
    val coroutines = "1.4.0"

    val fragment = "1.2.1"
    val lifecycle = "2.2.0"
    val material = "1.2.0"

    val umeng_analytics = "8.1.4"
    val umeng_common = "2.1.8"
    val liveEventBusX = "1.5.0"

    val vectordrawable = "1.1.0"
    val viewpager2 = "1.0.0"
    val swiperefreshlayout = "1.1.0"
    val cardview = "1.0.0"
    val constraint_layout = "2.0.0-beta3"
    val dexmaker = "2.2.0"
    val recyclerview = "1.2.0-alpha04"

    val glide = "4.10.0"
    val dagger = "2.16"
    val navigation = "2.3.0-alpha01"
    val okhttpLoggingInterceptor = "3.9.0"
    val paging = "3.0.0-alpha05"
    val retrofit = "2.9.0"
    val room = "2.3.0-alpha01"
    val work = "2.2.0"
    val apacheCommons = "2.5"

    //testing
    val benchmark = "1.1.0-alpha01"
    val mockito = "2.25.0"
    val mockito_all = "1.10.19"
    val mockito_android = "2.25.0"
    val mockwebserver = "3.8.1"
    val robolectric = "4.2"
    val hamcrest = "1.3"
    val espresso = "3.2.0"
    val junit = "4.12"
    val constraintlayout = "2.0.4"
    val timber = "4.7.1"
    val koin = "2.1.5"

    val junitExt = "1.1.1"
    val espressoCore = "3.2.0"
    val jDatabinding = "1.0.1"
}

object AndroidX {
    val activityKtx = "androidx.activity:activity-ktx:${Versions.activity}"
    val appCompat = "androidx.appcompat:appcompat:${Versions.appcompat}"
    val coreKtx = "androidx.core:core-ktx:${Versions.core_ktx}"
    val annotations = "androidx.annotation:annotation:${Versions.annotations}"
    val multidex = "androidx.multidex:multidex:${Versions.multidex}"

    val constraintLayout = "androidx.constraintlayout:constraintlayout:${Versions.constraintlayout}"
    val constraintLayoutSolver = "androidx.constraintlayout:constraintlayout-solver:${Versions.constraint_layout}"
    val pagingRuntime = "androidx.paging:paging-runtime:${Versions.paging}"

    val workRuntime = "androidx.work:work-runtime:${Versions.work}"
    val workTesting = "androidx.work:work-testing:${Versions.work}"
    val cardview = "androidx.cardview:cardview:${Versions.cardview}"
    val recyclerview = "androidx.recyclerview:recyclerview:${Versions.recyclerview}"

    val viewpager2 = "androidx.viewpager2:viewpager2:${Versions.viewpager2}"
    val vectordrawable = "androidx.vectordrawable:vectordrawable:${Versions.vectordrawable}"
    val swiperefreshlayout = "androidx.swiperefreshlayout:swiperefreshlayout:${Versions.swiperefreshlayout}"
    val dexmaker = "com.linkedin.dexmaker:dexmaker-mockito:${Versions.dexmaker}"
}

object Arch {
    val runtime = "androidx.arch.core:core-runtime:${Versions.arch_core}"
    val testing = "androidx.arch.core:core-testing:${Versions.arch_core}"
}

object Coroutines {
    val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
    val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"
}

object Fragment {
    val runtime = "androidx.fragment:fragment:${Versions.fragment}"
    val runtimeKtx = "androidx.fragment:fragment-ktx:${Versions.fragment}"
    val testing = "androidx.fragment:fragment-testing:${Versions.fragment}"
}

object Lifecycle {
    val runtime = "androidx.lifecycle:lifecycle-runtime:${Versions.lifecycle}"
    val java8 = "androidx.lifecycle:lifecycle-common-java8:$[Versions.lifecycle]"
    val compiler = "androidx.lifecycle:lifecycle-compiler:${Versions.lifecycle}"
    val viewmodel = "androidx.lifecycle:lifecycle-viewmodel:${Versions.lifecycle}"
    val viewmodelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycle}"
    val livedataKtx = "androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycle}"
    val extensions = "androidx.lifecycle:lifecycle-extensions:${Versions.lifecycle}"
}

object Kotlin {
    val stdlibJdk7 = "org.jetbrains.kotlin:kotlin-stdlib-jdk7:${Versions.kotlin}"
    val stdlibJdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
    val allopen = "org.jetbrains.kotlin:kotlin-allopen:$Versions.kotlin"
    val plugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    val test = "org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}"
}

object Umeng {
    val analytics = "com.umeng.umsdk:analytics:${Versions.umeng_analytics}"
    val umengCommon = "com.umeng.umsdk:common:${Versions.umeng_common}"
}

object Retrofit {
    val runtime = "com.squareup.retrofit2:retrofit:${Versions.retrofit}"
    val gson = "com.squareup.retrofit2:converter-gson:${Versions.retrofit}"
    val mock = "com.squareup.retrofit2:retrofit-mock:${Versions.retrofit}"
}

object Atsl {
    val core = "androidx.test:core:${Versions.atsl_core}"
    val ext_junit = "androidx.test.ext:junit:${Versions.atsl_junit}"
    val runner = "androidx.test:runner:${Versions.atsl_runner}"
    val rules = "androidx.test:rules:${Versions.atsl_rules}"
}

object Room {
    val runtime = "androidx.room:room-runtime:${Versions.room}"
    val compiler = "androidx.room:room-compiler:${Versions.room}"
    val ktx = "androidx.room:room-ktx:${Versions.room}"
    val rxjava2 = "androidx.room:room-rxjava2:${Versions.room}"
    val testing = "androidx.room:room-testing:${Versions.room}"
}

object Navigation {
    val runtime = "androidx.navigation:navigation-runtime:${Versions.navigation}"
    val runtimeKtx = "androidx.navigation:navigation-runtime-ktx:${Versions.navigation}"
    val fragment = "androidx.navigation:navigation-fragment:${Versions.navigation}"
    val fragmentKtx = "androidx.navigation:navigation-fragment-ktx:${Versions.navigation}"
    val testing = "androidx.navigation:navigation-testing:${Versions.navigation}"
    val ui = "androidx.navigation:navigation-ui:${Versions.navigation}"
    val ui_ktx = "androidx.navigation:navigation-ui-ktx:${Versions.navigation}"
    val safe_args_plugin = "androidx.navigation:navigation-safe-args-gradle-plugin:${Versions.navigation}"
}

object Dependency {
    val androidGradlePlugin = "com.android.tools.build:gradle:${Versions.androidGradlePlugin}"
    val material = "com.google.android.material:material:${Versions.material}"
    val liveEventBusX = "com.jeremyliao:live-event-bus-x:${Versions.liveEventBusX}"
    val okhttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.okhttpLoggingInterceptor}"
    val pagingRuntime = "androidx.paging:paging-runtime:${Versions.paging}"

    val junit = "junit:junit:${Versions.junit}"
    val androidTestJunit = "androidx.test.ext:junit:${Versions.junitExt}"
    val espressoCore = "androidx.test.espresso:espresso-core:${Versions.espressoCore}"
    val jDatabinding = "com.hi-dhl:jdatabinding:${Versions.jDatabinding}"
    val timber = "com.jakewharton.timber:timber:${Versions.timber}"
    val benchmark = "androidx.benchmark:benchmark-junit4:${Versions.benchmark}"
    val benchmarkGradle = "androidx.benchmark:benchmark-gradle-plugin:${Versions.benchmark}"
}

object Koin {
    val core = "org.koin:koin-core:${Versions.koin}"
    val androidCore = "org.koin:koin-android:${Versions.koin}"
    val viewmodel = "org.koin:koin-androidx-viewmodel:${Versions.koin}"
    val androidScope = "org.koin:koin-android-scope:$${Versions.koin}"
}

