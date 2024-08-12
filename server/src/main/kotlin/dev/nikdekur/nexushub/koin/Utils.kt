/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.koin

import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.dsl.ModuleDeclaration
import org.koin.dsl.module

/** Wrapper for [org.koin.dsl.module] that immediately loads the module for the current [Koin] instance. **/
fun loadModule(
    createdAtStart: Boolean = false,
    moduleDeclaration: ModuleDeclaration,
): Module {
    val moduleObj = module(createdAtStart, moduleDeclaration)

    NexusHubKoinContext.loadKoinModules(moduleObj)

    return moduleObj
}

/** Retrieve the current [Koin] instance. **/
fun getKoin(): Koin = NexusHubKoinContext.get()