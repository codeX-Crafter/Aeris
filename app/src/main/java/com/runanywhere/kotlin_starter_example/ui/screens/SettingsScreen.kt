package com.runanywhere.kotlin_starter_example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.runanywhere.kotlin_starter_example.data.SettingsRepository
import com.runanywhere.kotlin_starter_example.services.ModelService
import com.runanywhere.kotlin_starter_example.viewmodel.AuthViewModel

@Composable
fun SettingsScreen(
    modelService: ModelService,
    authViewModel: AuthViewModel,
    onHistoryClick: () -> Unit,
    onLogout: () -> Unit
) {
    val notificationsEnabled by SettingsRepository.notificationsEnabled.collectAsState()
    val adaptiveMode by SettingsRepository.adaptiveMode.collectAsState()
    val flashEnabled by SettingsRepository.flashEnabled.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF6FB1FC), Color(0xFFA7C6FF))))
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                text = "Settings",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(Modifier.height(24.dp))

        // Preferences Section
        SettingsSection(title = "Alert Preferences") {
            SettingsToggleItem(
                icon = Icons.Default.Notifications,
                label = "Push Notifications",
                description = "Get alerts for critical sounds",
                checked = notificationsEnabled,
                onCheckedChange = { SettingsRepository.setNotificationsEnabled(it) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F2F8))
            SettingsToggleItem(
                icon = Icons.Default.FlashOn,
                label = "Flash Alerts",
                description = "Flash screen on detection",
                checked = flashEnabled,
                onCheckedChange = { SettingsRepository.setFlashEnabled(it) }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F2F8))
            SettingsToggleItem(
                icon = Icons.Default.AutoAwesome,
                label = "Adaptive Mode",
                description = "AI auto-adjusts sensitivity",
                checked = adaptiveMode,
                onCheckedChange = { SettingsRepository.setAdaptiveMode(it) }
            )
        }

        Spacer(Modifier.height(24.dp))

        // Help & Onboarding
        SettingsSection(title = "Help & Support") {
            SettingsActionItem(
                icon = Icons.AutoMirrored.Filled.HelpCenter,
                label = "Show Walkthrough",
                description = "Learn how to use Aeris again",
                onClick = { SettingsRepository.setWalkthroughCompleted(false) }
            )
        }

        Spacer(Modifier.height(24.dp))

        // Data & History
        SettingsSection(title = "Data & History") {
            SettingsActionItem(
                icon = Icons.Default.History,
                label = "Detection History",
                description = "Review past sound alerts",
                onClick = onHistoryClick
            )
        }

        Spacer(Modifier.height(24.dp))

        // Account Section
        SettingsSection(title = "Account") {
            SettingsActionItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                label = "Logout",
                description = "Sign out of your account",
                onClick = { 
                    authViewModel.logout(context)
                    onLogout()
                },
                color = Color(0xFFFF6B6B)
            )
        }

        Spacer(Modifier.height(24.dp))

        // Model Management Section
        SettingsSection(title = "Model Management") {
            SettingsActionItem(
                icon = Icons.Default.DeleteSweep,
                label = "Unload All Models",
                description = "Free up device memory",
                onClick = { modelService.unloadAllModels() },
                color = Color(0xFFFF6B6B)
            )
        }

        Spacer(Modifier.height(24.dp))

        // About Section
        SettingsSection(title = "About") {
            SettingsInfoItem(label = "Version", value = "1.0.0 (Alpha)")
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F2F8))
            SettingsInfoItem(label = "Engine", value = "RunAnywhere local AI")
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7A9A),
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color(0xFF6FB1FC), modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A2340))
            Text(description, fontSize = 12.sp, color = Color(0xFF6B7A9A))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF6FB1FC))
        )
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    color: Color = Color(0xFF1A2340)
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (color == Color(0xFF1A2340)) Color(0xFF6FB1FC) else color, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = color)
            Text(description, fontSize = 12.sp, color = Color(0xFF6B7A9A))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFFB0B0B0))
    }
}

@Composable
private fun SettingsInfoItem(label: String, value: String) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 15.sp, color = Color(0xFF1A2340))
        Text(value, fontSize = 14.sp, color = Color(0xFF6B7A9A))
    }
}
