/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.scope

import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.database.mongo.MongoDatabase
import dev.nikdekur.nexushub.database.mongo.ensureCollectionExists
import dev.nikdekur.nexushub.database.mongo.indexOptions
import dev.nikdekur.nexushub.database.mongo.scope.MongoScopeTable
import dev.nikdekur.nexushub.database.mongo.scope.MongoScopesTable
import dev.nikdekur.nexushub.database.scope.ScopeDAO
import dev.nikdekur.nexushub.database.scope.ScopesTable
import dev.nikdekur.nexushub.service.NexusHubService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.bson.Document
import java.util.concurrent.ConcurrentHashMap

class MongoScopesService(
    override val app: NexusHubServer,
    val database: MongoDatabase
) : NexusHubService, ScopesService {

    val scopes = ConcurrentHashMap<String, Scope>()
    lateinit var table: ScopesTable

    override fun onLoad() {
        runBlocking {
            val collection = database.database.ensureCollectionExists<ScopeDAO>("scopes") {
                val indexOptions = indexOptions {
                    unique(true)
                }

                createIndex(Document("name", 1), indexOptions)
            }

            table = MongoScopesTable(collection)

            reloadScopes()
        }
    }

    override fun onUnload() {
        scopes.clear()
    }


    override suspend fun reloadScopes() {
        scopes.clear()

        val names = database.getAllCollectionsNames().toList()
        names.forEach {
            if (!it.startsWith("holders:")) return@forEach
            val scope = it.removePrefix("holders:")
            createScope(scope)
        }
    }

    override suspend fun createScope(scopeId: String): Scope {
        check(!scopes.containsKey(scopeId)) { "Scope $scopeId already exists" }

        val collection = getScopeCollection(scopeId)

        return Scope(scopeId, collection).also {
            scopes[scopeId] = it
        }
    }

    /**
     * Get scope by name or create it if it doesn't exist
     *
     * @param scopeId scope name
     * @return scope
     */
    override suspend fun getScope(scopeId: String): Scope {
        return scopes[scopeId] ?: createScope(scopeId)
    }

    override suspend fun getScopeCollection(scopeId: String): MongoScopeTable {
        val collectionId = "holders:$scopeId"
        val mongoCollection = database.database.ensureCollectionExists<Document>(collectionId) {
            val indexOptions = indexOptions {
                unique(true)
            }

            createIndex(Document("holderId", 1), indexOptions)
        }

        val dao = findScopeData(scopeId) ?: run {
            val dao = ScopeDAO.new(scopeId, emptySet())
            createScopeData(dao)
            dao
        }
        return MongoScopeTable(scopeId, mongoCollection, dao)
    }


    override suspend fun createScopeData(data: ScopeDAO) {
        table.createScope(data)
    }

    override suspend fun findScopeData(scopeId: String): ScopeDAO? {
        return table.findScope(scopeId)
    }

    override suspend fun updateScopeData(data: ScopeDAO) {
        table.updateScope(data)
    }

    override suspend fun deleteScopeData(scopeId: String) {
        table.deleteScope(scopeId)
    }
}