@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.scope

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import dev.nikdekur.ndkore.ext.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withPermit
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


    suspend fun setDataSync(holderId: String, data: NexusData) {
        queueDataSet(holderId, data).await()
    }


    /**
     * Save data safe and parallel
     *
     * Parallelism can be limited by [NexusDataConfig.save_parallelism]
     *
     * @param holderId holder id
     * @param data data to save
     */
    fun queueDataSet(holderId: String, data: NexusData): Deferred<Unit> {
        return ScopesManager.saveScope.async {
            ScopesManager.saveLimiter.withPermit {
                try {
                    val clean = data.removeEmpty(maps = true, collections = true)
                    cache.put(holderId, clean)
                    collection.save(holderId, clean)
                } catch (e: Exception) {
                    logger.error("Error while saving data", e)
                }
            }
        }
    }



    suspend fun getLeaderboard(path: String, startFrom: Int, limit: Int): Leaderboard {
        val leaderboard = logger.recordTiming(name = "getLeaderboard") {
            val rawLeaderboard = collection.getLeaderboard(path, startFrom, limit)

            logger.info("Raw leaderboard: $rawLeaderboard")

            val pathList = path.split('.')

            ensureIndexAsync(path)

            val leaderboard = Leaderboard(rawLeaderboard.size)

            rawLeaderboard.forEachSafe {
                val holderId = it["holderId"] as String
                @Suppress("kotlin:S6611") // We know that the field is present

                // Get the value from the nested path
                var value: Any = it
                pathList.forEach { pathPart ->
                    value = (value as? Map<*, *>)?.get(pathPart) ?: return@forEach
                }
                val number = value as? Number ?: return@forEachSafe

                leaderboard.addEntry(holderId, number.toDouble())
            }
            leaderboard
        }

        return leaderboard
    }


    /**
     * Get the top position in the leaderboard for the given field of the given holder
     *
     * May throw [NumberFormatException] if the field is not a number
     *
     * @param field field to get the top position for
     * @param holderId holder to get the top position for
     * @return the top position in the leaderboard for the given field of the given holder (where 0 is max)
     * or null if not found
     */
    suspend fun getTopPosition(holderId: String, field: String): LeaderboardEntry? {
        val position = logger.recordTiming(name = "getTopPosition") {

            val data = loadData(holderId)
            logger.info("Data is $data")
            val fieldValue = data.getNested(field, ".") ?: return null
            logger.info("Field Value: $fieldValue")
            if (fieldValue !is Number)
                throw NumberFormatException("Field $field is not a number")
            val value = fieldValue.toDouble()

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