/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network

import dev.nikdekur.ndkore.ext.log
import dev.nikdekur.ndkore.ext.warn
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.auth.AuthenticationService
import dev.nikdekur.nexushub.account.AccountsService
import dev.nikdekur.nexushub.dataset.DataSetService
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
import dev.nikdekur.nexushub.network.ratelimit.PeriodRateLimiter
import dev.nikdekur.nexushub.network.ratelimit.RateLimiter
import dev.nikdekur.nexushub.network.talker.sendPacket
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.node.isNodeExists
import dev.nikdekur.nexushub.packet.PacketOk
import dev.nikdekur.nexushub.packet.`in`.PacketAuth
import dev.nikdekur.nexushub.packet.`in`.PacketHello
import dev.nikdekur.nexushub.packet.out.PacketReady
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.talker.KtorClientTalker
import dev.nikdekur.nexushub.talker.TalkersService
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
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.html.body
import kotlinx.html.p
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.Duration
import java.util.*
import kotlin.time.Duration.Companion.seconds

class Routing(
    override val app: NexusHubServer,
    val application: Application
) : NexusHubService {

    val logger = LoggerFactory.getLogger(javaClass)

    val datasetService: DataSetService by inject()
    val talkersService: TalkersService by inject()
    val authService: AuthenticationService by inject()
    val httpAuthService: HTTPAuthService by inject()
    val nodesService: NodesService by inject()
    val accountsService: AccountsService by inject()

    lateinit var processingScope: CoroutineScope

    override fun onLoad() {
        processingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
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

                val dataset = datasetService.getDataSet()
                val pingConfig = dataset.ping
                val rateConfig = dataset.network.rateLimit

                val rateLimiter = PeriodRateLimiter(
                    limit = rateConfig.maxRequests,
                    period = rateConfig.timeWindow.seconds
                )

                val interval = pingConfig.interval * 1000

                webSocket {
                    protocol(rateLimiter, interval)
                }
            }

            post("login") {
                val request = call.receive<AuthRequest>()
                httpAuthService.login(call, request)
            }

            authenticate {

                post("logout") {
                    logger.info("[${call.addressStr}] Logging out requested")
                    httpAuthService.logout(call)
                }

                route("accounts") {

                    post("list") {
                        logger.info("[${call.addressStr}] Accounts list requested")
                        val accounts = accountsService.getAccounts()

                        val response = AccountsListResponse(
                            accounts.map {
                                Account(it.login, it.allowedScopes)
                            }
                        )
                        logger.info("[${call.addressStr}] Responding with accounts list: $response")
                        call.respond(response)
                    }

                    post("create") {
                        logger.info("[${call.addressStr}] Account creation requested")
                        val request = call.receive<AccountCreateRequest>()
                        val login = request.login
                        val account = accountsService.getAccount(login)
                        if (account != null) {
                            logger.info("[${call.addressStr}] Account '$login' already exists")
                            call.respondText(
                                text = "Account '$login' already exists",
                                status = HttpStatusCode.Conflict
                            )
                            return@post
                        }

                        val password = request.password
                        val scopes = request.scopes
                        accountsService.createAccount(login, password, scopes)
                        logger.info("[${call.addressStr}] Account '$login' created with scopes: $scopes")
                        call.respondText("Success")
                    }

                    post("delete") {
                        logger.info("[${call.addressStr}] Account deletion requested")
                        val request = call.receive<AccountDeleteRequest>()
                        val login = request.login
                        accountsService.deleteAccount(login)
                        logger.info("[${call.addressStr}] Account '$login' deleted")
                        call.respondText("Success")
                    }

                    post("password") {
                        logger.info("[${call.addressStr}] Password change requested")
                        val request = call.receive<AccountPasswordRequest>()
                        val login = request.login
                        val account = accountsService.getAccount(login) ?: run {
                            call.respondText("Account '$login' not found")
                            return@post
                        }
                        val newPassword = request.newPassword
                        account.changePassword(newPassword)
                        logger.info("[${call.addressStr}] Password for account '$login' changed")
                        call.respondText("Success")
                    }


                    route("scopes") {

                        post("list") {
                            logger.info("[${call.addressStr}] Account scopes list requested")
                            val request = call.receive<AccountScopesListRequest>()
                            val login = request.login
                            val account = accountsService.getAccount(login) ?: run {
                                call.respondText("Account '$login' not found")
                                return@post
                            }
                            val scopes = account.allowedScopes
                            val response = AccountScopesListResponse(scopes)
                            logger.info("[${call.addressStr}] Responding with account '$login' scopes list: $scopes")
                            call.respond(response)
                        }

                        post("update") {
                            logger.info("[${call.addressStr}] Account scopes update requested")
                            val request = call.receive<AccountScopesUpdateRequest>()

                            val login = request.login
                            val account = accountsService.getAccount(login) ?: run {
                                logger.info("[${call.addressStr}] Account '$login' not found")
                                call.respondText("Account '$login' not found")
                                return@post
                            }

                            val action = request.action
                            val scopes = request.scopes
                            when (action) {
                                AccountScopesUpdateRequest.Action.ADD -> {
                                    scopes.forEach {
                                        logger.info("[${call.addressStr}] Adding scope '$it' to account '$login'")
                                        account.allowScope(it)
                                    }
                                }

                                AccountScopesUpdateRequest.Action.REMOVE -> {
                                    scopes.forEach {
                                        logger.info("[${call.addressStr}] Removing scope '$it' from account '$login'")
                                        account.removeScope(it)
                                    }
                                }

                                AccountScopesUpdateRequest.Action.SET -> {
                                    account.clearScopes()
                                    scopes.forEach {
                                        logger.info("[${call.addressStr}] Allowing scope '$it' (by set) to account '$login'")
                                        account.allowScope(it)
                                    }
                                }

                                AccountScopesUpdateRequest.Action.CLEAR -> {
                                    logger.info("[${call.addressStr}] Clearing all scopes from account '$login'")
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

    override fun onUnload() {
        processingScope.cancel()
    }


    @Suppress("NOTHING_TO_INLINE")
    suspend inline fun DefaultWebSocketServerSession.protocol(
        rateLimiter: RateLimiter,
        pingInterval: Int
    ) {

        val address = call.addressHash
        val addressStr = call.addressStr

        val talker = KtorClientTalker(app, this)
        talkersService.setTalker(address, talker)

        val job = launch {
            try {
                incoming.consumeEach { frame ->
                    if (talker.isBlocked) {
                        logger.info("[$addressStr] Connection is blocked. Closing")
                        this@launch.cancel()
                        return@consumeEach
                    }

                    if (!rateLimiter.acquire(talker)) {
                        talker.closeWithBlock(CloseCode.RATE_LIMITED)
                        logger.info("[$addressStr] Rate limited. Closing")
                        this@launch.cancel()
                        return@consumeEach
                    }

                    if (frame !is Frame.Binary) return@consumeEach

                    // Run as new coroutine to avoid blocking and handle multiple connections
                    processingScope.launch {
                        val context = talker.receive(frame.readBytes()) ?: return@launch

                        val level = if (context.packet is PacketAuth) Level.TRACE else Level.DEBUG
                        logger.log(level) { "[$addressStr] Received packet: ${context.packet}" }

                        try {
                            authService.executeAuthenticatedPacket(talker, context)
                        } catch (_: CancellationException) {
                            // Connection closed while a processing packet
                            // Do nothing
                        } catch (e: Exception) {
                            logger.warn(e) { "Error while processing packet" }
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Throwable) {
                logger.warn(e) { "[$addressStr] Exception occurred in job!" }
            }
        }

        try {
            val packetHello = PacketHello()
            talker.sendPacket<Unit>(packetHello) {
                timeout(10000) {
                    talker.closeWithBlock(CloseCode.TIMEOUT, "No authentication packet received in time")
                }

                receive<PacketAuth> {
                    authService.processAuth(talker, packet)
                    respond<Unit>(PacketOk("Authenticated successfully"))
                }

                receive {
                    talker.closeWithBlock(CloseCode.UNEXPECTED_BEHAVIOUR, "Unexpected packet. Was expecting PacketAuth")
                }
            }.await()

            if (nodesService.isNodeExists(talker)) {
                talker.sendPacket<Unit>(PacketReady(pingInterval))
                job.join()
            }

        } catch (e: Throwable) {
            logger.warn("[$addressStr] Exception occurred!", e)
        } finally {
            logger.info("[$addressStr] Closing connection")
            talker.closeWithBlock(CloseCode.INTERNAL_ERROR, "Node hasn't been closed properly")
        }

    }
}


inline val ApplicationCall.addressHash: Int
    get() = request.origin.let {
        Objects.hash(it.remoteAddress, it.remotePort)
    }

inline val ApplicationCall.addressStr: String
    get() = request.origin.let {
        "${it.remoteAddress}:${it.remotePort}"
    }


