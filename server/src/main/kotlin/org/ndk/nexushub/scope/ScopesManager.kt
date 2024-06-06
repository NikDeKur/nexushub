package org.ndk.nexushub.scope

import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Semaphore
import org.bson.Document
import org.ndk.global.scheduler.SchedulerTask
import org.ndk.klib.addShutdownHook
import org.ndk.nexushub.NexusHub
import org.ndk.nexushub.NexusHub.config
import org.ndk.nexushub.database.Database
import org.ndk.nexushub.database.scope.ScopeCollection
import org.ndk.nexushub.database.scope.ScopeDAO
import org.ndk.nexushub.database.scope.ScopesCollection
import org.ndk.nexushub.util.ensureCollectionExists
import org.ndk.nexushub.util.indexOptions
import java.util.concurrent.ConcurrentHashMap

object ScopesManager {

    lateinit var saveParallelismSemaphore: Semaphore
    lateinit var saveJob: SchedulerTask

    val scopes = ConcurrentHashMap<String, Scope>()

    suspend fun init() {
        ScopesCollection.init()
        reloadScopes()
        initSaving()
    }

    suspend fun reloadScopes() {
        saveAllCached()
        scopes.clear()

        val collections = Database.database.listCollectionNames().toList()
        collections.forEach {
            if (!it.startsWith("holders:")) return@forEach
            val scope = it.removePrefix("holders:")
            createScope(scope)
        }
    }

    fun initSaving() {
        saveParallelismSemaphore = Semaphore(config.data.save_parallelism)

        // Interval is stored in seconds in the config
        val saveInterval = config.data.save_interval * 1000L
        saveJob = NexusHub.blockingScope.runTaskTimer(saveInterval, ::saveAllCached)

        addShutdownHook {
            saveJob.cancel()
            saveAllCached()
        }
    }

    suspend fun createScope(name: String): Scope {
        check(!scopes.containsKey(name)) { "Scope $name already exists" }

        val mongoCollection = ensureCollectionExists(name)
        val collection = ScopeCollection(mongoCollection)
        val dao = ScopesCollection.loadScope(name) ?: ScopeDAO.new(name, emptySet())
        return Scope(name, dao, collection).also {
            scopes[name] = it
        }
    }

    /**
     * Get scope by name or create it if it doesn't exist
     *
     * @param name scope name
     * @return scope
     */
    suspend fun getScope(name: String): Scope {
        return scopes[name] ?: createScope(name)
    }


    suspend fun ensureCollectionExists(scope: String): MongoCollection<Document> {
        val name = "holders:$scope"
        return Database.database.ensureCollectionExists(name) {
            val indexOptions = indexOptions {
                unique(true)
            }

            createIndex(Document("holderId", 1), indexOptions)
        }
    }





    /**
     * Save all cached data to the database without clearing the cache
     *
     * Iterate throw each scope and save all cached in data
     *
     * Use [saveSafeAsync] to save data safe and parallel
     */
    fun saveAllCached() {
        scopes.values.forEach { scope ->
            scope.cache.asMap().forEach { (holderId, data) ->
                scope.queueSave(holderId, data)
            }
        }
    }
}