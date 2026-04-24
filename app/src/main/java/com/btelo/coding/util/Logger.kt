package com.btelo.coding.util

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * 统一的日志记录工具
 */
object Logger {
    private const val TAG_PREFIX = "BTELO_"
    private val enabledTags = ConcurrentHashMap.newKeySet<String>()
    
    // 需要调试的标签，发布时可清空
    private val debugTags = setOf(
        "WebSocket",
        "Crypto",
        "Database",
        "Network"
    )
    
    fun init() {
        debugTags.forEach { tag ->
            enabledTags.add(tag)
        }
    }
    
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        if (isEnabled(tag)) {
            val fullTag = "$TAG_PREFIX$tag"
            if (throwable != null) {
                Log.d(fullTag, message, throwable)
            } else {
                Log.d(fullTag, message)
            }
        }
    }
    
    fun i(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = "$TAG_PREFIX$tag"
        if (throwable != null) {
            Log.i(fullTag, message, throwable)
        } else {
            Log.i(fullTag, message)
        }
    }
    
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = "$TAG_PREFIX$tag"
        if (throwable != null) {
            Log.w(fullTag, message, throwable)
        } else {
            Log.w(fullTag, message)
        }
    }
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = "$TAG_PREFIX$tag"
        if (throwable != null) {
            Log.e(fullTag, message, throwable)
        } else {
            Log.e(fullTag, message)
        }
    }
    
    private fun isEnabled(tag: String): Boolean {
        return enabledTags.contains(tag)
    }
    
    fun enableTag(tag: String) {
        enabledTags.add(tag)
    }
    
    fun disableTag(tag: String) {
        enabledTags.remove(tag)
    }
}
