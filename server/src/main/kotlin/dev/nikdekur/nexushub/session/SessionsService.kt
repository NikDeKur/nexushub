/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.session

import dev.nikdekur.nexushub.node.Node
import dev.nikdekur.nexushub.scope.Scope

interface SessionsService {

    /**
     * Get the session for the given scope and holder.
     *
     * Sessions are used for server to know what node is currently having relevant data for a holder.
     *
     * @param scopeId The scope to get the session for.
     * @param holderId The holder to get the session for.
     * @return The session for the given scope and holder or null if there is no session.
     * @see Session
     */
    fun getExistingSession(scopeId: String, holderId: String): Session?

    /**
     * Get all nodes that have sessions in the given scope.
     *
     * @param scope The scope to get nodes for.
     * @return The nodes that have sessions in the given scope.
     * @see Node
     */
    fun getNodes(scope: Scope): Iterable<Node>

    /**
     * Check if there are any sessions for the given node.
     *
     * Sessions are used for server to know what node is currently having relevant data for a holder.
     *
     * @param node The node to check for sessions.
     * @return True if there are any sessions for the given node, false otherwise.
     * @see Node
     */
    fun hasAnySessions(node: Node): Boolean

    /**
     * Start a session for the given node in the given scope.
     *
     * Sessions are used for server to know what node is currently having relevant data for a holder.
     *
     * @param node The node to start the session for.
     * @param scope The scope to start the session in.
     * @param holderId The holder to start the session for.
     * @see Node
     * @see Scope
     */
    fun startSession(node: Node, scope: Scope, holderId: String)

    /**
     * Stop the session for the given scope and holder.
     *
     * Stopping a session will unregister it, but no data will change.
     *
     * @param scopeId The scope to stop the session for.
     * @param holderId The holder to stop the session for.
     */
    fun stopSession(scopeId: String, holderId: String)

    /**
     * Stop all sessions for the given node.
     *
     * Stopping a session will unregister it, but no data will change.
     *
     * @param node The node to stop all sessions for.
     * @see Node
     */
    fun stopAllSessions(node: Node)
}