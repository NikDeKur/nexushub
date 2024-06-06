package org.ndk.nexushub.config

import kotlinx.serialization.Serializable

@Serializable
data class NexusConfig(
    val network: NexusNetworkConfig,
    val data: NexusDataConfig,
    val database: NexusDatabaseConfig
)