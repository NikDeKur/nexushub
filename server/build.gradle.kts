@file:Suppress("PropertyName")

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.licenser)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    application
    id("maven-publish")
}

group = "dev.nikdekur.nexushub"
version = "1.0.2"

val authorId: String by project
val authorName: String by project

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    mavenLocal()
}

license {
    header(project.file("HEADER"))
    properties {
        set("year", "2024-present")
        set("name", authorName)
    }
    ignoreFailures = true
}

dependencies {
    implementation(project(":common"))
    implementation(libs.common)
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.html.builder)
    implementation(libs.gson)
    implementation(libs.ndkore)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.yamlkt)
    implementation(libs.mongodb)
    implementation(libs.bouncycastle)
    implementation(libs.guava)
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
}

val javaVersion = JavaVersion.VERSION_11
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    withJavadocJar()
    withSourcesJar()
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