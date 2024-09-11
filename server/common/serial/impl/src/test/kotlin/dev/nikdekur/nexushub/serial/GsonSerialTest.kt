/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.serial

import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.lightWeightNexusHubServer
import dev.nikdekur.nexushub.serial.gson.GsonSerialService

class GsonSerialTest : SerialTest {

    val server = lightWeightNexusHubServer {
        service(::GsonSerialService, SerialService::class)
    }

    override val service: SerialService by server.inject()
}