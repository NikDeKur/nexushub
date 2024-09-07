/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.ktor.auth

import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.Routing
import io.ktor.server.routing.RoutingResolveContext

class AuthenticationRouteSelector : RouteSelector() {
    override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
        return RouteSelectorEvaluation.Transparent
    }

    override fun toString(): String = "AuthenticationRouteSelector"
}

fun Routing.authenticate(build: Routing.() -> Unit): Routing {
    val route = createChild(AuthenticationRouteSelector())
    route.install(TokenAuthenticationPlugin)
    route.build()
    return route
}