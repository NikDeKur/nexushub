/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub

import dev.nikdekur.nexushub.boot.Environment

/**
 * A factory for creating servers.
 */
object ServerFactory {

    /**
     * Creates a server.
     *
     * @param type the type of server to create or null for the default server
     * @return the server
     */
    fun createServer(environment: Environment, type: String? = null): NexusHubServer {
        val constructor = when (type) {
            "ktor" -> ::KtorNexusHubServer
            else -> ::KtorNexusHubServer
        }

        return constructor(environment)
    }
}