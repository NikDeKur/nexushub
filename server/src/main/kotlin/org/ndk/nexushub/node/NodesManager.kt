package org.ndk.nexushub.node

import org.ndk.nexushub.network.talker.Talker
import java.util.concurrent.ConcurrentHashMap

object NodesManager {

    val connectedNodes = ConcurrentHashMap<String, ClientNode>()
    val socketToNode = ConcurrentHashMap<Talker, ClientNode>()

    fun addNode(node: ClientNode) {
        connectedNodes[node.id] = node
        socketToNode[node.talker] = node
    }



    fun removeNode(node: ClientNode) {
        connectedNodes.remove(node.id)
        socketToNode.remove(node.talker)
    }

    fun getAuthenticatedNode(talker: Talker): ClientNode? {
        return socketToNode[talker]
    }

    fun isNodeExists(node: String): Boolean {
        return connectedNodes.containsKey(node)
    }

    fun isNodeExists(talker: Talker): Boolean {
        return socketToNode.containsKey(talker)
    }

    suspend fun closeAll(code: Short, reason: String) {
        connectedNodes.values.forEach { it.talker.close(code, reason) }
    }
}