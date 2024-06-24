package org.ndk.nexushub.client.connection

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.ndk.nexushub.client.connection.retry.LinearRetry
import org.ndk.nexushub.client.connection.retry.Retry
import kotlin.time.Duration.Companion.seconds

data class ConnectionConfiguration(
    val gatewayConfiguration: GatewayConfiguration,
    val node: String,
    val login: String,
    val password: String,
) {
    class Builder {
        var host: String? = null
        var port: Int? = null
        var node: String? = null
        var login: String? = null
        var password: String? = null
        var retry: Retry = LinearRetry(2.seconds, 30.seconds, 10)
        var dispatcher: CoroutineDispatcher = Dispatchers.IO

        fun environment(): Builder {
            node = getEnv("NEXUSHUB_NODE")
            host = getEnv("NEXUSHUB_HOST")
            val portStr = getEnv("NEXUSHUB_PORT")
            port = portStr.toIntOrNull() ?: error("Environment-specified port ($portStr) is not a number")
            login = getEnv("NEXUSHUB_LOGIN")
            password = getEnv("NEXUSHUB_PASSWORD")
            return this
        }

        fun build(): ConnectionConfiguration {
            checkNotNull(host) { "Host is not set" }
            checkNotNull(port) { "Port is not set" }
            checkNotNull(node) { "Node is not set" }
            checkNotNull(login) { "Login is not set" }
            checkNotNull(password) { "Password is not set" }

            val gateway = GatewayConfiguration(host!!, port!!, retry, dispatcher)

            return ConnectionConfiguration(gateway, node!!, login!!, password!!)
        }
    }


    companion object {

        private fun getEnv(name: String): String {
            return System.getenv(name) ?: error("Environment setup require variables: " +
                    "[NEXUSHUB_NODE, NEXUSHUB_HOST, NEXUSHUB_PORT, NEXUSHUB_LOGIN, NEXUSHUB_PASSWORD], " +
                    "but $name is missing"
            )
        }
    }
}
