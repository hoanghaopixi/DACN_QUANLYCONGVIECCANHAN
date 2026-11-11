package com.hhpc.smarttask.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hhpc.smarttask.ui.theme.*

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val email by authViewModel.emailState.collectAsState()
    val password by authViewModel.passwordState.collectAsState()
    val fullName by authViewModel.fullNameState.collectAsState()
    val isRegisterMode by authViewModel.isRegisterMode.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    val appColors = LocalAppColors.current

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
            // BRANDED SPLASH SCREEN OVERLAY (FB/YT Style) - Shown when Loading or Already Logged In!
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
            // LOGIN / REGISTER FORM - Only shown when AuthState.Idle or AuthState.Error!
            AnimatedVisibility(
                visible = authState is AuthState.Idle || authState is AuthState.Error,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(28.dp))
                        .background(appColors.glassCardBg)
                        .border(1.5.dp, appColors.glassBorderActive, RoundedCornerShape(28.dp))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // App Logo & Title
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

                    Text(
                        text = if (isRegisterMode) "Tạo tài khoản mới" else "Quản lý & Lập lịch Công việc Cá nhân",
                        fontSize = 13.sp,
                        color = appColors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Full Name Input (Only in Register Mode)
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
                                focusedContainerColor = appColors.glassSurface,
                                unfocusedContainerColor = appColors.glassSurface.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
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
                            focusedContainerColor = appColors.glassSurface,
                            unfocusedContainerColor = appColors.glassSurface.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                            focusedContainerColor = appColors.glassSurface,
                            unfocusedContainerColor = appColors.glassSurface.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Error Message Display
                    if (authState is AuthState.Error) {
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = NeonRose,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // Submit Button (Login / Register)
                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                authViewModel.register()
                            } else {
                                authViewModel.login()
                            }
                        },
                        enabled = authState !is AuthState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (isRegisterMode) "Đăng Ký" else "Đăng Nhập",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mode Toggle Link
                    TextButton(
                        onClick = { authViewModel.toggleMode() }
                    ) {
                        Text(
                            text = if (isRegisterMode) "Đã có tài khoản? Đăng nhập ngay" else "Chưa có tài khoản? Đăng ký ngay",
                            color = EmeraldPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
