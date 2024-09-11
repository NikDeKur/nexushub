group = "dev.nikdekur.nexushub"
version = "1.0.0"

dependencies {
    implementation(project(":server:common:access:api"))
    implementation(project(":server:common:access:impl"))

    implementation(project(":server:common:account:api"))
    implementation(project(":server:common:account:impl"))

    implementation(project(":server:common:authentication:api"))
    implementation(project(":server:common:authentication:impl"))

    implementation(project(":server:common:dataset:api"))
    implementation(project(":server:common:dataset:impl"))

    implementation(project(":server:common:node:api"))
    implementation(project(":server:common:node:impl"))

    implementation(project(":server:common:ping:api"))
    implementation(project(":server:common:ping:impl"))

    implementation(project(":server:common:protection:api"))
    implementation(project(":server:common:protection:impl"))

    implementation(project(":server:common:ratelimit:api"))
    implementation(project(":server:common:ratelimit:impl"))

    implementation(project(":server:common:scope:api"))
    implementation(project(":server:common:scope:impl"))

    implementation(project(":server:common:serial:api"))
    implementation(project(":server:common:serial:impl"))

    implementation(project(":server:common:session:api"))
    implementation(project(":server:common:session:impl"))

    implementation(project(":server:common:setup:api"))
    implementation(project(":server:common:setup:impl"))

    implementation(project(":server:common:storage:api"))
    implementation(project(":server:common:storage:impl"))


    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.html.builder)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.barray)
    implementation(libs.yamlkt)
    implementation(libs.guava)

    testImplementation(project(":server:core:impl"))
}