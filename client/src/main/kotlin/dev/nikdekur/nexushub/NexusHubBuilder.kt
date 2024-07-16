package dev.nikdekur.nexushub

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import dev.nikdekur.nexushub.connection.gateway.DefaultGateway
import dev.nikdekur.nexushub.connection.gateway.GatewayConfiguration
import dev.nikdekur.nexushub.connection.gateway.GatewayData
import dev.nikdekur.nexushub.connection.retry.LinearRetry
import dev.nikdekur.nexushub.event.Event
import dev.nikdekur.nexushub.util.NexusHubBuilderDSL
import kotlin.time.Duration.Companion.seconds

class NexusHubBuilder {

    private var _connection: ConnectionDataBuilder? = null
    val connection: ConnectionDataBuilder
        get() = checkNotNull(_connection) { "Connection is not initialized" }

    var dispatcher: CoroutineDispatcher = Dispatchers.Default

    /**
     * The event flow used by [NexusHub.eventFlow] to publish [events][NexusHub.events].
     *
     *
     * By default, a [MutableSharedFlow] with an `extraBufferCapacity` of `Int.MAX_VALUE` is used.
     */
    var eventFlow: MutableSharedFlow<Event> = MutableSharedFlow(
        extraBufferCapacity = Int.MAX_VALUE
    )

    @NexusHubBuilderDSL
    fun connection(block: ConnectionDataBuilder.() -> Unit) {
        _connection = ConnectionDataBuilder().apply(block)
    }

    fun build(): NexusHub {

        val connection = connection

        val gatewayData = GatewayData(
            host = connection.host!!,
            port = connection.port!!,
            retry = connection.retry,
            connection.dispatcher
        )

        val gateway = DefaultGateway(gatewayData)

        val gatewayConfiguration = GatewayConfiguration(
            login = connection.login!!,
            password = connection.password!!,
            node = connection.node!!
        )

        return NexusHub(gateway, gatewayConfiguration, eventFlow, dispatcher)
    }

    class ConnectionDataBuilder {
        var host: String? = null
        var port: Int? = null
        var login: String? = null
        var password: String? = null
        var node: String? = null

        var dispatcher: CoroutineDispatcher = Dispatchers.Default
        val retry = LinearRetry(2.seconds, 30.seconds, 10)

        fun validate() {
            checkNotNull(host) { "Host is not initialized" }
            checkNotNull(port) { "Port is not initialized" }
            checkNotNull(login) { "Login is not initialized" }
            checkNotNull(password) { "Password is not initialized" }
            checkNotNull(node) { "Node is not initialized" }
        }
    }
}