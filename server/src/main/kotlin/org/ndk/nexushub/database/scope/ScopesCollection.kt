@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.database.scope

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import org.bson.conversions.Bson
import org.ndk.nexushub.database.Database
import org.ndk.nexushub.util.ensureCollectionExists


/**
 * Class responsible for table contains all scopes DAOs
 */
object ScopesCollection {


    lateinit var collection: MongoCollection<ScopeDAO>

    suspend fun init() {
        collection = Database.database.ensureCollectionExists("scopes")
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


    inline fun filterScope(scope: String): Bson = Filters.eq(ScopeDAO::name.name, scope)
}