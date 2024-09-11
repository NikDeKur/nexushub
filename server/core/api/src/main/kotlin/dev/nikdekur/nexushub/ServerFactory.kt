/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub

import dev.nikdekur.nexushub.boot.Environment
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

/**
 * A factory for creating servers.
 */
object ServerFactory {

    val ktor: KFunction<NexusHubServer> by lazy {
        val clazz = Class.forName("dev.nikdekur.nexushub.KtorNexusHubServer").kotlin
                as KClass<NexusHubServer>
        clazz.constructors.first()
    }

    /**
     * Creates a server.
     *
     * @param type the type of server to create or null for the default server
     * @return the server
     */
    fun createServer(environment: Environment, type: String? = null): NexusHubServer {
        val constructor = when (type) {
            "ktor" -> ktor
            else -> ktor
        }

        return constructor.call(environment)
    }
}