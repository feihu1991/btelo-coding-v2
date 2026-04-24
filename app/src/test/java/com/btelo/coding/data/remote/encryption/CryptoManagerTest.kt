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
    fun `createCipherFromSharedSecret should create working cipher`() {
        val sharedSecret = ByteArray(32) { it.toByte() }
        val cipher = cryptoManager.createCipherFromSharedSecret(sharedSecret)

        val plaintext = "Hello, Yami Coding!"
        val encrypted = cryptoManager.encrypt(plaintext.toByteArray(), cipher)
        val decrypted = cryptoManager.decrypt(encrypted, cipher)

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

        val aliceCipher = cryptoManager.createCipherFromSharedSecret(aliceSharedSecret)
        val bobCipher = cryptoManager.createCipherFromSharedSecret(bobSharedSecret)

        val plaintext = "Hello, Yami Coding!"

        val encrypted = cryptoManager.encrypt(plaintext.toByteArray(), aliceCipher)
        val decrypted = cryptoManager.decrypt(encrypted, bobCipher)
        assertEquals(plaintext, String(decrypted))

        val encryptedByBob = cryptoManager.encrypt(plaintext.toByteArray(), bobCipher)
        val decryptedByAlice = cryptoManager.decrypt(encryptedByBob, aliceCipher)
        assertEquals(plaintext, String(decryptedByAlice))
    }
}
