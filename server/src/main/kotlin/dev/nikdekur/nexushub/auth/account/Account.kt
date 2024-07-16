package dev.nikdekur.nexushub.auth.account

import dev.nikdekur.nexushub.auth.password.EncryptedPassword
import dev.nikdekur.nexushub.database.account.AccountDAO
import dev.nikdekur.nexushub.database.account.AccountsTable

data class Account(
    var dao: AccountDAO,
    val login: String,
    val password: EncryptedPassword,
    val allowedScopes: MutableSet<String>
) {


    suspend fun updateScopes() {
        dao = dao.copy(allowedScopes = allowedScopes)
        AccountsTable.updateAccount(dao)
    }

    suspend fun allowScope(scope: String) {
        allowedScopes.add(scope)
        updateScopes()
    }

    suspend fun removeScope(scope: String) {
        allowedScopes.remove(scope)
        updateScopes()
    }

    suspend fun clearScopes() {
        allowedScopes.clear()
        updateScopes()
    }
}