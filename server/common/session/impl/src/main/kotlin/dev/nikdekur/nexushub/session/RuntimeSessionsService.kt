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
import dev.nikdekur.nexushub.service.NexusHubService
import java.util.concurrent.ConcurrentHashMap

class RuntimeSessionsService(
    override val app: NexusHubServer
) : NexusHubService(), SessionsService {

    override fun onDisable() {
        sessions.clear()
        nodeToSessions.clear()
        scopeToNodes.clear()
    }

    //                            scope   holder
    val sessions: MutableMultiMap<String, String, Session> = ConcurrentHashMap()
    val nodeToSessions: MutableSetsMap<String, Session> = ConcurrentHashMap()
    val scopeToNodes: MutableSetsMap<String, dev.nikdekur.nexushub.node.Node> = ConcurrentHashMap()

    override fun getExistingSession(scopeId: String, holderId: String): Session? {
        return sessions[scopeId, holderId]
    }

    override fun getNodes(scope: dev.nikdekur.nexushub.scope.Scope): Iterable<dev.nikdekur.nexushub.node.Node> {
        return scopeToNodes[scope.id] ?: emptySet()
    }

    override fun startSession(
        node: dev.nikdekur.nexushub.node.Node,
        scope: dev.nikdekur.nexushub.scope.Scope,
        holderId: String
    ) {
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
        }
    }


    override fun stopAllSessions(node: dev.nikdekur.nexushub.node.Node) {
        val nodeSessions = nodeToSessions.remove(node.id)
        nodeSessions?.forEach {
            sessions.remove(
                it.scope.id,
                it.holderId
            )
        }
    }

    override fun hasAnySessions(node: dev.nikdekur.nexushub.node.Node): Boolean {
        return nodeToSessions.contains(node.id)
    }
}