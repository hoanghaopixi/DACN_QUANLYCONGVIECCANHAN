package com.hhpc.smarttask.data.api

import com.hhpc.smarttask.data.model.AuthResponse
import com.hhpc.smarttask.data.model.GoogleLoginRequest
import com.hhpc.smarttask.data.model.LoginRequest
import com.hhpc.smarttask.data.model.RegisterRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part

data class UpdateProfileRequest(
    val fullName: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class CreatePasswordRequest(
    val newPassword: String
)

data class SendRegisterOtpRequest(
    val fullName: String,
    val email: String
)

data class SendOtpRequest(
    val email: String
)

data class VerifyOtpRequest(
    val email: String,
    val otp: String
)

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val newPassword: String
)

interface AuthApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/register/send-otp")
    suspend fun sendRegisterOtp(@Body request: SendRegisterOtpRequest): Response<Map<String, Any>>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<AuthResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: SendOtpRequest): Response<Map<String, Any>>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<Map<String, Any>>

    @POST("auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<Map<String, Any>>

    @GET("auth/me")
    suspend fun getProfile(): Response<AuthResponse>

    @PUT("auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<AuthResponse>

    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<Map<String, Any>>

    @POST("auth/create-password")
    suspend fun createPassword(@Body request: CreatePasswordRequest): Response<Map<String, Any>>

    @POST("auth/verify-password")
    suspend fun verifyPassword(@Body body: Map<String, String>): Response<Map<String, Any>>

    @Multipart
    @POST("upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): Response<Map<String, Any>>

    @Multipart
    @POST("auth/upload-avatar")
    suspend fun uploadAvatar(
        @Part file: MultipartBody.Part
    ): Response<Map<String, Any>>
}
