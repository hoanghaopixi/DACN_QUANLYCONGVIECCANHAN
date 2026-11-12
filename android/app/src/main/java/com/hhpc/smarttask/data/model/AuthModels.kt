package com.hhpc.smarttask.data.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val otp: String? = null
)

data class AuthResponse(
    val token: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val role: String? = null,
    val avatarUrl: String? = null,
    val phone: String? = null
)

data class GoogleLoginRequest(
    val idToken: String? = null,
    val email: String,
    val fullName: String? = null,
    val avatarUrl: String? = null
)
