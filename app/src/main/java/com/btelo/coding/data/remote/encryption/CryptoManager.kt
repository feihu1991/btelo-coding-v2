package com.btelo.coding.data.remote.encryption

import com.google.crypto.tink.subtle.ChaCha20Poly1305
import com.google.crypto.tink.subtle.Hkdf
import com.google.crypto.tink.subtle.X25519
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class CryptoManager {

    companion object {
        private const val HKDF_HASH_ALGO = "HMACSHA256"
        private const val HKDF_KEY_SIZE = 32
        private const val CHACHA20_KEY_SIZE = 32
        private const val HKDF_INFO = "btelo-coding-v1"
    }

    fun generateKeyPair(): KeyPair {
        val privateKeyBytes = X25519.generatePrivateKey()
        val publicKeyBytes = X25519.publicFromPrivate(privateKeyBytes)
        return KeyPair(publicKey = publicKeyBytes, privateKey = privateKeyBytes)
    }

    fun deriveSharedSecret(privateKey: ByteArray, publicKey: ByteArray): ByteArray {
        return X25519.computeSharedSecret(privateKey, publicKey)
    }
    
    /**
     * Derive a symmetric key from shared secret using HKDF
     * This follows NIST SP 800-56C recommendation for ECDH key derivation
     * 
     * @param sharedSecret The shared secret from ECDH key agreement
     * @param salt Optional salt (uses empty byte array if not provided)
     * @param info Context-specific information string
     * @return Derived key bytes of HKDF_KEY_SIZE length
     */
    fun deriveKeyWithHkdf(
        sharedSecret: ByteArray,
        salt: ByteArray = ByteArray(0),
        info: ByteArray = HKDF_INFO.toByteArray()
    ): ByteArray {
        return Hkdf.computeHkdf(
            HKDF_HASH_ALGO,
            sharedSecret,
            salt,
            info,
            HKDF_KEY_SIZE
        )
    }
    
    /**
     * Create a cipher using HKDF-derived key from shared secret
     * This replaces the direct use of sharedSecret as cipher key
     * 
     * @param sharedSecret The shared secret from ECDH
     * @param salt Optional salt for HKDF
     * @return ChaCha20Poly1305 cipher with HKDF-derived key
     */
    fun createCipherFromSharedSecret(sharedSecret: ByteArray, salt: ByteArray = ByteArray(0)): ChaCha20Poly1305 {
        val derivedKey = deriveKeyWithHkdf(sharedSecret, salt)
        return ChaCha20Poly1305(derivedKey)
    }

    /**
     * Derive a symmetric key from shared secret using HKDF and return as SecretKey
     */
    fun deriveSecretKey(
        sharedSecret: ByteArray,
        salt: ByteArray = ByteArray(0),
        info: ByteArray = HKDF_INFO.toByteArray()
    ): SecretKey {
        val keyBytes = deriveKeyWithHkdf(sharedSecret, salt, info)
        return SecretKeySpec(keyBytes, "ChaCha20")
    }


    fun encrypt(plaintext: ByteArray, cipher: ChaCha20Poly1305, associatedData: ByteArray = ByteArray(0)): ByteArray {
        return cipher.encrypt(plaintext, associatedData)
    }

    fun decrypt(ciphertext: ByteArray, cipher: ChaCha20Poly1305, associatedData: ByteArray = ByteArray(0)): ByteArray {
        return cipher.decrypt(ciphertext, associatedData)
    }
}
