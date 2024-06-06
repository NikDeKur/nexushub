package org.ndk.nexushub.network.packet

import org.ndk.nexushub.network.packet.serialize.PacketDeserializer
import org.ndk.nexushub.network.packet.serialize.PacketSerializer
import org.ndk.nexushub.network.packet.type.PacketTypes

/**
 * (IN) Packet for authentication
 *
 * All clients must send this packet first to authenticate.
 */
class PacketAuth : Packet {

    override val packetId = PacketTypes.AUTH.id

    var login: String = ""
    var password: String = ""
    var node: String = ""

    constructor()
    constructor(login: String, password: String, node: String) {
        this.login = login
        this.password = password
        this.node = node
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        login = deserializer.readString()
        password = deserializer.readString()
        node = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(login)
        serializer.writeString(password)
        serializer.writeString(node)
    }

    override fun toString(): String {
        return "PacketAuth(login='$login', password='$password', node='$node')"
    }
}
