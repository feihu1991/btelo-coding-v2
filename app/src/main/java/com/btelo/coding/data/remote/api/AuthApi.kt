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

data class DeviceRegisterRequest(
    val device_name: String,
    val device_type: String = "mobile"
)

data class DeviceRegisterResponse(
    val success: Boolean,
    val device_id: String,
    val pairing_code: String?,
    val message: String
)

data class PairingCodeResponse(
    val device_id: String,
    val pairing_code: String,
    val expires_at: String? = null
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

    suspend fun registerDevice(
        serverAddress: String,
        deviceName: String,
        deviceType: String = "mobile"
    ): Result<DeviceRegisterResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = DeviceRegisterRequest(
                    device_name = deviceName,
                    device_type = deviceType
                )
                val json = gson.toJson(requestBody)
                val request = Request.Builder()
                    .url("$serverAddress/api/device/register")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val registerResponse = gson.fromJson(body, DeviceRegisterResponse::class.java)
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
    ): Result<PairingCodeResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$serverAddress/api/device/$deviceId/pairing-code")
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val pairingResponse = gson.fromJson(body, PairingCodeResponse::class.java)
                    Result.success(pairingResponse)
                } else {
                    Result.failure(Exception("Failed to get pairing code: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
