package com.btelo.coding.data.remote.encryption

import com.google.crypto.tink.Aead
import com.google.crypto.tink.InsecureSecretKeyAccess
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.SecretBytes
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.ChaCha20Poly1305Key
import com.google.crypto.tink.subtle.X25519

class CryptoManager {

    init {
        AeadConfig.register()
    }

    fun generateKeyPair(): KeyPair {
        val privateKeyBytes = X25519.generatePrivateKey()
        val publicKeyBytes = X25519.publicFromPrivate(privateKeyBytes)
        return KeyPair(publicKey = publicKeyBytes, privateKey = privateKeyBytes)
    }

    fun deriveSharedSecret(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        return X25519.computeSharedSecret(privateKey, publicKey)
    }

    fun createAeadFromSharedSecret(sharedSecret: ByteArray): Aead {
        val keyBytes = sharedSecret.take(32).toByteArray()
        val secretBytes = SecretBytes.copyFrom(keyBytes, InsecureSecretKeyAccess.get())
        val key = ChaCha20Poly1305Key.create(secretBytes)
        val keysetHandle = KeysetHandle.importKey(key).build()
        return keysetHandle.getPrimitive(Aead::class.java)
    }

    fun encrypt(plaintext: ByteArray, aead: Aead, associatedData: ByteArray = ByteArray(0)): ByteArray {
        return aead.encrypt(plaintext, associatedData)
    }

    fun decrypt(ciphertext: ByteArray, aead: Aead, associatedData: ByteArray = ByteArray(0)): ByteArray {
        return aead.decrypt(ciphertext, associatedData)
    }
}
