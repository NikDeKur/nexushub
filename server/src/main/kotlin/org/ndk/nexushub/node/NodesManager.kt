package org.ndk.nexushub.node

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.ndk.klib.info
import org.ndk.klib.removeIf
import org.ndk.nexushub.NexusHub
import org.ndk.nexushub.NexusHub.logger
import org.ndk.nexushub.network.talker.Talker
import java.util.concurrent.ConcurrentHashMap

object NodesManager {

    const val ALIVE_TIMEOUT = 1000 * 5L // 5 seconds

    val connectedNodes = ConcurrentHashMap<String, ClientNode>()
    val socketToNode = ConcurrentHashMap<Talker, ClientNode>()

    init {
        NexusHub.blockingScope.runTaskTimer(ALIVE_TIMEOUT) {
            checkActiveNodes()
        }
    }

    fun checkActiveNodes() {
        NexusHub.blockingScope.launch {
            connectedNodes.values.map { node ->
                async { node.checkAlive() }
            }.awaitAll()

            connectedNodes.removeIf { _, node ->
                val alive = node.isAlive

                if (!alive) {
                    logger.info { "Node ${node.id} is not alive. Removing it." }
                }
                !alive
            }
        }
    }

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