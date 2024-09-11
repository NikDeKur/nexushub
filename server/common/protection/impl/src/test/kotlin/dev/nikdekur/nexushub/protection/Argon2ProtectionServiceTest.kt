/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.protection

import dev.nikdekur.ndkore.service.get
import dev.nikdekur.nexushub.lightWeightNexusHubServer
import dev.nikdekur.nexushub.protection.password.ProtectionService
import dev.nikdekur.nexushub.protection.password.argon2.Argon2ProtectionService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.SAME_THREAD)
class Argon2ProtectionServiceTest : ProtectionServiceTest {

    @BeforeEach
    fun setup() {
        val server = lightWeightNexusHubServer {
            service(::Argon2ProtectionService, ProtectionService::class)
        }
        service = server.get()
    }


    override lateinit var service: ProtectionService
}