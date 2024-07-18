/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.connection.gateway

data class GatewayConfiguration(
    val login: String,
    val password: String,
    val node: String
) {

    class Builder {
        var node: String? = null
        var login: String? = null
        var password: String? = null

        fun build(): GatewayConfiguration {
            checkNotNull(login) { "Login is not set" }
            checkNotNull(password) { "Password is not set" }
            checkNotNull(node) { "Node is not set" }

            return GatewayConfiguration(login!!, password!!, node!!)
        }
    }
}