package dev.nikdekur.nexushub.connection.gateway

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import dev.nikdekur.nexushub.event.Event
import dev.nikdekur.nexushub.network.dsl.PacketReaction
import dev.nikdekur.nexushub.network.transmission.PacketTransmission
import dev.nikdekur.nexushub.packet.Packet
import kotlin.time.Duration

/**
 * An implementation of the Discord [Gateway](https://discord.com/developers/docs/topics/gateway) and its lifecycle.
 *
 * Allows consumers to receive [events](https://discord.com/developers/docs/topics/gateway-events#receive-events)
 * through [events] and send [commands](https://discord.com/developers/docs/topics/gateway-events#send-events)
 * through [send].
 */
interface Gateway : CoroutineScope {
    /**
     * The incoming [events](https://discord.com/developers/docs/topics/gateway-events) of the Gateway.
     *
     * Users should expect these [Flows](Flow) to be hot and remain open for the entire lifecycle of the Gateway.
     */
    val events: SharedFlow<Event>

    /**
     * The duration between the last [Heartbeat][Command.Heartbeat] and [HeartbeatACK].
     *
     * This flow will have a [value][StateFlow.value] of `null` if the gateway is not
     * [active][Gateway.start], or no [HeartbeatACK] has been received yet.
     */
    val ping: StateFlow<Duration?>

    /**
     * Starts a reconnection gateway connection with the given [configuration].
     * This function will suspend until the lifecycle of the gateway has ended.
     *
     * @param configuration the configuration for this gateway session.
     */
    suspend fun start(configuration: GatewayConfiguration)

    /**
     * Sends a [Packet] to the gateway, suspending until the message has been sent.
     *
     * @param packet The [Packet] to send it to the gateway.
     * @param builder The [PacketReaction.Builder] to build the reaction to the packet.
     * @throws Exception when the gateway connection isn't open.
     */
    suspend fun <R> sendPacket(
        packet: Packet,
        builder: PacketReaction.Builder<R>.() -> Unit = {}
    ): PacketTransmission<R>

    /**
     * Closes the Gateway and ends the current session, suspending until the underlying webSocket is closed.
     */
    suspend fun stop()

    /**
     * Close gateway and releases resources.
     *
     * **For some implementations this will render the Gateway untenable,
     * as such, all implementations should be handled as if they are irreversibly closed.**
     */
    suspend fun detach()
}