import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    `maven-publish`
    signing
}

group = "dev.zodiia"
version = "1.0.1"
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

java {
    withJavadocJar()
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    publications {
        create<MavenPublication>("release") {
            artifactId = rootProject.name
            groupId = rootProject.group.toString()
            version = rootProject.version.toString()

            pom {
                name.set("Simple DI")
                description.set(rootProject.description)
                url.set("https://github.com/zodiia/simple-di")

                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://spdx.org/licenses/MIT.html")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/zodiia/simple-di.git")
                    url.set("https://github.com/zodiia/simple-di")
                }
                developers {
                    developer {
                        name.set("Manon Grivot")
                        email.set("hey@manongrivot.me")
                        url.set("https://manongrivot.me")
                        organization.set("Zodiia")
                        organizationUrl.set("https://zodiia.dev")
                    }
                }
            }

            from(components["java"])

            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
        }
    }

    repositories {
        maven {
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            val ossrhUsername: String by project
            val ossrhPassword: String by project

            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            name = "mavenCentral"
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications["release"])
    sign(configurations.archives.get())
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
