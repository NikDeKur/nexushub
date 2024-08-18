/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.storage.scope

interface ScopesTable {

    suspend fun createScope(dao: ScopeDAO)
    suspend fun findScope(scope: String): ScopeDAO?
    suspend fun updateScope(scopeDAO: ScopeDAO)
    suspend fun deleteScope(scope: String)
}