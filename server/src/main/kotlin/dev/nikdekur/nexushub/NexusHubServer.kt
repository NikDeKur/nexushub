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
import dev.nikdekur.nexushub.auth.AuthenticationService
import dev.nikdekur.nexushub.auth.AuthenticationServiceImpl
import dev.nikdekur.nexushub.config.NexusHubServerConfig
import dev.nikdekur.nexushub.database.Database
import dev.nikdekur.nexushub.database.mongo.MongoDatabaseImpl
import dev.nikdekur.nexushub.koin.NexusHubContext
import dev.nikdekur.nexushub.koin.getKoin
import dev.nikdekur.nexushub.koin.loadModule
import dev.nikdekur.nexushub.network.Routing
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.session.SessionsService
import dev.nikdekur.nexushub.session.SessionsServiceImpl
import dev.nikdekur.nexushub.talker.TalkersService
import dev.nikdekur.nexushub.talker.TalkersServiceImpl
import dev.nikdekur.nexushub.util.CloseCode
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.environmentProperties
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.exitProcess

class NexusHubServer {

    lateinit var server: EmbeddedServer<*, *>
    lateinit var config: NexusHubServerConfig

    val logger = LoggerFactory.getLogger(javaClass)


    fun start(args: Array<String>) {
        val configPath = args.getOrElse(0) { "config.yml" }
        val configFile = File(configPath)
        if (!configFile.exists()) {
            throw Exception("Config file not found! ")
        }

        config = configFile.readText().let {
            Yaml.decodeFromString<NexusHubServerConfig>(it)
        }

        addBlockingShutdownHook {
            getKoin().get<NodesService>().closeAll(CloseCode.SHUTDOWN, "Server is shutting down")
        }

        val cmdCnf = CommandLineConfig(args)

        server = EmbeddedServer(cmdCnf.applicationProperties, Netty) {
            takeFrom(cmdCnf.engineConfig)
        }

        startKoin()

        loadModule {
            single { this@NexusHubServer }
            single { config }
        }


        loadModule {
            singleOf(::MongoDatabaseImpl) bind Database::class
        }

        loadModule {
            singleOf(::NodesService)
        }

        loadModule {
            singleOf(::SessionsServiceImpl) bind SessionsService::class
        }

        loadModule {
            singleOf(::TalkersServiceImpl) bind TalkersService::class
        }

        loadModule {
            singleOf(::AuthenticationServiceImpl) bind AuthenticationService::class
        }

        loadModule(true) {
            single {
                Routing().also {
                    it.init(server.application)
                }
            }
        }

        getKoin().get<Routing>()
        getKoin().get<MongoDatabaseImpl>()



        runBlocking {
//            AccountManager.newAccount(
//                login = ,
//                password = ,
//                allowedScopes = setOf()
//            )
        }



        logger.info("Starting server...")
        server.start(true)
    }


    fun startKoin() {
        NexusHubContext.startKoin {
            environmentProperties()
        }
    }
}


fun main(args: Array<String>) {
    val bootLogger = LoggerFactory.getLogger("NexusHubServerBoot")
    try {
        val server = NexusHubServer()
        server.start(args)
    } catch (e: Exception) {
        bootLogger.error("Fatal error occurred during server initialization. Shutting down...")
        e.printStackTrace()
        exitProcess(1)
    } finally {
        bootLogger.info("Shutting down...")
    }
}