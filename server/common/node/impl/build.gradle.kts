group = "dev.nikdekur.nexushub.node"
version = "1.0.0"

dependencies {
    implementation(project(":server:common:account:api"))
    implementation(project(":server:common:storage:api"))
    implementation(project(":server:common:session:api"))
    implementation(project(":server:common:scope:api"))
    implementation(project(":server:common:serial:api"))
}