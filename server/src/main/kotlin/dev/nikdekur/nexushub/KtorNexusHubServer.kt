/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:JvmName("NexusHubKt")

package dev.nikdekur.nexushub

import dev.nikdekur.ndkore.annotation.DelicateAPI
import dev.nikdekur.ndkore.ext.addShutdownHook
import dev.nikdekur.ndkore.service.getService
import dev.nikdekur.nexushub.boot.Environment
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.dataset.config.ConfigDataSetService
import dev.nikdekur.nexushub.network.Routing
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.storage.mongo.MongoStorageService
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import kotlin.time.Duration

class KtorNexusHubServer(
    override val environment: Environment
) : AbstractNexusHubServer() {

    lateinit var server: EmbeddedServer<*, *>

    override fun buildDataSetService(): DataSetService {
        // Config Part start
        val configPath = environment.getValue("config") ?: "config.yml"
        val configFile = File(configPath)
        require(configFile.exists()) { "Config file not found!" }
        // Config Part end

        return ConfigDataSetService(this@KtorNexusHubServer, configFile)
    }

    override fun buildStorageService(): StorageService {
        return MongoStorageService(this)
    }


    override fun start() {
        super.start()

        val dataset = servicesManager.getService<DataSetService>()
            .getDataSet()

        val networkDataSet = dataset.network
        val ssl = networkDataSet.ssl


        val keyStore = ssl?.let {
            createKeyStore(
                File(it.key),
                File(it.cert),
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
                port = networkDataSet.port
            }
        })

        val routing = Routing(this, server.application)
        routing.onLoad()

        addShutdownHook {
            logger.info("Unloading routing...")
            routing.onUnload()
            logger.info("Unloaded routing.")
        }

        logger.info("Starting ktor server...")
        server.start(true)
    }

    override fun stop(gracePeriod: Duration, timeout: Duration) {
        logger.info("Stopping ktor server...")
        server.stop(gracePeriod.inWholeMilliseconds, timeout.inWholeMilliseconds)
    }

    companion object {
        @JvmStatic
        @DelicateAPI
        lateinit var instance: KtorNexusHubServer


        fun createKeyStore(certFile: File, keyFile: File, alias: String): KeyStore {
            // Load private key from a PEM file
            val privateKey: PrivateKey = PEMParser(FileReader(keyFile)).use { pemParser ->
                val pemObject = pemParser.readObject() as PrivateKeyInfo
                JcaPEMKeyConverter().getPrivateKey(pemObject)
            }

            // Load certificate from a PEM file
            val certificate: X509Certificate = PEMParser(FileReader(certFile)).use { pemParser ->
                val pemObject = pemParser.readObject() as X509CertificateHolder
                JcaX509CertificateConverter().getCertificate(pemObject)
            }

            // Create a KeyStore and add the private key and certificate
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setKeyEntry(alias, privateKey, null, arrayOf(certificate))

            return keyStore
        }
    }
}


