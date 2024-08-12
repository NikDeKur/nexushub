/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub

import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

class NexusHubServerBoot {

    companion object {

        private val builder: () -> NexusHubServer = {
            KtorNexusHubServer()
        }

        private lateinit var instance: NexusHubServer

        @JvmStatic
        fun main(args: Array<String>) {
            val logger = LoggerFactory.getLogger(javaClass)
            try {
                val server = builder()
                logger.info("Starting ${server.id}...")
                instance = server

                server.start(args)

                logger.info("Uptime: ${instance.uptime}")
            } catch (e: Exception) {
                logger.error("Fatal error occurred during server initialization. Shutting down...")
                e.printStackTrace()
                exitProcess(1)
            } finally {
                logger.info("Bye!")
            }
        }
    }
}