package org.ndk.nexushub.client.sesion

import org.ndk.nexushub.client.service.NexusService
import org.ndk.nexushub.data.LeaderboardEntry

interface Session<H, S> {

    val service: NexusService<H, S>

    val holder: H
    val id: String

    var state: State
    val data: S

    fun serializeData(): String {
        return service.serializer.serialize(this, data)
    }


    suspend fun loadData()

    suspend fun saveData()

    suspend fun stop() {
        service.stopSession(id)
    }



    suspend fun getTopPosition(field: String): LeaderboardEntry?


    fun hasToBeSaved(): Boolean
    fun markUpdated()


    enum class State {
        LOADING, ACTIVE, STOPPING, INACTIVE

        ;
        inline val isActive : Boolean
            get() = this == ACTIVE

        inline val isInactive : Boolean
            get() = this == STOPPING || this == INACTIVE
    }
}