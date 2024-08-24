/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.boot

import dev.nikdekur.nexushub.TestNexusHubServer
import dev.nikdekur.nexushub.dataset.map.MapDataSetService
import dev.nikdekur.nexushub.protection.none.NoneProtectionService
import dev.nikdekur.nexushub.storage.runtime.RuntimeStorageService
import org.junit.jupiter.api.Test

class SimpleTest {

    @Test
    fun firstTest() {
        val environment = object : Environment {
            override fun getValue(key: String): String? {
                return null
            }

            override fun requestValue(key: String, description: String): String? {
                return when (key) {
                    "root_password" -> "Password1"
                    else -> null
                }
            }
        }

        val server = object : TestNexusHubServer() {
            override val environment = environment

            override fun buildDataSetService() = MapDataSetService(this, mapOf())

            override fun buildStorageService() = RuntimeStorageService(this)

            override fun buildProtectionService() = NoneProtectionService(this)
        }

        server.start()

    }
}