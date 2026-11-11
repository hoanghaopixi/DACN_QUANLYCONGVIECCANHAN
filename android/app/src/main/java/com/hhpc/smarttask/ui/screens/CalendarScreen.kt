package com.hhpc.smarttask.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Schedule
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
import com.hhpc.smarttask.data.repository.TaskRepository
import com.hhpc.smarttask.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarScreen(
    taskViewModel: TaskViewModel
) {
    val tasks by taskViewModel.tasks.collectAsState()
    val context = LocalContext.current
    val taskRepository = remember { TaskRepository(context) }
    val scope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var viewMode by remember { mutableStateOf("WEEK") } // "DAY", "WEEK", "MONTH"
    var isScheduling by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var scheduleMessage by remember { mutableStateOf<String?>(null) }

    val appColors = LocalAppColors.current

    // Generate current week days
    val startOfWeek = remember { LocalDate.now().minusDays(LocalDate.now().dayOfWeek.value.toLong() - 1) }
    val weekDays = remember { (0..6).map { startOfWeek.plusDays(it.toLong()) } }

    // Filter tasks by selected date (Matching Web Calendar!)
    val dayTasks = remember(tasks, selectedDate) {
        val dateIso = selectedDate.toString()
        tasks.filter { task ->
            task.startTime != null && (
                task.startTime.contains(dateIso) ||
                task.startTime.startsWith(dateIso)
            )
        }
    }

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
            // Clean Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lịch trình công việc",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )

                // AI Auto Schedule Button
                Button(
                    onClick = {
                        scope.launch {
                            isScheduling = true
                            scheduleMessage = null
                            val result = taskRepository.autoSchedule(
                                workspaceId = taskViewModel.selectedWorkspaceId.value,
                                applyImmediately = true
                            )
                            result.onSuccess { res ->
                                scheduleMessage = res.aiStrategySummary ?: "Đã tự động xếp lịch bằng AI thành công!"
                                taskViewModel.loadTasks()
                                taskViewModel.loadEisenhower()
                            }.onFailure { e ->
                                scheduleMessage = e.message ?: "Lỗi tự động lập lịch"
                            }
                            isScheduling = false
                        }
                    },
                    enabled = !isScheduling,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isScheduling) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = LimeHighlight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Xếp lịch AI", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (scheduleMessage != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = scheduleMessage!!,
                    color = EmeraldPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Day / Week / Month View Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mapOf("DAY" to "Ngày", "WEEK" to "Tuần", "MONTH" to "Tháng").forEach { (key, label) ->
                    val isSelected = viewMode == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) EmeraldPrimary else appColors.glassCardBg)
                            .border(1.dp, if (isSelected) EmeraldPrimary else appColors.glassBorder, RoundedCornerShape(12.dp))
                            .clickable { viewMode = key }
                            .padding(vertical = 8.dp),
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

            // Days of the week bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(count = weekDays.size) { index ->
                    val date = weekDays[index]
                    val isSelected = date == selectedDate
                    val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("vi"))
                    val dayNumber = date.dayOfMonth.toString()

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .width(52.dp)
                            .height(68.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) EmeraldPrimary else appColors.glassCardBg)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) EmeraldPrimary else appColors.glassBorder,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedDate = date }
                            .padding(8.dp)
                    ) {
                        Text(
                            text = dayName,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else appColors.textSecondary
                        )
                        Text(
                            text = dayNumber,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else appColors.textPrimary
                        )
                    }
                }
            }

            // Agenda Hourly Timeline for selectedDate
            if (dayTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = appColors.textSecondary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Không có lịch trình cho ngày này",
                            color = appColors.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Bấm 'Xếp lịch AI' để tự động phân bổ lịch trình",
                            color = appColors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(count = dayTasks.size) { index ->
                        val task = dayTasks[index]
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(appColors.glassCardBg)
                                .border(1.dp, appColors.glassBorder, RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = task.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.textPrimary
                                    )
                                    Text(
                                        text = "Thời lượng dự kiến: ${task.estimatedDurationMinutes ?: 30} phút",
                                        fontSize = 12.sp,
                                        color = appColors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
