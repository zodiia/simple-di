import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
}

group = "moe.zodiia"
version = "1.0-SNAPSHOT"
description = "Simple dependency injection library for Kotlin (JVM)"

repositories {
    mavenCentral()
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt", "detekt-formatting", "1.22.0")
}

detekt {
    parallel = true
    config = files("./detekt.yml")
    buildUponDefaultConfig = true
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Detekt> {
    reports {
        html {
            required.set(true)
        }
    }
}
