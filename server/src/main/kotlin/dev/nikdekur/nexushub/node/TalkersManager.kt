package dev.nikdekur.nexushub.node

import dev.nikdekur.nexushub.NexusHub.logger
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.session.SessionsManager
import java.util.concurrent.ConcurrentHashMap

object TalkersManager {

    val talkers = ConcurrentHashMap<Int, Talker>()

    fun getExistingTalker(address: Int): Talker? {
        return talkers[address]
    }

    fun setTalker(address: Int, talker: Talker) {
        talkers[address] = talker
    }

    fun removeTalker(talker: Int) {
        talkers.remove(talker)
    }


    fun cleanUp(address: Int) {
        getExistingTalker(address)?.let { talker ->
            val node = NodesManager.getAuthenticatedNode(talker)
            if (node != null) {
                logger.info("[${talker.addressStr}] Node ${node.id} disconnected")
                NodesManager.removeNode(node)
                SessionsManager.stopAllSessions(node)
            }

            removeTalker(address)
        }
    }
}