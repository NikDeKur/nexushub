/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.dataset.map

import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.dataset.DataSetService

class MapDataSetService(
    override val app: NexusHubServer,
    map: Map<String, Any>
) : MapDataSetSection(map), DataSetService