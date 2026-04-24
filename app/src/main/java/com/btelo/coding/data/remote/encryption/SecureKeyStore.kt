package com.btelo.coding.data.remote.encryption

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.btelo.coding.util.AppException
import com.btelo.coding.util.Logger
import com.google.crypto.tink.subtle.X25519
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android Keystore 密钥存储管理器
 * 使用 Android Keystore 存储私钥，支持 API 23+
 */
@Singleton
class SecureKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = "SecureKeyStore"
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PRIVATE_KEY_ALIAS_PREFIX = "btelo_private_key_"
        private const val ENCRYPTED_KEY_ALIAS_PREFIX = "btelo_encrypted_key_"
        private const val AES_KEY_ALIAS = "btelo_aes_key"
        private const val AES_GCM_TAG_LENGTH = 128
        private const val AES_GCM_IV_LENGTH = 12
    }
    
    /**
     * 生成并存储新的密钥对
     */
    fun generateAndStoreKeyPair(sessionId: String): KeyPair {
        try {
            // 生成 X25519 密钥对
            val privateKeyBytes = X25519.generatePrivateKey()
            val publicKeyBytes = X25519.publicFromPrivate(privateKeyBytes)
            
            // 使用 Android Keystore 加密存储私钥
            val encryptedPrivateKey = encryptPrivateKeyWithKeystore(privateKeyBytes, sessionId)
            
            // 保存加密后的私钥
            saveEncryptedPrivateKey(sessionId, encryptedPrivateKey)
            
            Logger.i(tag, "为会话 $sessionId 生成并存储密钥对成功")
            
            return KeyPair(publicKeyBytes, privateKeyBytes)
        } catch (e: Exception) {
            Logger.e(tag, "生成密钥对失败: ${e.message}", e)
            throw AppException.KeyStoreException("生成密钥对失败", e)
        }
    }
    
    /**
     * 获取存储的密钥对
     */
    fun getKeyPair(sessionId: String): KeyPair? {
        return try {
            val encryptedPrivateKey = loadEncryptedPrivateKey(sessionId) ?: return null
            val privateKeyBytes = decryptPrivateKeyWithKeystore(encryptedPrivateKey, sessionId)
            val publicKeyBytes = X25519.publicFromPrivate(privateKeyBytes)
            
            KeyPair(publicKeyBytes, privateKeyBytes)
        } catch (e: Exception) {
            Logger.e(tag, "获取密钥对失败: ${e.message}", e)
            // 如果获取失败，删除旧的密钥并返回null
            deleteKeyPair(sessionId)
            null
        }
    }
    
    /**
     * 删除指定会话的密钥对
     */
    fun deleteKeyPair(sessionId: String) {
        try {
            val privateKeyAlias = getPrivateKeyAlias(sessionId)
            val encryptedKeyAlias = getEncryptedKeyAlias(sessionId)
            
            if (keyStore.containsAlias(privateKeyAlias)) {
                keyStore.deleteEntry(privateKeyAlias)
            }
            if (keyStore.containsAlias(encryptedKeyAlias)) {
                keyStore.deleteEntry(encryptedKeyAlias)
            }
            
            // 同时删除SharedPreferences中的加密密钥
            val prefs = context.getSharedPreferences("secure_keys", Context.MODE_PRIVATE)
            prefs.edit()
                .remove(privateKeyAlias)
                .remove(encryptedKeyAlias)
                .apply()
            
            Logger.i(tag, "删除会话 $sessionId 的密钥对成功")
        } catch (e: Exception) {
            Logger.e(tag, "删除密钥对失败: ${e.message}", e)
        }
    }
    
    /**
     * 检查是否有存储的密钥对
     */
    fun hasKeyPair(sessionId: String): Boolean {
        return loadEncryptedPrivateKey(sessionId) != null
    }
    
    /**
     * 使用 Android Keystore 中的 AES 密钥加密私钥
     */
    private fun encryptPrivateKeyWithKeystore(privateKey: ByteArray, sessionId: String): ByteArray {
        // 确保 AES 密钥存在
        val aesKey = getOrCreateAesKey()
        
        // 生成随机 IV
        val iv = ByteArray(AES_GCM_IV_LENGTH)
        java.security.SecureRandom().nextBytes(iv)
        
        // 使用 AES-GCM 加密
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, aesKey, gcmSpec)
        
        val encryptedData = cipher.doFinal(privateKey)
        
        // 合并 IV 和加密数据
        return iv + encryptedData
    }
    
    /**
     * 使用 Android Keystore 中的 AES 密钥解密私钥
     */
    private fun decryptPrivateKeyWithKeystore(encryptedData: ByteArray, sessionId: String): ByteArray {
        val aesKey = getOrCreateAesKey()
        
        // 分离 IV 和加密数据
        val iv = encryptedData.copyOfRange(0, AES_GCM_IV_LENGTH)
        val cipherText = encryptedData.copyOfRange(AES_GCM_IV_LENGTH, encryptedData.size)
        
        // 使用 AES-GCM 解密
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, aesKey, gcmSpec)
        
        return cipher.doFinal(cipherText)
    }
    
    /**
     * 获取或创建 AES 密钥（用于加密私钥）
     */
    private fun getOrCreateAesKey(): SecretKey {
        return try {
            keyStore.getKey(AES_KEY_ALIAS, null) as? SecretKey
        } catch (e: java.security.KeyStoreException) {
            null
        } ?: createAesKey()
    }
    
    /**
     * 创建 AES 密钥
     */
    private fun createAesKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        
        val keySpec = KeyGenParameterSpec.Builder(
            AES_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(false) // 简化处理，无需用户认证
            .build()
        
        keyGenerator.init(keySpec)
        
        return keyGenerator.generateKey()
    }
    
    /**
     * 保存加密后的私钥到 SharedPreferences
     */
    private fun saveEncryptedPrivateKey(sessionId: String, encryptedPrivateKey: ByteArray) {
        val keyAlias = getEncryptedKeyAlias(sessionId)
        val prefs = context.getSharedPreferences("secure_keys", Context.MODE_PRIVATE)
        
        prefs.edit()
            .putString(keyAlias, Base64.encodeToString(encryptedPrivateKey, Base64.NO_WRAP))
            .apply()
        
        // 同时在 Keystore 中记录元数据
        storeMetadataInKeystore(keyAlias, sessionId)
    }
    
    /**
     * 从 SharedPreferences 加载加密后的私钥
     */
    private fun loadEncryptedPrivateKey(sessionId: String): ByteArray? {
        val keyAlias = getEncryptedKeyAlias(sessionId)
        val prefs = context.getSharedPreferences("secure_keys", Context.MODE_PRIVATE)
        
        val encoded = prefs.getString(keyAlias, null) ?: return null
        return Base64.decode(encoded, Base64.NO_WRAP)
    }
    
    /**
     * 在 Keystore 中存储元数据
     */
    private fun storeMetadataInKeystore(alias: String, sessionId: String) {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            
            // 使用特殊构建器存储元数据（不影响密钥生成）
            val keySpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_SIGN
            )
                .setDigests(KeyProperties.DIGEST_NONE)
                .build()
            
            keyGenerator.init(keySpec)
            // 不实际使用这个密钥，只是为了在 Keystore 中记录会话ID映射
        } catch (e: Exception) {
            // 元数据存储失败不影响主要功能
            Logger.w(tag, "存储元数据失败: ${e.message}")
        }
    }
    
    private fun getPrivateKeyAlias(sessionId: String) = "$PRIVATE_KEY_ALIAS_PREFIX$sessionId"
    private fun getEncryptedKeyAlias(sessionId: String) = "$ENCRYPTED_KEY_ALIAS_PREFIX$sessionId"
}
