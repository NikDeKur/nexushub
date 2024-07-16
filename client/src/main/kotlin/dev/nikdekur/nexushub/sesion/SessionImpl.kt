package dev.nikdekur.nexushub.sesion

import dev.nikdekur.nexushub.packet.*
import dev.nikdekur.nexushub.packet.`in`.PacketLoadData
import dev.nikdekur.nexushub.packet.`in`.PacketSaveData
import dev.nikdekur.nexushub.packet.out.PacketUserData
import dev.nikdekur.nexushub.service.NexusService
import dev.nikdekur.nexushub.sesion.Session.State

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

    override var state = State.INACTIVE

    /**
     * Represent the latest data that were saved to the server.
     */
    var latestSaveScreen: S? = null

    override fun hasDataUpdated(): Boolean {
        if (state != State.ACTIVE)
            return false

        val data = _data
        if (data == null) return false

        return latestSaveScreen != data && !service.serializer.isDefault(this, data)
    }

    override suspend fun loadData() {
        check(state != State.LOADING) { "Session is already loading." }
        state = State.LOADING

        try {

            val loadPacket = PacketLoadData(service.scope, id)
            @Suppress("RemoveExplicitTypeArguments")
            val dataStr = service.hub.gateway.sendPacket<String?>(loadPacket) {
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


            val serializer = service.serializer
            _data = serializer.deserialize(this, dataStr)
            latestSaveScreen = serializer.deserialize(this, dataStr)

            state = State.ACTIVE

        } finally {
            if (state != State.ACTIVE) {
                state = State.INACTIVE
            }
        }
    }

    override suspend fun saveData() {
        if (!hasDataUpdated()) return

        val serializer = service.serializer
        val dataStr = serializer.serialize(this, data)

        val packet = PacketSaveData(service.scope, id, dataStr)

        val screen = serializer.deserialize(this, dataStr)

        service.hub.gateway.sendPacket<Unit>(packet) {
            receive<PacketOk> {
                latestSaveScreen = screen
            }

            timeout(5000) {
                logger.warn("Timeout while saving data.")
            }

            receive {
                logger.warn("Error while saving data: $packet")
            }
        }.await()
    }
}