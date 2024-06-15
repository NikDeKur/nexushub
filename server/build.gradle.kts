@file:Suppress("PropertyName")


val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val log4j2_version: String by project

plugins {
    kotlin("jvm") version "1.9.24"
    application
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"
    id("maven-publish")
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
    implementation(project(":common"))

    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-websockets-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
//    implementation("org.apache.logging.log4j:log4j-api:$log4j2_version")
//    implementation("org.apache.logging.log4j:log4j-core:$log4j2_version")

    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("io.ktor:ktor-server-config-yaml:3.0.0-beta-1")

    // https://mvnrepository.com/artifact/org.bouncycastle/bcprov-jdk18on
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.0.0-beta0")
    implementation("com.google.guava:guava:33.2.0-jre")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
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
                        id.set("nikdekur")
                        name.set("Nik De Kur")
                    }
                }
            }

            from(components["java"])
        }
    }
}