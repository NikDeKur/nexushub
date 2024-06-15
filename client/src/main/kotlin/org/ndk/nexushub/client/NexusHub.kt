@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.client

import dev.nikdekur.ndkore.ext.*
import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import kotlinx.coroutines.*
import org.ndk.nexushub.client.connection.ConnectException
import org.ndk.nexushub.client.connection.NexusHubConnection
import org.ndk.nexushub.client.service.NexusService
import org.ndk.nexushub.client.util.NexusHubBuilderDSL
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class NexusHub(val builder: Builder) {

    val logger: Logger = LoggerFactory.getLogger("NexusHub")

    private var _blockingScope: CoroutineScheduler? = null
    val blockingScope: CoroutineScheduler
        get() = _blockingScope ?: error("NexusHub is not started yet.")

    val connection = NexusHubConnection(this)

    var isRunning = false

    lateinit var startWaiter: CompletableDeferred<Unit>

    /**
    * Starts the client and connect to NexusHub Server
     *
     * If failed to connect, will throw an [ConnectException].
     *
     * Will suspend coroutine until [stop] is called.
    */
    suspend fun start() {
        _blockingScope = CoroutineScheduler(CoroutineScope(Dispatchers.IO + SupervisorJob()))

        connection.connect()

        isRunning = true
        builder.onReady()

        // Wait until stop is called
        startWaiter = CompletableDeferred()
        startWaiter.smartAwait()
    }

    /**
     * Stops the service, disconnect from NexusHub Server.
     */
    suspend fun stop() {
        if (!isRunning) return
        logger.info { "Stopping NexusHub..." }
        stopServices()
        connection.disconnect()
        _blockingScope?.let {
            it.cancel()
            _blockingScope = null
        }
        isRunning = false
        startWaiter.complete(Unit)
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
        var node: String = ""
        var host: String = ""
        var port: Int = 0
        var login: String = ""
        var password: String = ""

        internal var onReady: () -> Unit = {}

        @NexusHubBuilderDSL
        fun onReady(block: () -> Unit) {
            onReady = block
        }

        fun build(): NexusHub {
            check(node.isNotEmpty()) { "Node is not set" }
            check(host.isNotEmpty()) { "Host is not set" }
            check(port > 0) { "Port is not set" }
            check(login.isNotEmpty()) { "Login is not set" }
            check(password.isNotEmpty()) { "Password is not set" }
            return NexusHub(this)
        }
    }

    companion object {



        private inline fun getEnv(name: String): String {
            return System.getenv(name) ?: error("Environment setup require variables: " +
                    "[NEXUSHUB_NODE, NEXUSHUB_HOST, NEXUSHUB_PORT, NEXUSHUB_USERNAME, NEXUSHUB_PASSWORD], " +
                    "but $name is missing"
            )
        }

        fun fromEnvironment(block: Builder.() -> Unit): NexusHub {
            val builder = Builder()
            builder.node = getEnv("NEXUSHUB_NODE")
            builder.host = getEnv("NEXUSHUB_HOST")
            builder.port = getEnv("NEXUSHUB_PORT").toInt()
            builder.login = getEnv("NEXUSHUB_USERNAME")
            builder.password = getEnv("NEXUSHUB_PASSWORD")
            builder.block()
            return builder.build()
        }


        fun build(block: Builder.() -> Unit): NexusHub {
            val builder = Builder()
            builder.block()
            return builder.build()
        }
    }
}