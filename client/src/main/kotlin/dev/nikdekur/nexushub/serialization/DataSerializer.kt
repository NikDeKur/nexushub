/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.serialization

import dev.nikdekur.nexushub.sesion.Session


interface DataSerializer<H, S> {
    fun serialize(session: Session<H, S>, data: S): String
    fun deserialize(session: Session<H, S>, dataJson: String): S

    fun isDefault(session: Session<H, S>, data: S): Boolean
}