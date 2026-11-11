package com.hhpc.smarttask.data.repository

import android.content.Context
import com.hhpc.smarttask.data.api.ApiClient
import com.hhpc.smarttask.data.local.TokenManager
import com.hhpc.smarttask.data.model.AuthResponse
import com.hhpc.smarttask.data.model.LoginRequest
import com.hhpc.smarttask.data.model.RegisterRequest

class AuthRepository(context: Context) {
    private val authApi = ApiClient.getAuthApi(context)
    val tokenManager = TokenManager(context.applicationContext)

    suspend fun login(email: String, pass: String): Result<AuthResponse> {
        return try {
            val response = authApi.login(LoginRequest(email, pass))
            if (response.isSuccessful && response.body() != null) {
                val authResp = response.body()!!
                authResp.token?.let { token ->
                    tokenManager.saveAuth(token, authResp.email, authResp.fullName, authResp.avatarUrl)
                }
                Result.success(authResp)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Email hoặc mật khẩu không chính xác."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, pass: String): Result<AuthResponse> {
        return try {
            val response = authApi.register(RegisterRequest(name, email, pass))
            if (response.isSuccessful && response.body() != null) {
                val authResp = response.body()!!
                authResp.token?.let { token ->
                    tokenManager.saveAuth(token, authResp.email, authResp.fullName, authResp.avatarUrl)
                }
                Result.success(authResp)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Đăng ký thất bại."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.clearAuth()
    }
}
