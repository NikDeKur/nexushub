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
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.auth.account.Account
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.talker.ClientTalker
import dev.nikdekur.nexushub.util.CloseCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class RuntimeNodesService(
    override val app: NexusHubServer
) : NexusHubService, NodesService {

    // Specify it as last, to be first for unloading
    // We have to close all nodes before unloading other services
    override val dependencies = Dependencies.last()

    val datasetService: DataSetService by inject()

    lateinit var scope: CoroutineScheduler

    // Where key is either the node id or the talker address
    val nodesMap = ConcurrentHashMap<String, DefaultNode>()

    override val nodes: Collection<Node>
        get() = nodesMap.values

    override fun onLoad() {
        scope = CoroutineScheduler.fromSupervisor(Dispatchers.Default)
        val config = datasetService.getDataSet().network.ping
        val interval = config.interval * 1000L
        val deadInterval = interval + config.extraInterval

        scope.runTaskTimer(interval) {
            nodesMap.values.forEach { node ->
                if (node.isAlive(deadInterval)) return@forEach
                node.close(CloseCode.PING_FAILED, "Ping failed")
            }
        }
    }

    override fun onUnload() {
        runBlocking {
            closeAllNodes(CloseCode.SHUTDOWN, "Server is shutting down")
        }
        scope.cancel()
    }


    override fun newNode(talker: ClientTalker, account: Account, id: String): Node {
        val node = DefaultNode(app, talker, account, id)
        nodesMap[node.id] = node
        nodesMap[node.talker.addressStr] = node
        return node
    }

    override fun getNode(talker: ClientTalker): Node? {
        return nodesMap[talker.addressStr]
    }

    override fun getNode(id: String): Node? {
        return nodesMap[id]
    }


    override fun removeNode(talker: ClientTalker): Node? {
        return getNode(talker)?.also {
            nodesMap.remove(it.id)
            nodesMap.remove(it.addressStr)
        }
    }


    fun getAuthenticatedNode(talker: Talker): DefaultNode? {
        return nodesMap[talker.addressStr]
    }

    fun isNodeExists(nodeId: String): Boolean {
        return nodesMap.containsKey(nodeId)
    }


    override suspend fun closeAllNodes(code: CloseCode, reason: String) {
        nodes.map {
            scope.async {
                it.close(code, reason)
            }
        }.awaitAll()
    }
}