package org.ndk.nexushub.auth

import dev.nikdekur.ndkore.ext.info
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.ndk.nexushub.NexusHub.logger
import org.ndk.nexushub.auth.account.AccountManager
import org.ndk.nexushub.auth.password.EncryptedPassword
import org.ndk.nexushub.auth.password.PasswordEncryptor
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.node.ClientNode
import org.ndk.nexushub.node.NodesManager
import org.ndk.nexushub.packet.Packet
import org.ndk.nexushub.packet.PacketOk
import org.ndk.nexushub.packet.`in`.PacketAuth
import org.ndk.nexushub.util.CloseCode
import org.ndk.nexushub.util.close

object AuthenticationManager {



    suspend fun executePacket(talker: Talker, context: IncomingContext<Packet>) {
        when (context.packet) {
            is PacketAuth -> {
                val node = NodesManager.getAuthenticatedNode(talker)
                if (node != null) {
                    node.close(CloseCode.UNEXPECTED_BEHAVIOUR, "You are already authenticated!", true)
                    return
                }

                @Suppress("UNCHECKED_CAST")
                processAuth(talker, context as IncomingContext<PacketAuth>)
            }

            else -> {

                // Authenticated node required
                val node = NodesManager.getAuthenticatedNode(talker)
                if (node == null) {
                    talker.close(CloseCode.NODE_IS_NOT_AUTHENTICATED, "Authenticate before accessing this!", true)
                    return
                }

                node.processAuthenticatedPacket(context)
            }
        }
    }

    private suspend fun processAuth(talker: Talker, context: IncomingContext<PacketAuth>) {
        val packet = context.packet
        logger.info { "Authenticating node: ${packet.node}" }

        val account = AccountManager.fetchAccount(packet.login)
        if (account == null) {
            // Imitate hashing delay to hacker think login exists
            logger.info { "Account not found: ${packet.login}" }
            delay(PasswordEncryptor.averageHashTime())
            talker.close(CloseCode.WRONG_CREDENTIALS, "", true)
            return
        }

        val isCorrect = verifyPassword(account.password, packet.password)
        if (!isCorrect) {
            logger.info { "Incorrect password for account: ${packet.login}" }
            talker.close(CloseCode.WRONG_CREDENTIALS, "", true)
            return
        }

        val nodeStr = packet.node
        if (!isValidNodeName(nodeStr)) {
            talker.close(CloseCode.INVALID_DATA, "Provided node name is not valid", true)
            return
        }

        if (NodesManager.isNodeExists(talker)) {
            talker.close(CloseCode.NODE_ALREADY_EXISTS, "Node at this address already exists", true)
            return
        }

        if (NodesManager.isNodeExists(nodeStr)) {
            talker.close(CloseCode.NODE_ALREADY_EXISTS, "Node with this id already exists", true)
            return
        }

        val node = ClientNode(talker, nodeStr, account)
        NodesManager.addNode(node)

        context.respond(PacketOk("Authentication successful"))

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