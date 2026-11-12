package com.hhpc.smarttask.data.repository

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.hhpc.smarttask.data.api.ApiClient
import com.hhpc.smarttask.data.api.ChangePasswordRequest
import com.hhpc.smarttask.data.api.CreatePasswordRequest
import com.hhpc.smarttask.data.api.UpdateProfileRequest
import com.hhpc.smarttask.data.local.TokenManager
import com.hhpc.smarttask.data.model.AuthResponse
import com.hhpc.smarttask.data.model.LoginRequest
import com.hhpc.smarttask.data.model.RegisterRequest
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class AuthRepository(context: Context) {
    private val authApi = ApiClient.getAuthApi(context)
    val tokenManager = TokenManager(context.applicationContext)
    private val credentialManager = CredentialManager.create(context.applicationContext)

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
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(
                    response,
                    "Email hoặc mật khẩu không chính xác. Vui lòng thử lại."
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(
                e,
                "Không thể kết nối đến máy chủ. Vui lòng kiểm tra mạng."
            )
            Result.failure(Exception(msg))
        }
    }

    suspend fun sendRegisterOtp(fullName: String, email: String): Result<String> {
        return try {
            val response = authApi.sendRegisterOtp(com.hhpc.smarttask.data.api.SendRegisterOtpRequest(fullName, email))
            if (response.isSuccessful && response.body() != null) {
                val msg = response.body()!!["message"]?.toString() ?: "Mã OTP đã được gửi đến email của bạn!"
                Result.success(msg)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(
                    response,
                    "Không thể gửi mã OTP. Vui lòng kiểm tra lại họ tên và email."
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Không thể kết nối đến máy chủ.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun register(name: String, email: String, pass: String, otp: String? = null): Result<AuthResponse> {
        return try {
            val response = authApi.register(RegisterRequest(name, email, pass, otp))
            if (response.isSuccessful && response.body() != null) {
                val authResp = response.body()!!
                authResp.token?.let { token ->
                    tokenManager.saveAuth(token, authResp.email, authResp.fullName, authResp.avatarUrl)
                }
                Result.success(authResp)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(
                    response,
                    "Mã OTP không chính xác hoặc đã hết hạn."
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(
                e,
                "Không thể kết nối đến máy chủ. Vui lòng kiểm tra mạng."
            )
            Result.failure(Exception(msg))
        }
    }

    suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val response = authApi.forgotPassword(com.hhpc.smarttask.data.api.SendOtpRequest(email))
            if (response.isSuccessful && response.body() != null) {
                val msg = response.body()!!["message"]?.toString() ?: "Mã OTP khôi phục đã được gửi đến email!"
                Result.success(msg)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(
                    response,
                    "Email không tồn tại trên hệ thống."
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Không thể kết nối đến máy chủ.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun verifyOtp(email: String, otp: String): Result<Boolean> {
        return try {
            val response = authApi.verifyOtp(com.hhpc.smarttask.data.api.VerifyOtpRequest(email, otp))
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(
                    response,
                    "Mã OTP không chính xác hoặc đã hết hạn."
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Không thể kết nối đến máy chủ.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun resetPassword(email: String, otp: String, newPass: String): Result<String> {
        return try {
            val response = authApi.resetPassword(com.hhpc.smarttask.data.api.ResetPasswordRequest(email, otp, newPass))
            if (response.isSuccessful && response.body() != null) {
                val msg = response.body()!!["message"]?.toString() ?: "Đặt lại mật khẩu thành công!"
                Result.success(msg)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(
                    response,
                    "Mã OTP không chính xác hoặc không thể đổi mật khẩu."
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(e, "Không thể kết nối đến máy chủ.")
            Result.failure(Exception(msg))
        }
    }

    suspend fun loginWithGoogle(
        email: String,
        fullName: String? = null,
        idToken: String? = null,
        avatarUrl: String? = null
    ): Result<AuthResponse> {
        return try {
            val response = authApi.loginWithGoogle(
                com.hhpc.smarttask.data.model.GoogleLoginRequest(
                    idToken = idToken,
                    email = email,
                    fullName = fullName,
                    avatarUrl = avatarUrl
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val authResp = response.body()!!
                authResp.token?.let { token ->
                    tokenManager.saveAuth(token, authResp.email, authResp.fullName, authResp.avatarUrl ?: avatarUrl)
                }
                Result.success(authResp)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(
                    response,
                    "Xác thực tài khoản Google không thành công. Vui lòng thử lại."
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(
                e,
                "Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng."
            )
            Result.failure(Exception(msg))
        }
    }

    suspend fun getProfile(): Result<AuthResponse> {
        return try {
            val response = authApi.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val authResp = response.body()!!
                authResp.token?.let { token ->
                    val currentAvatar = tokenManager.userAvatarFlow.firstOrNull()
                    tokenManager.saveAuth(token, authResp.email, authResp.fullName, authResp.avatarUrl ?: currentAvatar)
                }
                Result.success(authResp)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(
                    response,
                    "Không thể tải thông tin hồ sơ."
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(
                e,
                "Không thể kết nối đến máy chủ."
            )
            Result.failure(Exception(msg))
        }
    }

    suspend fun uploadAvatarFile(file: File): Result<String> {
        return try {
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            var response = try { authApi.uploadAvatar(body) } catch (e: Exception) { null }
            if (response == null || !response.isSuccessful) {
                response = try { authApi.uploadFile(body) } catch (e: Exception) { null }
            }

            if (response != null && response.isSuccessful && response.body() != null) {
                val map = response.body()!!
                val url = map["url"]?.toString()
                    ?: map["path"]?.toString()
                    ?: map["fileUrl"]?.toString()
                    ?: map["avatarUrl"]?.toString()
                    ?: "/uploads/avatars/${file.name}"
                Result.success(url)
            } else {
                val serverRelativePath = "/uploads/avatars/${file.name}"
                Result.success(serverRelativePath)
            }
        } catch (e: Exception) {
            val serverRelativePath = "/uploads/avatars/${file.name}"
            Result.success(serverRelativePath)
        }
    }

    suspend fun updateProfile(fullName: String?, phone: String?, avatarUrl: String?): Result<AuthResponse> {
        return try {
            // Process local Android file path into backend upload relative URL!
            var serverAvatarUrl = avatarUrl
            if (!avatarUrl.isNullOrBlank() && avatarUrl.startsWith("/")) {
                val file = File(avatarUrl)
                if (file.exists()) {
                    val uploadResult = uploadAvatarFile(file)
                    uploadResult.onSuccess { uploadedUrl ->
                        serverAvatarUrl = uploadedUrl
                    }
                }
            }

            val finalAvatar = serverAvatarUrl ?: avatarUrl

            if (!finalAvatar.isNullOrBlank()) {
                tokenManager.saveAvatarUrl(finalAvatar)
            }
            if (!fullName.isNullOrBlank()) {
                tokenManager.updateProfileInfo(fullName, finalAvatar)
            }

            val response = authApi.updateProfile(UpdateProfileRequest(fullName, phone, finalAvatar))
            if (response.isSuccessful && response.body() != null) {
                val authResp = response.body()!!
                authResp.token?.let { token ->
                    tokenManager.saveAuth(token, authResp.email, authResp.fullName, authResp.avatarUrl ?: finalAvatar)
                }
                Result.success(authResp)
            } else {
                Result.success(AuthResponse(email = "", fullName = fullName, avatarUrl = finalAvatar))
            }
        } catch (e: Exception) {
            Result.success(AuthResponse(email = "", fullName = fullName, avatarUrl = avatarUrl))
        }
    }

    suspend fun changePassword(currentPass: String, newPass: String): Result<Boolean> {
        return try {
            val response = authApi.changePassword(ChangePasswordRequest(currentPass, newPass))
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(
                    response,
                    "Đổi mật khẩu không thành công. Mật khẩu hiện tại không chính xác."
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(
                e,
                "Không thể kết nối đến máy chủ."
            )
            Result.failure(Exception(msg))
        }
    }

    suspend fun createPassword(newPass: String): Result<Boolean> {
        return try {
            val response = authApi.createPassword(CreatePasswordRequest(newPass))
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(
                    response,
                    "Tạo mật khẩu không thành công."
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(
                e,
                "Không thể kết nối đến máy chủ."
            )
            Result.failure(Exception(msg))
        }
    }

    suspend fun verifyPassword(password: String): Result<Boolean> {
        return try {
            val response = authApi.verifyPassword(mapOf("password" to password))
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseError(
                    response,
                    "Mật khẩu không chính xác. Vui lòng kiểm tra lại!"
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            val msg = com.hhpc.smarttask.data.api.ApiErrorParser.parseException(
                e,
                "Không thể kết nối đến máy chủ."
            )
            Result.failure(Exception(msg))
        }
    }

    suspend fun logout() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            // Ignore clear credential exception
        }
        tokenManager.clearAuth()
    }
}
