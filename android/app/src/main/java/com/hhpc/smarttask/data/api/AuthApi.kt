package com.hhpc.smarttask.data.api

import com.hhpc.smarttask.data.model.AuthResponse
import com.hhpc.smarttask.data.model.LoginRequest
import com.hhpc.smarttask.data.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getProfile(): Response<AuthResponse>
}
