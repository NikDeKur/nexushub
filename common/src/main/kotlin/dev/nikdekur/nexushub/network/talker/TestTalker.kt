/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.talker

import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.packet.Packet
import java.util.function.Predicate

interface TestTalker : Talker {

    /**
     * Return packet that was received.
     *
     * If many packets were received, return all of them one by one.
     *
     * @return received a packet
     */
    fun <T : Packet> receive(clazz: Class<T>, condition: Predicate<T> = Predicate { true }): IncomingContext<T>?
}


inline fun <reified T : Packet> TestTalker.receive(condition: Predicate<T> = Predicate<T> { true }) =
    receive(T::class.java, condition)