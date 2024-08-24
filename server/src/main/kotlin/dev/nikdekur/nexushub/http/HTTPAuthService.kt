/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.http

import dev.nikdekur.nexushub.modal.`in`.AuthRequest
import dev.nikdekur.nexushub.service.NexusHubService
import io.ktor.server.application.ApplicationCall

interface HTTPAuthService : NexusHubService {

    /**
     * Login to the server with the given [request].
     *
     * @param request The [AuthRequest] to log in with.
     * @return Whether the login was successful.
     */
    suspend fun login(call: ApplicationCall, request: AuthRequest): Boolean

    /**
     * Logout from the server.
     *
     * @return Whether the logout was successful.
     */
    suspend fun logout(call: ApplicationCall): Boolean

    /**
     * Ensure that the given [request] is authenticated.
     *
     * @param request The [AuthRequest] to ensure is authenticated.
     * @return Whether the request is authenticated.
     */
    suspend fun ensureAuthenticated(call: ApplicationCall): EnsureAuthResponse


    enum class EnsureAuthResponse {
        AUTHENTICATED,
        UNAUTHENTICATED,
        BAD_CREDENTIALS
    }
}