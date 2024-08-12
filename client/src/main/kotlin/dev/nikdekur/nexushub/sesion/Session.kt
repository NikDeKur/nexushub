/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.sesion

import dev.nikdekur.nexushub.data.LeaderboardEntry
import dev.nikdekur.nexushub.scope.ScopeData
import dev.nikdekur.nexushub.service.NexusService

interface Session<H, S : ScopeData<S>> {

    /**
     * The NexusService instance that is responsible for the session.
     */
    val service: NexusService<H, S>

    /**
     * The holder of the session.
     */
    val holder: H

    /**
     * The unique identifier of the session.
     */
    val id: String

    /**
     * The state of the session.
     */
    var state: State

    /**
     * The data of the session.
     */
    val data: S


    /**
     * Serializes the data of the session to a string.
     *
     * Uses the serializer of the service [NexusService.serializer] to serialize the data.
     *
     * @return The serialized data as a string.
     */
    suspend fun serializeData(): String {
        return service.serializer.serialize(this, data)
    }


    /**
     * Loads the data of the session.
     *
     * Usually, the method is called automatically when the session is started.
     *
     * If the session is already loading, the method will throw an exception.
     */
    suspend fun loadData()

    suspend fun saveData()

    /**
     * Stops the session and starting data save.
     *
     * If the session is not active, the method will do nothing.
     *
     * The session will be stopped and the data will be saved asynchronously.
     *
     * Alias for [NexusService.stopSession]
     */
    suspend fun stop() {
        service.stopSession(this)
    }


    /**
     * Request a leaderboard position of a holder from the server with the specified parameters
     *
     * Alias for [NexusService.getLeaderboardPosition]
     *
     * @param field The user data field to get the leaderboard position for
     * @return The leaderboard entry or null if the holder has no data at the field
     * @see [LeaderboardEntry]
     */
    suspend fun getLeaderboardPosition(field: String): LeaderboardEntry? {
        return service.getLeaderboardPosition(field, id)
    }


    /**
     * Checks if the data of the session has been updated.
     *
     * This method is used to determine should the data be saved to the database.
     *
     * @return `true` if the data has been updated, `false` otherwise.
     */
    suspend fun hasDataUpdated(): Boolean


    /**
     * Represents the possible states of a session.
     *
     * Each state indicates a specific phase in the session's lifecycle.
     */
    enum class State {
        /**
         * The session is currently loading its data is not yet available.
         */
        LOADING,

        /**
         * The session is active and operational.
         */
        ACTIVE,

        /**
         * The session is in the process of stopping.
         */
        STOPPING,

        /**
         * The session is inactive and not operational.
         *
         * The Session might not be started or has been stopped.
         */
        INACTIVE;

        /**
         * Checks if the session is currently active.
         *
         * @return `true` if the session is in the [ACTIVE] state, `false` otherwise.
         */
        inline val isActive : Boolean
            get() = this == ACTIVE

        /**
         * Checks if the session is currently inactive.
         *
         * @return `true` if the session is in the [STOPPING] or [INACTIVE] state, `false` otherwise.
         */
        inline val isInactive : Boolean
            get() = this == STOPPING || this == INACTIVE
    }
}