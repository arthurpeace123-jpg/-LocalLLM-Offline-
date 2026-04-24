package com.example.localllm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.localllm.ui.chat.ChatScreen
import com.example.localllm.ui.models.ModelsScreen
import com.example.localllm.ui.theme.LocalLLMTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocalLLMTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "models") {
                        composable("models") {
                            ModelsScreen(
                                onModelSelected = { modelId ->
                                    navController.navigate("chat/$modelId")
                                }
                            )
                        }
                        composable("chat/{modelId}") { backStackEntry ->
                            val modelId = backStackEntry.arguments?.getString("modelId") ?: ""
                            ChatScreen(
                                modelId = modelId,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
