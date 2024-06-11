package org.ndk.nexushub.auth.account

import org.ndk.nexushub.auth.password.EncryptedPassword
import org.ndk.nexushub.database.account.AccountDAO
import org.ndk.nexushub.database.account.AccountsTable

data class Account(
    var dao: AccountDAO,
    val login: String,
    val password: EncryptedPassword,
    val allowedScopes: MutableSet<String>
) {

    suspend fun allowScope(scope: String) {
        allowedScopes.add(scope)
        dao = dao.copy(allowedScopes = allowedScopes)
        AccountsTable.updateAccount(dao)
    }
}