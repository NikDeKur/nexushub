group = "dev.nikdekur.nexushub.authentication"
version = "1.0.0"

dependencies {
    implementation(project(":server:common:account:api"))
    implementation(project(":server:common:protection:api"))
    implementation(project(":server:common:node:api"))

    testImplementation(project(":server:common:storage:api"))
    testImplementation(project(":server:common:storage:impl"))
    testImplementation(project(":server:common:protection:impl"))
    testImplementation(project(":server:common:node:impl"))
    testImplementation(project(":server:common:account:impl"))
}