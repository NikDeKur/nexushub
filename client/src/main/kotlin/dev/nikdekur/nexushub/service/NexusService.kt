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
import dev.nikdekur.nexushub.serialization.DataSerializer
import dev.nikdekur.nexushub.sesion.Session
import org.slf4j.Logger
import java.util.*

/**
 * A NexusHub Client service used to store a group of sessions on a client.
 *
 *
 */
interface NexusService<H, S> {

    val hub: NexusHub

    val logger: Logger

    var isRunning: Boolean

    val serializer: DataSerializer<H, S>

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


    fun removeSession(session: Session<H, S>)

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
     * @param field The field to sort by
     * @param limit The limit of entries to return
     */
    suspend fun getLeaderboard(field: String, startFrom: Int, limit: Int): Leaderboard


    suspend fun getLeaderboardPosition(field: String, holderId: String): LeaderboardEntry?


    suspend fun getLeaderboardAndPosition(field: String, startFrom: Int, limit: Int, holderId: String): Pair<Leaderboard, LeaderboardEntry?>
}
