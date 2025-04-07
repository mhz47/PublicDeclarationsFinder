plugins {
    kotlin("jvm") version "2.1.20"
    application
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
}

group = "org.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.1.20")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("MainKt")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "MainKt",
        )
    }
    from({
        configurations.runtimeClasspath.get().filter {
            it.exists()
        }.map {
            if (it.isDirectory) it else zipTree(it)
        }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
