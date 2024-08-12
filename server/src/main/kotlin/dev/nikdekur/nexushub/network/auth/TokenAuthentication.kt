/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.auth

import dev.nikdekur.nexushub.http.HTTPAuthService
import dev.nikdekur.nexushub.koin.getKoin
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.plugins.origin
import io.ktor.server.response.respondText

val TokenAuthenticationPlugin = createRouteScopedPlugin("TokenAuthentication") {
    onCall {
        it.request.origin.apply {

            val authService = getKoin().get<HTTPAuthService>()
            val auth = authService.ensureAuthenticated(it)
            when (auth) {
                HTTPAuthService.EnsureAuthResponse.AUTHENTICATED -> {
                    // Continue
                }

                HTTPAuthService.EnsureAuthResponse.UNAUTHENTICATED -> {
                    it.respondText(
                        text = "Use '/login' to authenticate before using this endpoint.",
                        status = HttpStatusCode.Unauthorized
                    )
                }

                HTTPAuthService.EnsureAuthResponse.BAD_CREDENTIALS -> {
                    it.respondText(
                        text = "Bad Token",
                        status = HttpStatusCode.Unauthorized
                    )
                }
            }
        }
    }
}




data class RootToken(
    val token: String,
    val validBy: Long
) {

    fun isValid(): Boolean {
        return System.currentTimeMillis() < validBy
    }
}