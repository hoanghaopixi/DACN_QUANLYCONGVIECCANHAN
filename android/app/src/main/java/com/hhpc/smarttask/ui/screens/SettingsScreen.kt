package com.hhpc.smarttask.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hhpc.smarttask.data.model.UserPreferenceDto
import com.hhpc.smarttask.ui.theme.*
import kotlinx.coroutines.launch

enum class SettingsTab {
    NONE, PROFILE, WORKING_HOURS, AI_PERMISSIONS, WALLPAPER_THEME, NOTIFICATIONS
}

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

    val userName = if (authState is AuthState.Success) (authState as AuthState.Success).fullName ?: "Nguyễn Hoàng Hảo" else "Nguyễn Hoàng Hảo"
    val userEmail = if (authState is AuthState.Success) (authState as AuthState.Success).email else "admin@gmail.com"

    var activeTab by remember { mutableStateOf(SettingsTab.NONE) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }

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
                text = "Tài khoản & Thiết lập cá nhân",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )

            Text(
                text = "Thiết lập kế hoạch làm việc của bạn cùng SmartTask",
                fontSize = 12.sp,
                color = appColors.textSecondary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // User Profile Summary Card
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
                        Text(
                            text = "NH",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "✓ Email đã xác minh",
                                    color = EmeraldPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = userEmail,
                            fontSize = 12.sp,
                            color = appColors.textSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Danh mục thiết lập",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // 5 Menu Category Rows (Single Row Items with Material Icons & Arrow)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1: Profile & Account
                SettingsCategoryRow(
                    icon = Icons.Default.Person,
                    title = "Hồ sơ & Tài khoản",
                    subtitle = "Họ tên, SĐT, Email, Ngôn ngữ, Múi giờ, Đổi mật khẩu",
                    onClick = { activeTab = SettingsTab.PROFILE }
                )

                // Row 2: Working Hours & Schedule
                SettingsCategoryRow(
                    icon = Icons.Default.AccessTime,
                    title = "Giờ làm việc & Lịch trình",
                    subtitle = "Thứ làm việc (T2-CN), Giờ bắt đầu/kết thúc, Nghỉ trưa, Đệm an toàn",
                    onClick = { activeTab = SettingsTab.WORKING_HOURS }
                )

                // Row 3: AI Assistant & Permissions
                SettingsCategoryRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "Trợ lý AI & Quyền hạn",
                    subtitle = "Tự tìm khung giờ, Hỏi trước khi thêm/sửa, Chia nhỏ việc, Quyền Workspace",
                    onClick = { activeTab = SettingsTab.AI_PERMISSIONS }
                )

                // Row 4: Wallpaper & Theme
                SettingsCategoryRow(
                    icon = Icons.Default.Palette,
                    title = "Hình nền & Giao diện",
                    subtitle = "Chế độ Sáng/Tối/Hệ thống, Video/Ảnh nền cá nhân hóa, Độ phủ sáng 50%",
                    onClick = { activeTab = SettingsTab.WALLPAPER_THEME }
                )

                // Row 5: Notifications & Reminders
                SettingsCategoryRow(
                    icon = Icons.Default.Notifications,
                    title = "Thông báo & Nhắc hẹn",
                    subtitle = "Thời gian nhắc trước (15p), Khung giờ Không làm phiền (22:00-07:00)",
                    onClick = { activeTab = SettingsTab.NOTIFICATIONS }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Logout Button Row (Opens Confirmation Dialog)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StatusRose.copy(alpha = 0.15f))
                        .border(1.dp, StatusRose.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .clickable {
                            showLogoutConfirmDialog = true
                        }
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = null,
                                tint = StatusRose,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Đăng xuất tài khoản",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusRose
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = StatusRose,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Logout Confirmation Dialog
        if (showLogoutConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutConfirmDialog = false },
                containerColor = appColors.glassCardBg,
                title = {
                    Text(text = "Xác nhận đăng xuất", fontWeight = FontWeight.Bold, color = appColors.textPrimary, fontSize = 18.sp)
                },
                text = {
                    Text(text = "Bạn có chắc chắn muốn đăng xuất khỏi tài khoản SmartTask AI không?", color = appColors.textSecondary, fontSize = 13.sp)
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutConfirmDialog = false
                            scope.launch {
                                authViewModel.logout()
                                onLogout()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRose)
                    ) {
                        Text("Đăng xuất", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutConfirmDialog = false }) {
                        Text("Hủy", color = appColors.textSecondary)
                    }
                }
            )
        }

        // Sub-Detail Modal Dialogs
        when (activeTab) {
            SettingsTab.PROFILE -> ProfileAccountDetailSheet(
                preferences = preferences,
                userName = userName,
                userEmail = userEmail,
                onDismiss = { activeTab = SettingsTab.NONE },
                onSave = { updated ->
                    taskViewModel?.updateUserPreferences(updated)
                    activeTab = SettingsTab.NONE
                }
            )

            SettingsTab.WORKING_HOURS -> WorkingHoursDetailSheet(
                preferences = preferences,
                onDismiss = { activeTab = SettingsTab.NONE },
                onSave = { updated ->
                    taskViewModel?.updateUserPreferences(updated)
                    activeTab = SettingsTab.NONE
                }
            )

            SettingsTab.AI_PERMISSIONS -> AiPermissionsDetailSheet(
                preferences = preferences,
                onDismiss = { activeTab = SettingsTab.NONE },
                onSave = { updated ->
                    taskViewModel?.updateUserPreferences(updated)
                    activeTab = SettingsTab.NONE
                }
            )

            SettingsTab.WALLPAPER_THEME -> WallpaperThemeDetailSheet(
                preferences = preferences,
                taskViewModel = taskViewModel,
                onDismiss = { activeTab = SettingsTab.NONE },
                onSave = { updated ->
                    taskViewModel?.updateUserPreferences(updated)
                    activeTab = SettingsTab.NONE
                }
            )

            SettingsTab.NOTIFICATIONS -> NotificationsDetailSheet(
                preferences = preferences,
                onDismiss = { activeTab = SettingsTab.NONE },
                onSave = { updated ->
                    taskViewModel?.updateUserPreferences(updated)
                    activeTab = SettingsTab.NONE
                }
            )

            else -> {}
        }
    }
}

