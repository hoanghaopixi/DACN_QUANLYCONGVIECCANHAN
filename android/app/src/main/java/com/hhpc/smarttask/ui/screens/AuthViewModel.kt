package com.hhpc.smarttask.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hhpc.smarttask.data.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class OtpSent(val message: String, val email: String) : AuthState()
    data class Success(
        val email: String,
        val fullName: String?,
        val avatarUrl: String? = null
    ) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    val authRepository = AuthRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val emailState = MutableStateFlow("")
    val passwordState = MutableStateFlow("")
    val fullNameState = MutableStateFlow("")
    val otpState = MutableStateFlow("")
    val newPasswordState = MutableStateFlow("")

    val isRegisterMode = MutableStateFlow(false)
    val isOtpSentForRegister = MutableStateFlow(false)

    val isForgotPasswordMode = MutableStateFlow(false)
    val forgotPasswordStep = MutableStateFlow(1) // 1: Email, 2: OTP + New Password

    val countdownSeconds = MutableStateFlow(0)

    init {
        checkSavedAuth()
    }

    private fun startCountdown(seconds: Int = 60) {
        viewModelScope.launch {
            countdownSeconds.value = seconds
            while (countdownSeconds.value > 0) {
                delay(1000L)
                countdownSeconds.value -= 1
            }
        }
    }

    fun checkSavedAuth() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val token = authRepository.tokenManager.tokenFlow.firstOrNull()
            val savedEmail = authRepository.tokenManager.userEmailFlow.firstOrNull()
            val savedName = authRepository.tokenManager.userNameFlow.firstOrNull()
            val savedAvatar = authRepository.tokenManager.userAvatarFlow.firstOrNull()

            if (!token.isNullOrEmpty()) {
                _authState.value = AuthState.Success(
                    email = savedEmail ?: "",
                    fullName = savedName ?: "Người dùng",
                    avatarUrl = savedAvatar
                )
                refreshProfileSilently()
            } else {
                _authState.value = AuthState.Idle
            }
        }
    }

    fun refreshProfileSilently() {
        viewModelScope.launch {
            val res = authRepository.getProfile()
            res.onSuccess { authResponse ->
                _authState.value = AuthState.Success(
                    email = authResponse.email ?: "",
                    fullName = authResponse.fullName ?: "Người dùng",
                    avatarUrl = authResponse.avatarUrl
                )
            }
        }
    }

    fun toggleMode() {
        isRegisterMode.value = !isRegisterMode.value
        isOtpSentForRegister.value = false
        isForgotPasswordMode.value = false
        _authState.value = AuthState.Idle
    }

    fun setAuthError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun login() {
        val email = emailState.value.trim()
        val password = passwordState.value.trim()

        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Vui lòng nhập đầy đủ email và mật khẩu")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.login(email, password)
            result.onSuccess { authResponse ->
                _authState.value = AuthState.Success(
                    email = authResponse.email ?: email,
                    fullName = authResponse.fullName ?: "Người dùng",
                    avatarUrl = authResponse.avatarUrl
                )
                clearInputs()
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.")
            }
        }
    }

    // Step 1: Request Register OTP
    fun sendRegisterOtp() {
        val fullName = fullNameState.value.trim()
        val email = emailState.value.trim()
        val password = passwordState.value.trim()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Vui lòng nhập đầy đủ họ tên, email và mật khẩu")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.sendRegisterOtp(fullName, email)
            result.onSuccess { msg ->
                isOtpSentForRegister.value = true
                _authState.value = AuthState.OtpSent(msg, email)
                startCountdown(60)
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Không thể gửi mã OTP. Email có thể đã tồn tại.")
            }
        }
    }

    // Step 2: Confirm Register with OTP
    fun registerWithOtp() {
        val fullName = fullNameState.value.trim()
        val email = emailState.value.trim()
        val password = passwordState.value.trim()
        val otp = otpState.value.trim()

        if (otp.length != 6) {
            _authState.value = AuthState.Error("Vui lòng nhập đủ 6 chữ số mã OTP")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.register(fullName, email, password, otp)
            result.onSuccess { authResponse ->
                _authState.value = AuthState.Success(
                    email = authResponse.email ?: email,
                    fullName = authResponse.fullName ?: fullName,
                    avatarUrl = authResponse.avatarUrl
                )
                clearInputs()
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Mã OTP không chính xác hoặc đã hết hạn.")
            }
        }
    }

    // Forgot Password - Step 1: Send OTP
    fun sendForgotPasswordOtp() {
        val email = emailState.value.trim()
        if (email.isEmpty()) {
            _authState.value = AuthState.Error("Vui lòng nhập email tài khoản của bạn")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.forgotPassword(email)
            result.onSuccess { msg ->
                forgotPasswordStep.value = 2
                _authState.value = AuthState.OtpSent(msg, email)
                startCountdown(60)
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Email không tồn tại trên hệ thống.")
            }
        }
    }

    // Forgot Password - Step 2: Reset Password with OTP
    fun resetPasswordWithOtp() {
        val email = emailState.value.trim()
        val otp = otpState.value.trim()
        val newPass = newPasswordState.value.trim()

        if (otp.length != 6 || newPass.isEmpty()) {
            _authState.value = AuthState.Error("Vui lòng nhập mã OTP 6 số và mật khẩu mới")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.resetPassword(email, otp, newPass)
            result.onSuccess { msg ->
                _authState.value = AuthState.Idle
                isForgotPasswordMode.value = false
                forgotPasswordStep.value = 1
                clearInputs()
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Mã OTP không chính xác hoặc đã hết hạn.")
            }
        }
    }

    fun loginWithGoogle(
        email: String,
        fullName: String? = null,
        idToken: String? = null,
        avatarUrl: String? = null
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.loginWithGoogle(email, fullName, idToken, avatarUrl)
            result.onSuccess { authResponse ->
                _authState.value = AuthState.Success(
                    email = authResponse.email ?: email,
                    fullName = authResponse.fullName ?: (fullName ?: "Người dùng Google"),
                    avatarUrl = authResponse.avatarUrl ?: avatarUrl
                )
                clearInputs()
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Đăng nhập Google thất bại.")
            }
        }
    }

    fun clearInputs() {
        emailState.value = ""
        passwordState.value = ""
        fullNameState.value = ""
        otpState.value = ""
        newPasswordState.value = ""
        isOtpSentForRegister.value = false
        isForgotPasswordMode.value = false
        forgotPasswordStep.value = 1
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Idle
            clearInputs()
        }
    }
}
