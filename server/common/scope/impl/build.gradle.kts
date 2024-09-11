group = "dev.nikdekur.nexushub.scope"
version = "1.0.0"

dependencies {
    implementation(project(":server:common:storage:api"))
    implementation(libs.guava)

    testImplementation(project(":server:common:storage:impl"))
}