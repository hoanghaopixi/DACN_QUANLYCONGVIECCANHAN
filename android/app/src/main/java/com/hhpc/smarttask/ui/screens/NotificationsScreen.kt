package com.hhpc.smarttask.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hhpc.smarttask.ui.theme.*
import kotlinx.coroutines.launch

data class NotificationItem(
    val id: Long,
    val title: String,
    val message: String,
    val timeAgo: String,
    val icon: ImageVector,
    val isUnread: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    taskViewModel: TaskViewModel
) {
    val appColors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    val notifications = listOf(
        NotificationItem(
            id = 1,
            title = "Tự động xếp lịch AI thành công",
            message = "Gemini AI đã tự động bổ sung 3 công việc quan trọng vào khung giờ vàng buổi sáng.",
            timeAgo = "10 phút trước",
            icon = Icons.Default.ElectricBolt,
            isUnread = true
        ),
        NotificationItem(
            id = 2,
            title = "Nhắc nhở công việc sắp tới",
            message = "Nhiệm vụ 'Làm báo cáo đồ án' sẽ bắt đầu sau 15 phút nữa. Hãy chuẩn bị nhé!",
            timeAgo = "30 phút trước",
            icon = Icons.Default.Schedule,
            isUnread = true
        ),
        NotificationItem(
            id = 3,
            title = "Cảnh báo trùng lịch",
            message = "Đã phát hiện và tự động giải quyết 1 cuộc họp bị trùng giờ trong Lịch trình cá nhân.",
            timeAgo = "2 giờ trước",
            icon = Icons.Default.Warning,
            isUnread = false
        ),
        NotificationItem(
            id = 4,
            title = "Tóm tắt năng suất ngày",
            message = "Bạn đã hoàn thành 85% mục tiêu công việc ngày hôm nay. Đạt điểm năng suất Xuất sắc!",
            timeAgo = "Hôm qua",
            icon = Icons.Default.CheckCircle,
            isUnread = false
        )
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                taskViewModel.loadTasks()
                isRefreshing = false
            }
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
            // Clean Header in Sentence Case without leading icon & without redundant subtitle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "Thông báo & nhắc hẹn",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                items(notifications) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (item.isUnread) appColors.glassCardBg else appColors.glassCardBg.copy(alpha = 0.6f))
                            .border(
                                width = 1.dp,
                                color = if (item.isUnread) EmeraldPrimary.copy(alpha = 0.6f) else appColors.glassBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (item.isUnread) EmeraldPrimary.copy(alpha = 0.2f) else appColors.glassSurface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = if (item.isUnread) EmeraldPrimary else appColors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = item.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = item.timeAgo,
                                        fontSize = 10.sp,
                                        color = appColors.textSecondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = item.message,
                                    fontSize = 12.sp,
                                    color = appColors.textSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
