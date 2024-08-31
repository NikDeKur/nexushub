@file:Suppress("PropertyName")

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.licenser)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shadowJar)
    id("maven-publish")
}

group = "dev.nikdekur.nexushub"
version = "1.1.1"

val authorId: String by project
val authorName: String by project


repositories {
    mavenCentral()
    mavenLocal()
    google()
}

dependencies {
    implementation(project(":common"))
    implementation(project(":ktor-utils"))

    implementation(libs.ndkore)
    implementation(libs.logback)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.barray)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.atomicfu)
    implementation(libs.guava)


    testImplementation(kotlin("test"))
    testImplementation(libs.slf4j.api)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.koin)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    // Include all output directories and runtime classpath from all subprojects
    allprojects.forEach { project ->
        from(project.sourceSets.main.get().allSource)
    }
}


val javaVersion = JavaVersion.VERSION_1_8
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
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


val repoUsernameProp = "NDK_REPO_USERNAME"
val repoPasswordProp = "NDK_REPO_PASSWORD"
val repoUsername = System.getenv(repoUsernameProp)
val repoPassword = System.getenv(repoPasswordProp)

if (repoUsername.isNullOrBlank() || repoPassword.isNullOrBlank()) {
    throw GradleException("Environment variables $repoUsernameProp and $repoPasswordProp must be set.")
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

            afterEvaluate {
                val shadowJar = tasks.findByName("shadowJar")
                if (shadowJar == null) from(components["java"])
                else artifact(shadowJar)

                // Source jar
                artifact(tasks.named("sourcesJar", Jar::class.java))
            }
        }
    }

    repositories {
        maven {
            name = "ndk-repo"
            url = uri("https://repo.nikdekur.tech/releases")
            credentials {
                username = repoUsername
                password = repoPassword
            }
        }

        mavenLocal()
    }
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    archiveFileName.set("${project.name}-${project.version}.jar")

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}