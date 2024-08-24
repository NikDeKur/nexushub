/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network

import dev.nikdekur.ndkore.ext.warn
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.access.AccessService
import dev.nikdekur.nexushub.access.AccessService.ReceiveResult
import dev.nikdekur.nexushub.account.AccountsService
import dev.nikdekur.nexushub.http.HTTPAuthService
import dev.nikdekur.nexushub.modal.Account
import dev.nikdekur.nexushub.modal.`in`.AccountCreateRequest
import dev.nikdekur.nexushub.modal.`in`.AccountDeleteRequest
import dev.nikdekur.nexushub.modal.`in`.AccountPasswordRequest
import dev.nikdekur.nexushub.modal.`in`.AccountScopesListRequest
import dev.nikdekur.nexushub.modal.`in`.AccountScopesUpdateRequest
import dev.nikdekur.nexushub.modal.`in`.AuthRequest
import dev.nikdekur.nexushub.modal.out.AccountScopesListResponse
import dev.nikdekur.nexushub.modal.out.AccountsListResponse
import dev.nikdekur.nexushub.network.auth.authenticate
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.talker.ClientTalker
import dev.nikdekur.nexushub.talker.KtorClientTalker
import dev.nikdekur.nexushub.util.CloseCode
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.html.respondHtml
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.*
import io.ktor.websocket.readBytes
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.html.body
import kotlinx.html.p
import org.slf4j.LoggerFactory
import java.time.Duration

