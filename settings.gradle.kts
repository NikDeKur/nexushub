plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "nexushub"

include(":ktor-utils")
include(":common")
include(":server")
include(":client")
