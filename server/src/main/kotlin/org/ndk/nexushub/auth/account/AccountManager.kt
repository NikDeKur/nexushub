package org.ndk.nexushub.auth.account

import kotlinx.coroutines.async
import org.ndk.nexushub.NexusHub
import org.ndk.nexushub.auth.password.PasswordEncryptor
import org.ndk.nexushub.database.account.AccountsTable
import java.util.concurrent.ConcurrentHashMap

object AccountManager {

    val cache = ConcurrentHashMap<String, Account>()


    suspend fun fetchAccount(login: String): Account? {
        return NexusHub.blockingScope.async {
            cache.getOrPut(login) {
                val dao = AccountsTable.fetchAccount(login) ?: return@async null
                dao.toHighLevel()
            }
        }.await()
    }

    suspend fun newAccount(login: String, password: String, allowedScopes: Set<String>) {
        val encryptedPassword = PasswordEncryptor.encrypt(password, PasswordEncryptor.newSalt())
        val account = Account(login, encryptedPassword, allowedScopes)
        AccountsTable.newAccount(account)
        cache[account.login] = account
    }
}