@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.database.scope

import com.mongodb.kotlin.client.coroutine.MongoCollection
import dev.nikdekur.nexushub.database.Database
import dev.nikdekur.nexushub.database.account.eq
import dev.nikdekur.nexushub.util.ensureCollectionExists
import kotlinx.coroutines.flow.firstOrNull
import org.bson.conversions.Bson


/**
 * Class responsible for table contains all scopes DAOs
 */
object ScopesCollection {


    lateinit var collection: MongoCollection<ScopeDAO>

    suspend fun init() {
        collection = Database.database.ensureCollectionExists("scopes")
    }


    suspend fun newScope(dao: ScopeDAO) {
        collection.insertOne(dao)
    }

    suspend fun loadScope(scope: String): ScopeDAO? {
        return collection
            .find(filterScope(scope))
            .firstOrNull()
    }

    suspend fun updateScope(scopeDAO: ScopeDAO) {
        collection
            .replaceOne(filterScope(scopeDAO.name), scopeDAO)
    }


    inline fun filterScope(scope: String): Bson = ScopeDAO::name.name eq scope

}