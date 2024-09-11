/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.session

import dev.nikdekur.ndkore.service.getService
import dev.nikdekur.nexushub.lightWeightNexusHubServer
import org.junit.jupiter.api.BeforeEach

class RuntimeSessionsServiceTest : SessionsServiceTest {

    @BeforeEach
    fun setup() {
        val server = lightWeightNexusHubServer {
            service(::RuntimeSessionsService, SessionsService::class)
        }

        service = server.servicesManager.getService()
    }


    override lateinit var service: SessionsService
}