package com.btelo.coding.util

/**
 * 统一的异常封装类
 */
sealed class AppException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    // JSON解析异常
    class JsonParseException(
        json: String,
        cause: Throwable
    ) : AppException("JSON解析失败: $json", cause)
    
    // 网络相关异常
    class NetworkException(
        message: String,
        cause: Throwable? = null
    ) : AppException(message, cause)
    
    // WebSocket相关异常
    class WebSocketException(
        message: String,
        cause: Throwable? = null
    ) : AppException(message, cause)
    
    // 加密相关异常
    class CryptoException(
        message: String,
        cause: Throwable? = null
    ) : AppException(message, cause)
    
    // 数据库相关异常
    class DatabaseException(
        message: String,
        cause: Throwable? = null
    ) : AppException(message, cause)
    
    // 密钥存储相关异常
    class KeyStoreException(
        message: String,
        cause: Throwable? = null
    ) : AppException(message, cause)
    
    // 认证相关异常
    class AuthException(
        message: String,
        cause: Throwable? = null
    ) : AppException(message, cause)
    
    // 未知异常
    class UnknownException(
        cause: Throwable
    ) : AppException("未知错误: ${cause.message}", cause)
    
    companion object {
        fun from(throwable: Throwable): AppException = when (throwable) {
            is AppException -> throwable
            is com.google.gson.JsonSyntaxException -> 
                JsonParseException(throwable.message ?: "", throwable)
            is java.net.UnknownHostException,
            is java.net.SocketTimeoutException,
            is java.io.IOException -> 
                NetworkException(throwable.message ?: "网络错误", throwable)
            else -> UnknownException(throwable)
        }
    }
}
