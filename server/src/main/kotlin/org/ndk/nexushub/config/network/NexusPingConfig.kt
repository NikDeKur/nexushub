package org.ndk.nexushub.config.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment

@Serializable
data class NexusPingConfig(
    @Comment("The interval in seconds between pings")
    val interval: Long,

    @Comment("The threshold in milliseconds for a ping to be send to a new node")
    @SerialName("warning_threshold")
    val warningThreshold: Long,
)