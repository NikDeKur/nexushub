package dev.nikdekur.nexushub.config

import kotlinx.serialization.Serializable

@Serializable
data class NexusDatabaseConfig(
    val connection: String
)
