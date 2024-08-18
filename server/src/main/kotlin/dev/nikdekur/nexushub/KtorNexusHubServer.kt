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
import dev.nikdekur.ndkore.koin.SimpleKoinContext
import dev.nikdekur.ndkore.service.KoinServicesManager
import dev.nikdekur.ndkore.service.ServicesManager
import dev.nikdekur.ndkore.service.getService
import dev.nikdekur.nexushub.auth.AccountAuthenticationService
import dev.nikdekur.nexushub.auth.AuthenticationService
import dev.nikdekur.nexushub.auth.account.AccountsService
import dev.nikdekur.nexushub.auth.account.TableAccountsService
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.dataset.config.ConfigDataSetService
import dev.nikdekur.nexushub.http.HTTPAuthService
import dev.nikdekur.nexushub.http.session.HTTPSessionAuthService
import dev.nikdekur.nexushub.network.Routing
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.node.RuntimeNodesService
import dev.nikdekur.nexushub.protection.ProtectionService
import dev.nikdekur.nexushub.protection.argon2.Argon2ProtectionService
import dev.nikdekur.nexushub.scope.MongoScopesService
import dev.nikdekur.nexushub.scope.ScopesService
import dev.nikdekur.nexushub.service.SetupService
import dev.nikdekur.nexushub.session.RuntimeSessionsService
import dev.nikdekur.nexushub.session.SessionsService
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.storage.mongo.MongoStorageService
import dev.nikdekur.nexushub.talker.RuntimeTalkersService
import dev.nikdekur.nexushub.talker.TalkersService
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
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

    override val logger = LoggerFactory.getLogger(javaClass)

    override lateinit var servicesManager: ServicesManager<NexusHubServer>

    var startTime by Delegates.notNull<Long>()

    override val uptime: Duration
        get() = (System.currentTimeMillis() - startTime).milliseconds

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun start(args: Array<String>) {
        startTime = System.currentTimeMillis()

        // Config Part start
        val configPath = args.firstOrNull() ?: "config.yml"
        val configFile = File(configPath)
        require(configFile.exists()) { "Config file not found!" }
        // Config Part end

        val context = SimpleKoinContext()
        context.startKoin {
            environmentProperties()
        }
        servicesManager = KoinServicesManager<NexusHubServer>(context, this)

        servicesManager.apply {
            val server = this@KtorNexusHubServer

            registerService(ConfigDataSetService(server, configFile), DataSetService::class)

            val db = MongoStorageService(server)
            registerService(db, StorageService::class)
            registerService(MongoScopesService(server, db), ScopesService::class)
            registerService(TableAccountsService(server, db), AccountsService::class)

            registerService(Argon2ProtectionService(server), ProtectionService::class)
            registerService(RuntimeNodesService(server), NodesService::class)
            registerService(RuntimeSessionsService(server), SessionsService::class)
            registerService(RuntimeTalkersService(server), TalkersService::class)
            registerService(AccountAuthenticationService(server), AuthenticationService::class)
            registerService(HTTPSessionAuthService(server), HTTPAuthService::class)

            registerService(SetupService(server))
        }


        logger.info("Loading services...")
        servicesManager.loadAll()

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

            logger.info("Unloading services...")
            routing.onUnload()
            servicesManager.unloadAll()

            logger.info("Shutting down ktor server...")
            logger.info("This may take a few seconds...")
            val config = networkDataSet.shutdown
            server.stop(config.gracePeriod, config.timeout, config.unit)
        }


        logger.info("Starting ktor server...")
        server.start(true)
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