/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.http.session

import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.account.AccountsService
import dev.nikdekur.nexushub.http.HTTPAuthService
import dev.nikdekur.nexushub.http.HTTPAuthService.EnsureAuthResponse
import dev.nikdekur.nexushub.modal.`in`.AuthRequest
import dev.nikdekur.nexushub.modal.out.AuthSuccess
import dev.nikdekur.nexushub.network.auth.RootToken
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import java.util.UUID

class HTTPSessionAuthService(
    override val app: NexusHubServer
) : HTTPAuthService {

    val accountsService: AccountsService by inject()

    var rootToken: RootToken? = null

    override suspend fun login(call: ApplicationCall, request: AuthRequest): Boolean {
        if (request.login != "root") {
            // TODO: When will create more accounts don't forget to make fake argon2 hashing time
            // To prevent users to know if the account exists or not.
            call.respondText(
                text = "Bad Credentials",
                status = HttpStatusCode.BadRequest
            )
            return false
        }

        val root = accountsService.getAccount("root") ?: run {
            call.respondText(
                text = "Root account is not yet setup. Setup root account in console first.",
                status = HttpStatusCode.BadRequest
            )
            return@login false
        }

        val match = accountsService.matchPassword(root.password, request.password)
        if (!match) {
            call.respondText(
                text = "Bad Credentials",
                status = HttpStatusCode.BadRequest
            )
            return false
        }


        val token = UUID.randomUUID().toString()
        val validBy = 10 * 60 * 1000 + System.currentTimeMillis() // 10 minutes
        rootToken = RootToken(token, validBy)
        val response = AuthSuccess(token, validBy)

        call.respond(response)
        return true
    }

    override suspend fun logout(call: ApplicationCall): Boolean {
        rootToken = null

        val auth = ensureAuthenticated(call)
        if (auth != EnsureAuthResponse.AUTHENTICATED) {
            call.respondText(
                text = "Not authenticated",
                status = HttpStatusCode.BadRequest
            )
            return false
        }

        call.respondText(
            text = "Logged out",
            status = HttpStatusCode.OK
        )
        return true
    }

    override suspend fun ensureAuthenticated(call: ApplicationCall): EnsureAuthResponse {
        val authToken = call.request.headers["Authorization"]
        if (authToken == null)
            return EnsureAuthResponse.UNAUTHENTICATED

        val realToken = rootToken

        if (realToken == null || authToken != realToken.token) {
            if (realToken?.isValid() == false)
                rootToken = null


            return EnsureAuthResponse.BAD_CREDENTIALS
        }

        return EnsureAuthResponse.AUTHENTICATED
    }

}