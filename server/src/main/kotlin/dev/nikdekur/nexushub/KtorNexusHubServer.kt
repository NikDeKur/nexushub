/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:JvmName("NexusHubKt")

package dev.nikdekur.nexushub

import dev.nikdekur.ndkore.ext.addBlockingShutdownHook
import dev.nikdekur.nexushub.auth.AccountAuthenticationService
import dev.nikdekur.nexushub.auth.AuthenticationService
import dev.nikdekur.nexushub.config.NexusHubServerConfig
import dev.nikdekur.nexushub.database.Database
import dev.nikdekur.nexushub.database.mongo.MongoDatabase
import dev.nikdekur.nexushub.http.HTTPAuthService
import dev.nikdekur.nexushub.http.session.HTTPSessionAuthService
import dev.nikdekur.nexushub.koin.NexusHubKoinContext
import dev.nikdekur.nexushub.koin.getKoin
import dev.nikdekur.nexushub.koin.loadModule
import dev.nikdekur.nexushub.network.Routing
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.session.RuntimeSessionsService
import dev.nikdekur.nexushub.session.SessionsService
import dev.nikdekur.nexushub.talker.RuntimeTalkersService
import dev.nikdekur.nexushub.talker.TalkersService
import dev.nikdekur.nexushub.util.CloseCode
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.environmentProperties
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import kotlin.properties.Delegates
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class KtorNexusHubServer : NexusHubServer {

    override val id = javaClass.simpleName

    lateinit var server: EmbeddedServer<*, *>
    lateinit var config: NexusHubServerConfig

    override val logger = LoggerFactory.getLogger(javaClass)

    var startTime by Delegates.notNull<Long>()

    override val uptime: Duration
        get() = (System.currentTimeMillis() - startTime).milliseconds

    override fun start(args: Array<String>) {
        startTime = System.currentTimeMillis()
        val configPath = args.getOrElse(0) { "config.yml" }
        val configFile = File(configPath)
        if (!configFile.exists()) {
            throw Exception("Config file not found! ")
        }

        config = configFile.readText().let {
            Yaml.decodeFromString<NexusHubServerConfig>(it)
        }
        val networkConfig = config.network
        val ssl = networkConfig.ssl


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
                port = networkConfig.port
            }
        })

        startKoin()

        loadModule {
            single { this@KtorNexusHubServer }
            single { config }
        }


        loadModule {
            singleOf(::MongoDatabase) bind Database::class
        }

        loadModule {
            singleOf(::NodesService)
        }

        loadModule {
            singleOf(::RuntimeSessionsService) bind SessionsService::class
        }

        loadModule {
            singleOf(::RuntimeTalkersService) bind TalkersService::class
        }

        loadModule {
            singleOf(::AccountAuthenticationService) bind AuthenticationService::class
        }

        loadModule {
            singleOf(::HTTPSessionAuthService) bind HTTPAuthService::class
        }

        loadModule(true) {
            single {
                Routing().also {
                    it.init(server.application)
                }
            }
        }

        getKoin().get<Routing>()
        getKoin().get<MongoDatabase>()

        addBlockingShutdownHook {
            logger.info("Disconnecting active nodes...")
            getKoin().get<NodesService>().closeAll(CloseCode.SHUTDOWN, "Server is shutting down")

            logger.info("Shutting down ktor server...")
            logger.info("This may take nearly 10 seconds to perform graceful shutdown")
            server.stop(5000, 5000)
        }

        logger.info("Starting ktor server...")
        server.start(true)
    }


    fun startKoin() {
        NexusHubKoinContext.startKoin {
            environmentProperties()
        }
    }
}



fun createKeyStore(certFile: File, keyFile: File, alias: String): KeyStore {
    // Загрузка приватного ключа из PEM файла
    val privateKey: PrivateKey = PEMParser(FileReader(keyFile)).use { pemParser ->
        val pemObject = pemParser.readObject() as PrivateKeyInfo
        JcaPEMKeyConverter().getPrivateKey(pemObject)
    }

    // Загрузка сертификата из PEM файла
    val certificate: X509Certificate = PEMParser(FileReader(certFile)).use { pemParser ->
        val pemObject = pemParser.readObject() as X509CertificateHolder
        JcaX509CertificateConverter().getCertificate(pemObject)
    }

    // Создание KeyStore и добавление ключа и сертификата
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
    keyStore.load(null, null)
    keyStore.setKeyEntry(alias, privateKey, null, arrayOf(certificate))

    return keyStore
}