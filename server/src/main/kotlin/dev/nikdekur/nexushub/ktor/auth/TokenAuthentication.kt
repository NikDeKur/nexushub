/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.ktor.auth

import dev.nikdekur.ndkore.annotation.DelicateAPI
import dev.nikdekur.ndkore.service.getService
import dev.nikdekur.nexushub.KtorNexusHubServer
import dev.nikdekur.nexushub.access.auth.AccessAuthService
import dev.nikdekur.nexushub.ktor.HeadersMap
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.plugins.origin
import io.ktor.server.response.respondText

@OptIn(DelicateAPI::class)
val TokenAuthenticationPlugin = createRouteScopedPlugin("TokenAuthentication") {
    onCall {
        it.request.origin.apply {

            val authService = KtorNexusHubServer.instance.servicesManager.getService<AccessAuthService>()
            val headers = it.request.headers
            val mapHeaders = HeadersMap(headers)
            val auth = authService.getAuthState(mapHeaders)
            when (auth) {
                AccessAuthService.AuthState.AUTHENTICATED -> {
                    // Continue
                }

                AccessAuthService.AuthState.UNAUTHENTICATED -> {
                    it.respondText(
                        text = "Use '/login' to authenticate before using this endpoint.",
                        status = HttpStatusCode.Unauthorized
                    )
                }

                AccessAuthService.AuthState.BAD_CREDENTIALS -> {
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

    inline fun toMap(): Map<String, Any> {
        return mapOf(
            "token" to token,
            "valid_by" to validBy
        )
    }
}