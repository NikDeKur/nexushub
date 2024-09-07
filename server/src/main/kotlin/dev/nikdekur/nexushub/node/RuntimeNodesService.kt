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
import dev.nikdekur.nexushub.account.Account
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.util.CloseCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class RuntimeNodesService(
    override val app: NexusHubServer
) : NexusHubService(), NodesService {

    // Specify it as last, to be first for unloading
    // We have to close all nodes before unloading other services
    override val dependencies = Dependencies.last()


    override lateinit var syncScope: CoroutineScheduler

    // Where key is either the node id or the talker address
    val nodesMap = ConcurrentHashMap<String, Node>()



    override val nodes: Collection<Node>
        get() = nodesMap.values

    override fun onEnable() {
        syncScope = CoroutineScheduler.fromSupervisor(Dispatchers.IO)

    }

    override fun onDisable() {
        runBlocking {
            closeAllNodes(CloseCode.SHUTDOWN, "Server is shutting down")
        }
        syncScope.cancel()
    }


    override fun newNode(talker: Talker, account: Account, id: String): Node {
        val node = DefaultNode(app, talker, account, id)
        nodesMap[node.id] = node
        nodesMap[node.talker.address.toString()] = node
        return node
    }

    override fun getNode(talker: Talker): Node? {
        return nodesMap[talker.address.toString()]
    }

    override fun getNode(id: String): Node? {
        return nodesMap[id]
    }


    override fun removeNode(talker: Talker): Node? {
        return getNode(talker)?.also {
            nodesMap.remove(it.id)
            nodesMap.remove(it.address.toString())
        }
    }


    override suspend fun closeAllNodes(code: CloseCode, reason: String) {
        nodes.map {
            syncScope.async {
                it.close(code, reason)
            }
        }.awaitAll()
    }
}