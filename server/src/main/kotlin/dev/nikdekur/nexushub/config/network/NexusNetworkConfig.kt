package dev.nikdekur.nexushub.config.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class NexusNetworkConfig(
    val ping: NexusPingConfig,
    @SerialName("rate_limit")
    val rateLimit: NexusRateLimitConfig
)