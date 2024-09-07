/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub

import dev.nikdekur.nexushub.access.AccessService
import dev.nikdekur.nexushub.access.ProductionAccessService
import dev.nikdekur.nexushub.access.auth.AccessAuthService
import dev.nikdekur.nexushub.access.auth.session.SessionAccessAuthService
import dev.nikdekur.nexushub.access.config.ConfigurationAccessService
import dev.nikdekur.nexushub.access.config.ProductionConfigurationAccessService
import dev.nikdekur.nexushub.account.AccountsService
import dev.nikdekur.nexushub.account.StorageAccountsService
import dev.nikdekur.nexushub.auth.AccountAuthenticationService
import dev.nikdekur.nexushub.auth.AuthenticationService
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.node.RuntimeNodesService
import dev.nikdekur.nexushub.protection.ProtectionService
import dev.nikdekur.nexushub.protection.argon2.Argon2ProtectionService
import dev.nikdekur.nexushub.ratelimit.PeriodRateLimitService
import dev.nikdekur.nexushub.ratelimit.RateLimitService
import dev.nikdekur.nexushub.scope.ScopesService
import dev.nikdekur.nexushub.scope.StorageScopesService
import dev.nikdekur.nexushub.serial.SerialService
import dev.nikdekur.nexushub.serial.gson.GsonSerialService
import dev.nikdekur.nexushub.session.RuntimeSessionsService
import dev.nikdekur.nexushub.session.SessionsService
import dev.nikdekur.nexushub.setup.EnvironmentSetupService
import dev.nikdekur.nexushub.setup.SetupService
import dev.nikdekur.nexushub.storage.StorageService

abstract class ProductionNexusHubServer : AbstractNexusHubServer() {

    abstract fun buildDataSetService(): DataSetService
    abstract fun buildStorageService(): StorageService

    open fun buildSerialService(): SerialService = GsonSerialService(this)
    open fun buildScopesService(): ScopesService = StorageScopesService(this)
    open fun buildAccountsService(): AccountsService = StorageAccountsService(this)
    open fun buildProtectionService(): ProtectionService = Argon2ProtectionService(this)
    open fun buildNodesService(): NodesService = RuntimeNodesService(this)
    open fun buildSessionsService(): SessionsService = RuntimeSessionsService(this)
    open fun buildAuthenticationService(): AuthenticationService = AccountAuthenticationService(this)
    open fun buildHTTPAuthService(): AccessAuthService = SessionAccessAuthService(this)
    open fun buildRateLimitService(): RateLimitService = PeriodRateLimitService(this)

    open fun buildAccessService(): AccessService = ProductionAccessService(this)
    open fun buildConfigurationAccessService(): ConfigurationAccessService = ProductionConfigurationAccessService(this)

    open fun buildSetupService(): SetupService = EnvironmentSetupService(this)


    override fun registerServices() {
        with(servicesManager) {
            registerService(buildDataSetService(), DataSetService::class)
            registerService(buildStorageService(), StorageService::class)

            registerService(buildSerialService(), SerialService::class)
            registerService(buildScopesService(), ScopesService::class)
            registerService(buildAccountsService(), AccountsService::class)
            registerService(buildProtectionService(), ProtectionService::class)
            registerService(buildNodesService(), NodesService::class)
            registerService(buildSessionsService(), SessionsService::class)
            registerService(buildAuthenticationService(), AuthenticationService::class)
            registerService(buildHTTPAuthService(), AccessAuthService::class)
            registerService(buildRateLimitService(), RateLimitService::class)

            registerService(buildAccessService(), AccessService::class)
            registerService(buildConfigurationAccessService(), ConfigurationAccessService::class)

            registerService(buildSetupService(), SetupService::class)
        }
    }
}