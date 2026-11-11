package com.hhpc.smarttask

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hhpc.smarttask.ui.navigation.SmartTaskNavGraph
import com.hhpc.smarttask.ui.screens.AuthViewModel
import com.hhpc.smarttask.ui.screens.TaskViewModel
import com.hhpc.smarttask.ui.theme.SmartTaskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val taskViewModel: TaskViewModel = viewModel()
            val authViewModel: AuthViewModel = viewModel()
            val userPrefs by taskViewModel.userPreferences.collectAsState()

            SmartTaskTheme(themePreference = userPrefs.theme ?: "SYSTEM") {
                SmartTaskNavGraph(authViewModel = authViewModel, taskViewModel = taskViewModel)
            }
        }
    }
}
