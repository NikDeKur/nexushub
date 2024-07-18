/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.session

import dev.nikdekur.nexushub.node.ClientNode
import dev.nikdekur.nexushub.scope.Scope

interface SessionsService {

    fun getExistingSession(scopeId: String, holderId: String): Session?
    fun hasAnySessions(node: ClientNode): Boolean

    fun startSession(node: ClientNode, scope: Scope, holderId: String)
    fun stopSession(scopeId: String, holderId: String)
    fun stopAllSessions(node: ClientNode)

    suspend fun requestSync(scope: Scope)
}