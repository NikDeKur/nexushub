group = "dev.nikdekur.nexushub.storage"
version = "1.0.0"

dependencies {
    implementation(project(":server:core:api"))
    implementation(project(":server:common:storage:api"))

    implementation(libs.mongodb)
}