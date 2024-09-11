package dev.nikdekur.nexushub.protection.cert

import java.io.File
import java.security.KeyStore

/**
 * # Certificates Service
 */
fun interface CertificatesService {

    /**
     * Creates a key store from a certificate and a key files with an alias.
     *
     * KeyStore is used to store keys and certificates.
     *
     * @param certFile The certificate file.
     * @param keyFile The key file.
     * @param alias The alias of the key.
     * @return The created key store.
     */
    fun createKeyStore(certFile: File, keyFile: File, alias: String): KeyStore
}