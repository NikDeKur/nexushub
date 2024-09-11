package dev.nikdekur.nexushub.protection.cert

import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.service.NexusHubService
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import java.io.File
import java.io.FileReader
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

class BCCertificatesService(
    override val app: NexusHubServer
) : NexusHubService(), CertificatesService {


    override fun createKeyStore(certFile: File, keyFile: File, alias: String): KeyStore {
        // Load private key from a PEM file
        val privateKey: PrivateKey = PEMParser(FileReader(keyFile)).use { pemParser ->
            val pemObject = pemParser.readObject() as PrivateKeyInfo
            JcaPEMKeyConverter().getPrivateKey(pemObject)
        }

        // Load certificate from a PEM file
        val certificate: X509Certificate = PEMParser(FileReader(certFile)).use { pemParser ->
            val pemObject = pemParser.readObject() as X509CertificateHolder
            JcaX509CertificateConverter().getCertificate(pemObject)
        }

        // Create a KeyStore and add the private key and certificate
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setKeyEntry(alias, privateKey, null, arrayOf(certificate))

        return keyStore
    }

}