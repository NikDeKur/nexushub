package org.ndk.nexushub.client.sesion

import dev.nikdekur.ndkore.ext.*
import org.ndk.nexushub.client.hook.HooksExecutor
import org.ndk.nexushub.client.service.NexusService
import org.ndk.nexushub.data.LeaderboardEntry
import org.ndk.nexushub.network.GsonSupport
import org.ndk.nexushub.packet.*
import java.util.concurrent.ConcurrentHashMap

open class SessionImpl<H : Any, S : Session<H, S>>(
    override val service: NexusService<H, S>,
    override val holder: H
) : Session<H, S> {

    override var isActive: Boolean = false

    override val data = ConcurrentHashMap<String, Any>()

    override val afterLoadHooks by lazy { HooksExecutor(this) }
    override val beforeLoadHooks by lazy { HooksExecutor(this) }

    override val afterSaveHooks by lazy { HooksExecutor(this) }
    override val beforeSaveHooks by lazy { HooksExecutor(this) }

    override var isLoading: Boolean = false
    override var isLoaded: Boolean = false
    override var isAfterLoadHooksExecuted: Boolean = false

    var updatedAt = -1L
    var savedAt = -1L

    override fun hasToBeSaved(): Boolean {
        service.logger.info {
            "Checking if session has to be saved. Updated at: $updatedAt, Saved at: $savedAt, Data: $data"
        }
        val has = updatedAt > savedAt && data.isNotEmpty()
        service.logger.info { "Session has to be saved: $has" }
        if (has) {
            savedAt = System.currentTimeMillis()
        }
        return has
    }

    fun markUpdated() {
        updatedAt = System.currentTimeMillis()
    }

    suspend fun createSession(): String? {
        check(!isActive) { "Session is already active." }

        val createPacket = PacketCreateSession(service.scope, holderId)
        val talker = service.hub.connection.talker!!
        @Suppress("RemoveExplicitTypeArguments")
        val datastr = talker.sendPacket<String?>(createPacket) {
            throwOnTimeout(5000)

            receive<PacketUserData> {
                packet.data
            }

            receive {
                service.logger.warn("Error while creating session: $packet")
                null
            }
        }.await()

        if (datastr != null)
            isActive = true

        return datastr
    }

    override suspend fun loadData() {
        check(!isLoading) { "Session is already loading." }
        isLoading = true

        // Invoke before load hooks
        beforeLoadHooks.executeHooks()

        val dataStr = if (!isActive) {
            createSession()
        } else {
            val loadPacket = PacketLoadData(service.scope, holderId)
            val talker = service.hub.connection.talker!!
            @Suppress("RemoveExplicitTypeArguments")
            talker.sendPacket<String?>(loadPacket) {
                throwOnTimeout(5000)

                receive<PacketUserData> {
                    packet.data
                }

                receive {
                    service.logger.warn("Error while loading data: $packet")
                    null
                }
            }.await()
        }

        try {
            if (dataStr == null) {
                return
            }

            val newData = GsonSupport.dataFromString(dataStr)

            data.clear()
            data.putAll(newData)

            // Mark the session as loaded and not loading
            isLoading = false
            isLoaded = true

            // Invoke after load hooks
            // After the session is marked as loaded to prevent errors on [ensureLoaded] function
            afterLoadHooks.executeHooks()

            isAfterLoadHooksExecuted = true

            // Calculate the time taken for loading

        } finally {
            isLoading = false
        }
    }

    override suspend fun saveData() {
        // If the data is empty, complete the future immediately and return
        if (data.isEmpty()) {
            return
        }

        // Invoke any hooks before saving the data
        beforeSaveHooks.executeHooks()

        val dataStr = serialiseData()

        val packet = PacketSaveData(holderId, service.scope, dataStr)

        service.hub.connection.talker!!.sendPacket<Unit>(packet) {
            receive<PacketOk> {
                afterSaveHooks.executeHooks()
            }

            timeout(5000) {
                service.hub.logger.warn("Timeout while saving data.")
            }

            receive {
                service.hub.logger.warn("Error while saving data: $packet")
            }
        }.await()
    }

    override fun serialiseData(): String {
        // Remove any empty maps and collections from the data
        // To prevent empty maps and collections from being saved
        val clean = data.removeEmpty(maps = true, collections = true)
        val json = GsonSupport.dataToString(clean)
        return json
    }


    override suspend fun stop() {
        // Invoke any hooks before saving the data
        beforeSaveHooks.executeHooks()

        val dataStr = serialiseData()

        val packet = PacketStopSession(holderId, service.scope, dataStr)

        @Suppress("RemoveExplicitTypeArguments")
        service.hub.connection.talker!!.sendPacket<Unit>(packet) {
            receive<PacketOk> {
                afterSaveHooks.executeHooks()
            }

            timeout(5000) {
                service.hub.logger.warn("Timeout while stopping session.")
            }

            receive {
                service.hub.logger.warn("Error while stopping session: $packet")
            }
        }.await()

        isActive = false

        service.stopSession(holderId)
    }

    override suspend fun getTopPosition(
        field: String
    ): LeaderboardEntry? {
        val packet = PacketRequestTopPosition(service.scope, holderId, field)

        @Suppress("RemoveExplicitTypeArguments")
        return service.hub.connection.talker!!.sendPacket<LeaderboardEntry?>(packet) {
            throwOnTimeout(5000)

            receive<PacketTopPosition> {
                this.packet.entry
            }

            receive {
                service.hub.logger.error("Unexpected behaviour while loading top position in scope '${service.scope}', for field '$field': $packet")
                null
            }
        }.await()
    }


    @Suppress("UNCHECKED_CAST")
    override operator fun get(key: String): Any? {
        ensureLoaded()
        // If the key is empty, return the entire player data
        if (key.isEmpty()) return this
        // If the key does not contain dot notation, retrieve the value directly
        if (!key.contains(".")) return data[key]
        // Split the key into individual parts
        val pathSplit = key.split('.')
        // Initialize a map reference to traverse nested data
        var map: Map<String, Any> = data
        val last = pathSplit.size - 1
        // Iterate through each part of the key
        for (pathI in pathSplit.indices) {
            val path = pathSplit[pathI]
            // If it's the last part of the key, return the value
            if (pathI == last) {
                return map[path]
            }
            // If it's not the last part, traverse deeper into nested maps
            map = map[path] as? Map<String, Any> ?: return null
        }
        return null
    }


    @Suppress("UNCHECKED_CAST")
    override operator fun set(key: String, value: Any?) {
        ensureLoaded()
        // If the key is empty and the value is a map, replace the entire player data
        if (key.isEmpty() && value is Map<*, *>) {
            data.clear()
            data.putAll(value as Map<String, Any>)
        }
        // Split the key into individual parts
        val pathSplit = key.split(".")
        val last = pathSplit.size - 1
        // Initialize a mutable map reference to traverse and update nested data
        var map: MutableMap<String, Any> = data
        // Iterate through each part of the key
        for (pathI in pathSplit.indices) {
            val path = pathSplit[pathI]
            // If it's the last part of the key, set the value
            if (pathI == last) {
                // If the value is null, remove the key from the map
                if (value == null) {
                    map.remove(path)
                } else {
                    // Otherwise, update the value in the map
                    map[path] = value
                }
                return
            }
            // If it's not the last part, traverse deeper into nested maps
            map = map.computeIfAbsent(path) { ConcurrentHashMap<String, Any>() } as? MutableMap<String, Any> ?: run {
                map[path] = ConcurrentHashMap<String, Any>()
                map[path] as MutableMap<String, Any>
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun remove(key: String): Any? {
        ensureLoaded()
        // If the key is empty, clear the entire holder data
        if (key.isEmpty()) {
            val oldData = HashMap(data)
            data.clear()
            return oldData
        }
        // Split the key into individual parts
        val pathSplit = key.split(".")
        val last = pathSplit.size - 1
        // Initialize a mutable map reference to traverse and update nested data
        var map: MutableMap<String, Any> = data
        // Iterate through each part of the key
        for (pathI in pathSplit.indices) {
            val path = pathSplit[pathI]
            // If it's the last part of the key, remove the value
            if (pathI == last) {
                return if (map === this)
                           data.remove(path)
                       else
                           map.remove(path)
            }
            // If it's not the last part, traverse deeper into nested maps
            map = map[path] as? MutableMap<String, Any> ?: return null
        }
        return null
    }


    class FinalSession<H : Any>(override val service: NexusService<H, FinalSession<H>>, override val holder: H)
        : SessionImpl<H, FinalSession<H>>(service, holder)

    companion object {
        fun <H : Any> newFinal(service: NexusService<H, FinalSession<H>>, holder: H): FinalSession<H> {
            return FinalSession(service, holder)
        }
    }
}