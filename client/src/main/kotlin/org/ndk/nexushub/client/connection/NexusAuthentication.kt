package org.ndk.nexushub.client.connection

import org.ndk.nexushub.packet.PacketAuth
import org.ndk.nexushub.packet.PacketOk

class NexusAuthentication(
    val connection: NexusHubConnection,
) {

    val logger = connection.hub.logger
    val talker = connection.talker!!
    var attempt = 1

    suspend fun start() {
        logger.info("Trying to authenticate...")
        authenticate()
    }

    private suspend fun authenticate() {
        val data = connection.hub.builder
        val authPacket = org.ndk.nexushub.packet.PacketAuth(data.username, data.password, data.node)

        talker.sendPacket(authPacket) {
            timeout(5000L * (attempt + 1)) {
                retry()
            }

            receive<org.ndk.nexushub.packet.PacketOk> {}
        }.await()
    }

    private suspend fun retry() {
        if (attempt >= 3)
            fail()

        logger.info("Authentication failed. Retrying $attempt time...")
        attempt++
        authenticate()
    }

    private fun fail(): Nothing {
        throw ConnectException.NoResponseException("Failed to authenticate after 3 attempts.")
    }
}