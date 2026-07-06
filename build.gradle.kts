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
