/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.protection.none

import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.protection.Password
import dev.nikdekur.nexushub.protection.ProtectionService
import dev.nikdekur.nexushub.service.NexusHubService

class NoneProtectionService(
    override val app: NexusHubServer
) : NexusHubService(), ProtectionService {
    override fun createPassword(string: String): Password {
        return NoneProtectionPassword(string)
    }

    override fun deserializePassword(string: String): Password {
        return createPassword(string)
    }

    override suspend fun imitateEncryption() {
        // Do nothing
    }
}