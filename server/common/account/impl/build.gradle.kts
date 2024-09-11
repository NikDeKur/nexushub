group = "dev.nikdekur.nexushub.account"
version = "1.0.0"

dependencies {
    implementation(project(":server:common:storage:api"))
    implementation(project(":server:common:protection:api"))

    testImplementation(project(":server:common:storage:impl"))
    testImplementation(project(":server:common:protection:impl"))
}