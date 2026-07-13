import java.io.File

plugins {
    java
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    // ADR-0021: generierter, publizierter API-Vertrag (OpenAPI).
    // Der Task `generateOpenApiDocs` startet die App kurz und schreibt die Spec (siehe openApi{} unten).
    id("org.springdoc.openapi-gradle-plugin") version "1.9.0"
}

group = "io.d4y"
// ADR-0022: Git-Tag `vX.Y.Z` ist die Versions-Wahrheit. CI übergibt `-Pversion=${TAG#v}`;
// zwischen Releases gilt die Dev-Default.
version = providers.gradleProperty("version").getOrElse("0.1.0-SNAPSHOT")
description = "D4Y — Git-native Runtime Platform"

java {
    // ADR-0003: Java 21. Toolchain wird bei Bedarf via foojay bezogen.
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// ADR-0022: Version & Build-Zeit zur Laufzeit — füllt /actuator/info und liefert einen
// BuildProperties-Bean (von OpenApiConfig für info.version genutzt).
springBoot {
    buildInfo()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Docker-HTTP-Adapter: reactor-netty spricht über den Unix-Socket mit der Engine.
    implementation("io.projectreactor.netty:reactor-netty-http")

    // Desired-State aus YAML (lokal oder aus dem Git-Config-Repo).
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

    // Git-Anbindung des Config-Repositories (ADR-0019): pure-Java, kein git-Binary im Image.
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.10.0.202406032230-r")

    // ADR-0021: OpenAPI-Vertrag der /api-Endpunkte aus den Controllern generieren.
    // `-api`-Variante (nur /v3/api-docs[.yaml], keine Swagger-UI) — vermeidet Kollision mit dem
    // statischen Frontend-Resource-Handling (ADR-0014).
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.6")

    // Native Netty-Transports für Unix-Domain-Sockets:
    //  - kqueue: macOS (Entwicklung), epoll: Linux (Produktion, x86_64 + aarch64).
    //  Netty-Klassifikatoren: aarch_64 / x86_64 (mit Unterstrich). Beide Linux-Klassifikatoren im
    //  Bundle — Netty wählt den passenden zur Laufzeit (ADR-0027: Multi-Arch-Auslieferung).
    runtimeOnly("io.netty:netty-transport-native-kqueue::osx-aarch_64")
    runtimeOnly("io.netty:netty-transport-native-epoll::linux-x86_64")
    runtimeOnly("io.netty:netty-transport-native-epoll::linux-aarch_64")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ---- Frontend: Next.js Static Export → wird ins Backend-Jar eingebettet ----
// ADR-0006 (Single-Image) / ADR-0014: Ein `gradlew build` erzeugt ein Artefakt,
// das UI und API zusammen auf server.port ausliefert (kein Node zur Laufzeit).
// Mit `-PskipFrontend` wird der Frontend-Build übersprungen (reine Backend-Iteration).
val frontendDir = layout.projectDirectory.dir("frontend")
val frontendOut = frontendDir.dir("out")
val skipFrontend = providers.gradleProperty("skipFrontend").isPresent

// npm-Auflösung: Gradle (z. B. aus IntelliJ/Daemon gestartet) erbt auf macOS oft nur einen
// minimalen PATH ohne Homebrew (`/opt/homebrew/bin`). Wir suchen die npm-Executable in PATH
// plus gängigen Node-Verzeichnissen und stellen sicher, dass `node` für die Tasks auffindbar ist.
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val extraNodeDirs = listOf("/opt/homebrew/bin", "/usr/local/bin")
fun findExecutable(exe: String): File? {
    val name = if (isWindows) "$exe.cmd" else exe
    val pathDirs = (System.getenv("PATH") ?: "").split(File.pathSeparator)
    return (pathDirs + extraNodeDirs)
        .filter { it.isNotBlank() }
        .map { File(it, name) }
        .firstOrNull { it.canExecute() }
}
val npmFile = findExecutable("npm")
val npm = npmFile?.absolutePath ?: (if (isWindows) "npm.cmd" else "npm")
// npm ruft intern `node` auf → dessen Verzeichnis muss auf dem Task-PATH liegen.
val augmentedPath = (listOfNotNull(npmFile?.parent) + (System.getenv("PATH") ?: ""))
    .filter { it.isNotBlank() }
    .joinToString(File.pathSeparator)

val frontendInstall by tasks.registering(Exec::class) {
    group = "frontend"
    description = "Installiert die Frontend-Abhängigkeiten (npm ci)."
    workingDir = frontendDir.asFile
    environment("PATH", augmentedPath)
    commandLine(npm, "ci")
    inputs.file(frontendDir.file("package.json"))
    inputs.file(frontendDir.file("package-lock.json"))
    outputs.file(frontendDir.file("node_modules/.package-lock.json"))
}

val frontendExport by tasks.registering(Exec::class) {
    group = "frontend"
    description = "Baut den statischen Next.js-Export nach frontend/out."
    dependsOn(frontendInstall)
    workingDir = frontendDir.asFile
    environment("D4Y_STATIC_EXPORT", "1")
    environment("PATH", augmentedPath)
    commandLine(npm, "run", "build")
    inputs.dir(frontendDir.dir("app"))
    inputs.dir(frontendDir.dir("components"))
    inputs.dir(frontendDir.dir("lib"))
    inputs.file(frontendDir.file("next.config.mjs"))
    inputs.file(frontendDir.file("package.json"))
    inputs.file(frontendDir.file("package-lock.json"))
    inputs.file(frontendDir.file("tsconfig.json"))
    outputs.dir(frontendOut)
}

tasks.processResources {
    if (!skipFrontend) {
        dependsOn(frontendExport)
        from(frontendOut) { into("static") }
    }
}

// ---- ADR-0021: API-Vertrag (OpenAPI) generieren ----
// `./gradlew generateOpenApiDocs -PskipFrontend` startet die App kurz auf einem freien Port,
// zieht die YAML-Spec von /v3/api-docs.yaml und schreibt sie nach docs/api/openapi.yaml.
// Das File wird versioniert; CI erzeugt es bei Änderungen neu (Drift-Schutz).
// Kein laufender Server für Konsumenten nötig — der gepinnte Vertrag liegt im Repo.
openApi {
    apiDocsUrl.set("http://localhost:8080/v3/api-docs.yaml")
    outputDir.set(layout.projectDirectory.dir("docs/api"))
    outputFileName.set("openapi.yaml")
    waitTimeInSeconds.set(60)
}

// ---- ADR-0027: Selbst-enthaltendes Host-Bundle (jlink-Runtime + jpackage app-image) ----
// Ersetzt den früheren OCI-Image-Build (bootBuildImage/GHCR). d4y läuft direkt auf dem Host unter
// systemd (kein System-Java): `./gradlew bundleTar` erzeugt build/dist/d4y-<version>.tar.gz mit der
// App (Fat-Jar inkl. Frontend) und einem eingebetteten Minimal-JRE. Das Tarball ist das
// Release-Asset (siehe .github/workflows/release.yml, site/install.sh).
val bundleLauncher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(21)
}
// java.se = voller SE-Aggregator (kompatibilitätssicher; vermeidet fehlende Module zur Laufzeit),
// plus die von Netty/TLS/JGit genutzten jdk.*-Module. Bei Bedarf später via `jdeps` trimmen.
val jlinkModules = "java.se,jdk.unsupported,jdk.crypto.ec,jdk.crypto.cryptoki,jdk.zipfs"
val jlinkOut = layout.buildDirectory.dir("jlink-runtime")
val bundleInput = layout.buildDirectory.dir("bundle-input")
val distDir = layout.buildDirectory.dir("dist")

val jlinkRuntime by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Erzeugt ein minimales JRE via jlink (wird ins Bundle eingebettet)."
    val binDir = bundleLauncher.map { it.metadata.installationPath.dir("bin").asFile.absolutePath }
    doFirst {
        delete(jlinkOut)
        commandLine(
            "${binDir.get()}/jlink",
            "--add-modules", jlinkModules,
            "--strip-debug", "--no-header-files", "--no-man-pages", "--compress=zip-6",
            "--output", jlinkOut.get().asFile.absolutePath
        )
    }
    outputs.dir(jlinkOut)
}

