/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.account

import dev.nikdekur.ndkore.ext.ConcurrentHashSet
import dev.nikdekur.nexushub.protection.Password

data class BasicAccount(
    override val login: String,
    override var password: Password,
) : Account {

    val allowedScopes = ConcurrentHashSet<String>()

    override suspend fun changePassword(newPassword: Password) {
        password = newPassword
    }

    override suspend fun getScopes() = allowedScopes

    override suspend fun allowScope(scope: String) {
        allowedScopes.add(scope)
    }

    override suspend fun removeScope(scope: String) {
        allowedScopes.remove(scope)
    }

    override suspend fun clearScopes() {
        allowedScopes.clear()
    }
}