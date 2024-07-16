/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet.`in`

import dev.nikdekur.nexushub.packet.type.PacketTypes

class PacketEndSession : PacketSaveData {

    override val packetId = PacketTypes.END_SESSION.id

    constructor() : super()
    constructor(scopeId: String, holderId: String, data: String) : super(scopeId, holderId, data)

    override fun toString(): String {
        return "PacketEndSession(scopeId='$scopeId', holderId='$holderId', data='$data')"
    }
}