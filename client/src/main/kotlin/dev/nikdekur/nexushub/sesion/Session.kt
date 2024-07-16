package dev.nikdekur.nexushub.sesion

import dev.nikdekur.nexushub.data.LeaderboardEntry
import dev.nikdekur.nexushub.service.NexusService

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
        service.stopSession(this)
    }



    suspend fun getLeaderboardPosition(field: String): LeaderboardEntry? {
        return service.getLeaderboardPosition(field, id)
    }


    fun hasDataUpdated(): Boolean


    enum class State {
        LOADING, ACTIVE, STOPPING, INACTIVE

        ;
        inline val isActive : Boolean
            get() = this == ACTIVE

        inline val isInactive : Boolean
            get() = this == STOPPING || this == INACTIVE
    }
}