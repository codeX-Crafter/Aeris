package com.runanywhere.kotlin_starter_example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.runanywhere.kotlin_starter_example.services.ModelService
import com.runanywhere.kotlin_starter_example.ui.screens.*
import com.runanywhere.kotlin_starter_example.ui.theme.KotlinStarterTheme
import com.runanywhere.kotlin_starter_example.viewmodel.MainViewModel
import com.runanywhere.sdk.core.onnx.ONNX
import com.runanywhere.sdk.foundation.bridge.extensions.CppBridgeModelPaths
import com.runanywhere.sdk.llm.llamacpp.LlamaCPP
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.storage.AndroidPlatformContext

class MainActivity : ComponentActivity() {

    private val requestMicPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) Log.w("MainActivity", "Mic permission denied")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        AndroidPlatformContext.initialize(this)
        RunAnywhere.initialize("development")

        val basePath = java.io.File(filesDir, "runanywhere").absolutePath
        CppBridgeModelPaths.setBaseDirectory(basePath)

        try {
            LlamaCPP.register(priority = 100)
        } catch (e: Throwable) {
            Log.w("MainActivity", "LlamaCPP: ${e.message}")
        }

        ONNX.register(priority = 100)
        ModelService.registerDefaultModels()

        setContent {
            KotlinStarterTheme {
                AerisApp()
            }
        }
    }
}

@Composable
fun AerisApp() {
    val navController = rememberNavController()
    val viewModel: MainViewModel = viewModel()
    val modelService: ModelService = viewModel()

    NavHost(navController = navController, startDestination = "home") {

        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                modelService = modelService,
                onLive = { navController.navigate("live") },
                onSettings = { navController.navigate("sensitivity") },
                onHaptics = { navController.navigate("haptics") },
                onCaptions = { navController.navigate("captions") },
                onHistory = { navController.navigate("history") },
                onVoiceProxy = { navController.navigate("voice_proxy") },
                onConversation = { navController.navigate("conversation") }
            )
        }

        composable("live") {
            LiveDetectionScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("sensitivity") {
            SensitivityScreen(onBack = { navController.popBackStack() })
        }

        composable("haptics") {
            HapticsScreen(onBack = { navController.popBackStack() })
        }

        composable("captions") {
            LiveCaptionScreen(
                modelService = modelService,
                onBack = { navController.popBackStack() }
            )
        }

        composable("history") {
            HistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("voice_proxy") {
            VoiceProxyScreen(
                modelService = modelService,
                onBack = { navController.popBackStack() }
            )
        }

        composable("conversation") {
            ConversationScreen(
                modelService = modelService,
                onBack = { navController.popBackStack() }
            )
        }

        composable("stt") {
            SpeechToTextScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}