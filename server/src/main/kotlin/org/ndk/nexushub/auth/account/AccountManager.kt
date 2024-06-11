package org.ndk.nexushub.auth.account

import kotlinx.coroutines.async
import org.ndk.nexushub.NexusHub
import org.ndk.nexushub.database.account.AccountDAO
import org.ndk.nexushub.database.account.AccountsTable
import java.util.concurrent.ConcurrentHashMap

object AccountManager {

    fun init() {
        AccountsTable.init()
    }

    val cache = ConcurrentHashMap<String, Account>()

    suspend fun fetchAccount(login: String): Account? {
        return NexusHub.blockingScope.async {
            cache.getOrPut(login) {
                val dao = AccountsTable.fetchAccount(login) ?: return@async null
                dao.toHighLevel()
            }
        }.await()
    }

    suspend fun newAccount(dao: AccountDAO) {
        AccountsTable.newAccount(dao)
        val account = dao.toHighLevel()
        cache[account.login] = account
    }


}