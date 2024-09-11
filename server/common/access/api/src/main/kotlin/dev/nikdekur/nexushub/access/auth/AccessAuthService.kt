/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.access.auth

typealias Headers = Map<String, List<String>>

interface AccessAuthService {

    /**
     * Logs in the user based on the headers.
     *
     * @param headers The headers of the request.
     * @return The result of the login.
     */
    suspend fun login(login: String, password: String): LoginResult

    sealed interface LoginResult {
        object AccountNotFound : LoginResult
        object WrongCredentials : LoginResult
        class Success(val data: Map<String, Any>) : LoginResult
    }

    /**
     * Logs out the user based on the headers.
     *
     * @param headers The headers of the request.
     * @return The result of the logout.
     */
    suspend fun logout(headers: Headers): LogoutResult

    sealed interface LogoutResult {
        object Success : LogoutResult
        object NotAuthenticated : LogoutResult
    }

    /**
     * Returns the authentication state of the user based on the headers.
     *
     * @param headers The headers of the request.
     * @return The authentication state.
     * @see AuthState
     */
    fun getAuthState(headers: Headers): AuthState


    /**
     * Represents the state of the authentication.
     *
     * State is returned by [getAuthState] method.
     */
    enum class AuthState {
        /**
         * The user is authenticated.
         */
        AUTHENTICATED,

        /**
         * The user is not authenticated.
         */
        UNAUTHENTICATED,

        /**
         * The user was authenticated, but authentication is no longer valid.
         *
         * Ex: session expired.
         */
        BAD_CREDENTIALS
    }
}