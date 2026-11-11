package com.hhpc.smarttask.ui.screens

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
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
import com.hhpc.smarttask.data.model.AiParseTaskResponse
import com.hhpc.smarttask.data.model.AutoScheduleResponse
import com.hhpc.smarttask.data.repository.TaskRepository
import com.hhpc.smarttask.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiAssistantScreen(
    taskViewModel: TaskViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val taskRepository = remember { TaskRepository(context) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val appColors = LocalAppColors.current

    var inputPrompt by remember { mutableStateOf("") }
    var autoAddWithoutConfirm by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var isScheduling by remember { mutableStateOf(false) }

    var parseResult by remember { mutableStateOf<AiParseTaskResponse?>(null) }
    var scheduleResult by remember { mutableStateOf<AutoScheduleResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Voice Input Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val spokenTextList = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!spokenTextList.isNullOrEmpty()) {
                inputPrompt = spokenTextList[0]
            }
        }
    }

    fun submitAiPrompt(promptText: String) {
        if (promptText.isBlank()) return
        scope.launch {
            isLoading = true
            errorMessage = null
            parseResult = null

            val currentIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            val result = taskRepository.parseAiTask(
                prompt = promptText.trim(),
                workspaceId = taskViewModel.selectedWorkspaceId.value,
                currentDateTime = currentIso
            )
            result.onSuccess { res ->
                parseResult = res
                if (autoAddWithoutConfirm && res.isValidTask == true && !res.title.isNullOrBlank()) {
                    taskViewModel.createTask(
                        title = res.title!!,
                        description = res.description ?: res.note,
                        priority = res.priority ?: "MEDIUM",
                        quadrant = res.eisenhowerQuadrant ?: "SCHEDULE"
                    )
                }
            }.onFailure { e ->
                errorMessage = e.message ?: "Không thể kết nối đến Trợ lý AI."
            }
            isLoading = false
        }
    }

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
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Trợ Lý AI Gemini",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )
                    Text(
                        text = "Bóc tách ngôn ngữ tự nhiên & tự động phân bổ lịch trình",
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Auto Add Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(appColors.glassCardBg)
                    .border(1.dp, appColors.glassBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tự động thêm vào lịch mà không cần xác nhận",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = appColors.textPrimary
                    )
                }
                Switch(
                    checked = autoAddWithoutConfirm,
                    onCheckedChange = { autoAddWithoutConfirm = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = EmeraldPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // AI Smart Auto-Reschedule Engine Button Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                appColors.glassCardBg,
                                appColors.glassCardBg.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .border(1.5.dp, appColors.glassBorderActive, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = LimeHighlight, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Động cơ Lập lịch Tự động (Auto-Scheduler)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Phân tích khoảng thời gian trống và tự động xếp lịch công việc quan trọng vào khung giờ vàng.",
                        fontSize = 12.sp,
                        color = appColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                isScheduling = true
                                errorMessage = null
                                val result = taskRepository.autoSchedule(
                                    workspaceId = taskViewModel.selectedWorkspaceId.value,
                                    applyImmediately = true
                                )
                                result.onSuccess { res ->
                                    scheduleResult = res
                                    taskViewModel.loadTasks()
                                    taskViewModel.loadEisenhower()
                                }.onFailure { e ->
                                    errorMessage = e.message ?: "Lỗi tự động lập lịch"
                                }
                                isScheduling = false
                            }
                        },
                        enabled = !isScheduling,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isScheduling) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(text = "Tối ưu & Tự động Lập lịch ngay", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (scheduleResult != null) {
                        val sched = scheduleResult!!
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(appColors.glassSurface)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = sched.aiStrategySummary ?: "Đã tự động xếp lịch thành công!",
                                    color = EmeraldPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!sched.proposals.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    sched.proposals.take(3).forEach { prop ->
                                        Text(
                                            text = "• ${prop.title ?: "Công việc"}: ${prop.slotRationale ?: "Đã xếp vào khung giờ phù hợp"}",
                                            fontSize = 11.sp,
                                            color = appColors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Natural Language Parsing Input Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(appColors.glassCardBg)
                    .border(1.dp, appColors.glassBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Nhập câu lệnh bằng giọng nói hoặc văn bản:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = appColors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputPrompt,
                        onValueChange = { inputPrompt = it },
                        placeholder = {
                            Text(
                                text = "Nhập công việc...",
                                color = appColors.textSecondary.copy(alpha = 0.9f),
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = appColors.textPrimary,
                            unfocusedTextColor = appColors.textPrimary,
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = appColors.glassBorder,
                            focusedContainerColor = appColors.glassSurface,
                            unfocusedContainerColor = appColors.glassSurface.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Voice Recognition Mic Button
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói câu lệnh (VD: Chiều mai 3h họp nhóm)...")
                                }
                                try {
                                    speechLauncher.launch(intent)
                                } catch (e: Exception) {
                                    errorMessage = "Thiết bị chưa cài đặt hoặc bật Google Speech Recognition."
                                }
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.2f))
                                .border(1.dp, EmeraldPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Nhập bằng giọng nói",
                                tint = EmeraldPrimary
                            )
                        }

                        // Submit AI Parsing Button
                        Button(
                            onClick = { submitAiPrompt(inputPrompt) },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Gửi AI", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Prompt Suggestions Section ("Bạn cũng có thể thử" - Matching Web App)
            Text(
                text = "Bạn cũng có thể thử:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            val suggestions = listOf(
                "📖 Sắp xếp 2 tiếng đọc sách sáng mai 8h",
                "🏃 Nhắc chạy bộ 45 phút vào 6h sáng thứ 2",
                "🇬🇧 Lịch Tiếng Anh 1h đến 2h30 chiều"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { prompt ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(appColors.glassCardBg)
                            .border(1.dp, appColors.glassBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                inputPrompt = prompt.substringAfter(" ")
                                submitAiPrompt(inputPrompt)
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = prompt,
                                fontSize = 13.sp,
                                color = appColors.textPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error Message
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = NeonRose,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // AI Result Handling (Chat / Greeting vs Valid Task)
            if (parseResult != null) {
                val res = parseResult!!

                // CASE A: Conversational Chat / Greeting / Invalid Task Message
                if (res.isValidTask == false || !res.validationMessage.isNullOrBlank() || res.title.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(appColors.glassCardBg)
                            .border(1.dp, appColors.glassBorderActive, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Trợ Lý AI Gemini",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = res.validationMessage ?: "Xin chào! Tôi là Trợ lý AI Gemini. Tôi có thể giúp bạn bóc tách công việc từ câu nói tự nhiên và tự động phân bổ lịch trình thông minh. Hãy thử bấm Micro và nói hoặc gõ câu lệnh như 'Chiều mai 3h họp nhóm 45 phút'.",
                                    fontSize = 13.sp,
                                    color = appColors.textPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                } else {
                    // CASE B: Valid Parsed Task
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.1f))
                            .border(1.dp, appColors.glassBorderActive, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Kết quả bóc tách từ Gemini AI:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "📌 Tiêu đề: ${res.title}",
                                fontSize = 15.sp,
                                color = appColors.textPrimary,
                                fontWeight = FontWeight.Bold
                            )

                            if (!res.formattedDateLabel.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📅 Thời gian: ${res.formattedDateLabel}",
                                    fontSize = 13.sp,
                                    color = appColors.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⏱️ Thời lượng ước tính: ${res.estimatedDurationMinutes ?: 30} phút",
                                fontSize = 13.sp,
                                color = appColors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🎯 Mức độ ưu tiên: ${res.priority ?: "MEDIUM"}",
                                fontSize = 13.sp,
                                color = appColors.textSecondary
                            )

                            if (res.hasConflict == true && !res.conflictMessage.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = NeonAmber, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Xung đột: ${res.conflictMessage}",
                                        fontSize = 12.sp,
                                        color = NeonAmber
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    taskViewModel.createTask(
                                        title = res.title ?: inputPrompt,
                                        description = res.description ?: res.note,
                                        priority = res.priority ?: "MEDIUM",
                                        quadrant = res.eisenhowerQuadrant ?: "SCHEDULE"
                                    )
                                    parseResult = null
                                    inputPrompt = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Thêm trực tiếp vào danh sách công việc", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
