@file:Suppress("PropertyName")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.licenser)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.shadowJar)
    application
}

group = "dev.nikdekur.nexushub"
version = "1.1.0"

val authorId: String by project
val authorName: String by project

application {
    mainClass.set("dev.nikdekur.nexushub.NexusHubServerBoot")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":common"))
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.html.builder)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.gson)
    implementation(libs.ndkore)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.yamlkt)
    implementation(libs.mongodb)
    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.pkix)
    implementation(libs.guava)
    implementation(libs.koin)
    testImplementation(libs.kotlin.test.junit)
}

val javaVersion = JavaVersion.VERSION_11
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    withJavadocJar()
    withSourcesJar()
}


license {
    header(project.file("../HEADER"))
    properties {
        set("year", "2024-present")
        set("name", authorName)
    }
    ignoreFailures = true
}



// Remove Java compatibility made by params non-null assertions
tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll("-Xno-param-assertions", "-Xno-call-assertions")
    }
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    archiveFileName.set("${project.name}-${project.version}.jar")

    // Include all output directories and runtime classpath from all subprojects
    allprojects.forEach { project ->
        from(project.sourceSets.main.get().output)
        configurations.add(project.configurations.runtimeClasspath.get())
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}