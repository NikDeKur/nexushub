/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.storage.table

import dev.nikdekur.ndkore.service.getService
import dev.nikdekur.nexushub.lightWeightNexusHubServer
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.storage.runtime.RuntimeStorageService
import org.junit.jupiter.api.BeforeEach

class RuntimeStorageServiceTableTest : StorageTableTest {

    @BeforeEach
    fun setup() {
        val server = lightWeightNexusHubServer {
            service(::RuntimeStorageService, StorageService::class)
        }

        service = server.servicesManager.getService()
    }


    lateinit var service: StorageService


    override fun <T : Any> getTable(name: String, clazz: Class<T>) = service.getTable(name, clazz)
}