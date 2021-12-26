plugins {
    `kotlin-dsl`
    `kotlin-dsl-precompiled-script-plugins`
}

buildscript {

    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0")
    }
}


repositories {
    mavenLocal()
    mavenCentral()
    google()
}