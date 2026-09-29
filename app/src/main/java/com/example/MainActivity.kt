package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.HomeScreen
import com.example.ui.PrivacyScreen
import com.example.ui.RecentScreen
import com.example.ui.ToolDetailScreen
import com.example.ui.components.AppBottomBar
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentScreen != AppScreen.TOOL_DETAIL) {
                            AppBottomBar(
                                currentScreen = currentScreen.name,
                                onNavigate = { screenName ->
                                    when (screenName) {
                                        "HOME" -> viewModel.navigateTo(AppScreen.HOME)
                                        "RECENTS" -> viewModel.navigateTo(AppScreen.RECENTS)
                                        "PRIVACY" -> viewModel.navigateTo(AppScreen.PRIVACY)
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    val modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)

                    when (currentScreen) {
                        AppScreen.HOME -> HomeScreen(viewModel = viewModel, modifier = modifier)
                        AppScreen.TOOL_DETAIL -> ToolDetailScreen(viewModel = viewModel, modifier = modifier)
                        AppScreen.RECENTS -> RecentScreen(viewModel = viewModel, modifier = modifier)
                        AppScreen.PRIVACY -> PrivacyScreen(modifier = modifier)
                    }
                }
            }
        }
    }
}
