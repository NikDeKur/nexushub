/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet.type

import dev.nikdekur.ndkore.ext.construct
import dev.nikdekur.nexushub.packet.Packet


interface PacketType<T : Packet> {

    val id: UByte
    val clazz: Class<out T>

    fun newInstance(): T {
        return clazz.construct()
    }
}