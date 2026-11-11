package com.hhpc.smarttask.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hhpc.smarttask.data.model.UserPreferenceDto
import com.hhpc.smarttask.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    taskViewModel: TaskViewModel? = null,
    onLogout: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val preferences by taskViewModel?.userPreferences?.collectAsState() ?: remember { mutableStateOf(UserPreferenceDto()) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val appColors = LocalAppColors.current

    val userName = if (authState is AuthState.Success) (authState as AuthState.Success).fullName ?: "Người dùng" else "Chưa đăng nhập"
    val userEmail = if (authState is AuthState.Success) (authState as AuthState.Success).email else ""

    var startTime by remember(preferences) { mutableStateOf(preferences.workStartTime ?: "08:00") }
    var endTime by remember(preferences) { mutableStateOf(preferences.workEndTime ?: "18:00") }
    var bufferMin by remember(preferences) { mutableStateOf(preferences.bufferMinutes ?: 15) }
    var peakTime by remember(preferences) { mutableStateOf(preferences.peakProductivityTime ?: "MORNING") }
    var autoReschedule by remember(preferences) { mutableStateOf(preferences.autoReschedule ?: true) }
    var currentTheme by remember(preferences) { mutableStateOf(preferences.theme ?: "SYSTEM") }

    var isSavedNoticeVisible by remember { mutableStateOf(false) }

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
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 90.dp)
        ) {
            Text(
                text = "Cài đặt & Thói quen",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp)
            )

            // User Profile Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(appColors.glassCardBg)
                    .border(1.dp, appColors.glassBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = userName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                        if (userEmail.isNotEmpty()) {
                            Text(
                                text = userEmail,
                                fontSize = 13.sp,
                                color = appColors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // User Preferences Form Section
            Text(
                text = "Cấu hình Thói quen & Lập lịch AI",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(appColors.glassCardBg)
                    .border(1.dp, appColors.glassBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    // Instant Theme Selector
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DarkMode, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Chế độ giao diện (Theme)", fontSize = 14.sp, color = appColors.textPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        mapOf("LIGHT" to "☀️ Sáng", "DARK" to "🌙 Tối", "SYSTEM" to "⚙️ Hệ thống").forEach { (key, label) ->
                            val isSelected = currentTheme == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                    .clickable {
                                        currentTheme = key
                                        val updated = preferences.copy(
                                            workStartTime = startTime,
                                            workEndTime = endTime,
                                            bufferMinutes = bufferMin,
                                            peakProductivityTime = peakTime,
                                            autoReschedule = autoReschedule,
                                            theme = key
                                        )
                                        taskViewModel?.updateUserPreferences(updated)
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else appColors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Working Hours Inputs
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Khung giờ làm việc hằng ngày", fontSize = 14.sp, color = appColors.textPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Giờ bắt đầu", color = appColors.textSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = appColors.textPrimary,
                                unfocusedTextColor = appColors.textPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = appColors.glassBorder
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("Giờ kết thúc", color = appColors.textSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = appColors.textPrimary,
                                unfocusedTextColor = appColors.textPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = appColors.glassBorder
                            ),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buffer Time Selector
                    Text(text = "Thời gian đệm an toàn giữa 2 việc:", fontSize = 13.sp, color = appColors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(10, 15, 30).forEach { mins ->
                            val isSelected = bufferMin == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                    .clickable { bufferMin = mins }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$mins phút",
                                    color = if (isSelected) Color.White else appColors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Peak Productivity Time Selector
                    Text(text = "Khung giờ vàng hiệu suất cao nhất:", fontSize = 13.sp, color = appColors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        mapOf("MORNING" to "Buổi sáng", "AFTERNOON" to "Buổi chiều", "EVENING" to "Buổi tối").forEach { (key, label) ->
                            val isSelected = peakTime == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                    .clickable { peakTime = key }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else appColors.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Auto Reschedule Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Tự động dời lịch khi quá hạn", fontSize = 14.sp, color = appColors.textPrimary, fontWeight = FontWeight.SemiBold)
                            Text(text = "AI sẽ chuyển việc trễ sang giờ rảnh kế tiếp", fontSize = 11.sp, color = appColors.textSecondary)
                        }
                        Switch(
                            checked = autoReschedule,
                            onCheckedChange = { autoReschedule = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldPrimary,
                                uncheckedBorderColor = appColors.glassBorder
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Save Button
                    Button(
                        onClick = {
                            val updated = preferences.copy(
                                workStartTime = startTime,
                                workEndTime = endTime,
                                bufferMinutes = bufferMin,
                                peakProductivityTime = peakTime,
                                autoReschedule = autoReschedule,
                                theme = currentTheme
                            )
                            taskViewModel?.updateUserPreferences(updated)
                            isSavedNoticeVisible = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Lưu Tất Cả Cài Đặt", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isSavedNoticeVisible) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✓ Đã lưu cài đặt thói quen sinh hoạt thành công!",
                            color = MintAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Backend Server Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(appColors.glassCardBg)
                    .border(1.dp, appColors.glassBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = NeonAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Máy chủ Spring Boot API",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = appColors.textPrimary
                        )
                        Text(
                            text = "http://10.0.2.2:8085/api/",
                            fontSize = 12.sp,
                            color = appColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = {
                    scope.launch {
                        authViewModel.logout()
                        onLogout()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonRose.copy(alpha = 0.2f),
                    contentColor = NeonRose
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = NeonRose)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Đăng xuất tài khoản", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
