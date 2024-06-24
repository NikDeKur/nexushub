package org.ndk.nexushub.client.connection

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.client.service.NexusService
import org.ndk.nexushub.client.sesion.Session
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.dsl.PacketReaction
import org.ndk.nexushub.network.transmission.PacketTransmission
import org.ndk.nexushub.packet.Packet
import org.ndk.nexushub.packet.PacketError
import org.ndk.nexushub.packet.PacketError.Level
import org.ndk.nexushub.packet.PacketOk
import org.ndk.nexushub.packet.`in`.PacketAuth
import org.ndk.nexushub.packet.`in`.PacketBatchSaveData
import org.ndk.nexushub.packet.`in`.PacketPong
import org.ndk.nexushub.packet.`in`.PacketSaveData
import org.ndk.nexushub.packet.out.PacketPing
import org.ndk.nexushub.packet.out.PacketRequestSync
import org.ndk.nexushub.packet.out.PacketStopSession

class NexusHubConnection(val hub: NexusHub, val data: ConnectionConfiguration) {

    val receivingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val packetsFlow = MutableSharedFlow<IncomingContext<out Packet>>()
        .apply {
            receivingScope.launch {
                collect {
                    receivingScope.launch {
                        onPacketReceived(it)
                    }
                }
            }
        }

    val gateway = DefaultGateway(
        configuration = data.gatewayConfiguration,
        packetsFlow = packetsFlow,
        onReady = ::onReady
    )

    val logger = hub.logger


    suspend fun start() {
        gateway.connect()
    }


    suspend fun onReady() {
        logger.info("Trying to authenticate...")
        val authPacket = PacketAuth(data.login, data.password, data.node)

        hub.connection.sendPacket(authPacket) {
            timeout(5000L) {
                throw ConnectException.NoResponse("Failed to authenticate, no response.")
            }

            receive<PacketOk> {}

            receive {
                throw NexusException.UnexpectedBehaviour("Unexpected behaviour while authenticating. Received: $packet", "")
            }
        }.await()

        logger.info("Authenticated successfully. Connection established.")

        hub.builder.onReady()
    }

    suspend fun stop() {
        gateway.disconnect()
    }

    suspend fun <R> sendPacket(packet: Packet, reaction: PacketReaction.Builder<R>.() -> Unit): PacketTransmission<R> {
        return gateway.sendPacket(packet, reaction)
    }


    suspend fun processStopSession(context: IncomingContext<PacketStopSession>) {
        val packet = context.packet
        val scope = packet.scopeId
        val holderId = packet.holderId

        val service = hub.getService(scope) ?: return
        val session = service.getExistingSession(holderId)
        if (session == null) {
            context.respond(
                PacketError(
                    Level.ERROR,
                    PacketError.Code.SESSION_NOT_FOUND,
                    "No session found"
                )
            )
            return
        }

        if (!session.hasToBeSaved()) {
            context.respond(PacketOk("No data to save"))
            return
        }

        val data = session.serializeData()
        val savePacket = PacketSaveData(scope, holderId, data)

        fun <H, S> remove(service: NexusService<H, S>, session: Session<*, *>) {
            service.removeSession(session as Session<H, S>)
        }

        remove(service, session)

        context.respond(savePacket)
    }

    suspend fun processPing(context: IncomingContext<PacketPing>) {
        context.respond(
            PacketPong()
        )
    }

    suspend fun processSyncData(context: IncomingContext<PacketRequestSync>) {
        val packet = context.packet
        val scopeId = packet.scope

        logger.info("Processing sync data for $scopeId")

        suspend fun ok() {
            context.respond(PacketOk("No data to save"))
        }

        val service = hub.getService(scopeId) ?: return ok()
        val sessions = service.sessions
        if (!service.isRunning || sessions.isEmpty())
            return ok()

        logger.info("Service is running and has sessions")

        val batchMap = HashMap<String, String>()

        sessions.forEach {
            if (!it.hasToBeSaved()) return@forEach
            logger.info("Session has to be saved!")

            val dataStr = it.serializeData()
            batchMap[it.id] = dataStr
        }

        logger.info("Batch map: $batchMap")



        if (batchMap.isEmpty())
            return ok()


        val batchPacket = PacketBatchSaveData(scopeId, batchMap)
        context.respond(batchPacket)

    }



    fun prepareBatchSaveData(service: NexusService<*, *>): Map<String, String> {
        val sessions = service.sessions
        if (!service.isRunning || sessions.isEmpty())
            return emptyMap()

        val batchMap = HashMap<String, String>()

        sessions.forEach {
            if (!it.hasToBeSaved()) return@forEach

            val dataStr = it.serializeData()
            batchMap[it.id] = dataStr
        }

        return batchMap
    }


    suspend fun onPacketReceived(context: IncomingContext<Packet>) {
        val packet = context.packet
        logger.debug("Packet received: $packet")

        // If the packet is a response, we don't need to process it
        if (context.isResponse)
            return

        @Suppress("UNCHECKED_CAST")
        when (packet) {
            is PacketPing -> processPing(context as IncomingContext<PacketPing>)
            is PacketRequestSync -> processSyncData(context as IncomingContext<PacketRequestSync>)
            is PacketStopSession -> processStopSession(context as IncomingContext<PacketStopSession>)
            else -> logger.warn("Unhandled packet: $packet")
        }
    }


}