class Routing(
    override val app: NexusHubServer,
    val application: Application
) : NexusHubService {

    val logger = LoggerFactory.getLogger(javaClass)

    val httpAuthService: HTTPAuthService by inject()
    val accountsService: AccountsService by inject()

    val accessService: AccessService by inject()

    override fun onEnable() {
        logger.info("Initializing routing")

        application.install(WebSockets) {
            pingPeriod = Duration.ofSeconds(10)
            timeout = Duration.ofSeconds(10)
        }

        application.install(ContentNegotiation) {
            json()
        }


        application.routing {
            get("/") {
                call.respondHtml(HttpStatusCode.OK) {
                    body {
                        p {
                            +"NexusHub server is running"
                        }
                    }
                }
            }


            route("connection") {

                webSocket {
                    wsConnection()
                }
            }

            post("login") {
                val request = call.receive<AuthRequest>()
                httpAuthService.login(call, request)
            }

            authenticate {

                post("logout") {
                    logger.info("[${call.address}] Logging out requested")
                    httpAuthService.logout(call)
                }

                route("accounts") {

                    post("list") {
                        logger.info("[${call.address}] Accounts list requested")
                        val accounts = accountsService.getAccounts()

                        val response = AccountsListResponse(
                            accounts.map {
                                Account(it.login, it.allowedScopes)
                            }
                        )
                        logger.info("[${call.address}] Responding with accounts list: $response")
                        call.respond(response)
                    }

                    post("create") {
                        logger.info("[${call.address}] Account creation requested")
                        val request = call.receive<AccountCreateRequest>()
                        val login = request.login
                        val account = accountsService.getAccount(login)
                        if (account != null) {
                            logger.info("[${call.address}] Account '$login' already exists")
                            call.respondText(
                                text = "Account '$login' already exists",
                                status = HttpStatusCode.Conflict
                            )
                            return@post
                        }

                        val password = request.password
                        val scopes = request.scopes
                        accountsService.createAccount(login, password, scopes)
                        logger.info("[${call.address}] Account '$login' created with scopes: $scopes")
                        call.respondText("Success")
                    }

                    post("delete") {
                        logger.info("[${call.address}] Account deletion requested")
                        val request = call.receive<AccountDeleteRequest>()
                        val login = request.login
                        accountsService.deleteAccount(login)
                        logger.info("[${call.address}] Account '$login' deleted")
                        call.respondText("Success")
                    }

                    post("password") {
                        logger.info("[${call.address}] Password change requested")
                        val request = call.receive<AccountPasswordRequest>()
                        val login = request.login
                        val account = accountsService.getAccount(login) ?: run {
                            call.respondText("Account '$login' not found")
                            return@post
                        }
                        val newPassword = request.newPassword
                        account.changePassword(newPassword)
                        logger.info("[${call.address}] Password for account '$login' changed")
                        call.respondText("Success")
                    }


                    route("scopes") {

                        post("list") {
                            logger.info("[${call.address}] Account scopes list requested")
                            val request = call.receive<AccountScopesListRequest>()
                            val login = request.login
                            val account = accountsService.getAccount(login) ?: run {
                                call.respondText("Account '$login' not found")
                                return@post
                            }
                            val scopes = account.allowedScopes
                            val response = AccountScopesListResponse(scopes)
                            logger.info("[${call.address}] Responding with account '$login' scopes list: $scopes")
                            call.respond(response)
                        }

                        post("update") {
                            logger.info("[${call.address}] Account scopes update requested")
                            val request = call.receive<AccountScopesUpdateRequest>()

                            val login = request.login
                            val account = accountsService.getAccount(login) ?: run {
                                logger.info("[${call.address}] Account '$login' not found")
                                call.respondText("Account '$login' not found")
                                return@post
                            }

                            val action = request.action
                            val scopes = request.scopes
                            when (action) {
                                AccountScopesUpdateRequest.Action.ADD -> {
                                    scopes.forEach {
                                        logger.info("[${call.address}] Adding scope '$it' to account '$login'")
                                        account.allowScope(it)
                                    }
                                }

                                AccountScopesUpdateRequest.Action.REMOVE -> {
                                    scopes.forEach {
                                        logger.info("[${call.address}] Removing scope '$it' from account '$login'")
                                        account.removeScope(it)
                                    }
                                }

                                AccountScopesUpdateRequest.Action.SET -> {
                                    account.clearScopes()
                                    scopes.forEach {
                                        logger.info("[${call.address}] Allowing scope '$it' (by set) to account '$login'")
                                        account.allowScope(it)
                                    }
                                }

                                AccountScopesUpdateRequest.Action.CLEAR -> {
                                    logger.info("[${call.address}] Clearing all scopes from account '$login'")
                                    account.clearScopes()
                                }
                            }
                            call.respondText("Success")
                        }
                    }
                }
            }
        }


        logger.info("Routing initialized")
    }

    @Suppress("NOTHING_TO_INLINE")
    suspend inline fun DefaultWebSocketServerSession.wsConnection() {

        val address = call.address
        val talker: ClientTalker = KtorClientTalker(app, this, address)


        logger.info("[$address] Connection established")

        launch {
            logger.info("[$address] Connection job started")
            try {
                incoming.consumeEach { frame ->

                    if (talker.isBlocked) {
                        logger.info("[$address] Connection is blocked. Closing")
                        this@launch.cancel()
                        return@consumeEach
                    }

                    val result = accessService.receiveData(talker, frame.readBytes())

                    when (result) {
                        ReceiveResult.RateLimited -> {
                            logger.info("[$address] Rate limited. Closing")
                            this@launch.cancel()
                            return@consumeEach
                        }

                        ReceiveResult.InvalidData -> {
                            // Do nothing
                        }

                        ReceiveResult.Ok -> {
                            // Do nothing
                        }
                    }
                }

                logger.info("[$address] Connection job finished. Closing connection")
                talker.close(CloseCode.NORMAL, "Connection closed")

            } catch (e: CancellationException) {
                logger.info("[$address] Connection job cancelled")
                throw e

            } catch (e: Throwable) {
                logger.warn(e) { "[$address] Exception occurred in job!" }
            }
        }

        try {
            accessService.onReady(talker)
        } catch (e: Throwable) {
            logger.warn("[$address] Exception occurred!", e)
        }



        logger.info("[$address] Closing connection")

        if (talker.isOpen)
            talker.close(CloseCode.NORMAL, "Done")
    }
}


inline val ApplicationCall.address: Address
    get() = request.origin.let {
        Address(it.remoteAddress, it.remotePort)
    }