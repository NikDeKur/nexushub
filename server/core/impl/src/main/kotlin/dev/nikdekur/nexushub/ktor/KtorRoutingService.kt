/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.ktor

import dev.nikdekur.ndkore.ext.warn
import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.access.AccessService.ReceiveResult
import dev.nikdekur.nexushub.access.auth.AccessAuthService
import dev.nikdekur.nexushub.access.auth.AccessAuthService.LoginResult
import dev.nikdekur.nexushub.access.auth.AccessAuthService.LogoutResult
import dev.nikdekur.nexushub.access.auth.Headers
import dev.nikdekur.nexushub.access.config.ConfigurationAccessService
import dev.nikdekur.nexushub.access.config.ConfigurationAccessService.AccountCreationResult
import dev.nikdekur.nexushub.access.config.ConfigurationAccessService.AccountDeletionResult
import dev.nikdekur.nexushub.access.config.ConfigurationAccessService.AccountScopesChangeResult
import dev.nikdekur.nexushub.access.config.ConfigurationAccessService.AccountScopesListResult
import dev.nikdekur.nexushub.access.config.ConfigurationAccessService.PasswordChangeResult
import dev.nikdekur.nexushub.ktor.auth.authenticate
import dev.nikdekur.nexushub.modal.Account
import dev.nikdekur.nexushub.modal.`in`.AccountCreateRequest
import dev.nikdekur.nexushub.modal.`in`.AccountDeleteRequest
import dev.nikdekur.nexushub.modal.`in`.AccountPasswordRequest
import dev.nikdekur.nexushub.modal.`in`.AccountScopesListRequest
import dev.nikdekur.nexushub.modal.`in`.AccountScopesUpdateRequest
import dev.nikdekur.nexushub.modal.`in`.AuthRequest
import dev.nikdekur.nexushub.modal.out.AccountsListResponse
import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.CloseCode
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.network.timeout.SchedulerTimeoutService
import dev.nikdekur.nexushub.service.NexusHubService
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.html.body
import kotlinx.html.p
import java.time.Duration
import io.ktor.http.Headers as KtorHeaders

