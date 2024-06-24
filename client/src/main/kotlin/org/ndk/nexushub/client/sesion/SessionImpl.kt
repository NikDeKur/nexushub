package org.ndk.nexushub.client.sesion

import dev.nikdekur.ndkore.ext.*
import org.ndk.nexushub.client.service.NexusService
import org.ndk.nexushub.client.sesion.Session.State
import org.ndk.nexushub.data.LeaderboardEntry
import org.ndk.nexushub.packet.*
import org.ndk.nexushub.packet.`in`.PacketLoadData
import org.ndk.nexushub.packet.`in`.PacketRequestTopPosition
import org.ndk.nexushub.packet.`in`.PacketSaveData
import org.ndk.nexushub.packet.out.PacketTopPosition
import org.ndk.nexushub.packet.out.PacketUserData

open class SessionImpl<H, S>(
    override val service: NexusService<H, S>,
    override val holder: H,
) : Session<H, S> {

    override val id = service.getId(holder)

    val logger by service::logger

    private var _data: S? = null

    override val data: S
        get() {
            check(state.isActive) { "Session is not active." }
            val data = _data
            check(data != null) { "Data is not loaded." }
            return data
        }

    override var state: State = State.INACTIVE

    var updatedAt = -1L
    var savedAt = -1L

    override fun hasToBeSaved(): Boolean {
        if (state != State.ACTIVE) {
            return false
        }

        logger.info {
            "Checking if session has to be saved. Updated at: $updatedAt, Saved at: $savedAt, Data: $data"
        }
        val has = updatedAt > savedAt
        logger.info { "Session has to be saved: $has" }
        if (has) {
            savedAt = System.currentTimeMillis()
        }
        return has
    }

    override fun markUpdated() {
        updatedAt = System.currentTimeMillis()
    }


    override suspend fun loadData() {
        check(state != State.LOADING) { "Session is already loading." }
        state = State.LOADING

        try {

            val loadPacket = PacketLoadData(service.scope, id)
            @Suppress("RemoveExplicitTypeArguments")
            val dataStr = service.hub.connection.sendPacket<String?>(loadPacket) {
                throwOnTimeout(5000)

                receive<PacketUserData> {
                    packet.data
                }

                receive {
                    logger.warn("Error while loading data: $packet")
                    null
                }
            }.await()

            if (dataStr == null) {
                return
            }


            _data = service.serializer.deserialize(this, dataStr)

            state = State.ACTIVE


            // Calculate the time taken for loading

        } finally {
            if (state != State.ACTIVE) {
                state = State.INACTIVE
            }
        }
    }

    override suspend fun saveData() {
        val dataStr = serializeData()

        val packet = PacketSaveData(service.scope, id, dataStr)

        service.hub.connection.sendPacket<Unit>(packet) {
            receive<PacketOk> {}

            timeout(5000) {
                logger.warn("Timeout while saving data.")
            }

            receive {
                logger.warn("Error while saving data: $packet")
            }
        }.await()
    }



    override suspend fun getTopPosition(
        field: String
    ): LeaderboardEntry? {
        val packet = PacketRequestTopPosition(service.scope, id, field)

        @Suppress("RemoveExplicitTypeArguments")
        return service.hub.connection.sendPacket<LeaderboardEntry?>(packet) {
            throwOnTimeout(5000)

            receive<PacketTopPosition> {
                this.packet.entry
            }

            receive {
                logger.error(
                    "Unexpected behaviour while loading top position in scope '${service.scope}', " +
                            "for field '$field': $packet")
                null
            }
        }.await()
    }
}