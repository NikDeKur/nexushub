package org.ndk.nexushub.client.service

import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.client.sesion.Session
import org.slf4j.Logger
import java.util.*

/**
 * A NexusHub Client service used to store a group of sessions on a client.
 *
 *
 */
interface NexusService<H : Any, S : Session<H, S>> {

    val hub: NexusHub

    val logger: Logger

    var isRunning: Boolean

    /**
     * The NexusHub scope of the service.
     *
     * Used to identify the collection of data in the database.
     */
    val scope: String

    /**
     * The collection of existing sessions.
     */
    val sessions: Collection<S>

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
     * Gets the holder id from the holder object.
     *
     * Holder id is used to identify the holder in the database.
     *
     * It should be unique for each holder.
     *
     * @param holder The holder to get the id from.
     * @return The holder id.
     */
    fun getId(holder: H): String

    /**
     * Gets the holder name from the holder object.
     *
     * Name is holder to identify the holder in the database.
     *
     * @param holder The holder to get the name from.
     * @return The holder name.
     */
    fun getName(holder: H): String

    /**
     * Creates a new session object for the given holder.
     *
     * Doesn't start the session, load data or register the session.
     *
     * @param holder The holder to create the session for.
     * @return The new session.
     */
    fun createSession(holder: H): S

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
    suspend fun startSession(holder: H): S

    /**
     * Stops the session and starting data save for the given holder.
     *
     * If the holder does not have an active session, nothing will happen.
     *
     * The session will be stopped and the data will be saved asynchronously.
     *
     * @param holder The holder to stop the session for.
     * @return The stopped session or null.
     */
    suspend fun stopSession(holderId: String): S?

    /**
     * Restarts the session for the given holder.
     *
     * If the holder does not have an active session, nothing will happen.
     *
     * @param holder The holder to restart the session for.
     * @return The new session.

     */
    suspend fun restartSession(holder: H): S {
        stopSession(getId(holder))
        return startSession(holder)
    }

    /**
     * Gets the active session for the given holder.
     *
     * If the holder does not have an active session, a new session will be created.
     *
     * @param holder The holder to get the session for.
     * @return The session.
     * @see [getExistingSession]
     * @see [startSession]
     */
    suspend fun getSession(holder: H): S {
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
    fun getExistingSession(holderId: String): S?

    /**
     * Checks if the holder has an active session.
     *
     * @param holder The holder to check.
     * @return True if the holder has an active session, false otherwise.
     */
    fun hasSession(holderId: String): Boolean {
        return getExistingSession(holderId) != null
    }
}