// Nur das ausführbare bootJar (kein *-plain.jar) als jpackage-Input isolieren.
val stageBundleInput by tasks.registering(Copy::class) {
    group = "distribution"
    from(tasks.named("bootJar"))
    into(bundleInput)
}

val jpackageImage by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Baut das app-image (App + eingebettetes JRE) via jpackage."
    dependsOn(jlinkRuntime, stageBundleInput)
    val binDir = bundleLauncher.map { it.metadata.installationPath.dir("bin").asFile.absolutePath }
    val bootJarName = tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar")
        .flatMap { it.archiveFileName }
    doFirst {
        delete(distDir.get().dir("d4y"))
        // Main-Class kommt aus dem Spring-Boot-Manifest (JarLauncher) — kein --main-class nötig.
        commandLine(
            "${binDir.get()}/jpackage",
            "--type", "app-image",
            "--name", "d4y",
            "--input", bundleInput.get().asFile.absolutePath,
            "--main-jar", bootJarName.get(),
            "--runtime-image", jlinkOut.get().asFile.absolutePath,
            "--dest", distDir.get().asFile.absolutePath
        )
    }
    outputs.dir(distDir.map { it.dir("d4y") })
}

// System-`tar` statt Gradle-Tar: erhält das Ausführbar-Bit von bin/d4y und runtime/bin/java.
val bundleTar by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Packt das app-image in build/dist/d4y-<version>.tar.gz (Release-Asset)."
    dependsOn(jpackageImage)
    val out = distDir.map { it.file("d4y-${project.version}.tar.gz").asFile.absolutePath }
    doFirst {
        commandLine("tar", "-czf", out.get(),
            "-C", distDir.get().asFile.absolutePath, "d4y")
    }
    outputs.file(distDir.map { it.file("d4y-${project.version}.tar.gz") })
}
