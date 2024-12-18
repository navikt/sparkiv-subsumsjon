dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            version("tbd-libs", "2024.11.29-15.07-105481e3")
            version("flyway", "11.1.0")
            version("postgres", "42.7.3")
            version("kotliquery", "1.9.0")
            version("hikaricp", "5.1.0")
            version("testcontainers", "1.20.4")
            version("logback", "1.5.12")
            version("logstash", "8.0")

            library("kafka", "com.github.navikt.tbd-libs", "kafka").versionRef("tbd-libs")
            library("naisful-app", "com.github.navikt.tbd-libs", "naisful-app").versionRef("tbd-libs")

            library("flyway-core", "org.flywaydb", "flyway-core").versionRef("flyway")
            library("flyway-pg", "org.flywaydb", "flyway-database-postgresql").versionRef("flyway")

            library("kotliquery", "com.github.seratch", "kotliquery").versionRef("kotliquery")

            library("postgres", "org.postgresql", "postgresql").versionRef("postgres")

            library("hikaricp", "com.zaxxer", "HikariCP").versionRef("hikaricp")

            library("tc-pg", "org.testcontainers", "postgresql").versionRef("testcontainers")
            library("tc-kafka", "org.testcontainers", "kafka").versionRef("testcontainers")

            library("logback", "ch.qos.logback", "logback-classic").versionRef("logback")
            library("logstash", "net.logstash.logback", "logstash-logback-encoder").versionRef("logstash")

            bundle("db", listOf("flyway-core", "flyway-pg", "kotliquery", "postgres", "hikaricp"))
            bundle("logging", listOf("logback", "logstash"))

        }
    }
}

rootProject.name = "sparkiv-subsumsjon"

