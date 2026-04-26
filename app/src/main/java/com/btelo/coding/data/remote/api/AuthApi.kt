package com.btelo.coding.data.remote.api

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

data class LoginResponse(
    val token: String,
    val userId: String,
    val username: String
)

data class DeviceRegisterApiRequest(
    val device_name: String,
    val device_type: String = "mobile"
)

data class DeviceRegisterApiResponse(
    val success: Boolean,
    val device_id: String,
    val pairing_code: String?,
    val token: String? = null,
    val message: String
)

data class PairingCodeApiResponse(
    val device_id: String,
    val pairing_code: String,
    val expires_at: String? = null
)

data class DeviceStatusApiResponse(
    val device_id: String,
    val paired: Boolean,
    val session_id: String?,
    val terminal_connected: Boolean = false
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val name: String
)

class AuthApi(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    suspend fun login(
        serverAddress: String,
        username: String,
        password: String
    ): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(mapOf("username" to username, "password" to password))
                val request = Request.Builder()
                    .url("$serverAddress/api/auth/login")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val loginResponse = gson.fromJson(body, LoginResponse::class.java)
                    Result.success(loginResponse)
                } else {
                    Result.failure(Exception("Login failed: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun register(
        serverAddress: String,
        username: String,
        password: String,
        name: String
    ): Result<LoginResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = RegisterRequest(
                    username = username,
                    password = password,
                    name = name
                )
                val json = gson.toJson(requestBody)
                val request = Request.Builder()
                    .url("$serverAddress/api/auth/register")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val registerResponse = gson.fromJson(body, LoginResponse::class.java)
                    Result.success(registerResponse)
                } else {
                    val errorBody = response.body?.string() ?: ""
                    Result.failure(Exception("注册失败: ${response.code} $errorBody"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun registerDevice(
        serverAddress: String,
        deviceName: String,
        deviceType: String = "mobile"
    ): Result<DeviceRegisterApiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = DeviceRegisterApiRequest(
                    device_name = deviceName,
                    device_type = deviceType
                )
                val json = gson.toJson(requestBody)
                val request = Request.Builder()
                    .url("$serverAddress/device/register")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val registerResponse = gson.fromJson(body, DeviceRegisterApiResponse::class.java)
                    Result.success(registerResponse)
                } else {
                    Result.failure(Exception("Device registration failed: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getPairingCode(
        serverAddress: String,
        deviceId: String
    ): Result<PairingCodeApiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$serverAddress/device/$deviceId/pairing-code")
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val pairingResponse = gson.fromJson(body, PairingCodeApiResponse::class.java)
                    Result.success(pairingResponse)
                } else {
                    Result.failure(Exception("Failed to get pairing code: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getDeviceStatus(
        serverAddress: String,
        deviceId: String
    ): Result<DeviceStatusApiResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$serverAddress/device/$deviceId/status")
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val statusResponse = gson.fromJson(body, DeviceStatusApiResponse::class.java)
                    Result.success(statusResponse)
                } else {
                    Result.failure(Exception("Failed to get device status: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
