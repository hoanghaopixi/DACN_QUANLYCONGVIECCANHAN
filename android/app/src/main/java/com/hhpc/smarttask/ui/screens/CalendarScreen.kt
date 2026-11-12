package com.hhpc.smarttask.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hhpc.smarttask.data.model.Category
import com.hhpc.smarttask.data.model.TaskDto
import com.hhpc.smarttask.data.repository.TaskRepository
import com.hhpc.smarttask.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarScreen(
    taskViewModel: TaskViewModel
) {
    val tasks by taskViewModel.tasks.collectAsState()
    val workspaces by taskViewModel.workspaces.collectAsState()
    val categories by taskViewModel.categories.collectAsState()
    val selectedWsId by taskViewModel.selectedWorkspaceId.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var viewMode by remember { mutableStateOf("DAY") } // "DAY", "WEEK", "MONTH"
    var isRefreshing by remember { mutableStateOf(false) }

    var showWorkspaceDropdown by remember { mutableStateOf(false) }
    var showViewModeDropdown by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    var editingTask by remember { mutableStateOf<TaskDto?>(null) }
    var taskToUnlock by remember { mutableStateOf<TaskDto?>(null) }

    val appColors = LocalAppColors.current

    val currentWs = workspaces.firstOrNull { it.id == selectedWsId }
    val wsName = currentWs?.name ?: "Không gian hiện tại"

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            delay(30000L)
        }
    }

    val dateFormatted = remember(selectedDate) {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        selectedDate.format(formatter)
    }

    val dayTasks = remember(tasks, selectedDate) {
        val dateIso = selectedDate.toString()
        tasks.filter { task ->
            task.startTime != null && (
                task.startTime.contains(dateIso) ||
                task.startTime.startsWith(dateIso)
            )
        }.sortedBy { parseTimeToMinutes(it.startTime) }
    }
    val weekStart = selectedDate.minusDays(selectedDate.dayOfWeek.value.toLong() - 1)
    val weekDates = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }

    fun handleOpenTask(task: TaskDto) {
        if (task.isLocked == true) {
            taskToUnlock = task
        } else {
            editingTask = task
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                val startTime = System.currentTimeMillis()
                taskViewModel.loadTasks()
                val elapsedTime = System.currentTimeMillis() - startTime
                val remainingTime = 1000L - elapsedTime
                if (remainingTime > 0) {
                    delay(remainingTime)
                }
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
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Title Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lịch của tôi",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )

                    // View Mode Switcher Dropdown
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(EmeraldPrimary)
                                .clickable { showViewModeDropdown = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when (viewMode) {
                                        "WEEK" -> "Tuần"
                                        "MONTH" -> "Tháng"
                                        else -> "Ngày"
                                    },
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showViewModeDropdown,
                            onDismissRequest = { showViewModeDropdown = false },
                            modifier = Modifier.background(appColors.glassCardBg)
                        ) {
                            mapOf("DAY" to "Ngày", "WEEK" to "Tuần", "MONTH" to "Tháng").forEach { (mode, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, color = appColors.textPrimary, fontSize = 13.sp) },
                                    onClick = {
                                        viewMode = mode
                                        showViewModeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Workspace Switcher Bar
                Box(
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(appColors.glassCardBg)
                            .border(1.dp, appColors.glassBorder, RoundedCornerShape(16.dp))
                            .clickable { showWorkspaceDropdown = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = wsName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = appColors.textPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showWorkspaceDropdown,
                        onDismissRequest = { showWorkspaceDropdown = false },
                        modifier = Modifier.background(appColors.glassCardBg)
                    ) {
                        workspaces.forEach { ws ->
                            DropdownMenuItem(
                                text = { Text(ws.name, color = appColors.textPrimary, fontSize = 13.sp) },
                                onClick = {
                                    taskViewModel.selectWorkspace(ws.id)
                                    showWorkspaceDropdown = false
                                }
                            )
                        }
                    }
                }

                // Date Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { selectedDate = selectedDate.minusDays(1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Ngày trước",
                                tint = appColors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = dateFormatted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = { selectedDate = selectedDate.plusDays(1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Ngày sau",
                                tint = appColors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    TextButton(
                        onClick = { selectedDate = LocalDate.now() }
                    ) {
                        Text("Hôm nay", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // MAIN AREA: 30-Minute Timetable Grid
                if (viewMode == "MONTH") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        MonthCalendarGrid(
                            selectedDate = selectedDate,
                            tasks = tasks,
                            onDateSelected = { date -> selectedDate = date }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Lịch trình ngày $dateFormatted",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        if (dayTasks.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(appColors.glassCardBg)
                                    .border(1.dp, appColors.glassBorder, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Không có lịch trình cho ngày này", color = appColors.textSecondary, fontSize = 13.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 85.dp)
                            ) {
                                items(count = dayTasks.size) { index ->
                                    val task = dayTasks[index]
                                    TaskScheduleRow(task = task, categories = categories, appColors = appColors, onClick = { handleOpenTask(task) })
                                }
                            }
                        }
                    }
                } else if (viewMode == "WEEK") {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        weekDates.forEach { date ->
                            val dateTasks = tasks.filter { it.startTime?.startsWith(date.toString()) == true }
                                .sortedBy { parseTimeToMinutes(it.startTime) }
                            if (dateTasks.isNotEmpty()) {
                                item(key = "week-${date}") {
                                    Text(
                                        text = "${date.dayOfMonth}/${date.monthValue} · ${date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale("vi"))}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = appColors.textSecondary,
                                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                    )
                                }
                                items(count = dateTasks.size, key = { index -> "${date}-${dateTasks[index].id ?: index}" }) { index ->
                                    val task = dateTasks[index]
                                    TaskScheduleRow(task = task, categories = categories, appColors = appColors, onClick = { handleOpenTask(task) })
                                }
                            }
                        }
                    }
                } else {
                    TimetableHourGrid(
                        selectedDate = selectedDate,
                        currentTime = currentTime,
                        tasks = dayTasks,
                        categories = categories,
                        appColors = appColors,
                        onTaskClick = { handleOpenTask(it) },
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Floating Action Button
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Thêm lịch",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Thêm lịch",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Add Task Glass Dialog
        if (showAddDialog) {
            AddTaskDialog(
                taskViewModel = taskViewModel,
                onDismiss = { showAddDialog = false },
                onAdd = { newTask ->
                    taskViewModel.createTask(newTask.title, newTask.description, newTask.priority ?: "MEDIUM", newTask.eisenhowerQuadrant ?: "SCHEDULE")
                    showAddDialog = false
                }
            )
        }

        if (taskToUnlock != null) {
            VerifyPasswordDialog(
                onDismiss = { taskToUnlock = null },
                onSuccess = {
                    editingTask = taskToUnlock
                    taskToUnlock = null
                }
            )
        }

        if (editingTask != null) {
            EditTaskDialog(
                task = editingTask!!,
                taskViewModel = taskViewModel,
                onDismiss = { editingTask = null },
                onSave = { updated ->
                    editingTask?.id?.let { id ->
                        taskViewModel.updateTask(id, updated)
                    }
                    editingTask = null
                }
            )
        }
    }
}

@Composable
fun ColumnScope.TimetableHourGrid(
    selectedDate: LocalDate,
    currentTime: LocalTime,
    tasks: List<TaskDto>,
    categories: List<Category>,
    appColors: AppColors,
    onTaskClick: (TaskDto) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isToday = selectedDate == LocalDate.now()
    val scrollState = rememberScrollState()

    val slotHeightDp = 32.dp
    val totalGridHeight = slotHeightDp * 48

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(appColors.glassCardBg)
            .border(1.dp, appColors.glassBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalGridHeight)
            ) {
                for (slot in 0..47) {
                    val slotTopOffset = slotHeightDp * slot
                    val hour = slot / 2
                    val min = (slot % 2) * 30
                    val isFullHour = min == 0
                    val timeText = String.format(Locale.getDefault(), "%02d:%02d", hour, min)

                    Text(
                        text = timeText,
                        fontSize = if (isFullHour) 11.sp else 10.sp,
                        fontWeight = if (isFullHour) FontWeight.Bold else FontWeight.Normal,
                        color = if (isFullHour) appColors.textPrimary else appColors.textSecondary.copy(alpha = 0.7f),
                        modifier = Modifier
                            .offset(x = 8.dp, y = slotTopOffset - 7.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .offset(y = slotTopOffset)
                            .padding(start = 54.dp)
                            .background(
                                if (isFullHour) appColors.glassBorder
                                else appColors.glassBorder.copy(alpha = 0.3f)
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .offset(x = 54.dp)
                        .background(appColors.glassBorder)
                )

                // Task Blocks Positioned on Grid (with Category Colors & Checkmarks)
                tasks.forEach { task ->
                    val startMin = parseTimeToMinutes(task.startTime)
                    val duration = task.estimatedDurationMinutes ?: 30
                    val topOffset = (startMin / 30f) * slotHeightDp.value
                    val blockHeight = ((duration / 30f) * slotHeightDp.value).coerceAtLeast(36f)

                    val category = categories.firstOrNull { it.id == task.categoryId }
                    val catColor = try {
                        Color(android.graphics.Color.parseColor(category?.colorCode ?: "#10B981"))
                    } catch (e: Exception) {
                        EmeraldPrimary
                    }

                    val isLocked = task.isLocked == true
                    val isCompleted = task.status == "COMPLETED"
                    val startStr = if (!task.startTime.isNullOrBlank()) task.startTime.substringAfter("T").take(5) else "08:00"
                    val endStr = if (!task.endTime.isNullOrBlank()) task.endTime.substringAfter("T").take(5) else ""
                    val timeRangeStr = if (endStr.isNotBlank()) "$startStr–$endStr" else "$startStr · $duration phút"

                    val cardBgColor = if (isLocked) Color(0xFF64748B).copy(alpha = 0.25f) else catColor.copy(alpha = 0.15f)
                    val cardBorderColor = if (isLocked) Color(0xFF64748B) else catColor

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(blockHeight.dp)
                            .offset(y = topOffset.dp)
                            .padding(start = 60.dp, end = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(cardBgColor)
                            .border(1.5.dp, cardBorderColor, RoundedCornerShape(12.dp))
                            .clickable { onTaskClick(task) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isLocked) "🔒 Công việc riêng tư" else task.title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = appColors.textPrimary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = timeRangeStr,
                                    fontSize = 10.sp,
                                    color = if (isLocked) appColors.textSecondary else catColor,
                                    fontWeight = FontWeight.SemiBold
                                )

                                if (blockHeight >= 50) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when {
                                                    isCompleted -> EmeraldPrimary.copy(alpha = 0.2f)
                                                    task.status == "OVERDUE" -> StatusRose.copy(alpha = 0.2f)
                                                    task.status == "IN_PROGRESS" -> StatusAmber.copy(alpha = 0.2f)
                                                    else -> appColors.glassSurface
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when {
                                                isCompleted -> "✓ Đã hoàn thành"
                                                task.status == "OVERDUE" -> "Đã qua"
                                                task.status == "IN_PROGRESS" -> "Đang làm"
                                                else -> "Cần làm"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                isCompleted -> EmeraldPrimary
                                                task.status == "OVERDUE" -> StatusRose
                                                task.status == "IN_PROGRESS" -> StatusAmber
                                                else -> appColors.textSecondary
                                            }
                                        )
                                    }
                                }
                            }

                            if (isCompleted && !isLocked) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Đã hoàn thành",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (isToday) {
                    val currentMinInDay = currentTime.hour * 60 + currentTime.minute
                    val redLineTopOffset = (currentMinInDay / 30f) * slotHeightDp.value

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = redLineTopOffset.dp)
                            .padding(start = 48.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color(0xFFEF4444))
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthCalendarGrid(
    selectedDate: LocalDate,
    tasks: List<TaskDto>,
    onDateSelected: (LocalDate) -> Unit
) {
    val appColors = LocalAppColors.current
    val daysInMonth = selectedDate.lengthOfMonth()
    val firstDayOfMonth = selectedDate.withDayOfMonth(1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
    val emptySlots = firstDayOfWeek - 1
    val totalSlots = emptySlots + daysInMonth
    val weeks = (0 until totalSlots).chunked(7)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(appColors.glassCardBg)
            .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { day ->
                Text(
                    text = day,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        weeks.forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                for (i in 0 until 7) {
                    if (i < week.size) {
                        val slot = week[i]
                        if (slot < emptySlots) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val dayNumber = slot - emptySlots + 1
                            val date = selectedDate.withDayOfMonth(dayNumber)
                            val isSelected = date == selectedDate
                            val isToday = date == LocalDate.now()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelected -> EmeraldPrimary
                                            isToday -> EmeraldPrimary.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { onDateSelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else appColors.textPrimary
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskScheduleRow(
    task: TaskDto,
    categories: List<Category>,
    appColors: AppColors,
    onClick: () -> Unit = {}
) {
    val start = task.startTime?.substringAfter('T', "")?.take(5)?.ifBlank { "--:--" } ?: "--:--"
    val end = task.endTime?.substringAfter('T', "")?.take(5)?.ifBlank { "" } ?: ""

    val category = categories.firstOrNull { it.id == task.categoryId }
    val catColor = try {
        Color(android.graphics.Color.parseColor(category?.colorCode ?: "#10B981"))
    } catch (e: Exception) {
        EmeraldPrimary
    }

    val isCompleted = task.status == "COMPLETED"
    val isLocked = task.isLocked == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isLocked) Color(0xFF64748B).copy(alpha = 0.2f) else appColors.glassCardBg)
            .border(1.dp, if (isLocked) Color(0xFF64748B) else appColors.glassBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.dp).height(34.dp).clip(CircleShape).background(if (isLocked) Color(0xFF64748B) else catColor))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(if (isLocked) "🔒 Công việc riêng tư" else task.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = appColors.textPrimary, maxLines = 1)
            Text(
                text = if (end.isNotBlank()) "$start – $end" else "$start · ${task.estimatedDurationMinutes ?: 30} phút",
                fontSize = 11.sp,
                color = appColors.textSecondary
            )
        }
        if (isCompleted && !isLocked) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

private fun parseTimeToMinutes(timeStr: String?): Int {
    if (timeStr.isNullOrBlank()) return 8 * 60
    return try {
        if (timeStr.contains("T")) {
            val timePart = timeStr.substringAfter("T").take(5)
            val parts = timePart.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            h * 60 + m
        } else if (timeStr.contains(":")) {
            val parts = timeStr.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            h * 60 + m
        } else {
            8 * 60
        }
    } catch (e: Exception) {
        8 * 60
    }
}
