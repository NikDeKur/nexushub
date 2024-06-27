@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.client

import dev.nikdekur.ndkore.ext.*
import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.ndk.nexushub.client.connection.ConnectException
import org.ndk.nexushub.client.connection.ConnectionConfiguration
import org.ndk.nexushub.client.connection.NexusHubConnection
import org.ndk.nexushub.client.service.NexusService
import org.ndk.nexushub.client.util.NexusHubBuilderDSL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class NexusHub(val builder: Builder) {

    val logger: Logger = LoggerFactory.getLogger("NexusHub")

    val connection = NexusHubConnection(this, builder.connection)
    val scheduler = CoroutineScheduler.fromSupervisor(builder.dispatcher)


    /**
    * Starts the client and connect to NexusHub Server
     *
     * If failed to connect, will throw an [ConnectException].
     *
     * Will suspend coroutine until [stop] is called.
    */
    suspend fun start() {
        connection.start()
    }

    /**
     * Stops the service, disconnect from NexusHub Server.
     */
    suspend fun stop() {
        logger.info { "Stopping NexusHub..." }

        runBlocking {
            scheduler.coroutineContext.job.cancelAndJoin()
        }

        stopServices()

        connection.stop()
    }

    internal suspend fun stopServices() {
        services.values.forEachSafe({
            logger.warn("Error occurred while stopping service with scope $scope", it)
        }) { it.stop() }
    }



    /**
     * Restarts the service.
     *
     * Usually this means stopping the service and then starting it again.
     */
    suspend fun restart() {
        stop()
        start()
    }





    val services = ConcurrentHashMap<String, NexusService<*, *>>()
    fun addService(service: NexusService<*, *>) {
        services[service.scope] = service
        service.start()
    }

    fun getService(scope: String): NexusService<*, *>? {
        return services[scope]
    }


    class Builder {
        private var _connection: ConnectionConfiguration? = null
        val connection: ConnectionConfiguration
            get() = checkNotNull(_connection) { "Connection is not initialized" }

        var onReady: suspend () -> Unit = {}
        var dispatcher: CoroutineDispatcher = Dispatchers.IO

        @NexusHubBuilderDSL
        fun connection(block: ConnectionConfiguration.Builder.() -> Unit) {
            _connection = ConnectionConfiguration.Builder().apply(block).build()
        }

        /**
         * Set the block to be executed when the client is connected and ready to use.
         *
         * Note: Block will be executed again if the connection has been lost and then reconnected.
         *
         * @param block The block to be executed
         */
        @NexusHubBuilderDSL
        fun onReady(block: suspend () -> Unit) {
            onReady = block
        }

        fun build(): NexusHub {
            checkNotNull(_connection) { "Connection is not initialized" }
            return NexusHub(this)
        }
    }

    companion object {

        fun build(block: Builder.() -> Unit): NexusHub {
            val builder = Builder()
            builder.block()
            return builder.build()
        }
    }
}