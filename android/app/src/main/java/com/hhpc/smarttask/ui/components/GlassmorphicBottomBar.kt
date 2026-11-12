package com.hhpc.smarttask.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hhpc.smarttask.ui.theme.*

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Calendar : BottomNavItem("calendar", "Lịch của tôi", Icons.Default.DateRange)
    object Tasks : BottomNavItem("tasks", "Công việc", Icons.Default.FormatListBulleted)
    object AiAssistant : BottomNavItem("ai", "Trợ lý AI", Icons.Default.AutoAwesome)
    object Notifications : BottomNavItem("notifications", "Thông báo", Icons.Default.Notifications)
    object Settings : BottomNavItem("settings", "Cài đặt", Icons.Default.Person)
}

@Composable
fun GlassmorphicBottomBar(
    currentRoute: String?,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // 5 Navigation Items with AI Assistant in the EXACT CENTER (Index 2)
    val items = listOf(
        BottomNavItem.Calendar,       // Index 0: Lịch của tôi
        BottomNavItem.Tasks,          // Index 1: Công việc
        BottomNavItem.AiAssistant,    // Index 2: Trợ lý AI
        BottomNavItem.Notifications,  // Index 3: Thông báo
        BottomNavItem.Settings        // Index 4: Cài đặt
    )

    val appColors = LocalAppColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Glass Navigation Bar Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(34.dp))
                .background(appColors.glassCardBg)
                .border(1.dp, appColors.glassBorder, RoundedCornerShape(34.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route

                    val activeColor by animateColorAsState(
                        targetValue = if (isSelected) EmeraldPrimary else appColors.textSecondary,
                        animationSpec = tween(durationMillis = 200),
                        label = "color"
                    )

                    val pillBgColor by animateColorAsState(
                        targetValue = if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else Color.Transparent,
                        animationSpec = tween(durationMillis = 200),
                        label = "pillBgColor"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(top = 4.dp) // Shifted down slightly by 4dp (~10px) for perfect nav bar balance!
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onItemSelected(item)
                            }
                    ) {
                        // Icon Container - DIRECTLY WRAPS ICON WITH 100% PERFECT CENTER ALIGNMENT INSIDE PILL!
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(pillBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = activeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Text Label
                        Text(
                            text = item.title,
                            color = activeColor,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
