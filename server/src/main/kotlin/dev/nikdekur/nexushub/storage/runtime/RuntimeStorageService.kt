/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.storage.runtime

import dev.nikdekur.nexushub.NexusHubServer
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
) : StorageService {

    override lateinit var scope: CoroutineScope

    val tables = ConcurrentHashMap<String, RuntimeStorageTable<*>>()

    override fun onEnable() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    override fun onDisable() {
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