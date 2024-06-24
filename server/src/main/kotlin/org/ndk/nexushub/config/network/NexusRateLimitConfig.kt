package org.ndk.nexushub.config.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment

@Serializable
data class NexusRateLimitConfig(
    @Comment("The maximum number of requests that can be made in the time window")
    @SerialName("max_requests")
    val maxRequests: Int,

    @Comment("The time window in seconds")
    @SerialName("time_window")
    val timeWindow: Int
)