/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.ratelimit

import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.dataset.get
import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.talker.Talker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.properties.Delegates
import kotlin.time.Duration

data class PeriodRateLimitService(
    override val app: NexusHubServer
) : RateLimitService {

    val datasetService: DataSetService by inject()

    val map = ConcurrentHashMap<Address, Entry>()

    var limit: Long by Delegates.notNull()
    var period: Duration by Delegates.notNull()

    override fun onEnable() {
        val dataSet = datasetService.get<PeriodRateLimitDataSet>("rate_limit") ?: PeriodRateLimitDataSet()
        limit = dataSet.maxRequests
        period = dataSet.timeWindow
    }

    override fun acquire(talker: Talker): Boolean {
        val now = System.currentTimeMillis()
        var limitExceeded = false

        val address = talker.address

        map.compute(address) { _, existingEntry ->
            existingEntry?.let {
                if (now - it.start > period.inWholeMilliseconds) {
                    Entry(address, now, AtomicInteger(1))
                } else {
                    if (it.count.incrementAndGet() > limit) {
                        limitExceeded = true
                        it
                    } else {
                        it
                    }
                }
            } ?: Entry(address, now, AtomicInteger(1))
        }

        return !limitExceeded
    }

    data class Entry(
        val address: Address,
        val start: Long,
        val count: AtomicInteger
    )
}
