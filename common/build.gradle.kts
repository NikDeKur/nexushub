@file:Suppress("PropertyName")

val ndkglobal_version: String by project


plugins {
    kotlin("jvm")
    id("maven-publish")
}

group = "org.ndk.nexushub"
version = "1.0.0"

repositories {
    mavenCentral()
    flatDir {
        dirs("C:/Users/nikdekur/.m2/repository/org/ndk/NDKGlobal/1.0.0")
    }
}

dependencies {
    api("org.ndk:NDKGlobal:$ndkglobal_version")
    api("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

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
