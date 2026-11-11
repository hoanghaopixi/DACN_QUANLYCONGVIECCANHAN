package com.hhpc.smarttask.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hhpc.smarttask.ui.components.BottomNavItem
import com.hhpc.smarttask.ui.components.GlassmorphicBottomBar
import com.hhpc.smarttask.ui.screens.*
import com.hhpc.smarttask.ui.theme.LocalAppColors

private fun getRouteIndex(route: String?): Int {
    return when (route) {
        BottomNavItem.Tasks.route -> 0
        BottomNavItem.Eisenhower.route -> 1
        BottomNavItem.Calendar.route -> 2
        BottomNavItem.AiAssistant.route -> 3
        BottomNavItem.Analytics.route -> 4
        BottomNavItem.Settings.route -> 5
        else -> 0
    }
}

@Composable
fun SmartTaskNavGraph(
    authViewModel: AuthViewModel = viewModel(),
    taskViewModel: TaskViewModel = viewModel()
) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.collectAsState()
    val appColors = LocalAppColors.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val mainItems = listOf(
        BottomNavItem.Tasks,
        BottomNavItem.Eisenhower,
        BottomNavItem.Calendar,
        BottomNavItem.AiAssistant,
        BottomNavItem.Analytics,
        BottomNavItem.Settings
    )

    val isMainScreen = currentRoute in mainItems.map { it.route }
    var dragOffsetX by remember { mutableStateOf(0f) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            taskViewModel.loadData()
            if (currentRoute == "login" || currentRoute == null) {
                navController.navigate(BottomNavItem.Tasks.route) {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        containerColor = appColors.bgBot,
        bottomBar = {
            if (isMainScreen && authState is AuthState.Success) {
                GlassmorphicBottomBar(
                    currentRoute = currentRoute,
                    onItemSelected = { item ->
                        navController.navigate(item.route) {
                            popUpTo(BottomNavItem.Tasks.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(currentRoute) {
                    if (isMainScreen) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragOffsetX = 0f },
                            onHorizontalDrag = { _, dragAmount ->
                                dragOffsetX += dragAmount
                            },
                            onDragEnd = {
                                val currIdx = getRouteIndex(currentRoute)
                                if (dragOffsetX < -100f && currIdx < mainItems.size - 1) {
                                    // Swipe Left -> Next tab to the right
                                    val next = mainItems[currIdx + 1]
                                    navController.navigate(next.route) {
                                        popUpTo(BottomNavItem.Tasks.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else if (dragOffsetX > 100f && currIdx > 0) {
                                    // Swipe Right -> Previous tab to the left
                                    val prev = mainItems[currIdx - 1]
                                    navController.navigate(prev.route) {
                                        popUpTo(BottomNavItem.Tasks.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
        ) {
            NavHost(
                navController = navController,
                startDestination = "login",
                enterTransition = {
                    val initialIdx = getRouteIndex(initialState.destination.route)
                    val targetIdx = getRouteIndex(targetState.destination.route)
                    val isMovingRight = targetIdx > initialIdx

                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> if (isMovingRight) fullWidth else -fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeIn(animationSpec = tween(durationMillis = 300))
                },
                exitTransition = {
                    val initialIdx = getRouteIndex(initialState.destination.route)
                    val targetIdx = getRouteIndex(targetState.destination.route)
                    val isMovingRight = targetIdx > initialIdx

                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> if (isMovingRight) -fullWidth else fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))
                },
                popEnterTransition = {
                    val initialIdx = getRouteIndex(initialState.destination.route)
                    val targetIdx = getRouteIndex(targetState.destination.route)
                    val isMovingRight = targetIdx > initialIdx

                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> if (isMovingRight) fullWidth else -fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeIn(animationSpec = tween(durationMillis = 300))
                },
                popExitTransition = {
                    val initialIdx = getRouteIndex(initialState.destination.route)
                    val targetIdx = getRouteIndex(targetState.destination.route)
                    val isMovingRight = targetIdx > initialIdx

                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> if (isMovingRight) -fullWidth else fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    ) + fadeOut(animationSpec = tween(durationMillis = 300))
                }
            ) {
                composable("login") {
                    LoginScreen(
                        authViewModel = authViewModel,
                        onLoginSuccess = {
                            taskViewModel.loadData()
                            navController.navigate(BottomNavItem.Tasks.route) {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }

                composable(BottomNavItem.Tasks.route) {
                    HomeScreen(taskViewModel = taskViewModel)
                }

                composable(BottomNavItem.Eisenhower.route) {
                    EisenhowerScreen(taskViewModel = taskViewModel)
                }

                composable(BottomNavItem.Calendar.route) {
                    CalendarScreen(taskViewModel = taskViewModel)
                }

                composable(BottomNavItem.Analytics.route) {
                    AnalyticsScreen(taskViewModel = taskViewModel)
                }

                composable(BottomNavItem.AiAssistant.route) {
                    AiAssistantScreen(taskViewModel = taskViewModel)
                }

                composable(BottomNavItem.Settings.route) {
                    SettingsScreen(
                        authViewModel = authViewModel,
                        taskViewModel = taskViewModel,
                        onLogout = {
                            navController.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
