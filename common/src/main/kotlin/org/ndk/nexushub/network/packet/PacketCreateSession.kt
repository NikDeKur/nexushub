package org.ndk.nexushub.network.packet

import org.ndk.nexushub.network.packet.type.PacketTypes.CREATE_SESSION

/**
 * (IN) Packet to load user data from the database
 *
 * Requires authentication to be processed
 */
class PacketCreateSession : PacketLoadData {

    override val packetId = CREATE_SESSION.id


    constructor() : super()
    constructor(scopeId: String, userId: String) : super(scopeId, userId)

    override fun toString(): String {
        return "PacketCreateSession(scopeId='$scopeId', holderId='$holderId')"
    }
}