/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.session

import dev.nikdekur.ndkore.ext.ConcurrentHashSet
import dev.nikdekur.ndkore.map.MutableMultiMap
import dev.nikdekur.ndkore.map.MutableSetsMap
import dev.nikdekur.ndkore.map.add
import dev.nikdekur.ndkore.map.get
import dev.nikdekur.ndkore.map.put
import dev.nikdekur.ndkore.map.remove
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.node.DefaultNode
import dev.nikdekur.nexushub.scope.Scope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentHashMap

class RuntimeSessionsService(
    override val app: NexusHubServer
) : SessionsService {

    lateinit var syncingScope: CoroutineScope

    override fun onEnable() {
        syncingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    override fun onDisable() {
        syncingScope.cancel()
        sessions.clear()
        nodeToSessions.clear()
        scopeToNodes.clear()
    }

    //                                    scope   holder
    val sessions: MutableMultiMap<String, String, Session> = ConcurrentHashMap()
    val nodeToSessions: MutableSetsMap<String, Session> = ConcurrentHashMap()
    val scopeToNodes: MutableSetsMap<String, DefaultNode> = ConcurrentHashMap()

    override fun getExistingSession(scopeId: String, holderId: String): Session? {
        return sessions[scopeId, holderId]
    }

    override fun startSession(node: DefaultNode, scope: Scope, holderId: String) {
        val session = Session(node, scope, holderId)
        sessions.put(scope.id, holderId, session, ::ConcurrentHashMap)
        nodeToSessions.add(node.id, session, ::ConcurrentHashSet)
        scopeToNodes.add(scope.id, node, ::ConcurrentHashSet)
    }

    override fun stopSession(scopeId: String, holderId: String) {
        val session = sessions.remove(scopeId, holderId)
        if (session != null) {
            val node = session.node
            nodeToSessions.remove(node.id, session)
            scopeToNodes.remove(scopeId, node)
        }
    }


    override fun stopAllSessions(node: DefaultNode) {
        val nodeSessions = nodeToSessions.remove(node.id)
        nodeSessions?.forEach {
            sessions.remove(it.scope.id, it.holderId)
            scopeToNodes.remove(it.scope.id, node)
        }
    }

    override fun hasAnySessions(node: DefaultNode): Boolean {
        return nodeToSessions.contains(node.id)
    }


    override suspend fun requestSync(scope: Scope) {
        val nodes = scopeToNodes[scope.id] ?: return

        nodes.map {
            syncingScope.async {
                it.requestSync(scope)
            }
        }.awaitAll()
    }
}