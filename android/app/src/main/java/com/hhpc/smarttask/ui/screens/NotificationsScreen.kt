package com.hhpc.smarttask.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hhpc.smarttask.data.api.ThongBaoDto
import com.hhpc.smarttask.data.repository.NotificationRepository
import com.hhpc.smarttask.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.OffsetDateTime

fun formatNotificationTime(createdAtStr: String?): String {
    if (createdAtStr.isNullOrBlank()) return "Gần đây"
    return try {
        val isoStr = if (createdAtStr.contains("Z") || createdAtStr.contains("+")) createdAtStr else "${createdAtStr}+07:00"
        val created = OffsetDateTime.parse(isoStr)
        val now = OffsetDateTime.now()
        val diffSec = Duration.between(created, now).seconds
        when {
            diffSec < 60 -> "Vừa xong"
            diffSec < 3600 -> "${diffSec / 60} phút trước"
            diffSec < 86400 -> "${diffSec / 3600} giờ trước"
            diffSec < 172800 -> "Hôm qua"
            else -> "${diffSec / 86400} ngày trước"
        }
    } catch (e: Exception) {
        "Gần đây"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    taskViewModel: TaskViewModel
) {
    val context = LocalContext.current
    val notificationRepository = remember { NotificationRepository(context) }
    val appColors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    var notifications by remember { mutableStateOf<List<ThongBaoDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    fun loadNotifications() {
        scope.launch {
            isLoading = true
            val res = notificationRepository.getNotifications()
            res.onSuccess { list ->
                notifications = list
                if (list.isEmpty()) {
                    val evalRes = notificationRepository.evaluateNotifications()
                    evalRes.onSuccess { evalList -> notifications = evalList }
                }
            }
            isLoading = false
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            loadNotifications()
            delay(5000L)
        }
    }

    val unreadCount = remember(notifications) { notifications.count { it.isRead != true } }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            loadNotifications()
        },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Clean Header Bar with Always Visible "Xóa tất cả" Button!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Thông báo",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(StatusRose)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$unreadCount mới",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Action Buttons Row: Đọc hết | Xóa tất cả
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Đọc hết Button
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    notificationRepository.markAllAsRead().onSuccess {
                                        notifications = notifications.map { it.copy(isRead = true) }
                                    }
                                }
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Đánh dấu tất cả là đã đọc",
                                tint = appColors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Xóa tất cả Button (ALWAYS VISIBLE AT TOP RIGHT)
                    OutlinedButton(
                        onClick = {
                            if (notifications.isNotEmpty()) {
                                showClearAllConfirmDialog = true
                            }
                        },
                        enabled = notifications.isNotEmpty(),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (notifications.isNotEmpty()) StatusRose.copy(alpha = 0.5f) else appColors.glassBorder
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (notifications.isNotEmpty()) StatusRose.copy(alpha = 0.12f) else appColors.glassSurface,
                            contentColor = StatusRose,
                            disabledContentColor = appColors.textSecondary.copy(alpha = 0.4f)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xóa tất cả",
                                tint = if (notifications.isNotEmpty()) StatusRose else appColors.textSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Xóa tất cả",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (notifications.isNotEmpty()) StatusRose else appColors.textSecondary.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Notifications List / Minimal Empty State
            if (isLoading && notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(28.dp))
                }
            } else if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = appColors.textSecondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Không có thông báo nào",
                            color = appColors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 85.dp)
                ) {
                    items(notifications, key = { it.id ?: 0 }) { item ->
                        val isUnread = item.isRead != true

                        val (iconVector, iconTint) = when (item.type?.lowercase()) {
                            "reminder" -> Icons.Default.Schedule to StatusCyan
                            "ai" -> Icons.Default.AutoAwesome to StatusPurple
                            "alert" -> Icons.Default.Warning to StatusAmber
                            "success" -> Icons.Default.CheckCircle to EmeraldPrimary
                            else -> Icons.Default.Notifications to EmeraldPrimary
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isUnread) appColors.glassCardBg else appColors.glassCardBg.copy(alpha = 0.6f))
                                .border(
                                    width = 1.dp,
                                    color = if (isUnread) EmeraldPrimary.copy(alpha = 0.6f) else appColors.glassBorder,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    if (isUnread && item.id != null) {
                                        scope.launch {
                                            notificationRepository.markAsRead(item.id).onSuccess { updated ->
                                                notifications = notifications.map { if (it.id == item.id) updated else it }
                                            }
                                        }
                                    }
                                }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Left Icon Circle
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(iconTint.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Main Content (Title, Message, TimeAgo)
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = item.title,
                                            fontSize = 14.sp,
                                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                                            color = appColors.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = formatNotificationTime(item.createdAt),
                                            fontSize = 10.sp,
                                            color = appColors.textSecondary,
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }

                                    if (!item.message.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.message,
                                            fontSize = 12.sp,
                                            color = appColors.textSecondary,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Unread Dot & Delete Action Button
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isUnread) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(StatusRose)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            if (item.id != null) {
                                                scope.launch {
                                                    notificationRepository.deleteNotification(item.id).onSuccess {
                                                        notifications = notifications.filter { n -> n.id != item.id }
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Xóa thông báo",
                                            tint = appColors.textSecondary.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Confirmation Prompt before clearing all notifications
        if (showClearAllConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllConfirmDialog = false },
                containerColor = appColors.glassCardBg,
                title = {
                    Text(
                        text = "Xóa tất cả thông báo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "Bạn có chắc chắn muốn xóa tất cả ${notifications.size} thông báo không? Thao tác này sẽ xóa khỏi cơ sở dữ liệu.",
                        fontSize = 13.sp,
                        color = appColors.textSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearAllConfirmDialog = false
                            scope.launch {
                                notificationRepository.clearAllNotifications().onSuccess {
                                    notifications = emptyList()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusRose)
                    ) {
                        Text("Xóa tất cả", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllConfirmDialog = false }) {
                        Text("Hủy", color = appColors.textSecondary)
                    }
                }
            )
        }
    }
}
