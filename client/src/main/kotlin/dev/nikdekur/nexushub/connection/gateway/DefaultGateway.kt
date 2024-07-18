/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.connection.gateway

import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import dev.nikdekur.nexushub.connection.ServerTalker
import dev.nikdekur.nexushub.connection.State
import dev.nikdekur.nexushub.connection.handler.AuthenticationHandler
import dev.nikdekur.nexushub.connection.handler.HeartbeatHandler
import dev.nikdekur.nexushub.event.Close
import dev.nikdekur.nexushub.event.Event
import dev.nikdekur.nexushub.event.NetworkEvent
import dev.nikdekur.nexushub.network.dsl.PacketReaction
import dev.nikdekur.nexushub.network.transmission.PacketTransmission
import dev.nikdekur.nexushub.packet.*
import dev.nikdekur.nexushub.util.CloseCode
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import java.nio.channels.UnresolvedAddressException
import kotlin.time.Duration

class DefaultGateway(
    val data: GatewayData,
) : Gateway {

    override val events: SharedFlow<Event>
        get() = data.eventFlow

    override val coroutineContext = data.dispatcher + SupervisorJob()

    val retry = data.retry

    private val authenticationHandler = AuthenticationHandler(data.eventFlow, retry)

    init {
        HeartbeatHandler(
            flow = data.eventFlow,
            send = { sendPacket<Unit>(it) {} },
            ping = { _ping.value = it },
            scheduler = CoroutineScheduler(this)
        )
    }

    val logger = LoggerFactory.getLogger("NexusHubGateway")

    var client: HttpClient? = null
    var talker: ServerTalker? = null

    private val _ping = MutableStateFlow<Duration?>(null)
    override val ping: StateFlow<Duration?> get() = _ping

    var state by atomic<State>(State.Stopped)

    override suspend fun <R> sendPacket(packet: Packet, block: PacketReaction.Builder<R>.() -> Unit): PacketTransmission<R> {
        check(state != State.Detached) { "The resources of this gateway are detached, create another one" }

        return talker!!.sendPacket(packet, block)
    }

    private fun resetState(configuration: GatewayConfiguration) {
        when (state) {
            is State.Running -> throw IllegalStateException("The Gateway is already running, call stop() first.")
            is State.Detached -> throw IllegalStateException("The Gateway has been detached and can no longer be used, create a new instance instead.")
            is State.Stopped -> Unit
        }

        authenticationHandler.configuration = configuration
        retry.reset()
        state = State.Running(true)  //resetting state
    }


    override suspend fun start(configuration: GatewayConfiguration) {
        resetState(configuration)

        val client = HttpClient {
            install(WebSockets)
        }

        this.client = client

        while (retry.hasNext && state is State.Running) {
            logger.info("Connecting to NexusHub...")

            val socket = try {
                client.webSocketSession(
                    HttpMethod.Get,
                    host = data.host,
                    port = data.port,
                    path = "/connection",
                )
            } catch (e: Exception) {
                if (e is UnresolvedAddressException) {
                    data.eventFlow.emit(Close.Timeout)
                }

                retry.retry()
                continue
            }


            talker = ServerTalker(socket, Dispatchers.IO)

            // Incoming would end on close
            try {
                readSocket()
            } catch (exception: Exception) {
                logger.error("", exception)
            }

            try {
                handleClose()
            } catch (exception: Exception) {
                logger.error("", exception)
            }

            if (state.retry) retry.retry()
            else data.eventFlow.emit(Close.RetryLimitReached)
        }

        _ping.value = null
    }


    private suspend fun readSocket() {
        talker!!.websocket.incoming.asFlow().buffer(Channel.UNLIMITED).collect {
            when (it) {
                is Frame.Binary -> {
                    val context = talker!!.receive(it.readBytes()) ?: return@collect

                    if (context.isResponse) return@collect

                    val event = NetworkEvent.decode(context)
                    data.eventFlow.emit(event)
                }

                else -> { /*ignore*/ }
            }
        }
    }





    private suspend fun handleClose() {
        val reason = withTimeoutOrNull(1500) {
            talker?.websocket?.closeReason?.await()
        } ?: return

        val code = CloseCode.fromCode(reason.code)
        if (code == null) {
            logger.warn("WebSocket channel closed with unknown reason. Code: $code")
            return
        }

        logger.info("Failed to connect to NexusHub: $code. Comment: ${reason.message}")

        data.eventFlow.emit(Close.ServerClose(code, reason.message, code.retry))

        when {
            !code.retry -> {
                state = State.Stopped
                throw IllegalStateException("Gateway closed: ${reason.code} ${reason.message}")
            }
        }
    }

    override suspend fun stop() {
        check(state !is State.Detached) { "The resources of this gateway are detached, create another one" }
        data.eventFlow.emit(Close.UserClose)
        state = State.Stopped
        _ping.value = null
        if (talker?.isOpen == true)
            talker!!.close(CloseCode.NORMAL, "Leaving")
    }

    override suspend fun detach() {
        (this as CoroutineScope).cancel()
        if (state is State.Detached) return
        state = State.Detached
        _ping.value = null
        data.eventFlow.emit(Close.Detach)
        val talker = talker
        if (talker?.isOpen == true) {
            talker.close(CloseCode.NORMAL, "Leaving")
        }
        client?.close()
    }
}


private fun <T> ReceiveChannel<T>.asFlow() = flow {
    try {
        for (value in this@asFlow) emit(value)
    } catch (_: CancellationException) {
        //reading was stopped from somewhere else, ignore
    }
}

