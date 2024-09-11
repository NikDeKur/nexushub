/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:JvmName("NexusHubKt")

package dev.nikdekur.nexushub

import dev.nikdekur.ndkore.ext.addShutdownHook
import dev.nikdekur.ndkore.service.get
import dev.nikdekur.nexushub.boot.Environment
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.dataset.config.ConfigDataSetService
import dev.nikdekur.nexushub.dataset.get
import dev.nikdekur.nexushub.ktor.Routing
import dev.nikdekur.nexushub.protection.cert.CertificatesService
import dev.nikdekur.nexushub.storage.mongo.MongoStorageService
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.time.Duration

data class SSL(
    val cert: String,
    val key: String
)

class KtorNexusHubServer(
    override val environment: Environment
) : ProductionNexusHubServer() {

    lateinit var server: EmbeddedServer<*, *>

    override fun buildDataSetService() =
        ConfigDataSetService(this@KtorNexusHubServer)
    override fun buildStorageService() = MongoStorageService(this)


    override fun start() {
        super.start()

        val dataset = servicesManager.get<DataSetService>()
        val certs = servicesManager.get<CertificatesService>()

        val port = dataset.get<Int>("port") ?: 8080

        val ssl = dataset.get<SSL>("ssl")
        val keyStore = ssl?.let {
            certs.createKeyStore(
                File(ssl.key),
                File(ssl.cert),
                "ktor"
            )
        }

        val environment = applicationEnvironment {
            log = LoggerFactory.getLogger("ktor.application")
        }


        server = embeddedServer(Netty, environment, {
            if (keyStore != null)
                sslConnector(
                    keyStore = keyStore,
                    keyAlias = "ktor",
                    keyStorePassword = { "".toCharArray() },
                    privateKeyPassword = { "".toCharArray() }
                ) {}

            connector {
                this.port = port
            }
        })

        val routing = Routing(this, server.application)
        routing.onEnable()

        addShutdownHook {
            logger.info("Unloading routing...")
            routing.onDisable()
            logger.info("Unloaded routing.")
        }

        logger.info("Starting ktor server...")
        server.start(true)
    }

    override fun stop(gracePeriod: Duration, timeout: Duration) {
        logger.info("Stopping ktor server...")
        server.stop(gracePeriod.inWholeMilliseconds, timeout.inWholeMilliseconds)
    }
}


