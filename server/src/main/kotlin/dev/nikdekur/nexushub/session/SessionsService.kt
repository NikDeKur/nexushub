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
import dev.nikdekur.nexushub.service.NexusHubService

interface SessionsService : NexusHubService {

    fun getExistingSession(scopeId: String, holderId: String): Session?
    fun hasAnySessions(node: Node): Boolean

    fun startSession(node: Node, scope: Scope, holderId: String)
    fun stopSession(scopeId: String, holderId: String)
    fun stopAllSessions(node: Node)

    suspend fun requestSync(scope: Scope)
}