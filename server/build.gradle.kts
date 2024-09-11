@file:Suppress("PropertyName")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.libs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask


plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.licenser)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
    alias(libs.plugins.shadowJar)
    application
}

group = "dev.nikdekur.nexushub"
version = "1.2.0"

val authorName: String by project

application {
    mainClass.set("dev.nikdekur.nexushub.boot.ConsoleNexusHubServerBoot")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}


subprojects {

    apply {
        plugin(rootProject.libs.plugins.kotlinJvm.get().pluginId)
        plugin(rootProject.libs.plugins.kotlinSerialization.get().pluginId)
    }

    val libs = rootProject.libs

    dependencies {
        implementation(project(":common"))
        implementation(project(":ktor-utils"))

        implementation(libs.ndkore)
        implementation(libs.kotlinx.coroutines)
        implementation(libs.kotlinx.serialization)
        implementation(libs.slf4j.api)

        val path = project.path
        if (!path.contains(":server:core:api")) {
            implementation(project(":server:core:api"))
            testImplementation(project(":server:core:api"))

            if (path.contains("impl")) {
                implementation(project(":server:common:dataset:api"))
                testImplementation(project(":server:common:dataset:api"))
            }
        }

        if (path.endsWith(":impl")) {
            val apiPath = path.replace(":impl", ":api")
            implementation(project(apiPath))
            testImplementation(project(apiPath))
        }

        testImplementation(kotlin("test"))
        testImplementation(libs.kotlinx.serialization.barray)
        testImplementation(libs.kotlinx.coroutines.test)
        testImplementation(libs.logback)
    }

    tasks.test {
        useJUnitPlatform()
    }

    val javaVersion = JavaVersion.VERSION_11
    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion))
        }
    }

    // Remove Java compatibility made by params non-null assertions
    tasks.named("compileKotlin", KotlinCompilationTask::class.java) {
        compilerOptions {
            freeCompilerArgs.addAll("-Xno-param-assertions", "-Xno-call-assertions")
        }
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