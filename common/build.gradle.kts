@file:Suppress("PropertyName")

import org.gradle.kotlin.dsl.compileOnly
import org.gradle.kotlin.dsl.libs
import org.gradle.kotlin.dsl.test
import org.gradle.kotlin.dsl.testImplementation


plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.licenser)
    alias(libs.plugins.kotlinSerialization)
    id("maven-publish")
}

group = "dev.nikdekur.nexushub"
version = "1.0.2"

val authorId: String by project
val authorName: String by project

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    compileOnly(libs.ndkore)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.kotlinx.serialization)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}


val javaVersion = JavaVersion.VERSION_11
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    // withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion))
    }
}

license {
    header(project.file("../HEADER"))
    properties {
        set("year", "2024-present")
        set("name", authorName)
    }
    ignoreFailures = true
}




publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            pom {
                developers {
                    developer {
                        id.set(authorId)
                        name.set(authorName)
                    }
                }
            }

            from(components["java"])
        }
    }
}