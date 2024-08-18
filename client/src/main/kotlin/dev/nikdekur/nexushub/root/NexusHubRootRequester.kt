/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.root

import dev.nikdekur.nexushub.modal.Account
import dev.nikdekur.nexushub.modal.`in`.AccountCreateRequest
import dev.nikdekur.nexushub.modal.`in`.AccountDeleteRequest
import dev.nikdekur.nexushub.modal.`in`.AccountPasswordRequest
import dev.nikdekur.nexushub.modal.`in`.AccountScopesListRequest
import dev.nikdekur.nexushub.modal.`in`.AccountScopesUpdateRequest
import dev.nikdekur.nexushub.modal.`in`.AuthRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMessage
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json


data class Response<R>(val original: HttpResponse, val body: R) : HttpMessage by original

data class Session(val token: String, val endsBy: Long) {

    inline val isExpired: Boolean
        get() = endsBy < System.currentTimeMillis()
}

class NexusHubRootRequester(
    val path: String,
    val rootPassword: String
) {

    var session: Session? = null


    val client = HttpClient(OkHttp) {
        engine {
            config {
                followRedirects(true)
            }
        }

        install(ContentNegotiation) {
            json()
        }
    }


    fun detach() {
        client.close()
    }


    suspend inline fun <reified T, reified R> request(path: Path<T, R>, body: T): Response<R> {
        val doAuth = path.requireAuth

        val request = client.post("${this.path}/${path.url}") {
            if (doAuth) {
                session.let {
                    if (it == null || it.isExpired)
                        login()
                }

                val token = session!!.token
                headers["Authorization"] = token
            }

            contentType(ContentType.Application.Json)
            setBody(body)
        }

        val status = request.status

        return when {
            status.isSuccess() -> Response(request, request.body())
            status == HttpStatusCode.Unauthorized -> throw Exception("Authorization failed. Check your root password.")
            else -> throw Exception("Request failed with status code ${status.value}")
        }
    }

    suspend fun login() {
        val request = AuthRequest("root", rootPassword)
        val response = request(RootPaths.LOGIN, request)
        val body = response.body
        session = Session(body.token, body.endsBy)
    }


    suspend fun getAccountsList(): List<Account> {
        return request(RootPaths.LIST_ACCOUNTS, Unit).body.accounts
    }

    suspend fun createAccount(login: String, password: String, scopes: Set<String>) {
        val request = AccountCreateRequest(login, password, scopes)
        request(RootPaths.CREATE_ACCOUNT, request)
    }

    suspend fun deleteAccount(login: String) {
        val request = AccountDeleteRequest(login)
        request(RootPaths.DELETE_ACCOUNT, request)
    }

    suspend fun changeAccountPassword(login: String, password: String) {
        val request = AccountPasswordRequest(login, password)
        request(RootPaths.CHANGE_ACCOUNT_PASSWORD, request)
    }


    suspend fun getAccountScopes(login: String): Set<String> {
        val request = AccountScopesListRequest(login)
        return request(RootPaths.LIST_ACCOUNT_SCOPES, request).body.scopes
    }

    suspend fun updateAccountScopes(login: String, action: AccountScopesUpdateRequest.Action, scopes: Set<String>) {
        val request = AccountScopesUpdateRequest(login, action, scopes)
        request(RootPaths.UPDATE_ACCOUNT_SCOPES, request)
    }
}