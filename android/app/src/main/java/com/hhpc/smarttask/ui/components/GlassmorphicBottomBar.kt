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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hhpc.smarttask.ui.theme.*

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Calendar : BottomNavItem("calendar", "Lịch trình công việc", Icons.Default.DateRange)
    object Tasks : BottomNavItem("tasks", "Quản lý công việc", Icons.Default.FormatListBulleted)
    object AiAssistant : BottomNavItem("ai", "Trợ lý AI Gemini", Icons.Default.AutoAwesome)
    object Notifications : BottomNavItem("notifications", "Thông báo & nhắc hẹn", Icons.Default.Notifications)
    object Settings : BottomNavItem("settings", "Tài khoản & thiết lập", Icons.Default.Person)
}

@Composable
fun GlassmorphicBottomBar(
    currentRoute: String?,
    onItemSelected: (BottomNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Exactly 5 Items with AI Assistant in the EXACT CENTER (Index 2)
    val items = listOf(
        BottomNavItem.Calendar,       // Index 0: Lịch trình công việc
        BottomNavItem.Tasks,          // Index 1: Quản lý công việc
        BottomNavItem.AiAssistant,    // Index 2: Trợ lý AI Gemini (CENTER HERO EMERALD BUTTON)
        BottomNavItem.Notifications,  // Index 3: Thông báo & nhắc hẹn
        BottomNavItem.Settings        // Index 4: Tài khoản & thiết lập
    )

    val appColors = LocalAppColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Sleek Minimalist Floating Navigation Pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(74.dp)
                .clip(RoundedCornerShape(37.dp))
                .background(appColors.glassCardBg)
                .border(1.dp, appColors.glassBorder, RoundedCornerShape(37.dp))
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val isCenterAi = item == BottomNavItem.AiAssistant

                    val springScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.1f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "springScale"
                    )

                    val activeColor by animateColorAsState(
                        targetValue = if (isSelected) EmeraldPrimary else appColors.textSecondary,
                        label = "color"
                    )

                    // Each item gets EXACTLY weight(1f) -> 20% width for perfect equal spacing!
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onItemSelected(item)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCenterAi) {
                            // HERO EMERALD GREEN CIRCLE BUTTON IN THE EXACT CENTER
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .scale(springScale)
                                        .clip(CircleShape)
                                        .background(EmeraldPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = item.title,
                                    color = if (isSelected) EmeraldPrimary else appColors.textSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        } else {
                            // STANDARD TAB WITH EQUAL WEIGHT & iOS SPRING ANIMATION
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.scale(springScale)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = activeColor,
                                    modifier = Modifier.size(22.dp)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = item.title,
                                    color = activeColor,
                                    fontSize = 9.sp,
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
    }
}
