@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.node

import org.ndk.nexushub.network.talker.Talker
import java.util.concurrent.ConcurrentHashMap

object TalkersManager {

    val talkers = ConcurrentHashMap<Int, Talker>()

    fun getExistingTalker(address: Int): Talker? {
        return talkers[address]
    }

    fun setTalker(address: Int, talker: Talker) {
        talkers[address] = talker
    }

    fun removeTalker(talker: Int) {
        talkers.remove(talker)
    }
}