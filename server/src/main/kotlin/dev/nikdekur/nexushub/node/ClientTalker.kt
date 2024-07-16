package dev.nikdekur.nexushub.node

import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.util.CloseCode

interface ClientTalker : Talker {

    /**
     * Represent if the talker is blocked from sending packets.
     *
     * The server could set this to ignore any packets after rate limiting or other reasons.
     */
    val isBlocked: Boolean

    suspend fun closeWithBlock(code: CloseCode, reason: String = "")
}