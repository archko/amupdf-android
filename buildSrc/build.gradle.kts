plugins {
    `kotlin-dsl`
    //`kotlin-dsl-precompiled-script-plugins`
}

buildscript {

    repositories {
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
        }
        mavenLocal()
        mavenCentral()
        google()
    }

    dependencies {
        //classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
    }
}


repositories {
    maven {
        url = uri("https://maven.aliyun.com/repository/public")
    }
    maven {
        url = uri("https://maven.aliyun.com/repository/google")
    }
    mavenLocal()
    mavenCentral()
    google()
}