class Routing(
    override val app: NexusHubServer,
    val application: Application
) : NexusHubService() {

    val accessAuthService: AccessAuthService by inject()
    val accessService: dev.nikdekur.nexushub.access.AccessService by inject()
    val configAccessService: ConfigurationAccessService by inject()

    val timeoutService = SchedulerTimeoutService(
        CoroutineScheduler.fromSupervisor(Dispatchers.IO)
    )

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
            get("/") { onRootRequest() }

            webSocket("connection") { wsConnection() }

            post("login") { onLoginRequest() }

            authenticate(app) {

                post("logout") { onLogoutRequest() }

                route("accounts") {

                    post("list") { onAccountsListRequest() }
                    post("create") { onAccountCreateRequest() }
                    post("delete") { onAccountDeleteRequest() }
                    post("password") { onAccountChangePasswordRequest() }

                    route("scopes") {

                        post("list") { onAccountScopesListRequest() }
                        post("update") { onAccountScopesUpdateRequest() }
                    }
                }
            }
        }

        logger.info("Routing initialized")
    }

    @Suppress("NOTHING_TO_INLINE")
    suspend inline fun DefaultWebSocketServerSession.wsConnection() {

        val address = call.address
        val talker: Talker = KtorPacketControllerTalker(this, address, timeoutService)

        logger.info("[$address] Connection established")

        val job = launch {
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
            val result = accessService.onReady(talker)
            if (result) {
                // Wait for the job to finish
                job.join()

                accessService.onClose(talker)
            }
        } catch (e: Throwable) {
            logger.warn("[$address] Exception occurred!", e)
        }



        logger.info("[$address] Closing connection")

        if (talker.isOpen)
            talker.close(CloseCode.NORMAL, "Done")
    }


    suspend fun RoutingContext.onRootRequest() {
        call.respondHtml(HttpStatusCode.OK) {
            body {
                p {
                    +"NexusHub server is running"
                }
            }
        }
    }

    suspend fun RoutingContext.onLoginRequest() {
        val request = call.receive<AuthRequest>()
        val result = accessAuthService.login(request.login, request.password)
        when (result) {
            LoginResult.AccountNotFound -> {
                // When creates more accounts, remember to imitate hashing time
                // To prevent users to know if the account exists or not.
                call.respondText(
                    text = "Account not found",
                    status = HttpStatusCode.BadRequest
                )
            }

            LoginResult.WrongCredentials -> {
                call.respondText(
                    text = "Bad Credentials",
                    status = HttpStatusCode.BadRequest
                )
            }

            is LoginResult.Success -> {
                call.respond(result.data)
            }
        }
    }


    suspend fun RoutingContext.onLogoutRequest() {
        val headers = HeadersMap(call.request.headers)
        val result = accessAuthService.logout(headers)
        val (status, text) = when (result) {
            LogoutResult.NotAuthenticated -> HttpStatusCode.BadRequest to "Not authenticated"
            LogoutResult.Success -> HttpStatusCode.OK to "Logged out"
        }
        call.respondText(text, status = status)
    }

    suspend fun RoutingContext.onAccountsListRequest() {
        logger.info("[${call.address}] Accounts list requested")
        val accounts = configAccessService.listAccounts()

        val response = AccountsListResponse(
            accounts.map {
                Account(it.login, it.getScopes())
            }
        )
        logger.info("[${call.address}] Responding with accounts list: $response")
        call.respond(response)
    }

    suspend fun RoutingContext.onAccountCreateRequest() {
        logger.info("[${call.address}] Account creation requested")
        val request = call.receive<AccountCreateRequest>()

        val result = configAccessService.createAccount(
            request.login,
            request.password,
            request.scopes
        )
        val (status, message) = when (result) {
            is AccountCreationResult.Success -> {
                logger.info("[${call.address}] Account '${request.login}' created with scopes: ${request.scopes}")
                HttpStatusCode.OK to "Success"
            }

            AccountCreationResult.AccountAlreadyExists -> {
                logger.info("[${call.address}] Account '${request.login}' already exists")
                HttpStatusCode.BadRequest to "Account '${request.login}' already exists"
            }
        }

        call.respondText(message, status = status)
    }


    suspend fun RoutingContext.onAccountDeleteRequest() {
        logger.info("[${call.address}] Account deletion requested")
        val request = call.receive<AccountDeleteRequest>()
        val login = request.login
        val result = configAccessService.deleteAccount(login)
        val (status, message) = when (result) {
            AccountDeletionResult.Success -> {
                logger.info("[${call.address}] Account '$login' deleted")
                HttpStatusCode.OK to "Success"
            }

            AccountDeletionResult.AccountNotFound -> {
                logger.info("[${call.address}] Account '$login' not found")
                HttpStatusCode.BadRequest to "Account '$login' not found"
            }
        }

        call.respondText(message, status = status)
    }


    suspend fun RoutingContext.onAccountChangePasswordRequest() {
        logger.info("[${call.address}] Password change requested")
        val request = call.receive<AccountPasswordRequest>()
        val login = request.login

        val result = configAccessService.changePassword(login, request.newPassword)
        val (status, message) = when (result) {
            PasswordChangeResult.Success -> {
                logger.info("[${call.address}] Password for account '$login' changed")
                HttpStatusCode.OK to "Success"
            }

            PasswordChangeResult.AccountNotFound -> {
                logger.info("[${call.address}] Account '$login' not found")
                HttpStatusCode.BadRequest to "Account '$login' not found"
            }
        }
        call.respondText(message, status = status)
    }

    suspend fun RoutingContext.onAccountScopesListRequest() {
        logger.info("[${call.address}] Account scopes list requested")
        val request = call.receive<AccountScopesListRequest>()
        val login = request.login
        val result = configAccessService.listAccountScopes(login)
        val (status, message) = when (result) {
            is AccountScopesListResult.Success -> {
                logger.info("[${call.address}] Account '$login' scopes list: ${result.scopes}")
                HttpStatusCode.OK to "Success"
            }

            AccountScopesListResult.AccountNotFound -> {
                logger.info("[${call.address}] Account '$login' not found")
                HttpStatusCode.BadRequest to "Account '$login' not found"
            }
        }
        call.respondText(message, status = status)
    }


    suspend fun RoutingContext.onAccountScopesUpdateRequest() {
        logger.info("[${call.address}] Account scopes update requested")
        val request = call.receive<AccountScopesUpdateRequest>()

        val login = request.login

        val action = ConfigurationAccessService.Action.valueOf(request.action.name)
        val result = configAccessService.changeAccountScopes(
            login,
            action,
            request.scopes
        )
        val (status, message) = when (result) {
            AccountScopesChangeResult.Success -> {
                logger.info("[${call.address}] Account '$login' scopes updated")
                HttpStatusCode.OK to "Success"
            }

            AccountScopesChangeResult.AccountNotFound -> {
                logger.info("[${call.address}] Account '$login' not found")
                HttpStatusCode.BadRequest to "Account '$login' not found"
            }
        }

        call.respondText(message, status = status)
    }
}


class HeadersMap(private val headers: KtorHeaders) : Headers {
    override val entries = headers.entries()
    override val keys = headers.names()
    override val size = headers.entries().size
    override val values: Collection<List<String>>
        get() = entries.mapTo(LinkedHashSet()) { it.value }

    override fun containsKey(key: String): Boolean = headers.contains(key)
    override fun containsValue(value: List<String>): Boolean = values.contains(value)
    override fun get(key: String): List<String>? = headers.getAll(key)
    override fun isEmpty(): Boolean = headers.isEmpty()
}

inline val ApplicationCall.address: Address
    get() = request.origin.let {
        Address(it.remoteAddress, it.remotePort)
    }