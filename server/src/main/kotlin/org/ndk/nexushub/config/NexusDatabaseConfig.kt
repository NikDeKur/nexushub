package org.ndk.nexushub.config

import kotlinx.serialization.Serializable

@Serializable
data class NexusDatabaseConfig(
    val username: String,
    val password: String,
)
