/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.scope

import dev.nikdekur.nexushub.database.scope.ScopeDAO
import dev.nikdekur.nexushub.database.scope.ScopeTable

interface ScopesService {

    suspend fun reloadScopes()

    suspend fun createScope(scopeId: String): Scope

    /**
     * Get scope by name or create it if it doesn't exist
     *
     * @param scopeId scope name
     * @return scope
     */
    suspend fun getScope(scopeId: String): Scope


    suspend fun getScopeCollection(name: String): ScopeTable

    suspend fun findScopeData(scopeId: String): ScopeDAO?
    suspend fun createScopeData(data: ScopeDAO)
    suspend fun updateScopeData(data: ScopeDAO)
    suspend fun deleteScopeData(scopeId: String)
}