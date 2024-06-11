import org.ndk.nexushub.packet.PacketRequestLeaderboard

fun main() {
    val packet = PacketRequestLeaderboard("scopeId", "filter", 0, 0, "requestPosition")
    println(packet)
}