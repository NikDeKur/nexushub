package org.ndk.nexushub.node

import io.ktor.websocket.CloseReason
import kotlinx.coroutines.launch
import org.ndk.nexushub.NexusHub
import org.ndk.nexushub.NexusHub.blockingScope
import org.ndk.nexushub.network.talker.Talker
import java.util.concurrent.ConcurrentHashMap

object NodesManager {

    fun init() {
        val config = NexusHub.config.network.ping
        val interval = config.interval * 1000L
        blockingScope.runTaskTimer(interval) {
            connectedNodes.values.forEach { node ->
                blockingScope.launch {
                    if (node.createdAt + config.warningThreshold > System.currentTimeMillis())
                        return@launch

                    val result = node.  ping()
                    if (!result) {
                        node.close(CloseReason.Codes.GOING_AWAY.code, "Ping failed.", true)
                    }
                }
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

    suspend fun closeAll(code: Short, reason: String) {
        connectedNodes.values.forEach { it.talker.close(code, reason, false) }
    }
}