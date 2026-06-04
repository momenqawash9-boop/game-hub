package com.aistudio.gamebooster

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aistudio.gamebooster.service.FloatingOverlayService
import com.aistudio.gamebooster.ui.screens.AppSelectorScreen
import com.aistudio.gamebooster.ui.screens.DashboardScreen
import com.aistudio.gamebooster.ui.screens.SettingsScreen
import com.aistudio.gamebooster.ui.theme.MyApplicationTheme
import com.aistudio.gamebooster.ui.viewmodel.GameBoosterViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: GameBoosterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                
                NavHost(
                    navController = navController,
                    startDestination = "dashboard",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { navController.navigate("settings") },
                            onNavigateToAppSelector = { navController.navigate("app_selector") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable("app_selector") {
                        AppSelectorScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            stopService(Intent(this, FloatingOverlayService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
