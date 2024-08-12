/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub

import dev.nikdekur.ndkore.`interface`.Snowflake
import org.slf4j.Logger
import kotlin.time.Duration

/**
 * The interface representing the NexusHub server.
 *
 * This interface allows creating different implementations of the server.
 */
interface NexusHubServer : Snowflake<String> {

    /**
     * The logger for the server.
     */
    val logger: Logger

    /**
     * The uptime of the server.
     *
     * This is the duration since the server was started.
     */
    val uptime: Duration

    /**
     * Starts the server.
     *
     * Function should block until the server is stopped.
     *
     * @param args The arguments passed to the server.
     */
    fun start(args: Array<String>)
}