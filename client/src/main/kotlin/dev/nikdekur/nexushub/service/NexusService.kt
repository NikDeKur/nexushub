/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.service

import dev.nikdekur.nexushub.NexusHub
import dev.nikdekur.nexushub.data.Leaderboard
import dev.nikdekur.nexushub.data.LeaderboardEntry
import dev.nikdekur.nexushub.event.NetworkEvent
import dev.nikdekur.nexushub.scope.ScopeData
import dev.nikdekur.nexushub.serialization.DataSerializer
import dev.nikdekur.nexushub.sesion.Session
import java.util.*

/**
 * A NexusHub Client service used to store a group of sessions on a client.
 *
 * The service is used to manage the sessions and the data of the holders.
 * The service is responsible for starting and stopping the sessions,
 * saving the data and managing the sessions' lifecycle.
 *
 * @param H The holder type.
 * @param S The session data type.
 * @see [Session]
 */
interface NexusService<H, S : ScopeData<S>> {

    /**
     * The NexusHub instance that is responsible for the service.
     */
    val hub: NexusHub


    /**
     * The serializer used to serialize and deserialize the data of the sessions.
     */
    val serializer: DataSerializer<H, S>


    /**
     * State of the service.
     */
    var isActive: Boolean


    /**
     * Gets the id of the holder.
     *
     * Id is used to identifying the holder in the database and should be unique for each holder.
     *
     * @param holder The holder to get the id for.
     * @return The id of the holder.
     */
    fun getId(holder: H): String


    /**
     * The NexusHub scope of the service.
     *
     * Used to identify the collection of data in the database.
     */
    val scope: String


    /**
     * The collection of existing sessions.
     */
    val sessions: Collection<Session<H, S>>


    /**
     * Starts the service
     */
    fun start()


    /**
     * Stops the service and all the sessions.
     *
     * All sessions will be stopped with [stopSession].
     */
    suspend fun stop()


    /**
     * Creates a new session object for the given holder.
     *
     * Doesn't start the session, load data or register the session.
     *
     * @param holder The holder to create the session for.
     * @return The new session.
     */
    fun createSession(holder: H): Session<H, S>


    /**
     * Starts a session for the given holder.
     *
     * If the holder already has an active session, it will be stopped and a new session would be created.
     *
     * If the session limit has been reached, the oldest session will be stopped.
     *
     * The session will be created and data loaded asynchronously.
     *
     * @param holder The holder to start a session for.
     * @return The new session.
     */
    suspend fun startSession(holder: H): Session<H, S>


    /**
     * Stops the session and starting data save for the given holder.
     *
     * If the holder does not have an active session, nothing will happen.
     *
     * The session will be stopped and the data will be saved asynchronously.
     *
     * @param session The session to stop.
     */
    suspend fun stopSession(session: Session<H, S>)


    /**
     * Saves all the sessions' data.
     *
     * Does not stop the sessions and doesn't interrupt any processes.
     */
    suspend fun saveAllSessions()


    /**
     * Restarts the session for the given holder.
     *
     * If the holder does not have an active session, nothing will happen.
     *
     * @param holderId The holder to restart the session for.
     * @return The new session.

     */
    suspend fun restartSession(holder: H): Session<H, S> {
        val session = getExistingSession(getId(holder))
        session?.let { stopSession(it) }
        return startSession(holder)
    }


    /**
     * Gets the active session for the given holder.
     *
     * If the holder does not have an active session, a new session will be created.
     *
     * @param holderId The holder to get the session for.
     * @return The session.
     * @see [getExistingSession]
     * @see [startSession]
     */
    suspend fun getSession(holder: H): Session<H, S> {
        return getExistingSession(getId(holder)) ?: startSession(holder)
    }


    /**
     * Gets the existing session for the given holder.
     *
     * If the holder does not have an active session, null will be returned.
     *
     * @param holderId The holder id to get the session for.
     * @return The session or null.
     */
    fun getExistingSession(holderId: String): Session<H, S>?


    /**
     * Checks if the holder has an active session.
     *
     * @param holder The holder to check.
     * @return True if the holder has an active session, false otherwise.
     */
    fun hasSession(holderId: String): Boolean {
        return getExistingSession(holderId) != null
    }


    /**
     * Request a leaderboard from the server with the specified parameters
     *
     * @param field The user data field to get the leaderboard for
     * @param startFrom The start index of the leaderboard, how many entries to skip
     * @param limit The limit of the leaderboard, how many entries to get
     * @return The leaderboard
     * @see [getLeaderboardAndPosition]
     * @see [Leaderboard]
     */
    suspend fun getLeaderboard(field: String, startFrom: Int, limit: Int): Leaderboard


    /**
     * Request a leaderboard position of a holder from the server with the specified parameters
     *
     * @param field The user data field to get the leaderboard position for
     * @param holderId The holder id to get the leaderboard position for
     * @return The leaderboard entry or null if the holder has no data at the field
     * @see [getLeaderboardAndPosition]
     * @see [LeaderboardEntry]
     */
    suspend fun getLeaderboardPosition(field: String, holderId: String): LeaderboardEntry?


    /**
     * Request a leaderboard and position of a holder from the server with the specified parameters
     *
     * Should be preferred over [getLeaderboard] and [getLeaderboardPosition]
     *
     * @param field The user data field to get the leaderboard and position for
     * @param startFrom The start index of the leaderboard, how many entries to skip
     * @param limit The limit of the leaderboard, how many entries to get
     * @param holderId The holder id to get the leaderboard position for
     * @return The leaderboard and the leaderboard entry or null if the holder has no data at the field
     * @see [getLeaderboard]
     * @see [getLeaderboardPosition]
     * @see [Leaderboard]
     * @see [LeaderboardEntry]
     */
    suspend fun getLeaderboardAndPosition(field: String, startFrom: Int, limit: Int, holderId: String): Pair<Leaderboard, LeaderboardEntry?>


    /**
     * Event Handler for the service.
     *
     * Handle network events related to the service.
     */
    suspend fun onEvent(event: NetworkEvent.ScopeEvent)
}
