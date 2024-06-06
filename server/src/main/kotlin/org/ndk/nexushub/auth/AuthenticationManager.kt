package org.ndk.nexushub.auth

import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.ndk.klib.info
import org.ndk.nexushub.NexusHub.logger
import org.ndk.nexushub.auth.account.AccountManager
import org.ndk.nexushub.auth.password.EncryptedPassword
import org.ndk.nexushub.auth.password.PasswordEncryptor
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.packet.Packet
import org.ndk.nexushub.network.packet.PacketAuth
import org.ndk.nexushub.network.packet.PacketOk
import org.ndk.nexushub.network.packet.PacketPong
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.node.ClientNode
import org.ndk.nexushub.node.NodesManager
import org.ndk.nexushub.node.close

object AuthenticationManager {
    suspend fun executePacket(talker: Talker, context: IncomingContext<Packet>) {
        try {
            val packet = context.packet

            if (packet !is PacketPong)
                logger.info { "Received packet: $packet" }

            if (packet is PacketAuth) {
                @Suppress("UNCHECKED_CAST")
                processAuth(talker, context as IncomingContext<PacketAuth>)
            } else {
                // Authenticated node required
                val node = NodesManager.getAuthenticatedNode(talker)
                if (node == null) {
                    talker.close(CloseReason.Codes.VIOLATED_POLICY, "Node is not authenticated")
                    return
                }

                node.processAuthenticatedPacket(context)
            }
        } catch (e: CancellationException) {
            // Connection closed while a processing packet
            // Do nothing
        } catch (e: Exception) {
            logger.info { "Error while processing packet: $e" }
            e.printStackTrace()
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
            talker.close(CloseReason.Codes.CANNOT_ACCEPT, "Incorrect authentication data")
            return
        }

        val isCorrect = verifyPassword(account.password, packet.password)
        if (!isCorrect) {
            logger.info { "Incorrect password for account: ${packet.login}" }
            talker.close(CloseReason.Codes.CANNOT_ACCEPT, "Incorrect authentication data")
            return
        }

        val nodeStr = packet.node
        if (nodeStr.isEmpty()) {
            talker.close(CloseReason.Codes.CANNOT_ACCEPT, "Node is empty")
            return
        }

        if (NodesManager.isNodeExists(talker)) {
            logger.info { "Node already exists" }
            talker.close(CloseReason.Codes.CANNOT_ACCEPT, "Node already exists")
            return
        }

        val node = ClientNode(talker, nodeStr, account)
        NodesManager.addNode(node)

        context.respond(PacketOk("Authentication successful"))

        logger.info { "Authenticated node: $node" }
    }


    val encryptingDispatcher = Dispatchers.IO

    suspend fun verifyPassword(real: EncryptedPassword, submitted: String): Boolean {
        return withContext(encryptingDispatcher) {
            real.isEqual(submitted)
        }
    }
}