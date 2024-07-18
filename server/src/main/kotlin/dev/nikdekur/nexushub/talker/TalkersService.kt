/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.talker

import dev.nikdekur.nexushub.network.talker.Talker

interface TalkersService {

    fun getExistingTalker(address: Int): Talker?
    fun setTalker(address: Int, talker: Talker)
    fun removeTalker(talker: Int)
    fun cleanUp(address: Int)
}