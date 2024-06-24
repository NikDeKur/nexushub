package org.ndk.nexushub.packet.`in`

import org.ndk.nexushub.packet.type.PacketTypes

class PacketEndSession : PacketSaveData {

    override val packetId = PacketTypes.END_SESSION.id

    constructor() : super()
    constructor(scopeId: String, holderId: String, data: String) : super(scopeId, holderId, data)

    override fun toString(): String {
        return "PacketEndSession(scopeId='$scopeId', holderId='$holderId', data='$data')"
    }
}