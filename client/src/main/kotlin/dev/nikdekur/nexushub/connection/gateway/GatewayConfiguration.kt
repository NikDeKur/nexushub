package dev.nikdekur.nexushub.connection.gateway

data class GatewayConfiguration(
    val login: String,
    val password: String,
    val node: String
) {

    class Builder {
        var node: String? = null
        var login: String? = null
        var password: String? = null

        fun build(): GatewayConfiguration {
            checkNotNull(login) { "Login is not set" }
            checkNotNull(password) { "Password is not set" }
            checkNotNull(node) { "Node is not set" }

            return GatewayConfiguration(login!!, password!!, node!!)
        }
    }
}