package com.btelo.coding.data.remote.encryption

import com.google.crypto.tink.subtle.ChaCha20Poly1305
import com.google.crypto.tink.subtle.X25519

class CryptoManager {

    fun generateKeyPair(): KeyPair {
        val privateKeyBytes = X25519.generatePrivateKey()
        val publicKeyBytes = X25519.publicFromPrivate(privateKeyBytes)
        return KeyPair(publicKey = publicKeyBytes, privateKey = privateKeyBytes)
    }

    fun deriveSharedSecret(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        return X25519.computeSharedSecret(privateKey, publicKey)
    }

    fun createCipherFromSharedSecret(sharedSecret: ByteArray): ChaCha20Poly1305 {
        val keyBytes = sharedSecret.take(32).toByteArray()
        return ChaCha20Poly1305(keyBytes)
    }

    fun encrypt(plaintext: ByteArray, cipher: ChaCha20Poly1305, associatedData: ByteArray = ByteArray(0)): ByteArray {
        return cipher.encrypt(plaintext, associatedData)
    }

    fun decrypt(ciphertext: ByteArray, cipher: ChaCha20Poly1305, associatedData: ByteArray = ByteArray(0)): ByteArray {
        return cipher.decrypt(ciphertext, associatedData)
    }
}
