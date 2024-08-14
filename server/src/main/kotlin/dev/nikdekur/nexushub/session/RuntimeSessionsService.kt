/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.session

import dev.nikdekur.ndkore.map.multi.ConcurrentMultiHashMap
import dev.nikdekur.ndkore.map.set.ConcurrentSetsHashMap
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.config.NexusHubServerConfig
import dev.nikdekur.nexushub.node.ClientNode
import dev.nikdekur.nexushub.scope.Scope
import dev.nikdekur.nexushub.service.NexusHubService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import org.koin.core.component.inject

class RuntimeSessionsService(
    override val app: NexusHubServer
) : NexusHubService, SessionsService {

    val config by inject<NexusHubServerConfig>()

    lateinit var syncingScope: CoroutineScope

    override fun onLoad() {
        syncingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    override fun onUnload() {
        syncingScope.cancel()
        sessions.clear()
        nodeToSessions.clear()
        scopeToNodes.clear()
    }

    //                                    scope   holder
    val sessions = ConcurrentMultiHashMap<String, String, Session>()
    val nodeToSessions = ConcurrentSetsHashMap<String, Session>()
    val scopeToNodes = ConcurrentSetsHashMap<String, ClientNode>()

    override fun getExistingSession(scopeId: String, holderId: String): Session? {
        return sessions[scopeId, holderId]
    }

    override fun startSession(node: ClientNode, scope: Scope, holderId: String) {
        val session = Session(node, scope, holderId)
        sessions.put(scope.id, holderId, session)
        nodeToSessions.add(node.id, session)
        scopeToNodes.add(scope.id, node)
    }

    override fun stopSession(scopeId: String, holderId: String) {
        val session = sessions.remove(scopeId, holderId)
        if (session != null) {
            val node = session.node
            nodeToSessions.delete(node.id, session)
            scopeToNodes.delete(scopeId, node)
        }
    }


    override fun stopAllSessions(node: ClientNode) {
        val nodeSessions = nodeToSessions.remove(node.id)
        nodeSessions?.forEach {
            sessions.remove(it.scope.id, it.holderId)
            scopeToNodes.delete(it.scope.id, node)
        }
    }

    override fun hasAnySessions(node: ClientNode): Boolean {
        return nodeToSessions.contains(node.id)
    }


    override suspend fun requestSync(scope: Scope) {
        val nodes = scopeToNodes[scope.id]

        nodes.map {
            syncingScope.async {
                it.requestSync(scope)
            }
        }.awaitAll()
    }
}