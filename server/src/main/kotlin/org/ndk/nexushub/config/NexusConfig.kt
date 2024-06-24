package org.ndk.nexushub.config

import kotlinx.serialization.Serializable
import org.ndk.nexushub.config.network.NexusNetworkConfig

@Serializable
data class NexusConfig(
    val network: NexusNetworkConfig,
    val data: NexusDataConfig,
    val database: NexusDatabaseConfig
)