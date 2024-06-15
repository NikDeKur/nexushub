@file:Suppress("PropertyName")

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project

plugins {
    kotlin("jvm") version "1.9.24"
    application
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"
}

group = "org.ndk.nexushub"
version = "1.0.0"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-config-yaml:3.0.0-beta-1")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
}

val javaVersion = JavaVersion.VERSION_11
val jlv = JavaLanguageVersion.of(javaVersion.majorVersion)
kotlin {
    jvmToolchain {
        languageVersion.set(jlv)
    }
}
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    toolchain {
        languageVersion.set(jlv)
    }
}

