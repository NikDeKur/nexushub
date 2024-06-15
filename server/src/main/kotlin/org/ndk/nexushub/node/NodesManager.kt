package org.ndk.nexushub.node

import org.ndk.nexushub.network.talker.Talker
import java.util.concurrent.ConcurrentHashMap

object NodesManager {

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

    fun isNodeExists(node: String): Boolean {
        return connectedNodes.containsKey(node)
    }

    fun isNodeExists(talker: Talker): Boolean {
        return socketToNode.containsKey(talker.addressHash)
    }

    suspend fun closeAll(code: Short, reason: String) {
        connectedNodes.values.forEach { it.talker.close(code, reason) }
    }
}