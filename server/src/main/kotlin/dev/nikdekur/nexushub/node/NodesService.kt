/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.node

import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import dev.nikdekur.ndkore.service.Dependencies
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.config.NexusHubServerConfig
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.util.CloseCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.koin.core.component.inject
import java.util.concurrent.ConcurrentHashMap

class NodesService(
    override val app: NexusHubServer
) : NexusHubService {

    // Specify it as last, to be first for unloading
    // We have to close all nodes before unloading other services
    override val dependencies = Dependencies.last()

    val config: NexusHubServerConfig by inject()

    lateinit var scope: CoroutineScheduler

    override fun onLoad() {
        scope = CoroutineScheduler.fromSupervisor(Dispatchers.Default)
        val config = config.network.ping
        val interval = config.interval * 1000L
        val deadInterval = interval + config.extraInterval

        scope.runTaskTimer(interval) {
            connectedNodes.values.forEach { node ->
                if (node.isAlive(deadInterval)) return@forEach
                node.close(CloseCode.PING_FAILED, "Ping failed")
            }
        }
    }

    override fun onUnload() {
        runBlocking {
            closeAll(CloseCode.SHUTDOWN, "Server is shutting down")
        }
        scope.cancel()
    }

    val connectedNodes = ConcurrentHashMap<String, ClientNode>()
    val socketToNode = ConcurrentHashMap<Int, ClientNode>()

    fun addNode(node: ClientNode) {
        connectedNodes[node.id] = node
        socketToNode[node.talker.addressHash] = node
    }



    fun removeNode(node: ClientNode) {
        connectedNodes.remove(node.id)
        socketToNode.remove(node.talker.addressHash)
    }

    fun getAuthenticatedNode(talker: Talker): ClientNode? {
        return socketToNode[talker.addressHash]
    }

    fun isNodeExists(nodeId: String): Boolean {
        return connectedNodes.containsKey(nodeId)
    }

    fun isNodeExists(talker: Talker): Boolean {
        return socketToNode.containsKey(talker.addressHash)
    }

    suspend fun closeAll(code: CloseCode, reason: String) {
        connectedNodes.values.forEach {
            it.talker.close(code, reason)
        }
    }
}