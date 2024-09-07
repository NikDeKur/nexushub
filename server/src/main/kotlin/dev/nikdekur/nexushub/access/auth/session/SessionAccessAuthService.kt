/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.access.auth.session

import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.access.auth.AccessAuthService
import dev.nikdekur.nexushub.access.auth.Headers
import dev.nikdekur.nexushub.account.AccountsService
import dev.nikdekur.nexushub.ktor.auth.RootToken
import dev.nikdekur.nexushub.service.NexusHubService
import java.util.UUID

class SessionAccessAuthService(
    override val app: NexusHubServer
) : NexusHubService(), AccessAuthService {

    val accountsService: AccountsService by inject()

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


        val token = UUID.randomUUID().toString()
        val validBy = 10 * 60 * 1000 + System.currentTimeMillis() // 10 minutes

        return AccessAuthService.LoginResult.Success(
            RootToken(token, validBy)
                .also { rootToken = it }
                .toMap()
        )
    }

    override suspend fun logout(headers: Headers): AccessAuthService.LogoutResult {
        val auth = getAuthState(headers)
        if (auth == AccessAuthService.AuthState.AUTHENTICATED) {
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