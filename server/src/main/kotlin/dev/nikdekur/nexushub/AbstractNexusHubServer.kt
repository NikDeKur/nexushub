/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub

import dev.nikdekur.ndkore.koin.SimpleKoinContext
import dev.nikdekur.ndkore.service.KoinServicesManager
import dev.nikdekur.ndkore.service.ServicesManager
import dev.nikdekur.nexushub.account.AccountsService
import dev.nikdekur.nexushub.account.StorageAccountsService
import dev.nikdekur.nexushub.auth.AccountAuthenticationService
import dev.nikdekur.nexushub.auth.AuthenticationService
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.http.HTTPAuthService
import dev.nikdekur.nexushub.http.session.HTTPSessionAuthService
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.node.RuntimeNodesService
import dev.nikdekur.nexushub.protection.ProtectionService
import dev.nikdekur.nexushub.protection.argon2.Argon2ProtectionService
import dev.nikdekur.nexushub.scope.ScopesService
import dev.nikdekur.nexushub.scope.StorageScopesService
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.session.RuntimeSessionsService
import dev.nikdekur.nexushub.session.SessionsService
import dev.nikdekur.nexushub.setup.EnvironmentSetupService
import dev.nikdekur.nexushub.setup.SetupService
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.talker.RuntimeTalkersService
import dev.nikdekur.nexushub.talker.TalkersService
import org.koin.environmentProperties
import org.slf4j.LoggerFactory
import kotlin.properties.Delegates
import kotlin.reflect.KClass
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

abstract class AbstractNexusHubServer : NexusHubServer {
    override val logger = LoggerFactory.getLogger(javaClass)

    override lateinit var servicesManager: ServicesManager<NexusHubServer>

    var startTime by Delegates.notNull<Long>()

    override val uptime: Duration
        get() = (System.currentTimeMillis() - startTime).milliseconds


    abstract fun buildDataSetService(): DataSetService
    abstract fun buildStorageService(): StorageService

    open fun buildScopesService(): ScopesService = StorageScopesService(this)
    open fun buildAccountsService(): AccountsService = StorageAccountsService(this)
    open fun buildProtectionService(): ProtectionService = Argon2ProtectionService(this)
    open fun buildNodesService(): NodesService = RuntimeNodesService(this)
    open fun buildSessionsService(): SessionsService = RuntimeSessionsService(this)
    open fun buildTalkersService(): TalkersService = RuntimeTalkersService(this)
    open fun buildAuthenticationService(): AuthenticationService = AccountAuthenticationService(this)
    open fun buildHTTPAuthService(): HTTPAuthService = HTTPSessionAuthService(this)
    open fun buildSetupService(): SetupService = EnvironmentSetupService(this)


    fun setupServices() {
        with(servicesManager) {
            register(buildDataSetService(), DataSetService::class)

            register(buildStorageService(), StorageService::class)
            register(buildScopesService(), ScopesService::class)
            register(buildAccountsService(), AccountsService::class)

            register(buildProtectionService(), ProtectionService::class)
            register(buildNodesService(), NodesService::class)
            register(buildSessionsService(), SessionsService::class)
            register(buildTalkersService(), TalkersService::class)
            register(buildAuthenticationService(), AuthenticationService::class)
            register(buildHTTPAuthService(), HTTPAuthService::class)

            register(buildSetupService(), SetupService::class)
        }
    }

    override fun start() {
        startTime = System.currentTimeMillis()

        logger.info("Loading koin...")
        val context = SimpleKoinContext()
        context.startKoin {
            environmentProperties()
        }

        servicesManager = KoinServicesManager<NexusHubServer>(context, this)

        logger.info("Applying services...")
        servicesManager.apply { setupServices() }

        logger.info("Loading services...")
        servicesManager.loadAll()
    }
}

fun <T : Any> ServicesManager<NexusHubServer>.register(service: T, bind: KClass<T>) {
    service as? NexusHubService ?: throw IllegalArgumentException("Service `$service` does not implement NexusHubService")

    registerService(service, bind)
    service.onLoad()
}