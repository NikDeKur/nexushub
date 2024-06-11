package org.ndk.nexushub.scope

import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Semaphore
import org.bson.Document
import org.ndk.nexushub.NexusHub.config
import org.ndk.nexushub.database.Database
import org.ndk.nexushub.database.scope.ScopeCollection
import org.ndk.nexushub.database.scope.ScopeDAO
import org.ndk.nexushub.database.scope.ScopesCollection
import org.ndk.nexushub.util.ensureCollectionExists
import org.ndk.nexushub.util.indexOptions
import java.util.concurrent.ConcurrentHashMap

object ScopesManager {

    val saveScope = CoroutineScope(Dispatchers.IO)
    lateinit var saveLimiter: Semaphore

    val scopes = ConcurrentHashMap<String, Scope>()

    suspend fun init() {
        ScopesCollection.init()
        reloadScopes()

        saveLimiter = Semaphore(config.data.save_parallelism)
    }

    suspend fun reloadScopes() {
        scopes.clear()

        val collections = Database.database.listCollectionNames().toList()
        collections.forEach {
            if (!it.startsWith("holders:")) return@forEach
            val scope = it.removePrefix("holders:")
            createScope(scope)
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
}