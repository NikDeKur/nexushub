/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub

import dev.nikdekur.ndkore.service.RuntimeServicesManager
import dev.nikdekur.ndkore.service.ServicesManager
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class AbstractNexusHubServer : NexusHubServer {

    override val logger = LoggerFactory.getLogger(javaClass)

    override lateinit var servicesManager: ServicesManager

    var startTime by Delegates.notNull<Long>()

    override val uptime: Duration
        get() = (System.currentTimeMillis() - startTime).milliseconds

    abstract fun registerServices()

    override fun start() {
        startTime = System.currentTimeMillis()

        servicesManager = RuntimeServicesManager()

        logger.info("Applying services...")
        servicesManager.apply { registerServices() }

        logger.info("Loading services...")
        servicesManager.enable()
    }
}