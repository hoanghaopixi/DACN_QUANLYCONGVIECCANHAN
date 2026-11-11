package com.hhpc.smarttask.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hhpc.smarttask.data.model.TaskDto
import com.hhpc.smarttask.ui.theme.*
import kotlinx.coroutines.launch

fun formatToDdMmYyyy(rawDate: String?): String {
    if (rawDate.isNullOrBlank()) return ""
    return try {
        if (rawDate.contains("T")) {
            val datePart = rawDate.substringBefore("T") // e.g. "2026-10-14"
            val timePart = rawDate.substringAfter("T").take(5) // e.g. "16:00"
            val parts = datePart.split("-")
            if (parts.size == 3) {
                val formattedDate = "${parts[2]}/${parts[1]}/${parts[0]}" // "14/10/2026"
                "$timePart · $formattedDate"
            } else {
                rawDate
            }
        } else if (rawDate.contains("-")) {
            val parts = rawDate.split("-")
            if (parts.size == 3) {
                "${parts[2]}/${parts[1]}/${parts[0]}" // "14/10/2026"
            } else {
                rawDate
            }
        } else {
            rawDate
        }
    } catch (e: Exception) {
        rawDate
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    taskViewModel: TaskViewModel
) {
    val tasks by taskViewModel.tasks.collectAsState()
    val workspaces by taskViewModel.workspaces.collectAsState()
    val selectedWsId by taskViewModel.selectedWorkspaceId.collectAsState()
    val isLoading by taskViewModel.isLoading.collectAsState()
    val report by taskViewModel.productivityReport.collectAsState()
    val appColors = LocalAppColors.current
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // "ALL", "TODO", "COMPLETED", "RECURRING"
    var selectedPriorityFilter by remember { mutableStateOf("ALL") } // "ALL", "URGENT", "HIGH", "MEDIUM", "LOW"

    var showAddDialog by remember { mutableStateOf(false) }
    var showAddWsDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskDto?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Real-time filtering
    val filteredTasks = remember(tasks, searchQuery, selectedStatusFilter, selectedPriorityFilter) {
        tasks.filter { task ->
            val matchesSearch = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                (task.description?.contains(searchQuery, ignoreCase = true) == true)

            val matchesStatus = when (selectedStatusFilter) {
                "TODO" -> task.status != "COMPLETED"
                "COMPLETED" -> task.status == "COMPLETED"
                "RECURRING" -> task.isRecurring == true
                else -> true
            }

            val matchesPriority = when (selectedPriorityFilter) {
                "URGENT" -> task.priority == "URGENT"
                "HIGH" -> task.priority == "HIGH"
                "MEDIUM" -> task.priority == "MEDIUM"
                "LOW" -> task.priority == "LOW"
                else -> true
            }

            matchesSearch && matchesStatus && matchesPriority
        }
    }

    val totalCount = tasks.size
    val todoCount = tasks.count { it.status != "COMPLETED" }
    val overdueCount = tasks.count { it.status == "OVERDUE" }
    val completedCount = tasks.count { it.status == "COMPLETED" }
    val recurringCount = tasks.count { it.isRecurring == true }

    // Dùng 1 LazyColumn duy nhất bao trọn toàn màn hình để Pull-To-Refresh bắt được sự kiện cuộn
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                taskViewModel.refreshData()
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // -- HEADER ITEM --
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quản lý công việc",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary)
                            .clickable { showAddDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Thêm công việc",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // -- SUMMARY CARDS ITEM --
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        CounterCard(
                            title = "Tổng công việc",
                            count = totalCount,
                            isSelected = selectedStatusFilter == "ALL",
                            color = appColors.textPrimary,
                            onClick = { selectedStatusFilter = "ALL" }
                        )
                    }
                    item {
                        CounterCard(
                            title = "Cần làm (TODO)",
                            count = todoCount,
                            isSelected = selectedStatusFilter == "TODO",
                            color = StatusAmber,
                            onClick = { selectedStatusFilter = "TODO" }
                        )
                    }
                    item {
                        CounterCard(
                            title = "Đã qua (OVERDUE)",
                            count = overdueCount,
                            isSelected = false,
                            color = StatusRose,
                            onClick = {}
                        )
                    }
                    item {
                        CounterCard(
                            title = "Đã hoàn thành",
                            count = completedCount,
                            isSelected = selectedStatusFilter == "COMPLETED",
                            color = EmeraldPrimary,
                            onClick = { selectedStatusFilter = "COMPLETED" }
                        )
                    }
                    item {
                        CounterCard(
                            title = "Thói quen lặp lại",
                            count = recurringCount,
                            isSelected = selectedStatusFilter == "RECURRING",
                            color = StatusCyan,
                            onClick = { selectedStatusFilter = "RECURRING" }
                        )
                    }
                }
            }

            // -- WORKSPACES FILTER ITEM --
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(workspaces) { ws ->
                        val isSelected = ws.id == selectedWsId
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) EmeraldPrimary else appColors.glassCardBg)
                                .border(1.dp, if (isSelected) EmeraldPrimary else appColors.glassBorder, RoundedCornerShape(18.dp))
                                .clickable { taskViewModel.selectWorkspace(ws.id) }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ws.name,
                                color = if (isSelected) Color.White else appColors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.2f))
                                .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), CircleShape)
                                .clickable { showAddWsDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Thêm Workspace", tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // -- SEARCH BAR ITEM --
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Tìm theo tên công việc...", color = appColors.textSecondary, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = appColors.textPrimary,
                        unfocusedTextColor = appColors.textPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = appColors.glassBorder,
                        focusedContainerColor = appColors.glassCardBg,
                        unfocusedContainerColor = appColors.glassCardBg
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
            }

            // -- LOADING OR EMPTY STATE --
            if (isLoading && !isRefreshing) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EmeraldPrimary)
                    }
                }
            } else if (filteredTasks.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "Không tìm thấy công việc phù hợp" else "Chưa có công việc nào trong danh mục này",
                            color = appColors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                // -- TASK LIST ITEMS --
                items(filteredTasks) { task ->
                    TaskCardItem(
                        task = task,
                        onToggleComplete = { taskViewModel.toggleTaskCompleted(task) },
                        onEdit = { editingTask = task },
                        onDelete = { task.id?.let { taskViewModel.deleteTask(it) } }
                    )
                }

                // -- FOOTER STATS ITEM --
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(appColors.glassCardBg)
                                .border(1.dp, appColors.glassBorder, RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Tuần này", fontSize = 10.sp, color = appColors.textSecondary)
                                    Text("${report?.totalTasks ?: tasks.size} việc", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(appColors.glassCardBg)
                                .border(1.dp, appColors.glassBorder, RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = StatusAmber, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Tập trung", fontSize = 10.sp, color = appColors.textSecondary)
                                    Text("${(report?.totalFocusMinutes ?: 0) / 60} giờ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(appColors.glassCardBg)
                                .border(1.dp, appColors.glassBorder, RoundedCornerShape(14.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text("Giờ trống AI", fontSize = 10.sp, color = appColors.textSecondary)
                                    Text("56 slot", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, desc, priority, quadrant ->
                    taskViewModel.createTask(title, desc, priority, quadrant)
                    showAddDialog = false
                }
            )
        }

        if (editingTask != null) {
            EditTaskDialog(
                task = editingTask!!,
                onDismiss = { editingTask = null },
                onSave = { updated ->
                    editingTask?.id?.let { id ->
                        taskViewModel.updateTask(id, updated)
                    }
                    editingTask = null
                }
            )
        }

        if (showAddWsDialog) {
            AddWorkspaceDialog(
                onDismiss = { showAddWsDialog = false },
                onAdd = { name ->
                    taskViewModel.createWorkspace(name)
                    showAddWsDialog = false
                }
            )
        }
    }
}

@Composable
fun CounterCard(
    title: String,
    count: Int,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    val appColors = LocalAppColors.current

    Box(
        modifier = Modifier
            .width(135.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else appColors.glassCardBg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) EmeraldPrimary else appColors.glassBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$count",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) EmeraldPrimary else color
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCardItem(
    task: TaskDto,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = task.status == "COMPLETED"
    val appColors = LocalAppColors.current

    val priorityColor = when (task.priority) {
        "URGENT" -> StatusRose
        "HIGH" -> StatusAmber
        "MEDIUM" -> EmeraldPrimary
        else -> StatusCyan
    }

    val priorityText = when (task.priority) {
        "URGENT" -> "Gấp"
        "HIGH" -> "Cao"
        "MEDIUM" -> "Trung bình"
        else -> "Thấp"
    }

    val quadrantText = when (task.eisenhowerQuadrant) {
        "DO_FIRST" -> "Q1: Làm ngay"
        "SCHEDULE" -> "Q2: Lên lịch"
        "DELEGATE" -> "Q3: Ủy quyền"
        "DONT_DO" -> "Q4: Bỏ qua"
        else -> "Q2: Lên lịch"
    }

    // Clean formatted time string in DD/MM/YYYY
    val timeLabel = remember(task.startTime, task.estimatedDurationMinutes) {
        if (!task.startTime.isNullOrBlank()) {
            formatToDdMmYyyy(task.startTime)
        } else {
            "Thời lượng: ${task.estimatedDurationMinutes ?: 30} phút"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(appColors.glassCardBg)
            .border(
                width = 1.dp,
                color = appColors.glassBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(28.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isCompleted) EmeraldPrimary else appColors.textSecondary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Task Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) appColors.textSecondary else appColors.textPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                if (!task.description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = appColors.textSecondary,
                        maxLines = 2
                    )
                }

                // Time Row (Formatted in DD/MM/YYYY!)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = appColors.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeLabel,
                        fontSize = 11.sp,
                        color = appColors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // FlowRow for Badges
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCompleted) EmeraldPrimary.copy(alpha = 0.15f) else StatusAmber.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isCompleted) "✓ Hoàn thành" else "Cần làm",
                            color = if (isCompleted) EmeraldPrimary else StatusAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(priorityColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = priorityText,
                            color = priorityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = quadrantText,
                            color = EmeraldPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (task.isRecurring == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StatusCyan.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🔥 1 ngày chuỗi",
                                color = StatusCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Chỉnh sửa",
                        tint = EmeraldPrimary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = StatusRose
                    )
                }
            }
        }
    }
}

@Composable
fun EditTaskDialog(
    task: TaskDto,
    onDismiss: () -> Unit,
    onSave: (updatedTask: TaskDto) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var desc by remember { mutableStateOf(task.description ?: "") }
    var priority by remember { mutableStateOf(task.priority ?: "MEDIUM") }
    var quadrant by remember { mutableStateOf(task.eisenhowerQuadrant ?: "SCHEDULE") }
    var status by remember { mutableStateOf(task.status ?: "TODO") }
    var isRecurring by remember { mutableStateOf(task.isRecurring ?: false) }
    var isLocked by remember { mutableStateOf(task.isLocked ?: false) }

    val scrollState = rememberScrollState()
    val appColors = LocalAppColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.glassCardBg,
        title = {
            Text(text = "Chỉnh sửa công việc", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(scrollState)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề công việc", color = appColors.textSecondary) },
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
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Ghi chú/Mô tả", color = appColors.textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = appColors.textPrimary,
                        unfocusedTextColor = appColors.textPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = appColors.glassBorder
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Mức độ ưu tiên:", fontSize = 12.sp, color = appColors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("LOW" to "Thấp", "MEDIUM" to "Vừa", "HIGH" to "Cao", "URGENT" to "Gấp").forEach { (key, label) ->
                        val isSelected = priority == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                .clickable { priority = key }
                                .padding(vertical = 8.dp),
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

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = "Ma trận Eisenhower:", fontSize = 12.sp, color = appColors.textSecondary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    mapOf("DO_FIRST" to "Q1", "SCHEDULE" to "Q2", "DELEGATE" to "Q3", "DONT_DO" to "Q4").forEach { (key, label) ->
                        val isSelected = quadrant == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) EmeraldPrimary else appColors.glassSurface)
                                .clickable { quadrant = key }
                                .padding(vertical = 8.dp),
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

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Thói quen lặp lại hằng ngày", fontSize = 12.sp, color = appColors.textPrimary)
                    Switch(
                        checked = isRecurring,
                        onCheckedChange = { isRecurring = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary,
                            uncheckedBorderColor = appColors.glassBorder
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Khóa riêng tư (Privacy)", fontSize = 12.sp, color = appColors.textPrimary)
                    Switch(
                        checked = isLocked,
                        onCheckedChange = { isLocked = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary,
                            uncheckedBorderColor = appColors.glassBorder
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = task.copy(
                        title = title.trim(),
                        description = desc.ifBlank { null },
                        note = desc.ifBlank { null },
                        priority = priority,
                        eisenhowerQuadrant = quadrant,
                        status = status,
                        isRecurring = isRecurring,
                        isLocked = isLocked
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Lưu cập nhật", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = appColors.textSecondary)
            }
        }
    )
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, desc: String?, priority: String, quadrant: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("MEDIUM") }

    val appColors = LocalAppColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.glassCardBg,
        title = {
            Text(text = "Tạo công việc mới", color = appColors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Tiêu đề công việc", color = appColors.textSecondary) },
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
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Ghi chú/Mô tả", color = appColors.textSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = appColors.textPrimary,
                        unfocusedTextColor = appColors.textPrimary,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = appColors.glassBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(title, desc.ifBlank { null }, selectedPriority, "SCHEDULE") },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Tạo mới", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = appColors.textSecondary)
            }
        }
    )
}

@Composable
fun AddWorkspaceDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String) -> Unit
) {
    var wsName by remember { mutableStateOf("") }
    val appColors = LocalAppColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.glassCardBg,
        title = {
            Text(text = "Tạo không gian mới", color = appColors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = wsName,
                onValueChange = { wsName = it },
                label = { Text("Tên không gian (VD: Học tập, Dự án)", color = appColors.textSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = appColors.textPrimary,
                    unfocusedTextColor = appColors.textPrimary,
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = appColors.glassBorder
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onAdd(wsName) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Tạo mới", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = appColors.textSecondary)
            }
        }
    )
}
