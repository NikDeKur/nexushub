/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.scope

import dev.nikdekur.ndkore.service.dependencies
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.storage.StorageTable
import dev.nikdekur.nexushub.storage.index.indexOptions
import dev.nikdekur.nexushub.storage.mongo.getTable
import dev.nikdekur.nexushub.storage.request.Filter
import dev.nikdekur.nexushub.storage.request.eq
import dev.nikdekur.nexushub.storage.scope.ScopeDAO
import dev.nikdekur.nexushub.util.NexusData
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class StorageScopesService(
    override val app: NexusHubServer
) : ScopesService {

    override val dependencies = dependencies {
        after(StorageService::class)
    }

    val storage by inject<StorageService>()

    val scopes = ConcurrentHashMap<String, Scope>()
    lateinit var table: StorageTable<ScopeDAO>

    override fun onEnable() {
        runBlocking {
            table = storage.getTable<ScopeDAO>("scopes")

            reloadScopes()
        }
    }

    override fun onDisable() {
        scopes.clear()
    }


    override suspend fun reloadScopes() {
        scopes.clear()

        val names = storage.getAllTables().toList()
        names.forEach {
            if (!it.startsWith("holders:")) return@forEach
            val scope = it.removePrefix("holders:")
            createScope(scope)
        }
    }

    override suspend fun createScope(scopeId: String): Scope {
        check(!scopes.containsKey(scopeId)) { "Scope $scopeId already exists" }

        val tableId = "holders:$scopeId"
        val table = storage.getTable<NexusData>(tableId)
        val index = indexOptions {
            unique = true
        }
        table.createIndex("holderId", mapOf("holderId" to 1), index)

        val dao = findScopeData(scopeId) ?: run {
            val dao = ScopeDAO.new(scopeId, emptySet())
            createScopeData(dao)
            dao
        }
        val scopeTable = StorageScopeTable(app, scopeId, table, dao)

        return StorageScope(app, scopeId, scopeTable).also {
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


    override suspend fun createScopeData(data: ScopeDAO) {
        table.insertOne(data)
    }

    inline fun filterScope(scope: String): Filter = ScopeDAO::name eq scope

    override suspend fun findScopeData(scopeId: String): ScopeDAO? {
        return table
            .find(listOf(filterScope(scopeId)))
            .firstOrNull()
    }

    override suspend fun updateScopeData(data: ScopeDAO) {
        return table
            .replaceOne(data, filterScope(data.name))
    }

    override suspend fun deleteScopeData(scopeId: String) {
        return table
            .deleteOne(filterScope(scopeId))
    }
}