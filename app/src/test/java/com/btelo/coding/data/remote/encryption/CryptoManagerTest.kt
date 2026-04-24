package com.btelo.coding.data.remote.encryption

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CryptoManagerTest {

    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setup() {
        cryptoManager = CryptoManager()
    }

    @Test
    fun `generateKeyPair should return non-null keys`() {
        val keyPair = cryptoManager.generateKeyPair()
        assertNotNull(keyPair.publicKey)
        assertNotNull(keyPair.privateKey)
    }

    @Test
    fun `createAeadFromSharedSecret should create working AEAD`() {
        val sharedSecret = ByteArray(32) { it.toByte() }
        val aead = cryptoManager.createAeadFromSharedSecret(sharedSecret)

        val plaintext = "Hello, BTELO Coding!"
        val encrypted = cryptoManager.encrypt(plaintext.toByteArray(), aead)
        val decrypted = cryptoManager.decrypt(encrypted, aead)

        assertEquals(plaintext, String(decrypted))
    }

    @Test
    fun `E2EE key derivation works correctly on both sides`() {
        val aliceKeyPair = cryptoManager.generateKeyPair()
        val bobKeyPair = cryptoManager.generateKeyPair()

        val aliceSharedSecret = cryptoManager.deriveSharedSecret(
            aliceKeyPair.privateKey, bobKeyPair.publicKey
        )
        val bobSharedSecret = cryptoManager.deriveSharedSecret(
            bobKeyPair.privateKey, aliceKeyPair.publicKey
        )

        assertArrayEquals(aliceSharedSecret, bobSharedSecret)

        val aliceAead = cryptoManager.createAeadFromSharedSecret(aliceSharedSecret)
        val bobAead = cryptoManager.createAeadFromSharedSecret(bobSharedSecret)

        val plaintext = "Hello, BTELO Coding!"

        val encrypted = cryptoManager.encrypt(plaintext.toByteArray(), aliceAead)
        val decrypted = cryptoManager.decrypt(encrypted, bobAead)
        assertEquals(plaintext, String(decrypted))

        val encryptedByBob = cryptoManager.encrypt(plaintext.toByteArray(), bobAead)
        val decryptedByAlice = cryptoManager.decrypt(encryptedByBob, aliceAead)
        assertEquals(plaintext, String(decryptedByAlice))
    }
}
