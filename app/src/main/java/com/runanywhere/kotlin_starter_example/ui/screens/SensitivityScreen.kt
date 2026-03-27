package com.runanywhere.kotlin_starter_example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.runanywhere.kotlin_starter_example.data.SoundType
import com.runanywhere.kotlin_starter_example.data.SettingsRepository

@Composable
fun SensitivityScreen(onBack: () -> Unit) {

    val sensitivities by SettingsRepository.sensitivities.collectAsState()
    val adaptiveMode by SettingsRepository.adaptiveMode.collectAsState()
    val notificationsEnabled by SettingsRepository.notificationsEnabled.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
    ) {

        // ── Header ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(Color(0xFF6FB1FC), Color(0xFFA7C6FF)))
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Notifications Toggle ──────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFF6FB1FC))
                Spacer(Modifier.width(12.dp))
                Text("Push Notifications", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { SettingsRepository.setNotificationsEnabled(it) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Adaptive Mode ─────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (adaptiveMode)
                    Color(0xFF6FB1FC).copy(alpha = 0.10f) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6FB1FC).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF6FB1FC),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Adaptive Mode",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A2340)
                    )
                    Text(
                        "Auto-adjusts based on surroundings",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7A9A)
                    )
                }
                Switch(
                    checked = adaptiveMode,
                    onCheckedChange = { SettingsRepository.setAdaptiveMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF6FB1FC)
                    )
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Threshold Sensitivities",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7A9A),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        SoundType.values().forEach { sound ->
            val value = sensitivities[sound] ?: 0.5f
            SensitivityCard(
                sound = sound,
                value = value,
                onChange = { SettingsRepository.setSensitivity(sound, it) }
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SensitivityCard(
    sound: SoundType,
    value: Float,
    onChange: (Float) -> Unit
) {
    val (icon, label) = soundMeta(sound)
    val levelLabel = when {
        value < 0.35f -> "High Sensitivity"
        value < 0.65f -> "Balanced"
        else -> "Low Sensitivity"
    }
    val levelColor = when {
        value < 0.35f -> Color(0xFF6FB1FC)
        value < 0.65f -> Color(0xFFFFD166)
        else -> Color(0xFFB0B0B0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(levelColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = levelColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A2340)
                    )
                    Text(
                        text = "Trigger at ${(value * 100).toInt()}% confidence",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7A9A)
                    )
                }
                Text(
                    text = levelLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = levelColor
                )
            }

            Spacer(Modifier.height(14.dp))

            Slider(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = levelColor,
                    activeTrackColor = levelColor,
                    inactiveTrackColor = Color(0xFFEEF0F5)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("High Sensitive", "Balanced", "Less Sensitive").forEach { l ->
                    Text(l, fontSize = 10.sp, color = Color(0xFFB0B0B0))
                }
            }
        }
    }
}

private fun soundMeta(sound: SoundType): Pair<ImageVector, String> = when (sound) {
    SoundType.SIREN    -> Icons.Default.LocalFireDepartment to "Siren"
    SoundType.HORN     -> Icons.Default.DirectionsCar       to "Car Horn"
    SoundType.ALARM    -> Icons.Default.Alarm               to "Alarm"
    SoundType.DOORBELL -> Icons.Default.Doorbell            to "Doorbell"
    SoundType.VOICE    -> Icons.Default.RecordVoiceOver     to "Voice"
}
