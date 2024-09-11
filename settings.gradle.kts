import jdk.nashorn.internal.runtime.Debug.id
import jdk.tools.jlink.resources.plugins

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "nexushub"

// Shared Modules
include(":common")
include(":ktor-utils")


// Client Modules
include(":client")


// Server Modules
include(":server")

include(":server:core:api")
include(":server:core:impl")

include(":server:common:access:api")
include(":server:common:access:impl")

include(":server:common:account:api")
include(":server:common:account:impl")

include(":server:common:authentication:api")
include(":server:common:authentication:impl")

include(":server:common:dataset:api")
include(":server:common:dataset:impl")

include(":server:common:node:api")
include(":server:common:node:impl")

include(":server:common:ping:api")
include(":server:common:ping:impl")

include(":server:common:protection:api")
include(":server:common:protection:impl")

include(":server:common:ratelimit:api")
include(":server:common:ratelimit:impl")

include(":server:common:scope:api")
include(":server:common:scope:impl")

include(":server:common:serial:api")
include(":server:common:serial:impl")

include(":server:common:session:api")
include(":server:common:session:impl")

include(":server:common:setup:api")
include(":server:common:setup:impl")

include(":server:common:storage:api")
include(":server:common:storage:impl")