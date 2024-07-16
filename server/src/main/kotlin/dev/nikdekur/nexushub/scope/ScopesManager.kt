package dev.nikdekur.nexushub.scope

import com.mongodb.kotlin.client.coroutine.MongoCollection
import dev.nikdekur.nexushub.NexusHub.config
import dev.nikdekur.nexushub.database.Database
import dev.nikdekur.nexushub.database.scope.ScopeCollection
import dev.nikdekur.nexushub.database.scope.ScopeDAO
import dev.nikdekur.nexushub.database.scope.ScopesCollection
import dev.nikdekur.nexushub.util.ensureCollectionExists
import dev.nikdekur.nexushub.util.indexOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.sync.Semaphore
import org.bson.Document
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
        val dao = ScopesCollection.loadScope(name) ?: run {
            val dao = ScopeDAO.new(name, emptySet())
            ScopesCollection.newScope(dao)
            dao
        }
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