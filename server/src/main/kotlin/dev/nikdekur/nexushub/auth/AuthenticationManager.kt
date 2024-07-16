package dev.nikdekur.nexushub.auth

import dev.nikdekur.ndkore.ext.info
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import dev.nikdekur.nexushub.NexusHub.logger
import dev.nikdekur.nexushub.auth.account.AccountManager
import dev.nikdekur.nexushub.auth.password.EncryptedPassword
import dev.nikdekur.nexushub.auth.password.PasswordEncryptor
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.node.ClientNode
import dev.nikdekur.nexushub.node.ClientTalker
import dev.nikdekur.nexushub.node.NodesManager
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.`in`.PacketAuth
import dev.nikdekur.nexushub.util.CloseCode

object AuthenticationManager {

    suspend fun executeAuthenticatedPacket(talker: ClientTalker, context: IncomingContext<Packet>) {
        // Authenticated node required
        val node = NodesManager.getAuthenticatedNode(talker)
        if (node == null) {
            talker.closeWithBlock(CloseCode.NOT_AUTHENTICATED)
            return
        }

        node.processAuthenticatedPacket(context)
    }

    suspend fun processAuth(talker: ClientTalker, packet: PacketAuth) {
        logger.info { "Authenticating node: ${packet.node}" }

        val account = AccountManager.getAccount(packet.login)
        if (account == null) {
            // Imitate hashing delay to hacker think login exists
            logger.info { "Account not found: ${packet.login}" }
            delay(PasswordEncryptor.averageHashTime())
            talker.closeWithBlock(CloseCode.WRONG_CREDENTIALS)
            return
        }

        val isCorrect = verifyPassword(account.password, packet.password)
        if (!isCorrect) {
            logger.info { "Incorrect password for account: ${packet.login}" }
            talker.closeWithBlock(CloseCode.WRONG_CREDENTIALS)
            return
        }

        val nodeStr = packet.node
        if (!isValidNodeName(nodeStr)) {
            talker.closeWithBlock(CloseCode.INVALID_DATA, "Provided node name is not valid")
            return
        }

        if (NodesManager.isNodeExists(talker)) {
            talker.closeWithBlock(CloseCode.NODE_ALREADY_EXISTS, "Node at this address already exists")
            return
        }

        if (NodesManager.isNodeExists(nodeStr)) {
            talker.closeWithBlock(CloseCode.NODE_ALREADY_EXISTS, "Node with this id already exists")
            return
        }

        val node = ClientNode(talker, nodeStr, account)
        NodesManager.addNode(node)

        logger.info { "Authenticated node: $node" }
    }

    val validNodePattern = Regex("[a-zA-Z0-9_]+")
    fun isValidNodeName(node: String): Boolean {
        return validNodePattern.matches(node) && node.length <= 32 && node.length >= 4
    }

    val encryptingDispatcher = Dispatchers.IO

    suspend fun verifyPassword(real: EncryptedPassword, submitted: String): Boolean {
        return withContext(encryptingDispatcher) {
            real.isEqual(submitted)
        }
    }
}