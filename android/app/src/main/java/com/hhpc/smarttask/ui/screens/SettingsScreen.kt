package com.hhpc.smarttask.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.hhpc.smarttask.data.model.Category
import com.hhpc.smarttask.data.model.UserPreferenceDto
import com.hhpc.smarttask.data.model.Workspace
import com.hhpc.smarttask.data.repository.AuthRepository
import com.hhpc.smarttask.ui.theme.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch

enum class SettingsTab {
    NONE, PROFILE, WORKSPACES_CATEGORIES, WORKING_HOURS, AI_PERMISSIONS, WALLPAPER_THEME, NOTIFICATIONS, ABOUT
}

// Converts relative backend URLs (e.g. /api/uploads/avatars/...), local file paths, or HTTP URLs into a Coil-resolvable model!
fun resolveAvatarUrl(url: String?): Any? {
    if (url.isNullOrBlank()) return null
    val trimmed = url.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("data:image") -> trimmed
        trimmed.startsWith("/data/") || trimmed.startsWith("/storage/") -> File(trimmed)
        trimmed.startsWith("/api/") -> "http://10.0.2.2:8085$trimmed"
        trimmed.startsWith("/uploads/") -> "http://10.0.2.2:8085/api$trimmed"
        trimmed.startsWith("uploads/") -> "http://10.0.2.2:8085/api/$trimmed"
        trimmed.startsWith("/") -> "http://10.0.2.2:8085/api$trimmed"
        else -> "http://10.0.2.2:8085/api/$trimmed"
    }
}

fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("avatar_", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        tempFile
    } catch (e: Exception) {
        null
    }
}

// Converts a local Uri to compressed Base64 Data URI string!
fun convertUriToBase64DataUri(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        val maxDim = 512
        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val scale = maxDim.toFloat() / Math.max(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)
        "data:image/jpeg;base64,$base64String"
    } catch (e: Exception) {
        null
    }
}

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    taskViewModel: TaskViewModel? = null,
    onLogout: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val preferences by taskViewModel?.userPreferences?.collectAsState() ?: remember { mutableStateOf(UserPreferenceDto()) }
    val scope = rememberCoroutineScope()
    val appColors = LocalAppColors.current

    val userEmail = remember(authState) {
        if (authState is AuthState.Success) (authState as AuthState.Success).email else ""
    }

    val userName = remember(authState, preferences) {
        val prefName = preferences.fullName
        val authName = if (authState is AuthState.Success) (authState as AuthState.Success).fullName else null
        when {
            !prefName.isNullOrBlank() -> prefName
            !authName.isNullOrBlank() && authName != "Người dùng" -> authName
            userEmail.isNotBlank() -> userEmail.substringBefore("@")
            else -> "Người dùng"
        }
    }

    val userPhone = remember(preferences) {
        preferences.phone ?: ""
    }

    val userAvatarUrl = remember(authState) {
        if (authState is AuthState.Success) (authState as AuthState.Success).avatarUrl else null
    }

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
    ) {
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                if (targetState != SettingsTab.NONE) {
                    (slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn()).togetherWith(
                        slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth / 3 }) + fadeOut()
                    )
                } else {
                    (slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth / 3 }) + fadeIn()).togetherWith(
                        slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut()
                    )
                }
            },
            label = "settingsScreenTransition"
        ) { targetTab ->
            when (targetTab) {
                SettingsTab.NONE -> MainSettingsView(
                    userName = userName,
                    userEmail = userEmail,
                    userAvatarUrl = userAvatarUrl,
                    onNavigateTo = { activeTab = it },
                    onLogoutClick = { showLogoutConfirmDialog = true }
                )

                SettingsTab.PROFILE -> ProfileAccountSubScreen(
                    preferences = preferences,
                    userName = userName,
                    userEmail = userEmail,
                    userPhone = userPhone,
                    userAvatarUrl = userAvatarUrl,
                    authViewModel = authViewModel,
                    onBack = { activeTab = SettingsTab.NONE },
                    onSave = { updated ->
                        taskViewModel?.updateUserPreferences(updated)
                        activeTab = SettingsTab.NONE
                    }
                )

                SettingsTab.WORKSPACES_CATEGORIES -> WorkspacesCategoriesSubScreen(
                    taskViewModel = taskViewModel,
                    onBack = { activeTab = SettingsTab.NONE }
                )

                SettingsTab.WORKING_HOURS -> WorkingHoursSubScreen(
                    preferences = preferences,
                    onBack = { activeTab = SettingsTab.NONE },
                    onSave = { updated ->
                        taskViewModel?.updateUserPreferences(updated)
                        activeTab = SettingsTab.NONE
                    }
                )

                SettingsTab.AI_PERMISSIONS -> AiPermissionsSubScreen(
                    preferences = preferences,
                    onBack = { activeTab = SettingsTab.NONE },
                    onSave = { updated ->
                        taskViewModel?.updateUserPreferences(updated)
                        activeTab = SettingsTab.NONE
                    }
                )

                SettingsTab.WALLPAPER_THEME -> WallpaperThemeSubScreen(
                    preferences = preferences,
                    taskViewModel = taskViewModel,
                    onBack = { activeTab = SettingsTab.NONE },
                    onSave = { updated ->
                        taskViewModel?.updateUserPreferences(updated)
                        activeTab = SettingsTab.NONE
                    }
                )

                SettingsTab.NOTIFICATIONS -> NotificationsDetailSubScreen(
                    preferences = preferences,
                    onBack = { activeTab = SettingsTab.NONE },
                    onSave = { updated ->
                        taskViewModel?.updateUserPreferences(updated)
                        activeTab = SettingsTab.NONE
                    }
                )

                SettingsTab.ABOUT -> AboutSmartTaskSubScreen(
                    onBack = { activeTab = SettingsTab.NONE }
                )
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
    }
}

