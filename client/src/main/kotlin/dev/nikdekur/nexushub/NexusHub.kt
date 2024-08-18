/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")
@file:OptIn(ExperimentalContracts::class)

package dev.nikdekur.nexushub

import dev.nikdekur.ndkore.ext.*
import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import dev.nikdekur.nexushub.connection.NexusHubHandler
import dev.nikdekur.nexushub.connection.gateway.Gateway
import dev.nikdekur.nexushub.connection.gateway.GatewayConfiguration
import dev.nikdekur.nexushub.event.Event
import dev.nikdekur.nexushub.service.NexusService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class NexusHub(
    val gateway: Gateway,
    val configuration: GatewayConfiguration,
    private val eventFlow: MutableSharedFlow<Event>,
    dispatcher: CoroutineDispatcher
) : CoroutineScheduler(CoroutineScope(dispatcher + SupervisorJob())) {

    init {
        gateway.events
            .buffer(Channel.UNLIMITED)
            .onEach { event ->
                eventFlow.emit(event)
            }
            .launchIn(this)
    }

    val handler = NexusHubHandler(this)

    val logger: Logger = LoggerFactory.getLogger("NexusHub")

    val events: SharedFlow<Event>
        get() = eventFlow


    /**
     * Starts the client and connect to NexusHub Server
     *
     * Will suspend coroutine until [stop] or [shutdown] is called.
     */
    suspend fun start() {
        gateway.start(configuration)
    }

    /**
     * Stops the service, disconnect from NexusHub Server.
     */
    suspend fun stop() {
        logger.info { "Stopping NexusHub..." }

        stopServices()
        gateway.stop()
    }

    suspend fun shutdown() {
        logger.info { "Shutting NexusHub down..." }

        stopServices()
        gateway.detach()

        // resolve ambiguous coroutineContext
        (this as CoroutineScope).cancel()
    }

    internal suspend fun stopServices() {
        services.values.forEachSafe({
            logger.warn("Error occurred while stopping service with scope $scope", this)
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


    inline fun <reified T : Event> on(
        scope: CoroutineScope = this,
        noinline consumer: suspend T.() -> Unit
    ): Job =
        events.buffer(Channel.UNLIMITED)
            .filterIsInstance<T>()
            .onEach {
                runCatching {
                    this.launch { consumer(it) }
                }.onFailure { logger.error("", it) }
            }.catch { logger.error("", it) }
            .launchIn(scope)

}


inline fun NexusHub(builder: NexusHubBuilder.() -> Unit): NexusHub {
    contract {
        callsInPlace(builder, InvocationKind.EXACTLY_ONCE)
    }

    return NexusHubBuilder().apply(builder).build()
}