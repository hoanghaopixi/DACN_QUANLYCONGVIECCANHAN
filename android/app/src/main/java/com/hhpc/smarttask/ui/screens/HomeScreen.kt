package com.hhpc.smarttask.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hhpc.smarttask.data.model.Category
import com.hhpc.smarttask.data.model.SubtaskDto
import com.hhpc.smarttask.data.model.TaskDto
import com.hhpc.smarttask.data.repository.AuthRepository
import com.hhpc.smarttask.data.repository.TaskRepository
import com.hhpc.smarttask.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun formatIsoToDisplayDate(isoDateStr: String?): String {
    if (isoDateStr.isNullOrBlank()) return "13/11/2026"
    val datePart = if (isoDateStr.contains("T")) isoDateStr.substringBefore("T") else isoDateStr.take(10)
    return try {
        if (datePart.contains("-")) {
            val parts = datePart.split("-")
            if (parts.size == 3 && parts[0].length == 4) {
                "${parts[2]}/${parts[1]}/${parts[0]}"
            } else datePart
        } else datePart
    } catch (e: Exception) {
        "13/11/2026"
    }
}

fun normalizeToIsoDateTime(dateStr: String, timeStr: String): String? {
    if (dateStr.isBlank() || timeStr.isBlank()) return null
    return try {
        val trimmedDate = dateStr.trim()
        val trimmedTime = timeStr.trim()
        val formattedTime = if (trimmedTime.length == 5) "$trimmedTime:00" else trimmedTime

        val yyyyMmDd = if (trimmedDate.contains("/")) {
            val parts = trimmedDate.split("/")
            if (parts.size == 3) {
                if (parts[0].length == 4) {
                    "${parts[0]}-${parts[1].padStart(2, '0')}-${parts[2].padStart(2, '0')}"
                } else {
                    "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"
                }
            } else trimmedDate
        } else trimmedDate

        "${yyyyMmDd}T${formattedTime}+07:00"
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    taskViewModel: TaskViewModel
) {
    val context = LocalContext.current
    val tasks by taskViewModel.tasks.collectAsState()
    val workspaces by taskViewModel.workspaces.collectAsState()
    val categories by taskViewModel.categories.collectAsState()
    val selectedWsId by taskViewModel.selectedWorkspaceId.collectAsState()
    val isLoading by taskViewModel.isLoading.collectAsState()
    val report by taskViewModel.productivityReport.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<Long?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }

    var editingTask by remember { mutableStateOf<TaskDto?>(null) }
    var taskToUnlock by remember { mutableStateOf<TaskDto?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddWsDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val appColors = LocalAppColors.current

    val totalCount = tasks.size
    val completedCount = tasks.count { it.status == "COMPLETED" }
    val todoCount = tasks.count { it.status != "COMPLETED" && it.status != "OVERDUE" }
    val overdueCount = tasks.count { it.status == "OVERDUE" }
    val recurringCount = tasks.count { it.isRecurring == true }

    val filteredTasks = remember(tasks, searchQuery, selectedCategoryFilter, selectedStatusFilter) {
        tasks.filter { task ->
            val matchesQuery = searchQuery.isBlank() || task.title.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == null || task.categoryId == selectedCategoryFilter
            val matchesStatus = when (selectedStatusFilter) {
                "TODO" -> task.status != "COMPLETED"
                "COMPLETED" -> task.status == "COMPLETED"
                "OVERDUE" -> task.status == "OVERDUE"
                "RECURRING" -> task.isRecurring == true
                else -> true
            }
            matchesQuery && matchesCategory && matchesStatus
        }
    }

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
                taskViewModel.refreshData()
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Công việc",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary
                            )
                            Text(
                                text = "Quản lý & Theo dõi tiến độ cá nhân",
                                fontSize = 12.sp,
                                color = appColors.textSecondary
                            )
                        }

                        IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary)
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

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            CounterCard(
                                title = "Tổng công việc",
                                count = totalCount,
                                isSelected = selectedStatusFilter == null,
                                color = EmeraldPrimary,
                                onClick = { selectedStatusFilter = null }
                            )
                        }
                        item {
                            CounterCard(
                                title = "Cần làm (TODO)",
                                count = todoCount,
                                isSelected = selectedStatusFilter == "TODO",
                                color = StatusCyan,
                                onClick = { selectedStatusFilter = if (selectedStatusFilter == "TODO") null else "TODO" }
                            )
                        }
                        if (overdueCount > 0) {
                            item {
                                CounterCard(
                                    title = "Đã qua (OVERDUE)",
                                    count = overdueCount,
                                    isSelected = selectedStatusFilter == "OVERDUE",
                                    color = StatusRose,
                                    onClick = { selectedStatusFilter = if (selectedStatusFilter == "OVERDUE") null else "OVERDUE" }
                                )
                            }
                        }
                        item {
                            CounterCard(
                                title = "Đã hoàn thành",
                                count = completedCount,
                                isSelected = selectedStatusFilter == "COMPLETED",
                                color = EmeraldPrimary,
                                onClick = { selectedStatusFilter = if (selectedStatusFilter == "COMPLETED") null else "COMPLETED" }
                            )
                        }
                        item {
                            CounterCard(
                                title = "Thói quen lặp lại",
                                count = recurringCount,
                                isSelected = selectedStatusFilter == "RECURRING",
                                color = StatusCyan,
                                onClick = { selectedStatusFilter = if (selectedStatusFilter == "RECURRING") null else "RECURRING" }
                            )
                        }
                    }
                }

                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(workspaces) { ws ->
                            val isSelected = selectedWsId == ws.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) EmeraldPrimary else appColors.glassCardBg)
                                    .border(
                                        1.dp,
                                        if (isSelected) EmeraldPrimary else appColors.glassBorder,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { taskViewModel.selectWorkspace(ws.id) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = ws.name,
                                    color = if (isSelected) Color.White else appColors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        item {
                            IconButton(
                                onClick = { showAddWsDialog = true },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(appColors.glassCardBg)
                                    .border(1.dp, appColors.glassBorder, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Tạo không gian",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Tìm theo tên công việc...", color = appColors.textSecondary, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = appColors.textSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appColors.textPrimary,
                            unfocusedTextColor = appColors.textPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = appColors.glassBorder,
                            focusedContainerColor = appColors.glassCardBg,
                            unfocusedContainerColor = appColors.glassCardBg
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (categories.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryFilter == null,
                                    onClick = { selectedCategoryFilter = null },
                                    label = { Text("Tất cả nhóm", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            items(categories) { cat ->
                                val isSel = selectedCategoryFilter == cat.id
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedCategoryFilter = if (isSel) null else cat.id },
                                    label = { Text(cat.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldPrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                if (filteredTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isLoading) "Đang tải danh sách công việc..." else "Không có công việc nào trong danh sách",
                                color = appColors.textSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(filteredTasks, key = { it.id ?: it.title.hashCode() }) { task ->
                        TaskCardItem(
                            task = task,
                            categories = categories,
                            appColors = appColors,
                            onToggleComplete = { taskViewModel.toggleTaskCompleted(task) },
                            onEdit = { handleOpenTask(task) },
                            onDelete = { task.id?.let { id -> taskViewModel.deleteTask(id) } }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryMiniCard(
                            icon = Icons.Default.CheckCircle,
                            iconTint = EmeraldPrimary,
                            title = "Tuần này",
                            subtitle = "${report?.totalTasks ?: tasks.size} việc",
                            appColors = appColors,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMiniCard(
                            icon = Icons.Default.ElectricBolt,
                            iconTint = StatusAmber,
                            title = "Tập trung",
                            subtitle = "${(report?.totalFocusMinutes ?: 0) / 60} giờ",
                            appColors = appColors,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMiniCard(
                            icon = Icons.Default.AutoAwesome,
                            iconTint = StatusCyan,
                            title = "Giờ trống AI",
                            subtitle = "56 slot",
                            appColors = appColors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

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
                        taskViewModel.updateTask(id, updated) { success, msg ->
                            Toast.makeText(context, msg ?: if (success) "Cập nhật công việc thành công!" else "Lỗi cập nhật", Toast.LENGTH_SHORT).show()
                        }
                    }
                    editingTask = null
                }
            )
        }

        if (showAddWsDialog) {
            AddWorkspaceDialog(
                onDismiss = { showAddWsDialog = false },
                onAdd = { wsName ->
                    taskViewModel.createWorkspace(wsName)
                    showAddWsDialog = false
                }
            )
        }
    }
}

@Composable
fun VerifyPasswordDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepo = remember { AuthRepository(context) }
    val appColors = LocalAppColors.current

    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.glassCardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Xác thực mật khẩu", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
            }
        },
        text = {
            Column {
                Text(
                    text = "Công việc này đã bị khóa riêng tư. Vui lòng nhập mật khẩu tài khoản để mở khóa.",
                    fontSize = 12.sp,
                    color = appColors.textSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu tài khoản", color = appColors.textSecondary) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null, tint = appColors.textSecondary)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg!!, color = StatusRose, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isBlank()) {
                        errorMsg = "Vui lòng nhập mật khẩu."
                        return@Button
                    }
                    scope.launch {
                        isLoading = true
                        errorMsg = null
                        val res = authRepo.verifyPassword(password)
                        res.onSuccess {
                            isLoading = false
                            onSuccess()
                        }.onFailure { err ->
                            isLoading = false
                            errorMsg = err.message ?: "Mật khẩu không chính xác."
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("Mở khóa", color = Color.White, fontWeight = FontWeight.Bold)
                }
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
            .width(130.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) color.copy(alpha = 0.15f) else appColors.glassCardBg)
            .border(
                1.5.dp,
                if (isSelected) color else appColors.glassBorder,
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) color else appColors.textSecondary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) color else appColors.textPrimary
            )
        }
    }
}

@Composable
fun SummaryMiniCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    appColors: AppColors,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(appColors.glassCardBg)
            .border(1.dp, appColors.glassBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontSize = 10.sp, color = appColors.textSecondary)
            Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = appColors.textPrimary)
        }
    }
}

