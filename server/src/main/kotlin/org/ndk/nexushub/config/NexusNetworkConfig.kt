@file:Suppress("PropertyName", "kotlin:S117")

package org.ndk.nexushub.config

import kotlinx.serialization.Serializable

@Serializable
class NexusNetworkConfig(
    val max_connections: Long,
    val authentication_timeout: Long,
)