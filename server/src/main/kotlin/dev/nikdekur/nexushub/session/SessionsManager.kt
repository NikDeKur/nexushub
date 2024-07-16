package dev.nikdekur.nexushub.session

import dev.nikdekur.ndkore.ext.parallel
import dev.nikdekur.ndkore.map.multi.ConcurrentMultiHashMap
import dev.nikdekur.ndkore.map.set.ConcurrentSetsHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import dev.nikdekur.nexushub.NexusHub.config
import dev.nikdekur.nexushub.node.ClientNode
import dev.nikdekur.nexushub.scope.Scope

object SessionsManager {

    //                                    scope   holder
    val sessions = ConcurrentMultiHashMap<String, String, Session>()

    //                                          node   session
    val nodeToSessions = ConcurrentSetsHashMap<String, Session>()

    val scopeToNodes = ConcurrentSetsHashMap<String, ClientNode>()

    fun getExistingSession(scopeId: String, holderId: String): Session? {
        return sessions[scopeId, holderId]
    }

    fun startSession(node: ClientNode, scope: Scope, holderId: String) {
        val session = Session(node, scope, holderId)
        sessions.put(scope.id, holderId, session)
        nodeToSessions.add(node.id, session)
        scopeToNodes.add(scope.id, node)
    }

    fun stopSession(scopeId: String, holderId: String) {
        val session = sessions.remove(scopeId, holderId)
        if (session != null) {
            val node = session.node
            nodeToSessions.delete(node.id, session)
            scopeToNodes.delete(scopeId, node)
        }
    }


    fun stopAllSessions(node: ClientNode) {
        val nodeSessions = nodeToSessions.remove(node.id)
        nodeSessions?.forEach {
            sessions.remove(it.scope.id, it.holderId)
            scopeToNodes.delete(it.scope.id, node)
        }
    }

    fun hasAnySessions(node: ClientNode): Boolean {
        return nodeToSessions.contains(node.id)
    }

    val syncingScope = CoroutineScope(Dispatchers.IO)

    suspend fun requestSync(scope: Scope) {
        val nodes = scopeToNodes[scope.id]

        syncingScope.parallel(config.data.sync_parallelism, nodes) {
            it.requestSync(scope)
        }.awaitAll()
    }
}