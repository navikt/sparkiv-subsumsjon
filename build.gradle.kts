group = "no.nav.helse"

plugins {
    alias(libs.plugins.sykepenger.deployable)
}

sykepengerDeployable {
    mainClass = "no.nav.helse.sparkiv.AppKt"
}

dependencies {
    implementation(libs.kafka)
    implementation(libs.naisful.app)
    implementation(libs.bundles.db)
    implementation(libs.bundles.logging)
    testImplementation(libs.tc.kafka)
    testImplementation(libs.tc.pg)
}

tasks.processResources {
    // Frontenden bygges med pnpm utenfor Gradle (se frontend/README.md og .github/actions/bygg-frontend).
    // De ferdigbygde statiske filene pakkes inn som statiske ressurser, og serveres av backend-appen (se App.kt).
    from(layout.projectDirectory.dir("frontend/dist")) {
        into("static")
    }
}
