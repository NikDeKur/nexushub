/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.sesion

import dev.nikdekur.nexushub.packet.*
import dev.nikdekur.nexushub.scope.ScopeData
import dev.nikdekur.nexushub.service.NexusService
import dev.nikdekur.nexushub.sesion.Session.State
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

open class RuntimeSession<H, S : ScopeData<S>>(
    override val service: NexusService<H, S>,
    override val holder: H,
) : Session<H, S> {

    override val id = service.getId(holder)

    val logger = LoggerFactory.getLogger(javaClass)

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

    override suspend fun hasDataUpdated(): Boolean {
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
                throwOnTimeout(5.seconds)

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
            serializer.deserialize(this, dataStr).let {
                _data = it
                latestSaveScreen = it.clone()
            }

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

            timeout(5.seconds) {
                logger.warn("Timeout while saving data.")
            }

            receive {
                logger.warn("Error while saving data: $packet")
            }
        }.await()
    }
}