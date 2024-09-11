/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.scope

interface ScopesService {

    /**
     * Get a collection of all existing scopes
     *
     * @return collection of all existing scopes
     */
    suspend fun getScopes(): Collection<Scope>

    /**
     * Get scope by name or create it if it doesn't exist
     *
     * @param scopeId scope name
     * @return scope
     */
    suspend fun getScope(scopeId: String): Scope
}