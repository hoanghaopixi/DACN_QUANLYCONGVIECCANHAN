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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
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

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddWsDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskDto?>(null) }

    val filteredTasks = remember(tasks, searchQuery) {
        if (searchQuery.isBlank()) tasks
        else tasks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            (it.description?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    val upcomingTask = tasks.firstOrNull { it.status != "COMPLETED" }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Bar with Notification Bell & Add Task (Symmetrical 42dp x 42dp Buttons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SmartTask AI",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )
                    Text(
                        text = "Chúc bạn một ngày tốt lành !",
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Notification Bell Glass Button (42dp x 42dp)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(appColors.glassCardBg)
                            .border(1.dp, appColors.glassBorder, CircleShape)
                            .clickable { /* Notification Click */ },
                        contentAlignment = Alignment.Center
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(containerColor = NeonRose, contentColor = Color.White) {
                                    Text("1", fontSize = 10.sp)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Thông báo",
                                tint = appColors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Add Task Button (42dp x 42dp)
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

            // Live Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm công việc, lịch hẹn...", color = appColors.textSecondary, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = appColors.textPrimary,
                    unfocusedTextColor = appColors.textPrimary,
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = appColors.glassBorder,
                    focusedContainerColor = appColors.glassCardBg,
                    unfocusedContainerColor = appColors.glassCardBg
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Upcoming Task Banner ("CÔNG VIỆC SẮP TỚI")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(appColors.glassCardBg)
                    .border(1.dp, appColors.glassBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "CÔNG VIỆC SẮP TỚI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textSecondary
                        )
                        Text(
                            text = upcomingTask?.title ?: "⏰ Không còn việc sắp tới hôm nay",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Workspaces Filter Bar (Balanced 38dp Height for Pills & Add Button)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(workspaces) { ws ->
                    val isSelected = ws.id == selectedWsId
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(
                                if (isSelected) EmeraldPrimary else appColors.glassCardBg
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) EmeraldPrimary else appColors.glassBorder,
                                shape = RoundedCornerShape(19.dp)
                            )
                            .clickable { taskViewModel.selectWorkspace(ws.id) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ws.name,
                            color = if (isSelected) Color.White else appColors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                        )
                    }
                }

                item {
                    // Add Workspace Button - Matching 38dp x 38dp Height!
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.2f))
                            .border(1.dp, EmeraldPrimary.copy(alpha = 0.5f), CircleShape)
                            .clickable { showAddWsDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Thêm Workspace",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Task List + Quick Stats Footer inside LazyColumn (Zero Overlap!)
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmeraldPrimary)
                }
            } else if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "Không tìm thấy công việc phù hợp" else "Chưa có công việc nào trong không gian này",
                        color = appColors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(filteredTasks) { task ->
                        TaskCardItem(
                            task = task,
                            onToggleComplete = { taskViewModel.toggleTaskCompleted(task) },
                            onEdit = { editingTask = task },
                            onDelete = { task.id?.let { taskViewModel.deleteTask(it) } }
                        )
                    }

                    // Spacer between task list and stats footer
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Quick Stats Footer Card
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Stat 1: Weekly tasks
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

                            // Stat 2: Focus hours
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(appColors.glassCardBg)
                                    .border(1.dp, appColors.glassBorder, RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = LimeHighlight, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text("Tập trung", fontSize = 10.sp, color = appColors.textSecondary)
                                        Text("${(report?.totalFocusMinutes ?: 0) / 60} giờ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
                                    }
                                }
                            }

                            // Stat 3: AI free slots
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(appColors.glassCardBg)
                                    .border(1.dp, appColors.glassBorder, RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(18.dp))
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
        }

        // Add Task Glass Dialog
        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, desc, priority, quadrant ->
                    taskViewModel.createTask(title, desc, priority, quadrant)
                    showAddDialog = false
                }
            )
        }

        // Edit Task Glass Dialog
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

        // Add Workspace Dialog
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
fun TaskCardItem(
    task: TaskDto,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = task.status == "COMPLETED"
    val appColors = LocalAppColors.current

    val priorityColor = when (task.priority) {
        "URGENT" -> NeonRose
        "HIGH" -> NeonAmber
        "MEDIUM" -> EmeraldPrimary
        else -> MintAccent
    }

    val quadrantText = when (task.eisenhowerQuadrant) {
        "DO_FIRST" -> "Q1: Làm ngay"
        "SCHEDULE" -> "Q2: Lên lịch"
        "DELEGATE" -> "Q3: Ủy quyền"
        "DONT_DO" -> "Q4: Bỏ qua"
        else -> "Q2: Lên lịch"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(appColors.glassCardBg)
            .border(
                width = 1.dp,
                color = appColors.glassBorder,
                shape = RoundedCornerShape(18.dp)
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

            Spacer(modifier = Modifier.width(8.dp))

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
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        fontSize = 12.sp,
                        color = appColors.textSecondary,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Badges Wrap Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Priority Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(priorityColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority ?: "MEDIUM",
                            color = priorityColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Eisenhower Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = quadrantText,
                            color = MintAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Recurring Tag
                    if (task.isRecurring == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🔁 Lặp lại",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Privacy Lock Tag
                    if (task.isLocked == true) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonAmber.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🔒 Riêng tư",
                                color = NeonAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action Buttons Row (Edit & Delete)
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
                        tint = EmeraldLight
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        tint = NeonRose.copy(alpha = 0.8f)
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

                // Priority Selection
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

                // Eisenhower Quadrant Selection
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

                // Switches Row
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
            Text(text = "Tạo Không gian mới", color = appColors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = wsName,
                onValueChange = { wsName = it },
                label = { Text("Tên Không gian (VD: Học tập, Dự án)", color = appColors.textSecondary) },
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
