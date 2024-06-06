package org.ndk.nexushub.client.service

import kotlinx.coroutines.CompletableDeferred
import org.ndk.klib.smartAwait
import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.data.Leaderboard
import org.ndk.nexushub.data.LeaderboardEntry
import org.ndk.nexushub.network.GsonSupport
import org.ndk.nexushub.network.NexusData
import org.ndk.nexushub.network.packet.*

class StatService(val hub: NexusHub) {

    suspend fun request(scope: String, holder: String): NexusData {
        val packet = PacketLoadData(scope, holder)
        val def = CompletableDeferred<String>()

        hub.connection.talker!!.sendPacket(packet) {
            timeout(5000) {
                def.completeExceptionally(Exception("Timeout while loading data"))
            }

            receive<PacketUserData> {
                def.complete(this.packet.data)
            }
        }

        val dataStr = def.smartAwait()
        val data = GsonSupport.dataFromString(dataStr)

        return data
    }

    suspend fun requestLeaderboard(scope: String, holder: String, field: String, limit: Int): Leaderboard {
        val packet = PacketRequestLeaderboard(scope, holder, field, limit)
        val def = CompletableDeferred<Leaderboard>()

        hub.connection.talker!!.sendPacket(packet) {
            timeout(5000) {
                def.completeExceptionally(Exception("Timeout while loading leaderboard"))
            }

            receive<PacketLeaderboard> {
                def.complete(this.packet.leaderboard)
            }
        }

        return def.smartAwait()
    }

    suspend fun requestTopPosition(
        scope: String,
        holder: String,
        field:
        String
    ): LeaderboardEntry {
        val packet = PacketRequestTopPosition(scope, holder, field)

        val def = CompletableDeferred<LeaderboardEntry>()

        hub.connection.talker!!.sendPacket(packet) {
            timeout(5000) {
                def.completeExceptionally(Exception("Timeout while loading top position"))
            }

            receive<PacketTopPosition> {
                def.complete(LeaderboardEntry(
                    position = this.packet.position,
                    holderId = holder,
                    value = this.packet.value
                ))
            }
        }

        return def.smartAwait()
    }
}