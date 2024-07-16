@file:JvmName("NexusHubKt")

package dev.nikdekur.nexushub

import dev.nikdekur.ndkore.ext.addBlockingShutdownHook
import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import dev.nikdekur.nexushub.auth.account.AccountManager
import dev.nikdekur.nexushub.config.NexusConfig
import dev.nikdekur.nexushub.database.Database
import dev.nikdekur.nexushub.network.configureRouting
import dev.nikdekur.nexushub.node.NodesManager
import dev.nikdekur.nexushub.scope.ScopesManager
import dev.nikdekur.nexushub.util.CloseCode
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.logging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.system.exitProcess

object NexusHub {

    lateinit var server: EmbeddedServer<*, *>

    lateinit var logger: Logger

    val blockingScope = CoroutineScheduler.fromSupervisor(Dispatchers.IO)

    lateinit var config: NexusConfig

    fun init(logger: Logger, args: Array<String>) {

        this.logger = logger

        val configPath = args.getOrElse(0) { "config.yml" }
        val configFile = File(configPath)
        if (!configFile.exists()) {
            throw Exception("Config file not found! ")
        }

        config = configFile.readText().let {
            Yaml.decodeFromString<NexusConfig>(it)
        }

        addBlockingShutdownHook {
            NodesManager.closeAll(CloseCode.SHUTDOWN, "Server is shutting down")
        }

        val cmdCnf = CommandLineConfig(args)

        server = EmbeddedServer(cmdCnf.applicationProperties, Netty) {
            takeFrom(cmdCnf.engineConfig)
        }

        server.start(false)

        runBlocking {
            Database.init()

            AccountManager.init()
            ScopesManager.init()
            NodesManager.init()

//            AccountManager.newAccount(
//                login = ,
//                password = ,
//                allowedScopes = setOf()
//            )


            logger.info("Connected to the database")
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