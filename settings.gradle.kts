pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.aliyun.com/repository/jcenter")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
        }
        maven {
            url = uri("https://repo1.maven.org/maven2/")
        }
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenLocal()
        mavenCentral()
        maven {
            url = uri("https://maven.aliyun.com/repository/jcenter")
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
        }
        maven {
            url = uri("https://repo1.maven.org/maven2/")
        }
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "amupdf-android"

include(":app")

include(":reader")
include(":readercommon")
include(":vudroid")
//include (":core-ui")
include(":paddle-ocr")
include(":pdf-recyclerview")
include(":subsampling-scale-image-view")

include(":mupdf-android-fitz")
project(":mupdf-android-fitz").projectDir = File("solib/mupdf-android-fitz")
