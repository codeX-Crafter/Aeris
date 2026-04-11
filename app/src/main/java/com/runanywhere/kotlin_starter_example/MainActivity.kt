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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.runanywhere.kotlin_starter_example.data.ConversationRepository
import com.runanywhere.kotlin_starter_example.data.SettingsRepository
import com.runanywhere.kotlin_starter_example.services.ModelService
import com.runanywhere.kotlin_starter_example.ui.screens.*
import com.runanywhere.kotlin_starter_example.ui.theme.KotlinStarterTheme
import com.runanywhere.kotlin_starter_example.viewmodel.AuthViewModel
import com.runanywhere.kotlin_starter_example.viewmodel.HistoryViewModel
import com.runanywhere.kotlin_starter_example.viewmodel.MainViewModel
import com.runanywhere.kotlin_starter_example.viewmodel.ProfileViewModel
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
                Log.d("MainActivity", "All core permissions granted")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        AndroidPlatformContext.initialize(this)
        SettingsRepository.init(this)
        ConversationRepository.init(this)
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
                        onDismiss = { showPermissionDialog = false },
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", packageName, null)
                            }
                            startActivity(intent)
                            showPermissionDialog = false
                        }
                    )
                }

                // Flash Overlay Permission Check
                val flashEnabled by SettingsRepository.flashEnabled.collectAsState()
                LaunchedEffect(flashEnabled) {
                    if (flashEnabled && !Settings.canDrawOverlays(this@MainActivity)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
                    }
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
    val authViewModel: AuthViewModel = viewModel()
    val profileViewModel: ProfileViewModel = viewModel()
    val historyViewModel: HistoryViewModel = viewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val showBottomBar = currentDestination?.route !in listOf("login", "signup")

    val items = listOf(
        Screen.Home,
        Screen.Communication,
        Screen.Profile,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF6FB1FC),
                                selectedTextColor = Color(0xFF6FB1FC),
                                indicatorColor = Color(0xFF6FB1FC).copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = if (authViewModel.currentUser != null) Screen.Home.route else "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                LoginScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = { navController.navigate("signup") }
                )
            }

            composable("signup") {
                SignupScreen(
                    authViewModel = authViewModel,
                    onSignupSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("signup") { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    modelService = modelService,
                    onSettings = { navController.navigate("sensitivity") },
                    onHaptics = { navController.navigate("haptics") }
                )
            }

            composable(Screen.Communication.route) {
                CommunicationScreen(
                    onConverse = { navController.navigate("conversation") },
                    onVoiceProxy = { navController.navigate("voice_proxy") },
                    onCaptions = { navController.navigate("captions") }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(profileViewModel = profileViewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    modelService = modelService,
                    authViewModel = authViewModel,
                    onHistoryClick = { navController.navigate("history") },
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // ── Sub-screens (accessible via communication/home) ──
            composable("sensitivity") { SensitivityScreen(onBack = { navController.popBackStack() }) }
            composable("haptics") { HapticsScreen(onBack = { navController.popBackStack() }) }
            composable("conversation") { 
                ConversationScreen(
                    modelService = modelService, 
                    historyViewModel = historyViewModel,
                    onBack = { navController.popBackStack() }
                ) 
            }
            composable("voice_proxy") { VoiceProxyScreen(modelService = modelService, onBack = { navController.popBackStack() }) }
            composable("captions") { 
                LiveCaptionScreen(
                    modelService = modelService, 
                    historyViewModel = historyViewModel,
                    onBack = { navController.popBackStack() }
                ) 
            }
            composable("history") { 
                HistoryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                ) 
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Communication : Screen("comm", "Chat", Icons.Default.ChatBubbleOutline)
    object Profile : Screen("profile", "Profile", Icons.Default.AccountCircle)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
