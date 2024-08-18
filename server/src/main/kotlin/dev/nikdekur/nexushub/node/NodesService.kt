/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.node

import dev.nikdekur.nexushub.auth.account.Account
import dev.nikdekur.nexushub.talker.ClientTalker
import dev.nikdekur.nexushub.util.CloseCode

interface NodesService {

    val nodes: Collection<Node>

    fun newNode(talker: ClientTalker, account: Account, id: String): Node
    fun getNode(talker: ClientTalker): Node?
    fun getNode(id: String): Node?
    fun removeNode(node: ClientTalker): Node?

    /**
     * Close all nodes with the specified code and reason.
     *
     * Suspends until all nodes are closed.
     *
     * @param code The close code.
     * @param reason The reason for closing.
     */
    suspend fun closeAllNodes(code: CloseCode, reason: String)
}

inline fun NodesService.isNodeExists(talker: ClientTalker) = getNode(talker) != null
inline fun NodesService.isNodeExists(id: String) = getNode(id) != null


