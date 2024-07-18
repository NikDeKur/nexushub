/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth.account

import dev.nikdekur.nexushub.auth.password.EncryptedPassword
import dev.nikdekur.nexushub.database.account.AccountDAO
import dev.nikdekur.nexushub.koin.NexusHubComponent
import org.koin.core.component.inject

data class Account(
    var dao: AccountDAO,
    val login: String,
    val password: EncryptedPassword,
    private val _allowedScopes: MutableSet<String>
) : NexusHubComponent {

    val accountsService: AccountsService by inject()

    val allowedScopes: Set<String>
        get() = _allowedScopes


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