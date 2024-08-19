/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub

import dev.nikdekur.ndkore.`interface`.Snowflake
import dev.nikdekur.ndkore.service.ServicesManager
import dev.nikdekur.nexushub.boot.Environment
import dev.nikdekur.nexushub.service.NexusHubComponent
import org.slf4j.Logger
import kotlin.time.Duration

/**
 * The interface representing the NexusHub server.
 *
 * This interface allows creating different implementations of the server.
 */
interface NexusHubServer : NexusHubComponent, Snowflake<String> {

    override val id: String
        get() = javaClass.simpleName

    override val app: NexusHubServer
        get() = this

    val environment: Environment

    /**
     * The logger for the server.
     */
    val logger: Logger

    /**
     * The services manager for the server.
     */
    val servicesManager: ServicesManager<NexusHubServer>

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
     */
    fun start()

    /**
     * Stops the server.
     *
     * @param gracePeriod The grace period to allow for the server to stop gracefully.
     * @param timeout The timeout to wait for the server to stop.
     */
    fun stop(gracePeriod: Duration, timeout: Duration)
}