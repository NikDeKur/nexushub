/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network

import dev.nikdekur.ndkore.ext.debug
import dev.nikdekur.ndkore.ext.warn
import dev.nikdekur.nexushub.auth.AuthenticationService
import dev.nikdekur.nexushub.config.NexusHubServerConfig
import dev.nikdekur.nexushub.koin.NexusHubComponent
import dev.nikdekur.nexushub.network.ratelimit.PeriodRateLimiter
import dev.nikdekur.nexushub.network.ratelimit.RateLimiter
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.packet.PacketOk
import dev.nikdekur.nexushub.packet.`in`.PacketAuth
import dev.nikdekur.nexushub.packet.`in`.PacketHello
import dev.nikdekur.nexushub.packet.out.PacketReady
import dev.nikdekur.nexushub.talker.KtorClientTalker
import dev.nikdekur.nexushub.talker.TalkersService
import dev.nikdekur.nexushub.util.CloseCode
import io.ktor.server.application.*
import io.ktor.server.plugins.*
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
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import kotlin.time.Duration.Companion.seconds


class Routing : NexusHubComponent {

    val logger = LoggerFactory.getLogger(javaClass)

    val config: NexusHubServerConfig by inject()
    val talkersService: TalkersService by inject()
    val authService: AuthenticationService by inject()
    val nodesService: NodesService by inject()

    val processingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun init(application: Application) {
        logger.info("Initializing routing")

        application.install(WebSockets) {
            pingPeriod = Duration.ofSeconds(10)
            timeout = Duration.ofSeconds(10)
        }

        application.routing {
            route("connection") {

                val networkConfig = config.network
                val pingConfig = networkConfig.ping
                val rateConfig = networkConfig.rateLimit

                val rateLimiter = PeriodRateLimiter(
                    limit = rateConfig.maxRequests,
                    period = rateConfig.timeWindow.seconds
                )

                val interval = pingConfig.interval * 1000

                webSocket {
                    protocol(rateLimiter, interval)
                }
            }
        }


        logger.info("Routing initialized")
    }


    @Suppress("NOTHING_TO_INLINE")
    suspend inline fun DefaultWebSocketServerSession.protocol(
        rateLimiter: RateLimiter,
        pingInterval: Int
    ) {

        val address = call.addressHash
        val addressStr = call.addressStr

        val talker = KtorClientTalker(this)
        talkersService.setTalker(address, talker)

        val job = launch {
            try {
                incoming.consumeEach { frame ->
                    if (talker.isBlocked) {
                        this@launch.cancel()
                        return@consumeEach
                    }

                    if (!rateLimiter.acquire(talker)) {
                        talker.closeWithBlock(CloseCode.RATE_LIMITED)
                        this@launch.cancel()
                        return@consumeEach
                    }

                    if (frame !is Frame.Binary) return@consumeEach

                    // Run as new coroutine to avoid blocking and handle multiple connections
                    processingScope.launch {
                        val context = talker.receive(frame.readBytes()) ?: return@launch

                        logger.debug { "[$addressStr] Received packet: ${context.packet}" }

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
            job.cancel() // cancel consumeEach on WebSocket close
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

fun Application.configureRouting() {

    routing {
//
//        get("/") {
//            call.respond(HtmlContent(HttpStatusCode.OK) {
//                body {
//                    p {
//                        +"NexusHub server is running"
//                    }
//                }
//            })
//        }
//
//        route("accounts") {
//            // Accounts page on server
//
//            get("create") {
//                call.respond(HtmlContent(HttpStatusCode.OK) {
//                    body {
//                        form(action = "create", encType = FormEncType.applicationXWwwFormUrlEncoded, method = FormMethod.post) {
//                            p {
//                                +"Login: "
//                                textInput(name = "login")
//                            }
//                            p {
//                                +"Password: "
//                                passwordInput(name = "password")
//                            }
//                            p {
//                                +"Allowed scopes: "
//                                textInput(name = "scopes")
//                            }
//                            p {
//                                submitInput { value = "Create" }
//                            }
//                        }
//                    }
//                })
//            }
//
//            post("create") {
//                val formParameters = call.receiveParameters()
//                val username = formParameters["login"].toString()
//                val password = formParameters["password"].toString()
//                val scopes = formParameters["scopes"].toString().split(",").toSet()
//                AccountManager.newAccount(username, password, scopes)
//                call.respondText("The '$username' account is created")
//            }
//
//
//            get("delete") {
//                call.respond(HtmlContent(HttpStatusCode.OK) {
//                    body {
//                        form(action = "delete", encType = FormEncType.applicationXWwwFormUrlEncoded, method = FormMethod.post) {
//                            p {
//                                +"Login: "
//                                textInput(name = "login")
//                            }
//                            p {
//                                submitInput { value = "Delete" }
//                            }
//                        }
//                    }
//                })
//            }
//
//            post("delete") {
//                val formParameters = call.receiveParameters()
//                val username = formParameters["login"].toString()
//
//                AccountManager.deleteAccount(username)
//                NodesManager.connectedNodes.values.forEach {
//                    if (it.account.login == username) {
//                        it.close(CloseCode.UNEXPECTED_BEHAVIOUR, "Account deleted")
//                    }
//                }
//
//                call.respondText("The '$username' account is deleted")
//            }
//
//            get("scopes") {
//                call.respond(HtmlContent(HttpStatusCode.OK) {
//                    body {
//                        form(action = "scopes", encType = FormEncType.applicationXWwwFormUrlEncoded, method = FormMethod.post) {
//                            p {
//                                +"Login: "
//                                textInput(name = "login")
//                            }
//                            p {
//                                +"Scope: "
//                                textInput(name = "scope")
//                            }
//
//                            // Choose: Add scope, Remove scope, Clear scopes
//                            p {
//                                +"Add: "
//                                checkBoxInput(name = "add") { value = "add" }
//                            }
//
//                            p {
//                                +"Remove: "
//                                checkBoxInput(name = "remove") { value = "remove" }
//                            }
//
//                            p {
//                                +"Clear all: "
//                                checkBoxInput(name = "clear all") { value = "clear" }
//                            }
//
//                            p {
//                                submitInput { value = "Go" }
//                            }
//                        }
//                    }
//                })
//            }
//
//            post("scopes") {
//                val formParameters = call.receiveParameters()
//                val username = formParameters["login"].toString()
//                val scope = formParameters["scope"].toString()
//
//                val add = formParameters["add"] != null
//                val remove = formParameters["remove"] != null
//                val clear = formParameters["clear all"] != null
//
//                val account = AccountManager.getAccount(username) ?: run {
//                    call.respondText("Account '$username' not found")
//                    return@post
//                }
//
//                println("add: $add, remove: $remove, clear: $clear")
//
//                when {
//                    add -> account.allowScope(scope)
//                    remove -> account.removeScope(scope)
//                    clear -> account.clearScopes()
//                }
//
//                call.respondText("Account '$username' edited. New scopes: ${account.allowedScopes}")
//            }
//
//            handle {
//                println("called accounts route")
//                call.respond(
//                    AccountManager.accounts.mapValues {
//                        it.value.dao
//                    }
//                        .let {
//                            json.encodeToString(it)
//                        }
//                )
//            }
//        }



    }
}



