@file:JvmName("NexusHubKt")

package org.ndk.nexushub

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import org.ndk.global.scheduler.impl.CoroutineScheduler
import org.ndk.klib.addBlockingShutdownHook
import org.ndk.nexushub.config.NexusConfig
import org.ndk.nexushub.database.Database
import org.ndk.nexushub.network.configureRouting
import org.ndk.nexushub.node.NodesManager
import org.ndk.nexushub.scope.ScopesManager
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.properties.Delegates
import kotlin.system.exitProcess

object NexusHub {

    lateinit var server: EmbeddedServer<*, *>

    lateinit var logger: Logger

    val blockingScope = CoroutineScheduler(CoroutineScope(Dispatchers.IO + SupervisorJob()))

    var MAX_CONNECTIONS by Delegates.notNull<Int>()

    lateinit var config: NexusConfig

    fun init(logger: org.slf4j.Logger, args: Array<String>) {

        addBlockingShutdownHook {
            NodesManager.closeAll(CloseReason.Codes.NORMAL.code, "Server is shutting down")
        }

        val cmdCnf = CommandLineConfig(args)
        this.logger = logger

        server = EmbeddedServer(cmdCnf.applicationProperties, Netty) {
            takeFrom(cmdCnf.engineConfig)
        }

        server.start(false)

        val configFile = File("environment/config.yml")
        if (!configFile.exists()) {
            logger.error("Config file not found. Exiting...")
            exitProcess(1)
        }

        config = configFile.readText().let {
            Yaml.decodeFromString<NexusConfig>(it)
        }

        runBlocking {
            Database.init()
            ScopesManager.init()
        }
    }
}

fun Application.module() {
    configureRouting()
}



fun main(args: Array<String>) {
    val logger = LoggerFactory.getLogger("NexusHub")
    try {
        NexusHub.init(logger, args)
        logger.info("Server started successfully!")
    } catch (e: Exception) {
        logger.error("Fatal error occurred during server initialization. Shutting down...")
        e.printStackTrace()
        exitProcess(1)
    }
}