@Composable
fun SettingsCategoryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(appColors.glassCardBg)
            .border(1.dp, appColors.glassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = appColors.textSecondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = appColors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// ----------------- DETAIL SUB-SHEET 1: PROFILE & ACCOUNT -----------------
@Composable
fun ProfileAccountDetailSheet(
    preferences: UserPreferenceDto,
    userName: String,
    userEmail: String,
    onDismiss: () -> Unit,
    onSave: (UserPreferenceDto) -> Unit
) {
    val appColors = LocalAppColors.current
    var name by remember { mutableStateOf(preferences.fullName ?: userName) }
    var phone by remember { mutableStateOf(preferences.phone ?: "0901234567") }
    var lang by remember { mutableStateOf(preferences.language ?: "Tiếng Việt") }
    var tz by remember { mutableStateOf(preferences.timeZone ?: "(GMT+07:00) Hồ Chí Minh") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.glassCardBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hồ sơ & Tài khoản", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = appColors.textSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header User Info Banner
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("NH", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = appColors.textPrimary)
                        Text(userEmail, fontSize = 12.sp, color = appColors.textSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("✓ Email đã xác minh", color = EmeraldPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Họ và tên", color = appColors.textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = appColors.textPrimary,
                        unfocusedTextColor = appColors.textPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = appColors.glassBorder
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại", color = appColors.textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = appColors.textPrimary,
                        unfocusedTextColor = appColors.textPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = appColors.glassBorder
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = userEmail,
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Email (Định danh tài khoản)", color = appColors.textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = appColors.textSecondary,
                        disabledBorderColor = appColors.glassBorder
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Ngôn ngữ:", fontSize = 12.sp, color = appColors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Tiếng Việt", "English").forEach { item ->
                        val isSelected = lang == item
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                .clickable { lang = item }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item, color = if (isSelected) Color.White else appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = tz,
                    onValueChange = { tz = it },
                    label = { Text("Múi giờ hệ thống", color = appColors.textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = appColors.textPrimary,
                        unfocusedTextColor = appColors.textPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = appColors.glassBorder
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Security Section
                Text("Bảo mật tài khoản:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(appColors.glassSurface)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Mật khẩu: ••••••••", fontSize = 12.sp, color = appColors.textPrimary)
                            Text("Đổi mật khẩu", fontSize = 12.sp, color = EmeraldPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable {})
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Phiên làm việc: Chrome · Windows (Phiên hiện tại · TP.HCM)", fontSize = 11.sp, color = appColors.textSecondary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(preferences.copy(fullName = name, phone = phone, language = lang, timeZone = tz))
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Lưu thay đổi hồ sơ", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ----------------- DETAIL SUB-SHEET 2: WORKING HOURS & SCHEDULE -----------------
@Composable
fun WorkingHoursDetailSheet(
    preferences: UserPreferenceDto,
    onDismiss: () -> Unit,
    onSave: (UserPreferenceDto) -> Unit
) {
    val appColors = LocalAppColors.current
    var startTime by remember { mutableStateOf(preferences.workStartTime ?: "08:00") }
    var endTime by remember { mutableStateOf(preferences.workEndTime ?: "18:00") }
    var lunchStart by remember { mutableStateOf(preferences.lunchBreakStart ?: "12:00") }
    var lunchEnd by remember { mutableStateOf(preferences.lunchBreakEnd ?: "13:00") }
    var bufferMin by remember { mutableStateOf(preferences.bufferMinutes ?: 15) }
    var allowWeekend by remember { mutableStateOf(preferences.allowWeekendTasks ?: true) }

    val daysList = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    var selectedDays by remember { mutableStateOf(setOf("T2", "T3", "T4", "T5", "T6")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.glassCardBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Giờ làm việc & Lịch trình", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = appColors.textSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Ngày làm việc trong tuần:", fontSize = 12.sp, color = appColors.textSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    daysList.forEach { day ->
                        val isSelected = day in selectedDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                .clickable {
                                    selectedDays = if (isSelected) selectedDays - day else selectedDays + day
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day, color = if (isSelected) Color.White else appColors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Giờ làm việc (Bắt đầu - Kết thúc):", fontSize = 12.sp, color = appColors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Bắt đầu", color = appColors.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Kết thúc", color = appColors.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Khung giờ Nghỉ trưa:", fontSize = 12.sp, color = appColors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lunchStart,
                        onValueChange = { lunchStart = it },
                        label = { Text("Từ", color = appColors.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lunchEnd,
                        onValueChange = { lunchEnd = it },
                        label = { Text("Đến", color = appColors.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Thời gian đệm an toàn giữa 2 việc:", fontSize = 12.sp, color = appColors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(10, 15, 30).forEach { mins ->
                        val isSelected = bufferMin == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                .clickable { bufferMin = mins }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$mins phút", color = if (isSelected) Color.White else appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cho phép Sức khỏe & Giải trí cuối tuần", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                        Text("SmartTask có thể đề xuất việc vào T7 hoặc CN, 08:00-18:00", fontSize = 11.sp, color = appColors.textSecondary)
                    }
                    Switch(
                        checked = allowWeekend,
                        onCheckedChange = { allowWeekend = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(preferences.copy(
                        workStartTime = startTime,
                        workEndTime = endTime,
                        lunchBreakStart = lunchStart,
                        lunchBreakEnd = lunchEnd,
                        bufferMinutes = bufferMin,
                        allowWeekendTasks = allowWeekend
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Lưu cài đặt lịch trình", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ----------------- DETAIL SUB-SHEET 3: AI PERMISSIONS -----------------
@Composable
fun AiPermissionsDetailSheet(
    preferences: UserPreferenceDto,
    onDismiss: () -> Unit,
    onSave: (UserPreferenceDto) -> Unit
) {
    val appColors = LocalAppColors.current
    var autoFindSlot by remember { mutableStateOf(preferences.aiAutoFindSlot ?: true) }
    var alwaysAsk by remember { mutableStateOf(preferences.aiAlwaysAskBeforeChange ?: true) }
    var allowSplit by remember { mutableStateOf(preferences.aiAllowSplitLongTasks ?: true) }
    var autoMoveFixed by remember { mutableStateOf(preferences.aiAutoMoveFixedTasks ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.glassCardBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trợ lý AI & Quyền hạn", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = appColors.textSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Quyền kiểm soát trợ lý AI:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Text("AI đề xuất lịch phù hợp, bạn luôn là người quyết định.", fontSize = 11.sp, color = appColors.textSecondary)
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tự động tìm khung giờ cho việc chưa có lịch", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = appColors.textPrimary)
                        Text("Ưu tiên việc quan trọng và tránh giờ nghỉ của bạn.", fontSize = 11.sp, color = appColors.textSecondary)
                    }
                    Switch(checked = autoFindSlot, onCheckedChange = { autoFindSlot = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Luôn hỏi trước khi thêm hoặc đổi lịch", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = appColors.textPrimary)
                        Text("Xem trước toàn bộ thay đổi và xác nhận để áp dụng.", fontSize = 11.sp, color = appColors.textSecondary)
                    }
                    Switch(checked = alwaysAsk, onCheckedChange = { alwaysAsk = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cho phép chia nhỏ công việc dài", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = appColors.textPrimary)
                        Text("Mỗi phiên tối thiểu 60 phút. Không chia lịch hẹn cố định.", fontSize = 11.sp, color = appColors.textSecondary)
                    }
                    Switch(checked = allowSplit, onCheckedChange = { allowSplit = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tự di chuyển công việc đã có giờ cố định", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = appColors.textPrimary)
                        Text("Đang tắt. Các cuộc họp và lịch cá nhân được giữ nguyên.", fontSize = 11.sp, color = appColors.textSecondary)
                    }
                    Switch(checked = autoMoveFixed, onCheckedChange = { autoMoveFixed = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Workspace AI Permission Button Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(appColors.glassSurface)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Quản lý quyền AI truy cập Không gian làm việc", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Kiểm soát không gian nào AI được phép đọc dữ liệu để bảo vệ bí mật công việc cá nhân.", fontSize = 11.sp, color = appColors.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary.copy(alpha = 0.2f), contentColor = EmeraldPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cấu hình quyền AI Workspace", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(preferences.copy(
                        aiAutoFindSlot = autoFindSlot,
                        aiAlwaysAskBeforeChange = alwaysAsk,
                        aiAllowSplitLongTasks = allowSplit,
                        aiAutoMoveFixedTasks = autoMoveFixed
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Lưu thiết lập AI", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ----------------- DETAIL SUB-SHEET 4: WALLPAPER & THEME -----------------
@Composable
fun WallpaperThemeDetailSheet(
    preferences: UserPreferenceDto,
    taskViewModel: TaskViewModel?,
    onDismiss: () -> Unit,
    onSave: (UserPreferenceDto) -> Unit
) {
    val appColors = LocalAppColors.current
    var currentTheme by remember { mutableStateOf(preferences.theme ?: "LIGHT") }
    var darkness by remember { mutableStateOf(preferences.overlayDarkness ?: 50) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.glassCardBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hình nền & Giao diện", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = appColors.textSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Chế độ giao diện (Theme):", fontSize = 12.sp, color = appColors.textSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    mapOf("LIGHT" to "☀️ Sáng", "DARK" to "🌙 Tối", "SYSTEM" to "⚙️ Hệ thống").forEach { (key, label) ->
                        val isSelected = currentTheme == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                .clickable {
                                    currentTheme = key
                                    val updated = preferences.copy(theme = key)
                                    taskViewModel?.updateUserPreferences(updated)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isSelected) Color.White else appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Wallpaper Customization Section
                Text("Hình nền cá nhân hóa & Video:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Text("Cài đặt ảnh hoặc video động làm hình nền. Kèm bộ lọc phủ sáng để giữ độ tương phản.", fontSize = 11.sp, color = appColors.textSecondary)
                Spacer(modifier = Modifier.height(10.dp))

                // Wallpaper Preview Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    EmeraldPrimary.copy(alpha = 0.6f),
                                    MintAccent.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .padding(10.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("▶ Video nền động • Độ phủ sáng: $darkness%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tải ảnh/Video", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {},
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = Brush.horizontalGradient(listOf(StatusRose, StatusRose))),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = StatusRose, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Xóa hình nền", fontSize = 11.sp, color = StatusRose)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Độ phủ hình nền (Phủ sáng): $darkness%", fontSize = 12.sp, color = appColors.textPrimary, fontWeight = FontWeight.Bold)
                Slider(
                    value = darkness.toFloat(),
                    onValueChange = { darkness = it.toInt() },
                    valueRange = 45f..90f,
                    colors = SliderDefaults.colors(thumbColor = EmeraldPrimary, activeTrackColor = EmeraldPrimary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Lịch sử hình nền & video đã tải lên (3):", fontSize = 12.sp, color = appColors.textSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("▶ Video · Goku Fiery Aura 4K Live Wallpaper", "🖼️ Ảnh · hinh-nen-goku-1.jpg", "🖼️ Ảnh · Moonlit Alpine Valley").forEach { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(appColors.glassSurface)
                                .padding(8.dp)
                        ) {
                            Text(item, fontSize = 11.sp, color = appColors.textPrimary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(preferences.copy(theme = currentTheme, overlayDarkness = darkness))
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Lưu giao diện", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ----------------- DETAIL SUB-SHEET 5: NOTIFICATIONS -----------------
@Composable
fun NotificationsDetailSheet(
    preferences: UserPreferenceDto,
    onDismiss: () -> Unit,
    onSave: (UserPreferenceDto) -> Unit
) {
    val appColors = LocalAppColors.current
    var reminderMins by remember { mutableStateOf(preferences.reminderMinutesBefore ?: 15) }
    var dndStart by remember { mutableStateOf(preferences.dndStartTime ?: "22:00") }
    var dndEnd by remember { mutableStateOf(preferences.dndEndTime ?: "07:00") }
    var enableDeviceNotif by remember { mutableStateOf(preferences.enableDeviceNotifications ?: true) }
    var enableSummaryEmail by remember { mutableStateOf(preferences.enableMorningSummaryEmail ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.glassCardBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Thông báo & Nhắc hẹn", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = appColors.textSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Thời gian nhắc trước khi bắt đầu:", fontSize = 12.sp, color = appColors.textSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 15, 30).forEach { mins ->
                        val isSelected = reminderMins == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                .clickable { reminderMins = mins }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$mins phút", color = if (isSelected) Color.White else appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Khung giờ Không làm phiền (Do Not Disturb):", fontSize = 12.sp, color = appColors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dndStart,
                        onValueChange = { dndStart = it },
                        label = { Text("Từ", color = appColors.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dndEnd,
                        onValueChange = { dndEnd = it },
                        label = { Text("Đến", color = appColors.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text("SmartTask sẽ không gửi nhắc việc trong khung giờ này.", fontSize = 11.sp, color = appColors.textSecondary, modifier = Modifier.padding(top = 4.dp))

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Thông báo trên thiết bị", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                        Text("Nhắc công việc và cảnh báo trùng lịch.", fontSize = 11.sp, color = appColors.textSecondary)
                    }
                    Switch(checked = enableDeviceNotif, onCheckedChange = { enableDeviceNotif = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tóm tắt lịch mỗi sáng", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                        Text("Gửi email tóm tắt công việc lúc 07:30 mỗi ngày.", fontSize = 11.sp, color = appColors.textSecondary)
                    }
                    Switch(checked = enableSummaryEmail, onCheckedChange = { enableSummaryEmail = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(preferences.copy(
                        reminderMinutesBefore = reminderMins,
                        dndStartTime = dndStart,
                        dndEndTime = dndEnd,
                        enableDeviceNotifications = enableDeviceNotif,
                        enableMorningSummaryEmail = enableSummaryEmail
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Lưu cài đặt thông báo", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}
