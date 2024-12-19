plugins {
    kotlin("jvm") version "2.0.21"
}

group = "no.nav.helse"

dependencies {
    implementation(libs.kafka)
    implementation(libs.naisful.app)
    implementation(libs.bundles.db)
    implementation(libs.bundles.logging)
    testImplementation(libs.tc.kafka)
    testImplementation(libs.tc.pg)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

tasks {
    jar {
        archiveBaseName.set("app")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.sparkiv.AppKt"
            attributes["Class-Path"] =
                configurations.runtimeClasspath.get().joinToString(separator = " ") {
                    it.name
                }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("${layout.buildDirectory.get()}/libs/${it.name}")
                if (!file.exists()) it.copyTo(file)
            }
        }
    }
}
