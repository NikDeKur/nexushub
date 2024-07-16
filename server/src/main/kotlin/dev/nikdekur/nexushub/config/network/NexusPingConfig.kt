package dev.nikdekur.nexushub.config.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment

@Serializable
data class NexusPingConfig(
    @Comment("The interval in seconds between pings")
    val interval: Int,

    @Comment("The extra interval in milliseconds to wait before considering a node dead")
    @SerialName("extra_interval")
    val extraInterval: Int,
)