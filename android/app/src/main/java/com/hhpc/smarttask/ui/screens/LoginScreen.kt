package com.hhpc.smarttask.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.hhpc.smarttask.ui.theme.*
import kotlinx.coroutines.launch

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val email by authViewModel.emailState.collectAsState()
    val password by authViewModel.passwordState.collectAsState()
    val fullName by authViewModel.fullNameState.collectAsState()
    val otp by authViewModel.otpState.collectAsState()
    val newPassword by authViewModel.newPasswordState.collectAsState()

    val isRegisterMode by authViewModel.isRegisterMode.collectAsState()
    val isOtpSentForRegister by authViewModel.isOtpSentForRegister.collectAsState()
    val isForgotPasswordMode by authViewModel.isForgotPasswordMode.collectAsState()
    val forgotPasswordStep by authViewModel.forgotPasswordStep.collectAsState()
    val countdownSeconds by authViewModel.countdownSeconds.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val appColors = LocalAppColors.current

    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        appColors.bgTop,
                        appColors.bgMid,
                        appColors.bgBot
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (authState is AuthState.Loading || authState is AuthState.Success) {
            // BRANDED SPLASH SCREEN OVERLAY
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    EmeraldPrimary,
                                    MintAccent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "SmartTask AI",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = appColors.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Quản lý & Lập lịch Công việc Cá nhân",
                    fontSize = 13.sp,
                    color = appColors.textSecondary
                )

                Spacer(modifier = Modifier.height(36.dp))

                CircularProgressIndicator(
                    color = EmeraldPrimary,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (authState is AuthState.Success) "Đã xác thực. Đang vào ứng dụng..." else "Đang kiểm tra đăng nhập...",
                    fontSize = 12.sp,
                    color = appColors.textSecondary
                )
            }
        } else {
            // LOGIN / REGISTER / FORGOT PASSWORD CARD
            AnimatedVisibility(
                visible = authState is AuthState.Idle || authState is AuthState.Error || authState is AuthState.OtpSent,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .verticalScroll(scrollState)
                        .padding(vertical = 24.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(appColors.glassCardBg)
                        .border(1.5.dp, appColors.glassBorderActive, RoundedCornerShape(28.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SmartTask AI",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val headerSubtext = when {
                        isForgotPasswordMode -> "Khôi phục mật khẩu tài khoản"
                        isRegisterMode && isOtpSentForRegister -> "Xác thực mã OTP Đăng ký"
                        isRegisterMode -> "Tạo tài khoản mới"
                        else -> "Quản lý & Lập lịch Công việc Cá nhân"
                    }

                    Text(
                        text = headerSubtext,
                        fontSize = 13.sp,
                        color = appColors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Notice Message Banner if OTP Sent
                    if (authState is AuthState.OtpSent) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.15f))
                                .border(1.dp, EmeraldPrimary, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = (authState as AuthState.OtpSent).message,
                                fontSize = 12.sp,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // --- SCENARIO 1: FORGOT PASSWORD FLOW ---
                    if (isForgotPasswordMode) {
                        if (forgotPasswordStep == 1) {
                            // Step 1: Email Input
                            OutlinedTextField(
                                value = email,
                                onValueChange = { authViewModel.emailState.value = it },
                                label = { Text("Email tài khoản", color = appColors.textSecondary) },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = EmeraldPrimary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = appColors.textPrimary,
                                    unfocusedTextColor = appColors.textPrimary,
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = appColors.glassBorder,
                                    focusedContainerColor = appColors.glassSurface
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { authViewModel.sendForgotPasswordOtp() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Gửi mã OTP khôi phục", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            // Step 2: OTP 6-digits + New Password
                            Text("Email: $email", fontSize = 12.sp, color = appColors.textSecondary)
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = otp,
                                onValueChange = { if (it.length <= 6) authViewModel.otpState.value = it },
                                label = { Text("Mã OTP 6 chữ số", color = appColors.textSecondary) },
                                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = EmeraldPrimary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = appColors.textPrimary,
                                    unfocusedTextColor = appColors.textPrimary,
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = appColors.glassBorder,
                                    focusedContainerColor = appColors.glassSurface
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { authViewModel.newPasswordState.value = it },
                                label = { Text("Mật khẩu mới", color = appColors.textSecondary) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = appColors.textPrimary,
                                    unfocusedTextColor = appColors.textPrimary,
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = appColors.glassBorder,
                                    focusedContainerColor = appColors.glassSurface
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { authViewModel.resetPasswordWithOtp() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Đặt lại mật khẩu", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(onClick = { authViewModel.clearInputs() }) {
                            Text("Quay lại Đăng nhập", color = EmeraldPrimary, fontSize = 13.sp)
                        }
                    }
                    // --- SCENARIO 2: REGISTER WITH OTP STEP 2 ---
                    else if (isRegisterMode && isOtpSentForRegister) {
                        Text(
                            text = "Mã xác thực OTP 6 số đã được gửi đến email:",
                            fontSize = 12.sp,
                            color = appColors.textSecondary
                        )
                        Text(
                            text = email,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // OTP 6 digits input
                        OutlinedTextField(
                            value = otp,
                            onValueChange = { if (it.length <= 6) authViewModel.otpState.value = it },
                            label = { Text("Nhập 6 chữ số OTP", color = appColors.textSecondary) },
                            leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = EmeraldPrimary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = appColors.textPrimary,
                                unfocusedTextColor = appColors.textPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = appColors.glassBorder,
                                focusedContainerColor = appColors.glassSurface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { authViewModel.registerWithOtp() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Hoàn tất đăng ký", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = { authViewModel.sendRegisterOtp() },
                                enabled = countdownSeconds <= 0
                            ) {
                                Text(
                                    text = if (countdownSeconds > 0) "Gửi lại OTP (${countdownSeconds}s)" else "Gửi lại mã OTP",
                                    color = if (countdownSeconds <= 0) EmeraldPrimary else appColors.textSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            TextButton(
                                onClick = { authViewModel.isOtpSentForRegister.value = false }
                            ) {
                                Text("Sửa email", color = appColors.textSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                    // --- SCENARIO 3: STANDARD LOGIN / REGISTER STEP 1 ---
                    else {
                        // Full Name Input (Register Only)
                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { authViewModel.fullNameState.value = it },
                                label = { Text("Họ và tên", color = appColors.textSecondary) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = appColors.textPrimary,
                                    unfocusedTextColor = appColors.textPrimary,
                                    focusedBorderColor = EmeraldPrimary,
                                    unfocusedBorderColor = appColors.glassBorder,
                                    focusedContainerColor = appColors.glassSurface
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        // Email Input
                        OutlinedTextField(
                            value = email,
                            onValueChange = { authViewModel.emailState.value = it },
                            label = { Text("Email", color = appColors.textSecondary) },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = EmeraldPrimary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = appColors.textPrimary,
                                unfocusedTextColor = appColors.textPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = appColors.glassBorder,
                                focusedContainerColor = appColors.glassSurface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Password Input
                        OutlinedTextField(
                            value = password,
                            onValueChange = { authViewModel.passwordState.value = it },
                            label = { Text("Mật khẩu", color = appColors.textSecondary) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = appColors.textSecondary
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = appColors.textPrimary,
                                unfocusedTextColor = appColors.textPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = appColors.glassBorder,
                                focusedContainerColor = appColors.glassSurface
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!isRegisterMode) {
                            // Forgot Password Link
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                TextButton(
                                    onClick = { authViewModel.isForgotPasswordMode.value = true }
                                ) {
                                    Text("Quên mật khẩu?", color = EmeraldPrimary, fontSize = 12.sp)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                if (isRegisterMode) {
                                    authViewModel.sendRegisterOtp()
                                } else {
                                    authViewModel.login()
                                }
                            },
                            enabled = authState !is AuthState.Loading,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text(
                                text = if (isRegisterMode) "Tiếp tục xác thực OTP" else "Đăng nhập",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Divider with "hoặc"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = appColors.glassBorder)
                            Text(text = " hoặc ", fontSize = 12.sp, color = appColors.textSecondary, modifier = Modifier.padding(horizontal = 8.dp))
                            HorizontalDivider(modifier = Modifier.weight(1f), color = appColors.glassBorder)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // GOOGLE NATIVE SIGN IN BUTTON
                        OutlinedButton(
                            onClick = {
                                val targetContext = context.findActivity() ?: context
                                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                                    serverClientId = "465213819257-jqupte1qp12afa0bpngr44dsv49utdll.apps.googleusercontent.com"
                                ).build()

                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(signInWithGoogleOption)
                                    .build()

                                scope.launch {
                                    try {
                                        val result = credentialManager.getCredential(context = targetContext, request = request)
                                        val credential = result.credential
                                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                            authViewModel.loginWithGoogle(
                                                email = googleIdTokenCredential.id,
                                                fullName = googleIdTokenCredential.displayName ?: googleIdTokenCredential.id.substringBefore("@"),
                                                idToken = googleIdTokenCredential.idToken,
                                                avatarUrl = googleIdTokenCredential.profilePictureUri?.toString()
                                            )
                                        } else {
                                            authViewModel.setAuthError("Không thể xác thực thông tin tài khoản Google.")
                                        }
                                    } catch (e: GetCredentialCancellationException) {
                                        // User cancelled
                                    } catch (e: Exception) {
                                        val errorMsg = com.hhpc.smarttask.data.api.ApiErrorParser.parseGoogleAuthException(e)
                                        authViewModel.setAuthError(errorMsg)
                                    }
                                }
                            },
                            enabled = authState !is AuthState.Loading,
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, appColors.glassBorder),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = appColors.glassSurface, contentColor = appColors.textPrimary),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("G", fontSize = 20.sp, fontWeight = FontWeight.Black, color = EmeraldPrimary, modifier = Modifier.padding(end = 10.dp))
                            Text("Đăng nhập với Google", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = appColors.textPrimary)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Toggle Mode Link
                        TextButton(onClick = { authViewModel.toggleMode() }) {
                            Text(
                                text = if (isRegisterMode) "Đã có tài khoản? Đăng nhập ngay" else "Chưa có tài khoản? Đăng ký ngay",
                                color = EmeraldPrimary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Display Error Message if any
                    if (authState is AuthState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = NeonRose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
