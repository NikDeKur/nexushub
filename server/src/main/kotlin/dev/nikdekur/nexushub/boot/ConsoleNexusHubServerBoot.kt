/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.boot

import dev.nikdekur.ndkore.annotation.DelicateAPI
import dev.nikdekur.ndkore.ext.addShutdownHook
import dev.nikdekur.ndkore.service.getService
import dev.nikdekur.nexushub.ServerFactory
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.dataset.get
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

class ConsoleNexusHubServerBoot {

    companion object {


        @OptIn(DelicateAPI::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val logger = LoggerFactory.getLogger(javaClass)
            try {
                val environment = ConsoleBootEnvironment.fromCommandLineArgs(args)
                val serverType = environment.getValue("server")
                val server = ServerFactory.createServer(environment, serverType)

                logger.info("Starting ${server.id}...")

                with(server) {

                    addShutdownHook {
                        val dataset = servicesManager.getService<DataSetService>()

                        logger.info("Unloading services...")
                        servicesManager.disable()

                        logger.info("Shutting down server...")
                        logger.info("This may take a few seconds...")
                        val shutdownDataSet = dataset.get<ShutdownDataSet>("shutdown") ?: ShutdownDataSet()

                        val gracePeriod = shutdownDataSet.gracePeriod
                        val timeout = shutdownDataSet.timeout
                        stop(gracePeriod, timeout)
                    }

                    start()
                }

                logger.info("Uptime: ${server.uptime}")
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