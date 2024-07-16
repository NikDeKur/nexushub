@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.connection

import dev.nikdekur.nexushub.NexusHub
import dev.nikdekur.nexushub.event.Close
import dev.nikdekur.nexushub.event.NetworkEvent
import dev.nikdekur.nexushub.packet.PacketError
import dev.nikdekur.nexushub.packet.PacketError.Level
import dev.nikdekur.nexushub.packet.PacketOk
import dev.nikdekur.nexushub.packet.`in`.PacketBatchSaveData
import dev.nikdekur.nexushub.packet.`in`.PacketSaveData
import dev.nikdekur.nexushub.service.NexusService
import dev.nikdekur.nexushub.sesion.Session
import org.slf4j.LoggerFactory

class NexusHubHandler(
    val hub: NexusHub
) {

    val logger = LoggerFactory.getLogger("NexusHubHandler")

    init {
        hub.on<NetworkEvent> {
            if (context.isResponse) return@on
            when (this) {
                is NetworkEvent.StopSession -> processStopSession(this)
                is NetworkEvent.Sync -> processSyncData(this)
                else -> {
                    // Ignore
                }
            }
        }

        hub.on<NetworkEvent.ReadyEvent> {
            logger.info("NexusHub is ready")
        }

        hub.on<Close.ServerClose> {
            if (!code.allowRespond) return@on
            hub.stopServices()
        }
    }

    suspend fun processStopSession(event: NetworkEvent.StopSession) {
        val scope = event.scopeId
        val holderId = event.holderId

        val service = hub.getService(scope) ?: return
        val session = service.getExistingSession(holderId)
        if (session == null) {
            event.context.respond<String>(
                PacketError(
                    Level.ERROR,
                    PacketError.Code.SESSION_NOT_FOUND,
                    "No session found"
                )
            )
            return
        }

        if (!session.hasDataUpdated()) {
            event.respond(PacketOk("No data to save"))
            return
        }

        val data = session.serializeData()
        val savePacket = PacketSaveData(scope, holderId, data)

        fun <H, S> remove(service: NexusService<H, S>, session: Session<*, *>) {
            @Suppress("UNCHECKED_CAST")
            service.removeSession(session as Session<H, S>)
        }

        remove(service, session)

        event.respond(savePacket)
    }



    suspend fun processSyncData(event: NetworkEvent.Sync) {
        val scopeId = event.scopeId

        logger.debug("[$scopeId] Processing sync data")

        suspend fun ok() {
            logger.debug("[$scopeId] No data to save")
            event.respond(PacketOk("No data to save"))
        }

        val service = hub.getService(scopeId) ?: return ok()

        val batchMap = service.prepareBatchSaveData()
        if (batchMap.isEmpty())
            return ok()


        val batchPacket = PacketBatchSaveData(scopeId, batchMap)
        logger.debug("[$scopeId] Sending batch save data")
        event.respond(batchPacket)
    }
}


fun NexusService<*, *>.prepareBatchSaveData(): Map<String, String> {
    val sessions = sessions
    if (!isRunning || sessions.isEmpty())
        return emptyMap()

    val batchMap = HashMap<String, String>()

    sessions.forEach {
        if (!it.hasDataUpdated()) return@forEach

        val dataStr = it.serializeData()
        batchMap[it.id] = dataStr
    }

    return batchMap
}


