@file:Suppress("PropertyName")


val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val log4j2_version: String by project

plugins {
    kotlin("jvm") version "1.9.24"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24"
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
    implementation(project(":common"))

//    implementation("org.apache.logging.log4j:log4j-api:$log4j2_version")
//    implementation("org.apache.logging.log4j:log4j-core:$log4j2_version")

    implementation("ch.qos.logback:logback-classic:$logback_version")


    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")
    implementation("com.google.guava:guava:33.2.0-jre")
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

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}