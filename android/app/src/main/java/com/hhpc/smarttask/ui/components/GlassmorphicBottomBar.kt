package com.hhpc.smarttask.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hhpc.smarttask.ui.theme.*

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Tasks : BottomNavItem("tasks", "Công việc", Icons.Default.FormatListBulleted)
    object Eisenhower : BottomNavItem("eisenhower", "Ma trận", Icons.Default.GridView)
    object Calendar : BottomNavItem("calendar", "Lịch trình", Icons.Default.DateRange)
    object AiAssistant : BottomNavItem("ai", "Trợ lý AI", Icons.Default.AutoAwesome)
    object Analytics : BottomNavItem("analytics", "Thống kê", Icons.Default.BarChart)
    object Settings : BottomNavItem("settings", "Cài đặt", Icons.Default.Person)
}

@Composable
fun GlassmorphicBottomBar(
    currentRoute: String?,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // 6 Items with AI Assistant in the EXACT CENTER (Index 3)
    val items = listOf(
        BottomNavItem.Tasks,
        BottomNavItem.Eisenhower,
        BottomNavItem.Calendar,
        BottomNavItem.AiAssistant, // CENTER HERO BUTTON
        BottomNavItem.Analytics,
        BottomNavItem.Settings
    )

    val appColors = LocalAppColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Pill Glass Capsule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (appColors.isDark) Color(0xF0112A22) else Color(0xFFFFFFFF),
                            if (appColors.isDark) Color(0xEE081C15) else Color(0xFFF5FCF8)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            appColors.glassBorderActive,
                            appColors.glassBorder
                        )
                    ),
                    shape = RoundedCornerShape(36.dp)
                )
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val isCenterAi = item == BottomNavItem.AiAssistant

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.2f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "scale"
                    )

                    val activeColor by animateColorAsState(
                        targetValue = if (isSelected) EmeraldPrimary else appColors.textSecondary,
                        label = "color"
                    )

                    if (isCenterAi) {
                        // SPECIAL HERO CENTER FLOATING BUTTON FOR AI
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onItemSelected(item)
                                }
                                .padding(horizontal = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .scale(iconScale)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                EmeraldPrimary,
                                                MintAccent
                                            )
                                        )
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = item.title,
                                color = if (isSelected) EmeraldPrimary else appColors.textSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // STANDARD NAV ITEMS
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onItemSelected(item)
                                }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        EmeraldPrimary.copy(alpha = 0.25f),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                }
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = activeColor,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .scale(iconScale)
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = item.title,
                                color = activeColor,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
