@file:Suppress("PropertyName", "kotlin:S117")

package org.ndk.nexushub.config

import kotlinx.serialization.Serializable
import net.mamoe.yamlkt.Comment

@Serializable
data class NexusDataConfig(
    @Comment("Interval to save all cached data to the database (in seconds)")
    val save_interval: Long,
    @Comment("Interval to clear cached holder data after write/access (in seconds)")
    val cache_expiration: Long,
    @Comment("Maximum number of cached holder data")
    val cache_max_size: Long,
    @Comment("Number of coroutines used to save data parallel")
    val save_parallelism: Int
) {
}