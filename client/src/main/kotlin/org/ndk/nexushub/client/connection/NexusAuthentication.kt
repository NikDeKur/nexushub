package org.ndk.nexushub.client.connection

import kotlinx.coroutines.CompletableDeferred
import org.ndk.klib.smartAwait
import org.ndk.nexushub.network.packet.PacketAuth
import org.ndk.nexushub.network.packet.PacketOk

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
        val authPacket = PacketAuth(data.username, data.password, data.node)
        val def = CompletableDeferred<Unit>()

        talker.sendPacket(authPacket) {
            timeout(5000L * (attempt + 1)) {
                retry(def)
            }

            receive<PacketOk> {
                def.complete(Unit)
            }
        }

        def.smartAwait()
    }

    private suspend fun retry(def: CompletableDeferred<Unit>) {
        if (attempt >= 3)
            fail(def)

        logger.info("Authentication failed. Retrying $attempt time...")
        attempt++
        authenticate()
    }

    private fun fail(def: CompletableDeferred<Unit>): Nothing {
        def.complete(Unit)
        throw ConnectException.NoResponseException("Failed to authenticate after 3 attempts.")
    }
}