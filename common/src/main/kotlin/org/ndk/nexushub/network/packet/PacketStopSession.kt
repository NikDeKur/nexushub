package org.ndk.nexushub.network.packet

import org.ndk.nexushub.network.packet.type.PacketTypes.STOP_SESSION

/**
 * (IN) Packet to stop user session and save data
 *
 * Requires authentication to be processed
 */
class PacketStopSession : PacketSaveData {

    constructor() : super()
    constructor(holderId: String, scopeId: String, data: String) : super(holderId, scopeId, data)

    override val packetId = STOP_SESSION.id

    override fun toString(): String {
        return "PacketStopSession(holderId='$holderId', scopeId='$scopeId', data='$data')"
    }
}