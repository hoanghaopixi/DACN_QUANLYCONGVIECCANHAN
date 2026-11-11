package com.hhpc.smarttask.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hhpc.smarttask.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val email: String, val fullName: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val emailState = MutableStateFlow("")
    val passwordState = MutableStateFlow("")
    val fullNameState = MutableStateFlow("")
    val isRegisterMode = MutableStateFlow(false)

    init {
        checkSavedAuth()
    }

    fun checkSavedAuth() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val token = authRepository.tokenManager.tokenFlow.firstOrNull()
            val savedEmail = authRepository.tokenManager.userEmailFlow.firstOrNull()
            val savedName = authRepository.tokenManager.userNameFlow.firstOrNull()

            if (!token.isNullOrEmpty()) {
                _authState.value = AuthState.Success(
                    email = savedEmail ?: "",
                    fullName = savedName ?: "Người dùng"
                )
            } else {
                _authState.value = AuthState.Idle
            }
        }
    }

    fun toggleMode() {
        isRegisterMode.value = !isRegisterMode.value
        _authState.value = AuthState.Idle
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
                    fullName = authResponse.fullName ?: "Người dùng"
                )
                clearInputs()
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.")
            }
        }
    }

    fun register() {
        val fullName = fullNameState.value.trim()
        val email = emailState.value.trim()
        val password = passwordState.value.trim()

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Vui lòng nhập đầy đủ họ tên, email và mật khẩu")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.register(fullName, email, password)
            result.onSuccess { authResponse ->
                _authState.value = AuthState.Success(
                    email = authResponse.email ?: email,
                    fullName = authResponse.fullName ?: fullName
                )
                clearInputs()
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Đăng ký thất bại. Email có thể đã tồn tại.")
            }
        }
    }

    private fun clearInputs() {
        emailState.value = ""
        passwordState.value = ""
        fullNameState.value = ""
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Idle
            clearInputs()
        }
    }
}
