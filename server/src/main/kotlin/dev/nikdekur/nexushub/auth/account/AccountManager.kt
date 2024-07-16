package dev.nikdekur.nexushub.auth.account

import dev.nikdekur.nexushub.auth.password.PasswordEncryptor
import dev.nikdekur.nexushub.database.account.AccountDAO
import dev.nikdekur.nexushub.database.account.AccountsTable
import java.util.concurrent.ConcurrentHashMap

object AccountManager {

    suspend fun init() {
        AccountsTable.init()
        AccountsTable.fetchAllAccounts().forEach {
            val account = it.toHighLevel()
            accounts[account.login] = account
        }
    }

    val accounts = ConcurrentHashMap<String, Account>()

    fun getAccount(login: String): Account? {
        return accounts[login]
    }

    suspend fun newAccount(dao: AccountDAO): Account {
        AccountsTable.newAccount(dao)
        val account = dao.toHighLevel()
        accounts[account.login] = account
        return account
    }

    suspend fun newAccount(login: String, password: String, allowedScopes : Set<String>): Account {
        val encryptedPassword = PasswordEncryptor.encryptNew(password)
        val dao = AccountDAO(
            login = login,
            password = encryptedPassword.hex,
            salt = encryptedPassword.salt.hex,
            allowedScopes = allowedScopes
        )
        return newAccount(dao)
    }


    /**
     * Deletes an account from the database and the cache.
     *
     * Note that method will not disconnect clients that are currently using the account.
     *
     * @param login The login of the account to delete.
     */
    suspend fun deleteAccount(login: String) {
        AccountsTable.deleteAccount(login)
        accounts.remove(login)
    }
}