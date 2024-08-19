/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.dataset.test

import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.dataset.DataSet
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.service.NexusHubService

class TestDataSetService(
    override val app: NexusHubServer,
    val dataset: DataSet
) : NexusHubService, DataSetService {
    override fun getDataSet(): DataSet {
        return dataset
    }
}