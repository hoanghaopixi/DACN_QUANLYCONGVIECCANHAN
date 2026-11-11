package com.hhpc.smarttask.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
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
import com.hhpc.smarttask.ui.theme.*

@Composable
fun AnalyticsScreen(
    taskViewModel: TaskViewModel
) {
    val report by taskViewModel.productivityReport.collectAsState()
    val scrollState = rememberScrollState()
    val appColors = LocalAppColors.current

    val score = report?.productivityScore ?: 85
    val scoreText = when {
        score >= 80 -> "Xuất sắc"
        score >= 60 -> "Khá tốt"
        else -> "Cần cải thiện"
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
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Báo cáo Năng suất & AI",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = appColors.textPrimary
                )
            }

            // Score Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                appColors.glassCardBg,
                                appColors.glassCardBg.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .border(1.5.dp, appColors.glassBorderActive, RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chỉ số Năng suất Hôm nay",
                            fontSize = 13.sp,
                            color = appColors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$score",
                                fontSize = 46.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = " / 100",
                                fontSize = 16.sp,
                                color = appColors.textSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Đánh giá: $scoreText",
                                color = EmeraldPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        EmeraldPrimary.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = LimeHighlight,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4 Key Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Completed Tasks
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(appColors.glassCardBg)
                        .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${report?.totalCompletedTasks ?: 0} / ${report?.totalTasks ?: 0}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                        Text(text = "Đã hoàn thành", fontSize = 11.sp, color = appColors.textSecondary)
                    }
                }

                // Card 2: On Time Rate
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(appColors.glassCardBg)
                        .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        val rate = (report?.onTimeRate ?: 1.0) * 100
                        Text(
                            text = "${rate.toInt()}%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                        Text(text = "Đúng hạn chót", fontSize = 11.sp, color = appColors.textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 3: Total Focus Minutes
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(appColors.glassCardBg)
                        .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = LimeHighlight, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${report?.totalFocusMinutes ?: 0} phút",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                        Text(text = "Thời gian tập trung", fontSize = 11.sp, color = appColors.textSecondary)
                    }
                }

                // Card 4: Streak Days
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(appColors.glassCardBg)
                        .border(1.dp, appColors.glassBorder, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Text(text = "🔥", fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${report?.currentStreakDays ?: 1} ngày",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                        Text(text = "Chuỗi thói quen", fontSize = 11.sp, color = appColors.textSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI Insights Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(EmeraldPrimary.copy(alpha = 0.1f))
                    .border(1.dp, appColors.glassBorderActive, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đánh giá Năng suất từ Gemini AI",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = appColors.textPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = report?.aiFeedback ?: "Bạn làm việc rất hiệu quả vào buổi sáng! Tỷ lệ hoàn thành công việc đúng hạn duy trì ở mức cao.",
                        fontSize = 13.sp,
                        color = appColors.textSecondary,
                        lineHeight = 18.sp
                    )

                    if (!report?.aiImprovementTips.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Khuyến nghị: ${report?.aiImprovementTips}",
                            fontSize = 12.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
