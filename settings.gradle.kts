dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            version("tbd-libs", "2024.11.29-15.07-105481e3")

            library("kafka", "com.github.navikt.tbd-libs", "kafka").versionRef("tbd-libs")
            library("naisful-app", "com.github.navikt.tbd-libs", "naisful-app").versionRef("tbd-libs")
        }
    }
}

rootProject.name = "sparkiv-subsumsjon"

