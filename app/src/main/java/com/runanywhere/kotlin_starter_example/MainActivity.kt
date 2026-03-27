package com.runanywhere.kotlin_starter_example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.runanywhere.kotlin_starter_example.data.SettingsRepository
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

    private val requiredPermissions = mutableListOf(Manifest.permission.RECORD_AUDIO).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val allGranted = results.values.all { it }
            if (allGranted) {
                Log.d("MainActivity", "All permissions granted")
            } else {
                Log.w("MainActivity", "Some permissions denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AndroidPlatformContext.initialize(this)
        SettingsRepository.init(this)
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
                var showPermissionDialog by remember { 
                    mutableStateOf(!hasAllPermissions()) 
                }

                if (showPermissionDialog) {
                    PermissionRequirementDialog(
                        onConfirm = {
                            showPermissionDialog = false
                            requestPermissionsLauncher.launch(requiredPermissions.toTypedArray())
                        },
                        onDismiss = {
                            showPermissionDialog = false
                        },
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                            showPermissionDialog = false
                        }
                    )
                }

                AerisApp()
            }
        }
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}

@Composable
fun PermissionRequirementDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions Required") },
        text = {
            Text(
                "Aeris needs Microphone and Notification permissions to detect sounds and alert you in real-time. " +
                "Please grant these permissions to continue."
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Grant Permissions")
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open Settings")
            }
        }
    )
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
                onLive = { /* Removed */ },
                onSettings = { navController.navigate("sensitivity") },
                onHaptics = { navController.navigate("haptics") },
                onCaptions = { navController.navigate("captions") },
                onHistory = { navController.navigate("history") },
                onVoiceProxy = { navController.navigate("voice_proxy") },
                onConversation = { navController.navigate("conversation") }
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
