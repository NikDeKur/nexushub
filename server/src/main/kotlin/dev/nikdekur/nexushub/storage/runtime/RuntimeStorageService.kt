package dev.nikdekur.nexushub.storage.runtime

import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.storage.StorageTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import java.util.concurrent.ConcurrentHashMap

class RuntimeStorageService(
    override val app: NexusHubServer
) : NexusHubService, StorageService {

    override lateinit var scope: CoroutineScope

    val tables = ConcurrentHashMap<String, RuntimeStorageTable<*>>()

    override fun onLoad() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    override fun onUnload() {
        scope.cancel()
    }

    override fun getAllTables(): Flow<String> {
        return tables.keys.asFlow()
    }

    override fun <T : Any> getTable(
        name: String,
        clazz: Class<T>
    ): StorageTable<T> {

        @Suppress("UNCHECKED_CAST")
        return tables.getOrPut(name) {
            RuntimeStorageTable<T>()
        } as StorageTable<T>
    }
}