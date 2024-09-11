group = "dev.nikdekur.nexushub.access"
version = "1.0.0"

dependencies {
    implementation(project(":server:common:authentication:api"))
    implementation(project(":server:common:account:api"))
    implementation(project(":server:common:protection:api"))
    implementation(project(":server:common:ratelimit:api"))
    implementation(project(":server:common:session:api"))
    implementation(project(":server:common:node:api"))
    implementation(project(":server:common:ping:api"))
}