@Composable
fun MainSettingsView(
    userName: String,
    userEmail: String,
    userAvatarUrl: String?,
    onNavigateTo: (SettingsTab) -> Unit,
    onLogoutClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    val appColors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
            .padding(bottom = 90.dp)
    ) {
        Text(
            text = "Cài đặt",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = appColors.textPrimary,
            modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
        )

        // User Profile Summary Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(appColors.glassCardBg)
                .border(1.dp, appColors.glassBorder, RoundedCornerShape(16.dp))
                .clickable { onNavigateTo(SettingsTab.PROFILE) }
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = resolveAvatarUrl(userAvatarUrl),
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = if (userName.isNotBlank()) userName.take(1).uppercase() else "N",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = userName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = userEmail,
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = appColors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main Settings List Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(appColors.glassCardBg)
                .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
                .padding(vertical = 4.dp)
        ) {
            Column {
                // CATEGORY 1: Tài khoản & bảo mật
                Text(
                    text = "Tài khoản & bảo mật",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
                )

                ClassicSettingsItem(
                    icon = Icons.Default.Person,
                    title = "Hồ sơ & tài khoản",
                    onClick = { onNavigateTo(SettingsTab.PROFILE) }
                )

                HorizontalDivider(
                    color = appColors.glassBorder.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 52.dp, end = 16.dp)
                )

                // CATEGORY 2: Cấu hình lịch & AI
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Cấu hình lịch & AI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp)
                )

                ClassicSettingsItem(
                    icon = Icons.Default.FolderSpecial,
                    title = "Không gian & nhóm công việc",
                    onClick = { onNavigateTo(SettingsTab.WORKSPACES_CATEGORIES) }
                )

                HorizontalDivider(
                    color = appColors.glassBorder.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 52.dp, end = 16.dp)
                )

                ClassicSettingsItem(
                    icon = Icons.Default.AccessTime,
                    title = "Thời gian & lịch trình",
                    onClick = { onNavigateTo(SettingsTab.WORKING_HOURS) }
                )

                HorizontalDivider(
                    color = appColors.glassBorder.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 52.dp, end = 16.dp)
                )

                ClassicSettingsItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "Trợ lý AI & quyền hạn",
                    onClick = { onNavigateTo(SettingsTab.AI_PERMISSIONS) }
                )

                HorizontalDivider(
                    color = appColors.glassBorder.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 52.dp, end = 16.dp)
                )

                // CATEGORY 3: Giao diện & thông báo
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Giao diện & thông báo",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp)
                )

                ClassicSettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Hình nền & giao diện",
                    onClick = { onNavigateTo(SettingsTab.WALLPAPER_THEME) }
                )

                HorizontalDivider(
                    color = appColors.glassBorder.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 52.dp, end = 16.dp)
                )

                ClassicSettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Thiết lập thông báo",
                    onClick = { onNavigateTo(SettingsTab.NOTIFICATIONS) }
                )

                HorizontalDivider(
                    color = appColors.glassBorder.copy(alpha = 0.4f),
                    modifier = Modifier.padding(start = 52.dp, end = 16.dp)
                )

                // CATEGORY 4: Khác
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Khác",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp)
                )

                ClassicSettingsItem(
                    icon = Icons.Default.Info,
                    title = "Về SmartTask AI",
                    onClick = { onNavigateTo(SettingsTab.ABOUT) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(appColors.glassCardBg)
                .border(1.dp, StatusRose.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .clickable { onLogoutClick() }
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = StatusRose,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đăng xuất tài khoản",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = StatusRose
                )
            }
        }
    }
}

