package dev.nikdekur.nexushub.config

import kotlinx.serialization.Serializable
import dev.nikdekur.nexushub.config.network.NexusNetworkConfig

@Serializable
data class NexusConfig(
    val network: NexusNetworkConfig,
    val data: NexusDataConfig,
    val database: NexusDatabaseConfig
)