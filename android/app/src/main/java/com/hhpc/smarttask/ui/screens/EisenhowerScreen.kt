package com.hhpc.smarttask.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hhpc.smarttask.data.model.TaskDto
import com.hhpc.smarttask.ui.theme.*

@Composable
fun EisenhowerScreen(
    taskViewModel: TaskViewModel
) {
    val eisenhowerMap by taskViewModel.eisenhowerMap.collectAsState()
    val appColors = LocalAppColors.current

    val doFirstTasks = eisenhowerMap["DO_FIRST"] ?: emptyList()
    val scheduleTasks = eisenhowerMap["SCHEDULE"] ?: emptyList()
    val delegateTasks = eisenhowerMap["DELEGATE"] ?: emptyList()
    val dontDoTasks = eisenhowerMap["DONT_DO"] ?: emptyList()

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
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Ma trận Eisenhower",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = appColors.textPrimary,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
            )
            Text(
                text = "Phân loại mức độ ưu tiên theo 4 góc phần tư",
                fontSize = 13.sp,
                color = appColors.textSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 2x2 Grid of Quadrants
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuadrantBox(
                        title = "Q1: LÀM NGAY",
                        subtitle = "Khẩn cấp & Quan trọng",
                        tasks = doFirstTasks,
                        accentColor = NeonRose,
                        modifier = Modifier.weight(1f)
                    )
                    QuadrantBox(
                        title = "Q2: LÊN LỊCH",
                        subtitle = "Quan trọng - Không khẩn cấp",
                        tasks = scheduleTasks,
                        accentColor = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuadrantBox(
                        title = "Q3: UỶ QUYỀN",
                        subtitle = "Khẩn cấp - Không quan trọng",
                        tasks = delegateTasks,
                        accentColor = NeonAmber,
                        modifier = Modifier.weight(1f)
                    )
                    QuadrantBox(
                        title = "Q4: BỎ QUA",
                        subtitle = "Không khẩn cấp & Không quan trọng",
                        tasks = dontDoTasks,
                        accentColor = MintAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun QuadrantBox(
    title: String,
    subtitle: String,
    tasks: List<TaskDto>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(appColors.glassCardBg)
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = appColors.textSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Trống",
                        fontSize = 11.sp,
                        color = appColors.textSecondary.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(tasks) { task ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = task.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = appColors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
