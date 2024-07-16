package dev.nikdekur.nexushub.node

import dev.nikdekur.nexushub.NexusHub
import dev.nikdekur.nexushub.NexusHub.blockingScope
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.util.CloseCode
import java.util.concurrent.ConcurrentHashMap

object NodesManager {

    fun init() {
        val config = NexusHub.config.network.ping
        val interval = config.interval * 1000L
        val deadInterval = interval + config.extraInterval

        blockingScope.runTaskTimer(interval) {
            connectedNodes.values.forEach { node ->
                if (node.isAlive(deadInterval)) return@forEach
                node.close(CloseCode.PING_FAILED, "Ping failed")
            }
        }
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