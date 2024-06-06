@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.scope

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.cache.RemovalCause
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
import org.ndk.klib.forEachSafe
import org.ndk.nexushub.NexusHub.blockingScope
import org.ndk.nexushub.NexusHub.config
import org.ndk.nexushub.NexusHub.logger
import org.ndk.nexushub.config.NexusDataConfig
import org.ndk.nexushub.data.Leaderboard
import org.ndk.nexushub.data.LeaderboardEntry
import org.ndk.nexushub.database.scope.ScopeCollection
import org.ndk.nexushub.database.scope.ScopeDAO
import org.ndk.nexushub.database.scope.ScopesCollection
import org.ndk.nexushub.network.NexusData
import org.ndk.nexushub.scope.ScopesManager.saveParallelismSemaphore
import org.ndk.nexushub.util.logTiming
import java.util.concurrent.TimeUnit

data class Scope(
    val id: String,
    var data: ScopeDAO,
    val collection: ScopeCollection,
) {

    val cacheExpiration = config.data.cache_expiration
    val cacheSize = config.data.cache_max_size

    //              HolderId
    val cache: Cache<String, NexusData> = CacheBuilder.newBuilder()
        .expireAfterWrite(cacheExpiration, TimeUnit.SECONDS)
        .expireAfterAccess(cacheExpiration, TimeUnit.SECONDS)
        .removalListener {
            if (it.cause != RemovalCause.EXPIRED || it.cause != RemovalCause.SIZE) return@removalListener
            val holderId = it.key!!
            val data = it.value!!
            queueSave(holderId, data)
        }
        .maximumSize(cacheSize)
        .build()


    suspend fun loadData(holderId: String): Map<String, Any> {
        // Don't use cache[holderId] because CacheBuilder doesn't support async loading
        val cached = cache.getIfPresent(holderId)
        if (cached != null) return cached
        val data = collection.loadOrNull(holderId) ?: emptyMap()
        cache.put(holderId, data)
        return data
    }

    fun setData(holderId: String, data: Map<String, Any>) {
        cache.put(holderId, data)
    }


    /**
     * Save data safe and parallel
     *
     * Parallelism can be limited by [NexusDataConfig.save_parallelism]
     *
     * @param holderId holder id
     * @param data data to save
     */
    fun queueSave(holderId: String, data: NexusData) {
        blockingScope.launch {
            saveParallelismSemaphore.withPermit {
                try {
                    collection.save(holderId, data)
                } catch (e: Exception) {
                    logger.error("Error while saving data", e)
                }
            }
        }
    }



    suspend fun getLeaderboard(field: String, limit: Int): Leaderboard {
        val leaderboard = logTiming("getLeaderboard") {


            val rawLeaderboard = collection.getLeaderboard(field, limit)

            ensureIndexAsync(field)

            val leaderboard = Leaderboard(rawLeaderboard.size)

            rawLeaderboard.forEachSafe ({
                logger.error("Exception during building leaderboard entry", it)
            }) {
                val holderId = it["holderId"] as String
                @Suppress("kotlin:S6611") // We know that the field is present
                val value = (it[field]!! as Number).toDouble()
                leaderboard.addEntry(holderId, value)
            }
            leaderboard
        }

        return leaderboard
    }


    /**
     * Get the top position in the leaderboard for the given field of the given holder
     *
     * If no field is found for the holder or field is not a double, null is returned
     *
     * @param field field to get the top position for
     * @param holderId holder to get the top position for
     * @return the top position in the leaderboard for the given field of the given holder
     */
    suspend fun getTopPosition(field: String, holderId: String): LeaderboardEntry? {
        val position = logTiming("getTopPosition") {
            val value = loadData(holderId)[field] as? Double ?: return null
            val position = collection.getTopPosition(holderId, field, value)

            ensureIndexAsync(field)

            LeaderboardEntry(position, holderId, value)
        }
        return position
    }


   inline fun ensureIndexAsync(field: String) {
        if (!data.indexes.contains(field)) {
            blockingScope.launch {
                // Update scope in another coroutine to avoid blocking
                data = data.copy(indexes = data.indexes + field)
                collection.createIndex(field, false)
                ScopesCollection.updateScope(data)
            }
        }
    }



}