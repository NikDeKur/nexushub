/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.account

import dev.nikdekur.nexushub.protection.Password

interface Account {

    val login: String
    val password: Password

    suspend fun changePassword(newPassword: Password)

    suspend fun getScopes(): Set<String>
    suspend fun allowScope(scope: String)
    suspend fun removeScope(scope: String)
    suspend fun clearScopes()
}