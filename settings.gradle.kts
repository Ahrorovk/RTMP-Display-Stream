pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Добавьте эту строку
    }
}

rootProject.name = "Real_stream"
include(":app")
include(
    ":library",
    ":encoder",
    ":rtmp",
    ":rtsp",
    ":srt",
    ":udp",
    ":common",
    ":extra-sources",
//    ":app"
)

//project(":pedroSG94-RootEncoder-dfea510").projectDir = File(rootDir, "pedroSG94-RootEncoder-dfea510")
project(":library").projectDir = File(rootDir, "../pedroSG94-RootEncoder-dfea510/library")
project(":encoder").projectDir = File(rootDir, "../pedroSG94-RootEncoder-dfea510/encoder")
project(":rtmp").projectDir = File(rootDir, "../pedroSG94-RootEncoder-dfea510/rtmp")
project(":rtsp").projectDir = File(rootDir, "../pedroSG94-RootEncoder-dfea510/rtsp")
project(":srt").projectDir = File(rootDir, "../pedroSG94-RootEncoder-dfea510/srt")
project(":udp").projectDir = File(rootDir, "../pedroSG94-RootEncoder-dfea510/udp")
project(":common").projectDir = File(rootDir, "../pedroSG94-RootEncoder-dfea510/common")
project(":extra-sources").projectDir = File(rootDir, "../pedroSG94-RootEncoder-dfea510/extra-sources")
//project(":app").projectDir = File(rootDir, "pedroSG94-RootEncoder-dfea510/app")
