package org.ndk.nexushub.client.sesion

import org.ndk.klib.complete
import org.ndk.klib.completedFuture
import org.ndk.nexushub.client.hook.HooksExecutor
import org.ndk.nexushub.client.hook.WhenSessionLoadedHook
import org.ndk.nexushub.client.service.NexusService
import org.ndk.nexushub.data.LeaderboardEntry
import org.ndk.nexushub.network.NexusData
import java.util.concurrent.CompletableFuture

interface Session<H : Any, S : Session<H, S>> {

    val service: NexusService<H, S>
    val data: NexusData

    val isActive: Boolean

    var isLoading: Boolean
    var isLoaded: Boolean
    var isAfterLoadHooksExecuted: Boolean

    val isFullyLoaded: Boolean
        get() = isLoaded && isAfterLoadHooksExecuted

    fun ensureLoaded() {
        check(isLoaded) { "Session is not loaded." }
    }

    val holder: H
    val holderId: String
        get() = service.getId(holder)
    val holderName: String
        get() = service.getName(holder)

    val beforeLoadHooks: HooksExecutor
    val afterLoadHooks: HooksExecutor

    val beforeSaveHooks: HooksExecutor
    val afterSaveHooks: HooksExecutor

    fun serialiseData(): String

    suspend fun loadData()

    suspend fun saveData()

    suspend fun stop()


    /**
     * Calls the block when the data [isFullyLoaded].
     *
     * If the data is, the block is called immediately.
     *
     * If the data is not, the block added to [afterLoadHooks] to be called when the data loaded.
     *
     * If the data is not loading, the block will never be called. Ensure the data loading has been started before calling this method.
     *
     * @param block The block to call when the data is loaded.
     * @see isLoaded
     */
    fun whenLoaded(customId: String? = null, block: () -> Unit) {
        if (isFullyLoaded) {
            block()
        } else {
            afterLoadHooks.addHook(WhenSessionLoadedHook(customId, block))
        }
    }



    /**
     * Calls the block when the data [isFullyLoaded].
     *
     * If the data is, the block is called immediately.
     *
     * If the data is not, the block added to [afterLoadHooks] to be called when the data loaded.
     *
     * If the data is not loading, the block will never be called. Ensure the data loading has been started before calling this method.
     *
     * @param block The block to call when the data is loaded.
     * @return A future that completes when the block is called.
     * @see isLoaded
     */
    fun <R> whenLoadedWithReturn(customId: String? = null, block: () -> R): CompletableFuture<R> {
        return if (isFullyLoaded) {
            block().completedFuture
        } else {
            val future = CompletableFuture<R>()
            afterLoadHooks.addHook(WhenSessionLoadedHook(customId) {
                future.complete(block)
            })
            future
        }
    }



    suspend fun getTopPosition(field: String): LeaderboardEntry?

    /**
     * Retrieves a value from the player data using the specified key.
     * This method allows accessing nested values within the player data using dot notation.
     * @param key The key to retrieve the value.
     * @return The value associated with the key, or null if not found.
     * @throws IllegalStateException If the accessor is not loaded.
     */
    operator fun get(key: String): Any?

    /**
     * Sets a value in the player data using the specified key.
     * This method allows setting nested values within the player data using dot notation.
     * @param key The key to set the value.
     * @param value The value to set.
     * @throws IllegalStateException If the accessor is not loaded.
     */
    operator fun set(key: String, value: Any?)

    /**
     * Removes a value from the player data using the specified key.
     * This method allows removing nested values within the player data using dot notation.
     *
     * @param key The key to remove the value.
     * @return The value that was removed or null if not found.
     */
    fun remove(key: String): Any?


    fun hasToBeSaved(): Boolean
}