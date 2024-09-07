/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.scope

import com.google.common.cache.CacheBuilder
import dev.nikdekur.ndkore.service.dependencies
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.ndkore.service.injectOrNull
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.dataset.get
import dev.nikdekur.nexushub.scope.table.StorageScopeData
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.storage.StorageTable
import dev.nikdekur.nexushub.storage.getTable
import dev.nikdekur.nexushub.storage.index.indexOptions
import dev.nikdekur.nexushub.storage.request.Filter
import dev.nikdekur.nexushub.storage.request.eq
import dev.nikdekur.nexushub.storage.scope.ScopeDAO
import dev.nikdekur.nexushub.util.NexusData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.toJavaDuration

class StorageScopesService(
    override val app: NexusHubServer,
    val data: CacheScopeDataSet? = null
) : NexusHubService(), ScopesService {

    override val dependencies = dependencies {
        after(StorageService::class)
    }

    val datasetService by injectOrNull<DataSetService>()
    val storage by inject<StorageService>()

    val scopes = ConcurrentHashMap<String, Scope>()
    lateinit var table: StorageTable<ScopeDAO>

    lateinit var asyncScope: CoroutineScope

    override fun onEnable() = runBlocking {
        table = storage.getTable<ScopeDAO>("scopes")

        asyncScope = CoroutineScope(Dispatchers.Default)

        reloadScopes()
    }

    override fun onDisable() {
        scopes.clear()
        asyncScope.cancel()
    }

    override suspend fun getScopes(): Collection<Scope> {
        return scopes.values
    }


    suspend fun reloadScopes() {
        scopes.clear()

        val names = storage.getAllTables().toList()
        names.forEach {
            if (!it.startsWith("holders:")) return@forEach
            val scope = it.removePrefix("holders:")
            createScope(scope)
        }
    }

    suspend fun createScope(scopeId: String): Scope {
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
        val scopeTable = StorageScopeData(this, asyncScope, scopeId, table, dao)

        val cache = CacheBuilder.newBuilder()
            .apply {
                val cache = data ?: datasetService?.get<CacheScopeDataSet>("cache") ?: CacheScopeDataSet()
                val cacheExpiration = cache.cacheExpiration.toJavaDuration()
                val cacheSize = cache.cacheMaxSize
                expireAfterWrite(cacheExpiration)
                expireAfterAccess(cacheExpiration)
                maximumSize(cacheSize)
            }.build<String, NexusData>()

        val scope = CachingScope(scopeId, scopeTable, cache.asMap())
        scopes[scopeId] = scope

        return scope
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


    suspend fun createScopeData(data: ScopeDAO) {
        table.insertOne(data)
    }

    inline fun filterScope(scope: String): Filter = ScopeDAO::name eq scope

    suspend fun findScopeData(scopeId: String): ScopeDAO? {
        return table
            .find(listOf(filterScope(scopeId)))
            .firstOrNull()
    }

    suspend fun updateScopeData(data: ScopeDAO) {
        return table
            .replaceOne(data, filterScope(data.name))
    }

    suspend fun deleteScopeData(scopeId: String) {
        return table
            .deleteOne(filterScope(scopeId))
    }
}