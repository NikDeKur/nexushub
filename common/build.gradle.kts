@file:Suppress("PropertyName")

val ndkore_version: String by project


plugins {
    kotlin("jvm") version "2.0.0"
    id("maven-publish")
}

group = "org.ndk.nexushub"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    api("dev.nikdekur:ndkore:$ndkore_version")
    api("com.google.code.gson:gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")



    // Logging
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("ch.qos.logback:logback-classic:1.5.6")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
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
    withJavadocJar()
    withSourcesJar()
}


// Publish to local maven repository
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
