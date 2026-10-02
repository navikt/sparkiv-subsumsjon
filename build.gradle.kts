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

val frontendDir = layout.projectDirectory.dir("frontend")

// pnpm er ikke installert på GitHub-runnerne, så vi kjører versjonen fra packageManager i package.json via npx.
val pnpm =
    Regex(""""packageManager"\s*:\s*"(pnpm@[^"]+)"""")
        .find(frontendDir.file("package.json").asFile.readText())
        ?.groupValues
        ?.get(1)
        ?: error("Fant ikke packageManager i frontend/package.json")

val installerFrontend =
    tasks.register<Exec>("installerFrontend") {
        workingDir = frontendDir.asFile
        commandLine("npx", "--yes", pnpm, "install", "--frozen-lockfile")
        inputs.files(frontendDir.file("package.json"), frontendDir.file("pnpm-lock.yaml"))
        outputs.dir(frontendDir.dir("node_modules"))
    }

val byggFrontend =
    tasks.register<Exec>("byggFrontend") {
        dependsOn(installerFrontend)
        workingDir = frontendDir.asFile
        commandLine("npx", "--yes", pnpm, "run", "build")
        inputs.files(
            frontendDir.dir("src"),
            frontendDir.dir("public"),
            frontendDir.files("index.html", "package.json", "pnpm-lock.yaml", "vite.config.ts"),
            frontendDir.asFileTree.matching { include("tsconfig*.json") },
        )
        outputs.dir(frontendDir.dir("dist"))
    }

tasks.processResources {
    // Frontendens ferdigbygde statiske filer pakkes inn som statiske ressurser, og serveres av backend-appen (se App.kt).
    from(byggFrontend) {
        into("static")
    }
}
