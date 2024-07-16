@file:Suppress("PropertyName")


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

license {
    header(project.file("HEADER"))
    properties {
        set("year", "2024-present")
        set("name", authorName)
    }
    ignoreFailures = true
}

repositories {
    mavenCentral()
    mavenLocal()
    google()
}

dependencies {
    implementation(project(":common"))

    implementation(libs.ndkore)
    implementation(libs.logback)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.websockets)
    implementation(libs.atomicfu)
    implementation(libs.guava)


}

val javaVersion = JavaVersion.VERSION_11
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion))
    }
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

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}