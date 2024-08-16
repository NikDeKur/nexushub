/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.root

import dev.nikdekur.nexushub.modal.`in`.AccountCreateRequest
import dev.nikdekur.nexushub.modal.`in`.AccountDeleteRequest
import dev.nikdekur.nexushub.modal.`in`.AccountPasswordRequest
import dev.nikdekur.nexushub.modal.`in`.AccountScopesListRequest
import dev.nikdekur.nexushub.modal.`in`.AccountScopesUpdateRequest
import dev.nikdekur.nexushub.modal.`in`.AuthRequest
import dev.nikdekur.nexushub.modal.out.AccountScopesListResponse
import dev.nikdekur.nexushub.modal.out.AccountsListResponse
import dev.nikdekur.nexushub.modal.out.AuthSuccess

object RootPaths {

    inline fun <T : Any, R : Any> path(path: String, requireAuth: Boolean = true) = object : Path<T, R> {
        override val url = path
        override val requireAuth = requireAuth
    }

    val LOGIN = path<AuthRequest, AuthSuccess>("login", false)

    val LIST_ACCOUNTS = path<Unit, AccountsListResponse>("accounts/list")
    val CREATE_ACCOUNT = path<AccountCreateRequest, Unit>("accounts/create")
    val DELETE_ACCOUNT = path<AccountDeleteRequest, Unit>("accounts/delete")
    val CHANGE_ACCOUNT_PASSWORD = path<AccountPasswordRequest, Unit>("accounts/password")

    val LIST_ACCOUNT_SCOPES = path<AccountScopesListRequest, AccountScopesListResponse>("accounts/scopes/list")
    val UPDATE_ACCOUNT_SCOPES = path<AccountScopesUpdateRequest, Unit>("accounts/scopes/update")
}