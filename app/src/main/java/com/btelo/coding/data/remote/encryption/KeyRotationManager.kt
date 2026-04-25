package com.btelo.coding.data.remote.encryption

import android.content.Context
import android.util.Base64
import com.btelo.coding.util.AppException
import com.btelo.coding.util.Logger
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 密钥轮换状态
 */
sealed class KeyRotationState {
    object Idle : KeyRotationState()
    data class Rotating(val newKeyVersion: Int) : KeyRotationState()
    data class Completed(val previousVersion: Int, val newVersion: Int) : KeyRotationState()
    data class Error(val message: String) : KeyRotationState()
}

/**
 * 密钥版本信息
 * 注意：私钥使用后应及时清零以减少内存暴露风险
 */
data class KeyVersion(
    val version: Int,
    val createdAt: Long,
    val publicKey: ByteArray,
    val privateKey: ByteArray, // 仅本地存储，用于解密历史消息
    val isActive: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KeyVersion
        return version == other.version && createdAt == other.createdAt &&
               publicKey.contentEquals(other.publicKey) && privateKey.contentEquals(other.privateKey)
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + privateKey.contentHashCode()
        return result
    }

    /**
     * 安全清零私钥字节数组
     * 在密钥不再需要时调用以减少内存暴露风险
     */
    fun clearPrivateKey() {
        privateKey.fill(0)
    }
}

/**
 * 密钥轮换握手消息
 */
data class KeyRotationMessage(
    val action: String, // "initiate" | "accept" | "complete"
    val newPublicKey: String, // Base64编码
    val keyVersion: Int,
    val timestamp: Long
)

/**
 * 密钥轮换管理器
 * 实现前向保密（Forward Secrecy）机制
 * 
 * 功能：
 * 1. 管理多个密钥版本，支持历史密钥保留
 * 2. 支持定时或手动触发密钥轮换
 * 3. 在密钥轮换时进行 ECDH 握手协商
 * 4. 保留历史密钥用于解密旧消息
 */
