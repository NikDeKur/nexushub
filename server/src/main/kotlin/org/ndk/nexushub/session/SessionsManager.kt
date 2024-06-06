package org.ndk.nexushub.session

import org.ndk.global.map.multi.ConcurrentMultiHashMap
import org.ndk.global.map.set.ConcurrentSetsHashMap
import org.ndk.nexushub.node.ClientNode
import org.ndk.nexushub.scope.Scope

object SessionsManager {

    //                                    scope   holder
    val sessions = ConcurrentMultiHashMap<String, String, Session>()

    //                                          node   session
    val nodeToSessions = ConcurrentSetsHashMap<String, Session>()

    fun getExistingSession(scopeId: String, holderId: String): Session? {
        return sessions.get(scopeId, holderId)
    }

    fun startSession(node: ClientNode, scope: Scope, holderId: String) {
        val session = Session(node, scope, holderId)
        sessions.put(scope.id, holderId, session)
        nodeToSessions.add(node.id, session)
    }

    fun stopSession(scopeId: String, holderId: String) {
        val session = sessions.remove(scopeId, holderId)
        if (session != null) {
            nodeToSessions.delete(session.node.id, session)
        }
    }

    fun stopAllSessions(node: ClientNode) {
        val nodeSessions = nodeToSessions.remove(node.id)
        nodeSessions?.forEach {
            sessions.remove(it.scope.id, it.holderId)
        }
    }

    fun doesNodeHaveAnySessions(node: ClientNode): Boolean {
        return nodeToSessions.contains(node.id)
    }
}