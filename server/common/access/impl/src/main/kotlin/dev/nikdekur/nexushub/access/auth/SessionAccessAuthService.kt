/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.access.auth

import dev.nikdekur.ndkore.service.inject
import java.util.UUID
import kotlin.time.Duration.Companion.minutes


class SessionAccessAuthService(
    override val app: dev.nikdekur.nexushub.NexusHubServer
) : dev.nikdekur.nexushub.service.NexusHubService(), AccessAuthService {

    val accountsService: dev.nikdekur.nexushub.account.AccountsService by inject()

    var rootToken: RootToken? = null

    override suspend fun login(login: String, password: String): AccessAuthService.LoginResult {
        if (login != "root") {
            return AccessAuthService.LoginResult.AccountNotFound
        }

        val root = accountsService.getAccount("root")
        if (root == null) {
            return AccessAuthService.LoginResult.AccountNotFound
        }

        val match = root.password.isEqual(password)
        if (!match) {
            return AccessAuthService.LoginResult.WrongCredentials
        }


        val id = UUID.randomUUID()
        val expires = 10.minutes

        return AccessAuthService.LoginResult.Success(
            RootToken(
                token = id.toString(),
                validBy = expires.inWholeMilliseconds
            ).toMap()
        )
    }

    override suspend fun logout(headers: Headers): AccessAuthService.LogoutResult {
        val auth = getAuthState(headers)
        if (auth != AccessAuthService.AuthState.AUTHENTICATED) {
//            call.respondText(
//                text = "Not authenticated",
//                status = HttpStatusCode.BadRequest
//            )
            return AccessAuthService.LogoutResult.NotAuthenticated
        }

        rootToken = null

//        call.respondText(
//            text = "Logged out",
//            status = HttpStatusCode.OK
//        )
        return AccessAuthService.LogoutResult.Success
    }

    override fun getAuthState(headers: Headers): AccessAuthService.AuthState {
        val authToken = headers["Authorization"]?.first()
        if (authToken == null)
            return AccessAuthService.AuthState.UNAUTHENTICATED

        val realToken = rootToken

        if (realToken == null || authToken != realToken.token) {
            if (realToken?.isValid() == false)
                rootToken = null


            return AccessAuthService.AuthState.BAD_CREDENTIALS
        }

        return AccessAuthService.AuthState.AUTHENTICATED
    }

}