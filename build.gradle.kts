plugins {
    kotlin("jvm") version "2.0.21"
}

group = "no.nav.helse"

dependencies {
    implementation(libs.kafka)
    implementation(libs.naisful.app)
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