@Composable
fun ClassicSettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = appColors.textPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = appColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = appColors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ----------------- SUB-PAGE: WORKSPACES & CATEGORIES MANAGEMENT -----------------
@Composable
fun WorkspacesCategoriesSubScreen(
    taskViewModel: TaskViewModel?,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val appColors = LocalAppColors.current

    val workspaces by taskViewModel?.workspaces?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val categories by taskViewModel?.categories?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    var showAddWsDialog by remember { mutableStateOf(false) }
    var showEditWsDialog by remember { mutableStateOf<Workspace?>(null) }
    var showAddCatDialog by remember { mutableStateOf(false) }
    var showEditCatDialog by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(Unit) {
        taskViewModel?.loadWorkspaces()
        taskViewModel?.loadCategories()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = appColors.textPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Không gian & nhóm công việc",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )
        }

        // SECTION 1: WORKSPACES (KHÔNG GIAN LÀM VIỆC)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Không gian làm việc",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary
            )
            Button(
                onClick = { showAddWsDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm không gian", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(appColors.glassCardBg)
                .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            if (workspaces.isEmpty()) {
                Text("Chưa có không gian làm việc nào", color = appColors.textSecondary, fontSize = 13.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    workspaces.forEach { ws ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(appColors.glassSurface)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FolderSpecial, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(ws.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = appColors.textPrimary)
                            }
                            Row {
                                IconButton(onClick = { showEditWsDialog = ws }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = appColors.textSecondary, modifier = Modifier.size(16.dp))
                                }
                                if (ws.id != null) {
                                    IconButton(onClick = { taskViewModel?.deleteWorkspace(ws.id) }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = StatusRose, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 2: TASK CATEGORIES (NHÓM CÔNG VIỆC)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Nhóm công việc (Danh mục)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary
            )
            Button(
                onClick = { showAddCatDialog = true },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm nhóm mới", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(appColors.glassCardBg)
                .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
                .padding(12.dp)
        ) {
            if (categories.isEmpty()) {
                Text("Chưa có nhóm công việc nào", color = appColors.textSecondary, fontSize = 13.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        val catColor = try {
                            Color(android.graphics.Color.parseColor(cat.colorCode ?: "#10B981"))
                        } catch (e: Exception) {
                            EmeraldPrimary
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(appColors.glassSurface)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(cat.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = appColors.textPrimary)
                            }
                            Row {
                                IconButton(onClick = { showEditCatDialog = cat }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = appColors.textSecondary, modifier = Modifier.size(16.dp))
                                }
                                if (cat.id != null) {
                                    IconButton(onClick = { taskViewModel?.deleteCategory(cat.id) }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = StatusRose, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // DIALOG 1: Add Workspace
    if (showAddWsDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddWsDialog = false },
            containerColor = appColors.glassCardBg,
            title = { Text("Tạo không gian mới", fontWeight = FontWeight.Bold, color = appColors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên không gian (VD: Học tập, Dự án)", color = appColors.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            taskViewModel?.createWorkspace(name.trim())
                            showAddWsDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("Tạo mới", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showAddWsDialog = false }) { Text("Hủy", color = appColors.textSecondary) }
            }
        )
    }

    // DIALOG 2: Edit Workspace
    if (showEditWsDialog != null) {
        val ws = showEditWsDialog!!
        var name by remember { mutableStateOf(ws.name) }
        AlertDialog(
            onDismissRequest = { showEditWsDialog = null },
            containerColor = appColors.glassCardBg,
            title = { Text("Chỉnh sửa không gian", fontWeight = FontWeight.Bold, color = appColors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên không gian", color = appColors.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && ws.id != null) {
                            taskViewModel?.updateWorkspace(ws.id, name.trim())
                            showEditWsDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("Lưu thay đổi", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showEditWsDialog = null }) { Text("Hủy", color = appColors.textSecondary) }
            }
        )
    }

    // DIALOG 3: Add Category
    if (showAddCatDialog) {
        var name by remember { mutableStateOf("") }
        var colorCode by remember { mutableStateOf("#10B981") }
        val colorsList = listOf("#10B981", "#3B82F6", "#8B5CF6", "#EF4444", "#F59E0B", "#EC4899")

        AlertDialog(
            onDismissRequest = { showAddCatDialog = false },
            containerColor = appColors.glassCardBg,
            title = { Text("Tạo nhóm công việc mới", fontWeight = FontWeight.Bold, color = appColors.textPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên nhóm (VD: Công việc, Sức khỏe)", color = appColors.textSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Màu đại diện:", fontSize = 12.sp, color = appColors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorsList.forEach { code ->
                            val c = Color(android.graphics.Color.parseColor(code))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(if (colorCode == code) 2.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { colorCode = code }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            taskViewModel?.createCategory(name.trim(), colorCode)
                            showAddCatDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("Tạo mới", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showAddCatDialog = false }) { Text("Hủy", color = appColors.textSecondary) }
            }
        )
    }

    // DIALOG 4: Edit Category
    if (showEditCatDialog != null) {
        val cat = showEditCatDialog!!
        var name by remember { mutableStateOf(cat.name) }
        var colorCode by remember { mutableStateOf(cat.colorCode ?: "#10B981") }
        val colorsList = listOf("#10B981", "#3B82F6", "#8B5CF6", "#EF4444", "#F59E0B", "#EC4899")

        AlertDialog(
            onDismissRequest = { showEditCatDialog = null },
            containerColor = appColors.glassCardBg,
            title = { Text("Chỉnh sửa nhóm công việc", fontWeight = FontWeight.Bold, color = appColors.textPrimary) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên nhóm", color = appColors.textSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Màu đại diện:", fontSize = 12.sp, color = appColors.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorsList.forEach { code ->
                            val c = Color(android.graphics.Color.parseColor(code))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(if (colorCode == code) 2.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { colorCode = code }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && cat.id != null) {
                            taskViewModel?.updateCategory(cat.id, name.trim(), colorCode)
                            showEditCatDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) { Text("Lưu thay đổi", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showEditCatDialog = null }) { Text("Hủy", color = appColors.textSecondary) }
            }
        )
    }
}

// ----------------- FULL-SCREEN SLIDING SUB-PAGE 1: PROFILE & ACCOUNT -----------------
@Composable
fun ProfileAccountSubScreen(
    preferences: UserPreferenceDto,
    userName: String,
    userEmail: String,
    userPhone: String,
    userAvatarUrl: String?,
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onSave: (UserPreferenceDto) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val appColors = LocalAppColors.current

    val authState by authViewModel.authState.collectAsState()
    val authRepository = remember { AuthRepository(context) }

    var fullNameInput by remember { mutableStateOf(userName) }
    var phoneInput by remember { mutableStateOf(userPhone) }

    var isUpdatingProfile by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var showChangePasswordModal by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = uriToFile(context, uri)
            if (file != null) {
                scope.launch {
                    isUploadingAvatar = true
                    val res = authRepository.uploadAvatarFile(file)
                    res.onSuccess { newAvatar ->
                        authViewModel.refreshProfileSilently()
                        Toast.makeText(context, "Đã cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: "Không thể tải ảnh đại diện lên máy chủ", Toast.LENGTH_SHORT).show()
                    }
                    isUploadingAvatar = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = appColors.textPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Hồ sơ & tài khoản",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(appColors.glassCardBg)
                .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userAvatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = resolveAvatarUrl(userAvatarUrl),
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Text(
                            text = if (userName.isNotBlank()) userName.take(1).uppercase() else "N",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (isUploadingAvatar) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                            .clickable { photoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Đổi ảnh đại diện",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = userName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = userEmail,
                    fontSize = 12.sp,
                    color = appColors.textSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text("✓ Email đã xác minh", color = EmeraldPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fullNameInput,
                    onValueChange = { fullNameInput = it },
                    label = { Text("Họ và tên", color = appColors.textSecondary) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldPrimary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = { phoneInput = it },
                    label = { Text("Số điện thoại", color = appColors.textSecondary) },
                    placeholder = { Text("Nhập số điện thoại...", color = appColors.textSecondary.copy(alpha = 0.7f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = userEmail,
                    onValueChange = {},
                    enabled = false,
                    label = { Text("Email (Định danh tài khoản)", color = appColors.textSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(disabledTextColor = appColors.textSecondary, disabledBorderColor = appColors.glassBorder),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(appColors.glassSurface)
                        .border(1.dp, appColors.glassBorder, RoundedCornerShape(14.dp))
                        .clickable { showChangePasswordModal = true }
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Đổi mật khẩu tài khoản", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = appColors.textPrimary)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = appColors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isUpdatingProfile = true
                            authRepository.updateProfile(fullNameInput, phoneInput, userAvatarUrl).onSuccess {
                                authViewModel.refreshProfileSilently()
                                onSave(preferences.copy(fullName = fullNameInput, phone = phoneInput))
                            }
                            isUpdatingProfile = false
                        }
                    },
                    enabled = !isUpdatingProfile,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isUpdatingProfile) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Lưu thay đổi hồ sơ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }

    if (showChangePasswordModal) {
        ChangePasswordDialog(
            userEmail = userEmail,
            authRepository = authRepository,
            onDismiss = { showChangePasswordModal = false }
        )
    }
}

@Composable
fun ChangePasswordDialog(
    userEmail: String,
    authRepository: AuthRepository,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appColors = LocalAppColors.current

    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    var currentPassVisible by remember { mutableStateOf(false) }
    var newPassVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isGoogleAccountMode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.glassCardBg,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isGoogleAccountMode) "Thiết lập mật khẩu mới" else "Đổi mật khẩu tài khoản",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng", tint = appColors.textSecondary)
                }
            }
        },
        text = {
            Column {
                if (!isGoogleAccountMode) {
                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it },
                        label = { Text("Mật khẩu hiện tại", color = appColors.textSecondary) },
                        trailingIcon = {
                            IconButton(onClick = { currentPassVisible = !currentPassVisible }) {
                                Icon(if (currentPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                            }
                        },
                        visualTransformation = if (currentPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("Mật khẩu mới", color = appColors.textSecondary) },
                    trailingIcon = {
                        IconButton(onClick = { newPassVisible = !newPassVisible }) {
                            Icon(if (newPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                        }
                    },
                    visualTransformation = if (newPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    label = { Text("Xác nhận mật khẩu mới", color = appColors.textSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg!!, color = NeonRose, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPass != confirmPass) {
                        errorMsg = "Mật khẩu mới không trùng khớp!"
                        return@Button
                    }
                    if (newPass.length < 8) {
                        errorMsg = "Mật khẩu phải từ 8 ký tự trở lên!"
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        errorMsg = null
                        if (isGoogleAccountMode) {
                            val res = authRepository.createPassword(newPass)
                            res.onSuccess {
                                Toast.makeText(context, "Tạo mật khẩu cho tài khoản thành công!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }.onFailure { err ->
                                errorMsg = err.message
                            }
                        } else {
                            val res = authRepository.changePassword(currentPass, newPass)
                            res.onSuccess {
                                Toast.makeText(context, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }.onFailure { err ->
                                if (err.message?.contains("chưa có mật khẩu", ignoreCase = true) == true) {
                                    isGoogleAccountMode = true
                                } else {
                                    errorMsg = err.message
                                }
                            }
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(if (isGoogleAccountMode) "Tạo mật khẩu" else "Cập nhật mật khẩu", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = appColors.textSecondary)
            }
        }
    )
}

fun parseWorkDays(workDaysStr: String?): Set<String> {
    if (workDaysStr.isNullOrBlank()) return setOf("T2", "T3", "T4", "T5", "T6")
    val upper = workDaysStr.uppercase()
    val result = mutableSetOf<String>()
    if (upper.contains("MON") || upper.contains("T2")) result.add("T2")
    if (upper.contains("TUE") || upper.contains("T3")) result.add("T3")
    if (upper.contains("WED") || upper.contains("T4")) result.add("T4")
    if (upper.contains("THU") || upper.contains("T5")) result.add("T5")
    if (upper.contains("FRI") || upper.contains("T6")) result.add("T6")
    if (upper.contains("SAT") || upper.contains("T7")) result.add("T7")
    if (upper.contains("SUN") || upper.contains("CN")) result.add("CN")
    return if (result.isEmpty()) setOf("T2", "T3", "T4", "T5", "T6") else result
}

fun formatWorkDays(selectedDays: Set<String>): String {
    val orderMap = mapOf("T2" to "MON", "T3" to "TUE", "T4" to "WED", "T5" to "THU", "T6" to "FRI", "T7" to "SAT", "CN" to "SUN")
    val daysOrder = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    return daysOrder.filter { it in selectedDays }.joinToString(",") { orderMap[it] ?: it }
}

// ----------------- FULL-SCREEN SLIDING SUB-PAGE 2: WORKING HOURS & SCHEDULE -----------------
@Composable
fun WorkingHoursSubScreen(
    preferences: UserPreferenceDto,
    onBack: () -> Unit,
    onSave: (UserPreferenceDto) -> Unit
) {
    val scrollState = rememberScrollState()
    val appColors = LocalAppColors.current

    var startTime by remember(preferences) { mutableStateOf(preferences.workStartTime ?: "08:00") }
    var endTime by remember(preferences) { mutableStateOf(preferences.workEndTime ?: "17:30") }
    var lunchStart by remember(preferences) { mutableStateOf(preferences.lunchBreakStart ?: "12:00") }
    var lunchEnd by remember(preferences) { mutableStateOf(preferences.lunchBreakEnd ?: "13:30") }
    var bufferMin by remember(preferences) { mutableStateOf(preferences.bufferMinutes ?: 15) }
    var allowWeekend by remember(preferences) { mutableStateOf(preferences.allowWeekendTasks ?: true) }
    var tz by remember(preferences) { mutableStateOf(preferences.timeZone ?: "Asia/Ho_Chi_Minh (UTC+7)") }
    var showTzDropdown by remember { mutableStateOf(false) }

    val daysList = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    var selectedDays by remember(preferences.workDays) { mutableStateOf(parseWorkDays(preferences.workDays)) }

    val timeZoneOptions = listOf(
        "Asia/Ho_Chi_Minh (UTC+7)",
        "Asia/Bangkok (UTC+7)",
        "Asia/Singapore (UTC+8)",
        "Asia/Tokyo (UTC+9)",
        "Europe/London (UTC+0)",
        "America/New_York (UTC-5)"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = appColors.textPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Thời gian & lịch trình",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(appColors.glassCardBg)
                .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Múi giờ làm việc:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))

                Box {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(appColors.glassSurface)
                            .border(1.dp, appColors.glassBorder, RoundedCornerShape(12.dp))
                            .clickable { showTzDropdown = true }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tz, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = appColors.textPrimary)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = EmeraldPrimary)
                        }
                    }

                    DropdownMenu(
                        expanded = showTzDropdown,
                        onDismissRequest = { showTzDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .heightIn(max = 280.dp)
                            .background(appColors.glassCardBg)
                    ) {
                        timeZoneOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        color = if (option == tz) EmeraldPrimary else appColors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (option == tz) EmeraldPrimary.let { FontWeight.Bold } else FontWeight.Medium
                                    )
                                },
                                onClick = {
                                    tz = option
                                    showTzDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Ngày làm việc trong tuần:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
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
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(day, color = if (isSelected) Color.White else appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Giờ làm việc (Bắt đầu - Kết thúc):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
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

                Spacer(modifier = Modifier.height(16.dp))

                Text("Khung giờ Nghỉ trưa:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
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

                Spacer(modifier = Modifier.height(16.dp))

                Text("Thời gian đệm giữa 2 việc (Buffer):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30).forEach { mins ->
                        val isSelected = bufferMin == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                .clickable { bufferMin = mins }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$mins phút", color = if (isSelected) Color.White else appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cho phép xếp việc vào Cuối tuần", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                    Switch(
                        checked = allowWeekend,
                        onCheckedChange = { allowWeekend = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        onSave(preferences.copy(
                            workStartTime = startTime,
                            workEndTime = endTime,
                            lunchBreakStart = lunchStart,
                            lunchBreakEnd = lunchEnd,
                            workDays = formatWorkDays(selectedDays),
                            bufferMinutes = bufferMin,
                            allowWeekendTasks = allowWeekend,
                            timeZone = tz
                        ))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Lưu thiết lập thời gian", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ----------------- FULL-SCREEN SLIDING SUB-PAGE 3: AI ASSISTANT & PERMISSIONS -----------------
@Composable
fun AiPermissionsSubScreen(
    preferences: UserPreferenceDto,
    onBack: () -> Unit,
    onSave: (UserPreferenceDto) -> Unit
) {
    val scrollState = rememberScrollState()
    val appColors = LocalAppColors.current

    var autoFind by remember(preferences) { mutableStateOf(preferences.aiAutoFindSlot ?: true) }
    var alwaysAsk by remember(preferences) { mutableStateOf(preferences.aiAlwaysAskBeforeChange ?: true) }
    var allowSplit by remember(preferences) { mutableStateOf(preferences.aiAllowSplitLongTasks ?: true) }
    var moveFixed by remember(preferences) { mutableStateOf(preferences.aiAutoMoveFixedTasks ?: false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = appColors.textPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Trợ lý AI & quyền hạn",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(appColors.glassCardBg)
                .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tự động tìm khung giờ trống", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                        Text("AI tự động sắp xếp công việc vào thời gian rảnh hợp lý.", fontSize = 12.sp, color = appColors.textSecondary)
                    }
                    Switch(
                        checked = autoFind,
                        onCheckedChange = { autoFind = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                    )
                }

                HorizontalDivider(color = appColors.glassBorder.copy(alpha = 0.5f))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hỏi trước khi thay đổi lịch trình", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                        Text("Luôn hiển thị đề xuất AI để bạn duyệt trước khi áp dụng.", fontSize = 12.sp, color = appColors.textSecondary)
                    }
                    Switch(
                        checked = alwaysAsk,
                        onCheckedChange = { alwaysAsk = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                    )
                }

                HorizontalDivider(color = appColors.glassBorder.copy(alpha = 0.5f))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cho phép chia nhỏ công việc dài", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                        Text("Tự động tách các việc >2 tiếng thành các phiên Pomodoro nhỏ.", fontSize = 12.sp, color = appColors.textSecondary)
                    }
                    Switch(
                        checked = allowSplit,
                        onCheckedChange = { allowSplit = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                    )
                }

                HorizontalDivider(color = appColors.glassBorder.copy(alpha = 0.5f))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tự động dời lịch công việc cố định", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                        Text("Cho phép AI thay đổi khung giờ họp/lịch đã chốt khi trùng.", fontSize = 12.sp, color = appColors.textSecondary)
                    }
                    Switch(
                        checked = moveFixed,
                        onCheckedChange = { moveFixed = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onSave(preferences.copy(
                            aiAutoFindSlot = autoFind,
                            aiAlwaysAskBeforeChange = alwaysAsk,
                            aiAllowSplitLongTasks = allowSplit,
                            aiAutoMoveFixedTasks = moveFixed
                        ))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Lưu thiết lập AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ----------------- FULL-SCREEN SLIDING SUB-PAGE 4: WALLPAPER & THEMES -----------------
@Composable
fun WallpaperThemeSubScreen(
    preferences: UserPreferenceDto,
    taskViewModel: TaskViewModel?,
    onBack: () -> Unit,
    onSave: (UserPreferenceDto) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val appColors = LocalAppColors.current

    var selectedLang by remember(preferences) { mutableStateOf(preferences.language ?: "vi") }
    var selectedTheme by remember(preferences) { mutableStateOf(preferences.theme ?: "DARK") }
    var selectedBgType by remember(preferences) { mutableStateOf(preferences.backgroundType ?: "PRESET") }
    var darkness by remember(preferences) { mutableStateOf(preferences.overlayDarkness ?: 60) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64Data = convertUriToBase64DataUri(context, uri)
            if (base64Data != null) {
                selectedBgType = "CUSTOM"
                val updatedPref = preferences.copy(
                    backgroundType = "CUSTOM",
                    backgroundUrl = base64Data,
                    overlayDarkness = darkness
                )
                taskViewModel?.updateUserPreferences(updatedPref)
                Toast.makeText(context, "Đã cập nhật hình nền tùy chỉnh!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = appColors.textPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Hình nền & giao diện",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(appColors.glassCardBg)
                .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Ngôn ngữ giao diện:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mapOf("vi" to "Tiếng Việt 🇻🇳", "en" to "English 🇺🇸").forEach { (code, item) ->
                        val isSelected = selectedLang == code
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                .clickable { selectedLang = code }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(item, color = if (isSelected) Color.White else appColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text("Chế độ chủ đề (Theme):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mapOf("DARK" to "Tối (Dark) 🌙", "LIGHT" to "Sáng (Light) ☀️").forEach { (mode, label) ->
                        val isSelected = selectedTheme == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                .clickable { selectedTheme = mode }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isSelected) Color.White else appColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text("Loại hình nền (Background):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mapOf("PRESET" to "Mặc định (Kính)", "CUSTOM" to "Tùy chỉnh ảnh").forEach { (type, label) ->
                        val isSelected = selectedBgType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                .clickable { selectedBgType = type }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isSelected) Color.White else appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (selectedBgType == "CUSTOM") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tải ảnh hình nền từ thiết bị", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text("Độ phủ mờ background (Overlay Darkness: ${darkness}%):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Slider(
                    value = darkness.toFloat(),
                    onValueChange = { darkness = it.toInt() },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = EmeraldPrimary, activeTrackColor = EmeraldPrimary)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onSave(preferences.copy(
                            language = selectedLang,
                            theme = selectedTheme,
                            backgroundType = selectedBgType,
                            overlayDarkness = darkness
                        ))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Áp dụng giao diện", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ----------------- FULL-SCREEN SLIDING SUB-PAGE 5: NOTIFICATIONS DETAILS -----------------
@Composable
fun NotificationsDetailSubScreen(
    preferences: UserPreferenceDto,
    onBack: () -> Unit,
    onSave: (UserPreferenceDto) -> Unit
) {
    val scrollState = rememberScrollState()
    val appColors = LocalAppColors.current

    var notifyBefore by remember(preferences) { mutableStateOf(preferences.reminderMinutesBefore ?: 15) }
    var quietStart by remember(preferences) { mutableStateOf(preferences.dndStartTime ?: "22:00") }
    var quietEnd by remember(preferences) { mutableStateOf(preferences.dndEndTime ?: "07:00") }
    var pushEnabled by remember(preferences) { mutableStateOf(preferences.enableDeviceNotifications ?: true) }
    var summaryEnabled by remember(preferences) { mutableStateOf(preferences.enableMorningSummaryEmail ?: true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = appColors.textPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Thiết lập thông báo",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(appColors.glassCardBg)
                .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Thời gian nhắc trước khi bắt đầu:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30).forEach { mins ->
                        val isSelected = notifyBefore == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                .clickable { notifyBefore = mins }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("$mins phút", color = if (isSelected) Color.White else appColors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text("Khung giờ Không làm phiền (Do Not Disturb):", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quietStart,
                        onValueChange = { quietStart = it },
                        label = { Text("Từ", color = appColors.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = quietEnd,
                        onValueChange = { quietEnd = it },
                        label = { Text("Đến", color = appColors.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("SmartTask sẽ không gửi nhắc việc trong khung giờ này.", fontSize = 12.sp, color = appColors.textSecondary)

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Thông báo trên thiết bị", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                        Text("Nhắc công việc và cảnh báo trùng lịch.", fontSize = 12.sp, color = appColors.textSecondary)
                    }
                    Switch(
                        checked = pushEnabled,
                        onCheckedChange = { pushEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = appColors.glassBorder.copy(alpha = 0.5f))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tóm tắt lịch mỗi sáng", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                        Text("Gửi email tóm tắt công việc lúc 07:30 mỗi ngày.", fontSize = 12.sp, color = appColors.textSecondary)
                    }
                    Switch(
                        checked = summaryEnabled,
                        onCheckedChange = { summaryEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = EmeraldPrimary)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onSave(preferences.copy(
                            reminderMinutesBefore = notifyBefore,
                            dndStartTime = quietStart,
                            dndEndTime = quietEnd,
                            enableDeviceNotifications = pushEnabled,
                            enableMorningSummaryEmail = summaryEnabled
                        ))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Lưu cài đặt thông báo", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ----------------- FULL-SCREEN SLIDING SUB-PAGE 6: ABOUT APP -----------------
@Composable
fun AboutSmartTaskSubScreen(
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    val appColors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .padding(bottom = 90.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = appColors.textPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Về SmartTask AI",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(appColors.glassCardBg)
                .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(EmeraldPrimary, MintAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "SmartTask AI",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = appColors.textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Phiên bản 2.4.0 (Build 2026.11)",
                    fontSize = 12.sp,
                    color = appColors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = appColors.glassBorder)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SmartTask AI là hệ thống quản lý và lập lịch công việc cá nhân thông minh hàng đầu, kết hợp giữa Ma trận Eisenhower, thuật toán phân bổ thời gian tự động và trợ lý ảo AI Gemini.",
                    fontSize = 13.sp,
                    color = appColors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Đồ án Tốt nghiệp CNTT © 2026 SmartTask AI Team",
                    fontSize = 11.sp,
                    color = appColors.textSecondary.copy(alpha = 0.7f)
                )
            }
        }
    }
}
