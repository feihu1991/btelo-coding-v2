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
}
