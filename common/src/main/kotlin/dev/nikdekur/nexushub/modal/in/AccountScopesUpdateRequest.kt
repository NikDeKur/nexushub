/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.modal.`in`

import kotlinx.serialization.Serializable

@Serializable
data class AccountScopesUpdateRequest(
    val login: String,
    val action: Action,
    val scopes: Set<String>
) {

    @Serializable
    enum class Action {
        ADD,
        REMOVE,
        SET,
        CLEAR // Clear all scopes, scopes parameter is ignored
    }
}

