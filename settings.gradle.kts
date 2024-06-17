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

rootProject.name = "amupdf"

include(":app")

include(":reader")
include(":readercommon")
include(":vudroid")
//include ":core-ui"
include(":paddle-ocr")
include(":pdf-recyclerview")

//include ":library"
//include ":editor"
//include ":wheel"
include(":mupdf-android-fitz")

//project(":library").projectDir = new File("solib/library")
//project(":editor").projectDir = new File("editor")
//project(":wheel").projectDir = new File("wheel")
project(":mupdf-android-fitz").projectDir = File ("solib/mupdf-android-fitz")