@Composable
fun TaskCardItem(
    task: TaskDto,
    categories: List<Category>,
    appColors: AppColors,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isCompleted = task.status == "COMPLETED"
    val isOverdue = task.status == "OVERDUE"
    val isLocked = task.isLocked == true

    val category = remember(categories, task.categoryId) {
        categories.firstOrNull { it.id == task.categoryId }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                when {
                    isLocked -> Color(0xFF64748B).copy(alpha = 0.2f)
                    isCompleted -> appColors.glassCardBg.copy(alpha = 0.5f)
                    else -> appColors.glassCardBg
                }
            )
            .border(
                1.dp,
                when {
                    isLocked -> Color(0xFF64748B)
                    isCompleted -> appColors.glassBorder
                    isOverdue -> StatusRose.copy(alpha = 0.6f)
                    else -> appColors.glassBorder
                },
                RoundedCornerShape(18.dp)
            )
            .clickable { onEdit() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            IconButton(
                onClick = onToggleComplete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isCompleted) EmeraldPrimary else appColors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isLocked) "🔒 Công việc riêng tư" else task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted || isLocked) appColors.textSecondary else appColors.textPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                if (!isLocked && (!task.description.isNullOrBlank() || !task.note.isNullOrBlank())) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description ?: task.note ?: "",
                        fontSize = 12.sp,
                        color = appColors.textSecondary,
                        maxLines = 2
                    )
                }

                if (!task.startTime.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = appColors.textSecondary, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${task.startTime.substringAfter("T").take(5)} · ${formatIsoToDisplayDate(task.startTime)}",
                            fontSize = 11.sp,
                            color = appColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when {
                                    isLocked -> Color(0xFF64748B).copy(alpha = 0.2f)
                                    task.status == "COMPLETED" -> EmeraldPrimary.copy(alpha = 0.15f)
                                    task.status == "OVERDUE" -> StatusRose.copy(alpha = 0.15f)
                                    else -> StatusAmber.copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when {
                                isLocked -> "Đã khóa"
                                task.status == "COMPLETED" -> "Đã xong"
                                task.status == "OVERDUE" -> "Quá hạn"
                                else -> "Cần làm"
                            },
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isLocked -> Color(0xFF94A3B8)
                                task.status == "COMPLETED" -> EmeraldPrimary
                                task.status == "OVERDUE" -> StatusRose
                                else -> StatusAmber
                            }
                        )
                    }

                    if (category != null && !isLocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = category.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    if (!isLocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StatusCyan.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when (task.eisenhowerQuadrant) {
                                    "DO_FIRST" -> "Q1: Làm ngay"
                                    "SCHEDULE" -> "Q2: Lên lịch"
                                    "DELEGATE" -> "Q3: Ủy quyền"
                                    else -> "Q4: Xóa/Bỏ"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = appColors.textSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = StatusRose.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ----------------- FULL-SCREEN EDIT TASK DIALOG -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: TaskDto,
    taskViewModel: TaskViewModel? = null,
    onDismiss: () -> Unit,
    onSave: (updatedTask: TaskDto) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val taskRepo = remember { TaskRepository(context) }
    val appColors = LocalAppColors.current

    var title by remember { mutableStateOf(task.title) }
    var desc by remember { mutableStateOf(task.description ?: task.note ?: "") }
    var priority by remember { mutableStateOf(task.priority ?: "MEDIUM") }
    var quadrant by remember { mutableStateOf(task.eisenhowerQuadrant ?: "SCHEDULE") }
    var status by remember { mutableStateOf(task.status ?: "TODO") }
    var isRecurring by remember { mutableStateOf(task.isRecurring ?: false) }
    var isLocked by remember { mutableStateOf(task.isLocked ?: false) }

    // Date & Time formatted as D/M/YYYY e.g. "13/11/2026"
    var dateStr by remember { mutableStateOf(formatIsoToDisplayDate(task.startTime)) }
    var startTimeStr by remember { mutableStateOf(if (!task.startTime.isNullOrBlank()) task.startTime.substringAfter("T").take(5) else "11:00") }
    var endTimeStr by remember { mutableStateOf(if (!task.endTime.isNullOrBlank()) task.endTime.substringAfter("T").take(5) else "12:00") }

    // Category
    var selectedCategoryId by remember { mutableStateOf(task.categoryId) }
    val categories by taskViewModel?.categories?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    // Subtasks & AI Breakdown State
    var subtasksList by remember { mutableStateOf<List<SubtaskDto>>(emptyList()) }
    var newSubtaskTitle by remember { mutableStateOf("") }
    var isBreakingDown by remember { mutableStateOf(false) }

    LaunchedEffect(task.id) {
        if (task.id != null) {
            val res = taskRepo.getSubtasks(task.id)
            res.onSuccess { list -> subtasksList = list }
        }
    }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
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
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header Bar with Title and Quick Delete Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = appColors.textPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Chỉnh sửa công việc", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }

                    if (task.id != null) {
                        OutlinedButton(
                            onClick = {
                                taskViewModel?.deleteTask(task.id)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, StatusRose.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = StatusRose.copy(alpha = 0.12f), contentColor = StatusRose),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa việc", tint = StatusRose, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Xóa việc", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Form Fields Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: TÊN CÔNG VIỆC / SỰ KIỆN *
                    Column {
                        Text(text = "TÊN CÔNG VIỆC / SỰ KIỆN *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = appColors.textPrimary,
                                unfocusedTextColor = appColors.textPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = appColors.glassBorder
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // SECTION 2: NHÓM CÔNG VIỆC (Pure Text Category Chips NO DOTS!)
                    Column {
                        Text(text = "NHÓM CÔNG VIỆC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        val displayCategories = if (categories.isNotEmpty()) categories else listOf(
                            Category(id = 1, name = "Công việc", colorCode = "#8B5CF6"),
                            Category(id = 2, name = "Học tập", colorCode = "#3B82F6"),
                            Category(id = 3, name = "Sức khỏe", colorCode = "#10B981"),
                            Category(id = 4, name = "Giải trí", colorCode = "#F59E0B")
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            displayCategories.forEach { cat ->
                                val isSelected = selectedCategoryId == cat.id
                                val catColor = try {
                                    Color(android.graphics.Color.parseColor(cat.colorCode ?: "#10B981"))
                                } catch (e: Exception) {
                                    EmeraldPrimary
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) catColor else appColors.glassSurface)
                                        .border(1.dp, if (isSelected) catColor else appColors.glassBorder, RoundedCornerShape(12.dp))
                                        .clickable { selectedCategoryId = cat.id }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat.name,
                                        color = if (isSelected) Color.White else appColors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 3: THỜI GIAN (NGÀY D/M/YYYY - BẮT ĐẦU - KẾT THÚC)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(text = "NGÀY (D/M/YYYY)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = { dateStr = it },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "BẮT ĐẦU", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = startTimeStr,
                                onValueChange = { startTimeStr = it },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "KẾT THÚC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = endTimeStr,
                                onValueChange = { endTimeStr = it },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // SECTION 4: MA TRẬN EISENHOWER & TRẠNG THÁI
                    Column {
                        Text(text = "MA TRẬN EISENHOWER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            mapOf("DO_FIRST" to "Q1", "SCHEDULE" to "Q2", "DELEGATE" to "Q3", "DONT_DO" to "Q4").forEach { (key, label) ->
                                val isSel = quadrant == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) EmeraldPrimary else appColors.glassSurface)
                                        .border(1.dp, if (isSel) EmeraldPrimary else appColors.glassBorder, RoundedCornerShape(10.dp))
                                        .clickable { quadrant = key }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSel) Color.White else appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Column {
                        Text(text = "TRẠNG THÁI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            mapOf("TODO" to "Cần làm", "IN_PROGRESS" to "Đang làm", "COMPLETED" to "Đã hoàn thành").forEach { (key, label) ->
                                val isSel = status == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) EmeraldPrimary else appColors.glassSurface)
                                        .border(1.dp, if (isSel) EmeraldPrimary else appColors.glassBorder, RoundedCornerShape(10.dp))
                                        .clickable { status = key }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSel) Color.White else appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // SECTION 5: TOGGLES (WITH REPEAT ICON & NO SUBTEXT UNDER LOCK)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(appColors.glassSurface)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Autorenew, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Thói quen lặp lại", fontSize = 13.sp, color = appColors.textPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Checkbox(
                                checked = isRecurring,
                                onCheckedChange = { isRecurring = it },
                                colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(appColors.glassSurface)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Khóa riêng tư", fontSize = 13.sp, color = appColors.textPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Checkbox(
                                checked = isLocked,
                                onCheckedChange = { isLocked = it },
                                colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                            )
                        }
                    }

                    // SECTION 6: CHECKLIST VIỆC CON (Subtasks + AI Gemini Breakdown)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "CHECKLIST VIỆC CON", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                            if (task.id != null) {
                                OutlinedButton(
                                    onClick = {
                                        isBreakingDown = true
                                        taskViewModel?.breakdownTaskWithAi(task.id) { _, _ ->
                                            scope.launch {
                                                val res = taskRepo.getSubtasks(task.id)
                                                res.onSuccess { list -> subtasksList = list }
                                            }
                                            isBreakingDown = false
                                        }
                                    },
                                    enabled = !isBreakingDown,
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldPrimary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isBreakingDown) "Đang phân rã..." else "AI Phân rã (Gemini)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (subtasksList.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                subtasksList.forEach { subtask ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(appColors.glassSurface)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Checkbox(
                                            checked = subtask.isCompleted == true,
                                            onCheckedChange = {
                                                subtask.id?.let { sid ->
                                                    taskViewModel?.toggleSubtask(sid)
                                                    subtasksList = subtasksList.map { s ->
                                                        if (s.id == sid) s.copy(isCompleted = !(s.isCompleted ?: false)) else s
                                                    }
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = subtask.title,
                                            fontSize = 12.sp,
                                            color = appColors.textPrimary,
                                            textDecoration = if (subtask.isCompleted == true) TextDecoration.LineThrough else TextDecoration.None,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (task.id != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = newSubtaskTitle,
                                    onValueChange = { newSubtaskTitle = it },
                                    placeholder = { Text("Thêm việc con mới...", fontSize = 12.sp, color = appColors.textSecondary) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (newSubtaskTitle.isNotBlank()) {
                                            taskViewModel?.createSubtask(task.id, newSubtaskTitle) { success ->
                                                if (success) {
                                                    scope.launch {
                                                        val res = taskRepo.getSubtasks(task.id)
                                                        res.onSuccess { list -> subtasksList = list }
                                                    }
                                                }
                                            }
                                            newSubtaskTitle = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Text("+ Thêm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    // SECTION 7: GHI CHÚ / CHÚ THÍCH
                    Column {
                        Text(text = "GHI CHÚ / CHÚ THÍCH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = appColors.textPrimary,
                                unfocusedTextColor = appColors.textPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = appColors.glassBorder
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                }

                // Bottom Footer Buttons
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.glassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = appColors.textPrimary),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Hủy bỏ", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) return@Button
                            val fullStart = normalizeToIsoDateTime(dateStr, startTimeStr) ?: task.startTime
                            val fullEnd = normalizeToIsoDateTime(dateStr, endTimeStr) ?: task.endTime
                            val updated = task.copy(
                                title = title.trim(),
                                description = desc.ifBlank { null },
                                note = desc.ifBlank { null },
                                categoryId = selectedCategoryId,
                                priority = priority,
                                eisenhowerQuadrant = quadrant,
                                status = status,
                                isRecurring = isRecurring,
                                isLocked = isLocked,
                                startTime = fullStart,
                                endTime = fullEnd
                            )
                            if (task.id != null && taskViewModel != null) {
                                taskViewModel.updateTask(task.id, updated) { success, msg ->
                                    Toast.makeText(context, msg ?: if (success) "Cập nhật công việc thành công!" else "Lỗi cập nhật", Toast.LENGTH_SHORT).show()
                                    if (success) {
                                        onSave(updated)
                                    }
                                }
                            } else {
                                onSave(updated)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.5f).height(50.dp)
                    ) {
                        Text("Cập nhật", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ----------------- FULL-SCREEN ADD TASK DIALOG (PURE MATERIAL 3 MATCHING EDIT TASK) -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    taskViewModel: TaskViewModel? = null,
    onDismiss: () -> Unit,
    onAdd: (newTask: TaskDto) -> Unit
) {
    val context = LocalContext.current
    val appColors = LocalAppColors.current

    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }
    var quadrant by remember { mutableStateOf("SCHEDULE") }
    var status by remember { mutableStateOf("TODO") }
    var isRecurring by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }

    var dateStr by remember { mutableStateOf("13/11/2026") }
    var startTimeStr by remember { mutableStateOf("09:00") }
    var endTimeStr by remember { mutableStateOf("10:00") }

    var selectedCategoryId by remember { mutableStateOf<Long?>(1) }
    val categories by taskViewModel?.categories?.collectAsState() ?: remember { mutableStateOf(emptyList()) }

    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
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
            color = Color.Transparent
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = appColors.textPrimary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Thêm công việc mới", color = appColors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: TÊN CÔNG VIỆC / SỰ KIỆN *
                    Column {
                        Text(text = "TÊN CÔNG VIỆC / SỰ KIỆN *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("VD: Làm báo cáo đồ án, Thiết kế giao diện...", fontSize = 13.sp, color = appColors.textSecondary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = appColors.textPrimary,
                                unfocusedTextColor = appColors.textPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = appColors.glassBorder
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // SECTION 2: NHÓM CÔNG VIỆC (Pure Text Category Chips NO DOTS!)
                    Column {
                        Text(text = "NHÓM CÔNG VIỆC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        val displayCategories = if (categories.isNotEmpty()) categories else listOf(
                            Category(id = 1, name = "Công việc", colorCode = "#8B5CF6"),
                            Category(id = 2, name = "Học tập", colorCode = "#3B82F6"),
                            Category(id = 3, name = "Sức khỏe", colorCode = "#10B981"),
                            Category(id = 4, name = "Giải trí", colorCode = "#F59E0B")
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            displayCategories.forEach { cat ->
                                val isSelected = selectedCategoryId == cat.id
                                val catColor = try {
                                    Color(android.graphics.Color.parseColor(cat.colorCode ?: "#10B981"))
                                } catch (e: Exception) {
                                    EmeraldPrimary
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) catColor else appColors.glassSurface)
                                        .border(1.dp, if (isSelected) catColor else appColors.glassBorder, RoundedCornerShape(12.dp))
                                        .clickable { selectedCategoryId = cat.id }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = cat.name,
                                        color = if (isSelected) Color.White else appColors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 3: THỜI GIAN (NGÀY D/M/YYYY - BẮT ĐẦU - KẾT THÚC)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(text = "NGÀY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = dateStr,
                                onValueChange = { dateStr = it },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "BẮT ĐẦU", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = startTimeStr,
                                onValueChange = { startTimeStr = it },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "KẾT THÚC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = endTimeStr,
                                onValueChange = { endTimeStr = it },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = appColors.textPrimary, unfocusedTextColor = appColors.textPrimary, focusedBorderColor = EmeraldPrimary, unfocusedBorderColor = appColors.glassBorder),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // SECTION 4: MA TRẬN EISENHOWER & TRẠNG THÁI
                    Column {
                        Text(text = "MA TRẬN EISENHOWER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            mapOf("DO_FIRST" to "Q1", "SCHEDULE" to "Q2", "DELEGATE" to "Q3", "DONT_DO" to "Q4").forEach { (key, label) ->
                                val isSel = quadrant == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) EmeraldPrimary else appColors.glassSurface)
                                        .border(1.dp, if (isSel) EmeraldPrimary else appColors.glassBorder, RoundedCornerShape(10.dp))
                                        .clickable { quadrant = key }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSel) Color.White else appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Column {
                        Text(text = "TRẠNG THÁI", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            mapOf("TODO" to "Cần làm", "IN_PROGRESS" to "Đang làm", "COMPLETED" to "Đã hoàn thành").forEach { (key, label) ->
                                val isSel = status == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) EmeraldPrimary else appColors.glassSurface)
                                        .border(1.dp, if (isSel) EmeraldPrimary else appColors.glassBorder, RoundedCornerShape(10.dp))
                                        .clickable { status = key }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSel) Color.White else appColors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // SECTION 5: TOGGLES (WITH REPEAT ICON & NO SUBTEXT UNDER LOCK)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(appColors.glassSurface)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Autorenew, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Thói quen lặp lại", fontSize = 13.sp, color = appColors.textPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Checkbox(
                                checked = isRecurring,
                                onCheckedChange = { isRecurring = it },
                                colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(appColors.glassSurface)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Khóa riêng tư", fontSize = 13.sp, color = appColors.textPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            Checkbox(
                                checked = isLocked,
                                onCheckedChange = { isLocked = it },
                                colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                            )
                        }
                    }

                    // SECTION 6: GHI CHÚ / CHÚ THÍCH
                    Column {
                        Text(text = "GHI CHÚ / CHÚ THÍCH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = appColors.textSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = appColors.textPrimary,
                                unfocusedTextColor = appColors.textPrimary,
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = appColors.glassBorder
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                    }
                }

                // Footer Buttons
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, appColors.glassBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = appColors.textPrimary),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text("Hủy bỏ", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank()) return@Button
                            val fullStart = normalizeToIsoDateTime(dateStr, startTimeStr)
                            val fullEnd = normalizeToIsoDateTime(dateStr, endTimeStr)
                            val newTask = TaskDto(
                                title = title.trim(),
                                description = desc.ifBlank { null },
                                note = desc.ifBlank { null },
                                categoryId = selectedCategoryId,
                                priority = priority,
                                eisenhowerQuadrant = quadrant,
                                status = status,
                                isRecurring = isRecurring,
                                isLocked = isLocked,
                                startTime = fullStart,
                                endTime = fullEnd
                            )
                            onAdd(newTask)
                            Toast.makeText(context, "Thêm công việc thành công!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.5f).height(50.dp)
                    ) {
                        Text("Thêm công việc", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddWorkspaceDialog(
    onDismiss: () -> Unit,
    onAdd: (wsName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val appColors = LocalAppColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appColors.glassCardBg,
        title = {
            Text(text = "Tạo không gian mới", color = appColors.textPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
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
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name.trim())
                    }
                },
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
