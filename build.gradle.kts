import java.io.File

plugins {
    java
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.d4y"
version = "0.1.0-SNAPSHOT"
description = "D4Y — Git-native Runtime Platform"

java {
    // ADR-0003: Java 21. Toolchain wird bei Bedarf via foojay bezogen.
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Docker-HTTP-Adapter: reactor-netty spricht über den Unix-Socket mit der Engine.
    implementation("io.projectreactor.netty:reactor-netty-http")

    // Desired-State aus lokaler YAML (ADR-0011 Interim).
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

    // Native Netty-Transports für Unix-Domain-Sockets:
    //  - kqueue: macOS (Entwicklung), epoll: Linux (Produktion).
    //  Netty-Klassifikatoren: aarch_64 / x86_64 (mit Unterstrich).
    runtimeOnly("io.netty:netty-transport-native-kqueue::osx-aarch_64")
    runtimeOnly("io.netty:netty-transport-native-epoll::linux-x86_64")

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
