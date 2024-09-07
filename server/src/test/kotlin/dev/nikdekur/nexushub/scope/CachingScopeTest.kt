/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.scope

import dev.nikdekur.ndkore.service.get
import dev.nikdekur.nexushub.lightWeightNexusHubServer
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.storage.runtime.RuntimeStorageService
import org.junit.jupiter.api.BeforeEach

class CachingScopeTest : ScopeTest {
    @BeforeEach
    fun setup() {
        val server = lightWeightNexusHubServer {
            service(::RuntimeStorageService, StorageService::class)
            service(::StorageScopesService, ScopesService::class)
        }

        service = server.get()
    }

    lateinit var service: ScopesService

    override suspend fun getScope(scopeId: String): Scope {
        return service.getScope(scopeId)
    }
}