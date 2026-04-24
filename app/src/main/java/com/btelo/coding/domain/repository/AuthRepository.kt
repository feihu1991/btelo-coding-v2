package com.btelo.coding.domain.repository

import com.btelo.coding.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(serverAddress: String, username: String, password: String): Result<User>
    suspend fun logout()
    fun isLoggedIn(): Flow<Boolean>
    fun getCurrentUser(): Flow<User?>
    fun getServerAddress(): Flow<String?>
    fun getToken(): Flow<String?>
}
