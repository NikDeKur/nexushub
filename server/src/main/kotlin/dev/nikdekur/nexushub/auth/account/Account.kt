/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth.account

import dev.nikdekur.nexushub.database.account.AccountDAO
import dev.nikdekur.nexushub.koin.NexusHubComponent
import dev.nikdekur.nexushub.protection.Password
import dev.nikdekur.nexushub.protection.ProtectionService
import org.koin.core.component.inject

data class Account(
    var dao: AccountDAO,
    var password: Password,
    private val _allowedScopes: MutableSet<String>
) : NexusHubComponent {

    val login get() = dao.login

    val protectionService: ProtectionService by inject()
    val accountsService: AccountsService by inject()

    val allowedScopes: Set<String>
        get() = _allowedScopes

    suspend fun changePassword(newPassword: String) {
        password = protectionService.createPassword(newPassword)
        dao = dao.copy(password = password.serialize())
        accountsService.updateAccount(dao)
    }

    suspend fun updateScopes() {
        dao = dao.copy(allowedScopes = allowedScopes)
        accountsService.updateAccount(dao)
    }

    suspend fun allowScope(scope: String) {
        _allowedScopes.add(scope)
        updateScopes()
    }

    suspend fun removeScope(scope: String) {
        _allowedScopes.remove(scope)
        updateScopes()
    }

    suspend fun clearScopes() {
        _allowedScopes.clear()
        updateScopes()
    }
}