@Singleton
class KeyRotationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureKeyStore: SecureKeyStore,
    private val cryptoManager: CryptoManager,
    private val gson: Gson
) {
    private val tag = "KeyRotationManager"
    private val prefs = context.getSharedPreferences("key_rotation", Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_CURRENT_VERSION = "current_key_version"
        private const val PREFS_LAST_ROTATION = "last_rotation_timestamp"
        private const val PREFS_ROTATION_INTERVAL_DAYS = "rotation_interval_days"
        private const val PREFS_HISTORY_PREFIX = "key_history_"
        
        private const val DEFAULT_ROTATION_INTERVAL_DAYS = 7 // 默认每周轮换
        private const val MAX_KEY_HISTORY = 5 // 最多保留5个历史密钥
        
        private val secureRandom = SecureRandom()
    }
    
    private val _rotationState = MutableStateFlow<KeyRotationState>(KeyRotationState.Idle)
    val rotationState: StateFlow<KeyRotationState> = _rotationState.asStateFlow()
    
    // 当前会话的密钥版本映射（线程安全）
    private val sessionKeyVersions = ConcurrentHashMap<String, MutableList<KeyVersion>>()

    // 当前活跃密钥版本（线程安全）
    private val currentVersions = ConcurrentHashMap<String, KeyVersion>()
    
    /**
     * 初始化会话的密钥版本管理
     */
    fun initializeSession(sessionId: String): KeyVersion {
        // 检查是否有现有密钥版本
        val existingVersions = loadKeyVersions(sessionId)
        
        if (existingVersions.isNotEmpty()) {
            val currentVersion = existingVersions.first()
            sessionKeyVersions[sessionId] = existingVersions.toMutableList()
            currentVersions[sessionId] = currentVersion
            return currentVersion
        }
        
        // 生成新的初始密钥版本
        return createNewKeyVersion(sessionId)
    }
    
    /**
     * 创建新的密钥版本
     */
    fun createNewKeyVersion(sessionId: String): KeyVersion {
        val keyPair = cryptoManager.generateKeyPair()
        val newVersion = getNextVersion(sessionId)
        val now = System.currentTimeMillis()
        
        val keyVersion = KeyVersion(
            version = newVersion,
            createdAt = now,
            publicKey = keyPair.publicKey,
            privateKey = keyPair.privateKey,
            isActive = true
        )
        
        // 保存到内存（线程安全）
        sessionKeyVersions.putIfAbsent(sessionId, mutableListOf())
        sessionKeyVersions[sessionId]?.add(0, keyVersion)
        currentVersions[sessionId] = keyVersion
        
        // 更新持久化存储
        saveKeyVersions(sessionId)
        prefs.edit()
            .putInt(PREFS_CURRENT_VERSION, newVersion)
            .putLong(PREFS_LAST_ROTATION, now)
            .apply()
        
        Logger.i(tag, "为会话 $sessionId 创建新密钥版本 v$newVersion")
        return keyVersion
    }
    
    /**
     * 获取当前活跃密钥版本
     */
    fun getCurrentVersion(sessionId: String): KeyVersion? {
        return currentVersions[sessionId]
    }
    
    /**
     * 获取所有密钥版本（用于解密历史消息）
     */
    fun getAllVersions(sessionId: String): List<KeyVersion> {
        return sessionKeyVersions[sessionId] ?: emptyList()
    }
    
    /**
     * 解密消息（尝试所有密钥版本）
     */
    fun decryptWithHistory(sessionId: String, ciphertext: ByteArray, keyVersionHint: Int?): ByteArray? {
        val versions = sessionKeyVersions[sessionId] ?: return null
        
        // 如果有版本提示，优先尝试该版本
        if (keyVersionHint != null) {
            val hintVersion = versions.find { it.version == keyVersionHint }
            if (hintVersion != null) {
                return tryDecryptWithKey(ciphertext, hintVersion)
            }
        }
        
        // 否则尝试所有版本
        for (version in versions) {
            val result = tryDecryptWithKey(ciphertext, version)
            if (result != null) return result
        }
        
        return null
    }
    
    /**
     * 尝试使用指定密钥版本解密
     */
    private fun tryDecryptWithKey(ciphertext: ByteArray, keyVersion: KeyVersion): ByteArray? {
        return try {
            val sharedSecret = cryptoManager.deriveSharedSecret(
                keyVersion.privateKey,
                getRemotePublicKey(keyVersion.version)
            )
            val cipher = cryptoManager.createCipherFromSharedSecret(sharedSecret)
            cryptoManager.decrypt(ciphertext, cipher)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 获取远程公钥（需要从消息中获取，这里用版本号作为占位）
     * 实际使用时需要传入远程公钥
     */
    private fun getRemotePublicKey(version: Int): ByteArray {
        // 这里需要根据实际实现调整
        // 在实际使用中，远程公钥应该从密钥交换消息中获取
        return ByteArray(32) { version.toByte() }
    }
    
    /**
     * 生成密钥轮换握手消息
     */
    fun generateRotationHandshake(sessionId: String): KeyRotationMessage {
        val newVersion = createNewKeyVersion(sessionId)
        _rotationState.value = KeyRotationState.Rotating(newVersion.version)
        
        return KeyRotationMessage(
            action = "initiate",
            newPublicKey = Base64.encodeToString(newVersion.publicKey, Base64.NO_WRAP),
            keyVersion = newVersion.version,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * 处理密钥轮换握手消息
     */
    fun handleRotationHandshake(sessionId: String, message: KeyRotationMessage): KeyRotationMessage? {
        return try {
            when (message.action) {
                "initiate" -> {
                    // 收到轮换请求，创建自己的新密钥并响应
                    val myNewVersion = createNewKeyVersion(sessionId)
                    _rotationState.value = KeyRotationState.Rotating(myNewVersion.version)
                    
                    KeyRotationMessage(
                        action = "accept",
                        newPublicKey = Base64.encodeToString(myNewVersion.publicKey, Base64.NO_WRAP),
                        keyVersion = myNewVersion.version,
                        timestamp = System.currentTimeMillis()
                    )
                }
                "accept" -> {
                    // 收到确认消息，密钥轮换完成
                    val currentVersion = currentVersions[sessionId]
                    if (currentVersion != null) {
                        _rotationState.value = KeyRotationState.Completed(
                            previousVersion = currentVersion.version,
                            newVersion = message.keyVersion
                        )
                    }
                    
                    KeyRotationMessage(
                        action = "complete",
                        newPublicKey = "",
                        keyVersion = message.keyVersion,
                        timestamp = System.currentTimeMillis()
                    )
                }
                "complete" -> {
                    // 轮换完成确认
                    val currentVersion = currentVersions[sessionId]
                    if (currentVersion != null) {
                        _rotationState.value = KeyRotationState.Completed(
                            previousVersion = currentVersion.version,
                            newVersion = message.keyVersion
                        )
                    }
                    null
                }
                else -> null
            }
        } catch (e: Exception) {
            Logger.e(tag, "处理密钥轮换握手失败", e)
            _rotationState.value = KeyRotationState.Error(e.message ?: "密钥轮换失败")
            null
        }
    }
    
    /**
     * 检查是否应该触发密钥轮换
     */
    fun shouldRotate(sessionId: String): Boolean {
        val intervalDays = prefs.getInt(PREFS_ROTATION_INTERVAL_DAYS, DEFAULT_ROTATION_INTERVAL_DAYS)
        val lastRotation = prefs.getLong(PREFS_LAST_ROTATION, 0)
        val now = System.currentTimeMillis()
        
        val intervalMs = intervalDays * 24 * 60 * 60 * 1000L
        return (now - lastRotation) >= intervalMs
    }
    
    /**
     * 设置密钥轮换间隔
     */
    fun setRotationInterval(sessionId: String, days: Int) {
        prefs.edit().putInt(PREFS_ROTATION_INTERVAL_DAYS, days).apply()
    }
    
    /**
     * 获取下一个版本号
     */
    private fun getNextVersion(sessionId: String): Int {
        val versions = sessionKeyVersions[sessionId]
        return (versions?.maxOfOrNull { it.version } ?: 0) + 1
    }
    
    /**
     * 保存密钥版本到持久化存储
     */
    private fun saveKeyVersions(sessionId: String) {
        val versions = sessionKeyVersions[sessionId] ?: return
        
        // 只保存必要的元数据，不保存私钥（私钥已由SecureKeyStore管理）
        val metadata = versions.map { v ->
            mapOf(
                "version" to v.version,
                "createdAt" to v.createdAt,
                "publicKey" to Base64.encodeToString(v.publicKey, Base64.NO_WRAP),
                "isActive" to v.isActive
            )
        }
        
        val json = gson.toJson(metadata)
        prefs.edit().putString(PREFS_HISTORY_PREFIX + sessionId, json).apply()
    }
    
    /**
     * 从持久化存储加载密钥版本
     */
    private fun loadKeyVersions(sessionId: String): List<KeyVersion> {
        val json = prefs.getString(PREFS_HISTORY_PREFIX + sessionId, null) ?: return emptyList()
        
        return try {
            val type = object : TypeToken<Array<Map<String, Any>>>() {}.type
            @Suppress("UNCHECKED_CAST")
            val metadataList = gson.fromJson<Array<Map<String, Any>>>(json, type)
            metadataList.mapNotNull { metadata ->
                val version = (metadata["version"] as Double).toInt()
                val createdAt = (metadata["createdAt"] as Double).toLong()
                val publicKey = Base64.decode(metadata["publicKey"] as String, Base64.NO_WRAP)
                val isActive = metadata["isActive"] as? Boolean ?: true
                
                // 从SecureKeyStore获取私钥
                val keyPair = secureKeyStore.getKeyPair(sessionId)
                if (keyPair != null) {
                    KeyVersion(
                        version = version,
                        createdAt = createdAt,
                        publicKey = publicKey,
                        privateKey = keyPair.privateKey,
                        isActive = isActive
                    )
                } else null
            }
        } catch (e: Exception) {
            Logger.e(tag, "加载密钥版本失败", e)
            emptyList()
        }
    }
    
    /**
     * 清理过期的密钥版本
     */
    fun cleanupOldVersions(sessionId: String) {
        val versions = sessionKeyVersions[sessionId] ?: return
        if (versions.size > MAX_KEY_HISTORY) {
            val toRemove = versions.drop(MAX_KEY_HISTORY)
            // 安全清零被删除的私钥
            toRemove.forEach { it.clearPrivateKey() }
            sessionKeyVersions[sessionId] = versions.take(MAX_KEY_HISTORY).toMutableList()
            saveKeyVersions(sessionId)
            Logger.i(tag, "清理了 ${toRemove.size} 个过期密钥版本")
        }
    }
    
    /**
     * 导出密钥轮换状态为JSON
     */
    fun exportRotationState(sessionId: String): String {
        val json = JsonObject()
        json.addProperty("currentVersion", currentVersions[sessionId]?.version ?: 0)
        json.addProperty("lastRotation", prefs.getLong(PREFS_LAST_ROTATION, 0))
        json.addProperty("rotationIntervalDays", prefs.getInt(PREFS_ROTATION_INTERVAL_DAYS, DEFAULT_ROTATION_INTERVAL_DAYS))
        json.addProperty("historyCount", sessionKeyVersions[sessionId]?.size ?: 0)
        return gson.toJson(json)
    }
}
