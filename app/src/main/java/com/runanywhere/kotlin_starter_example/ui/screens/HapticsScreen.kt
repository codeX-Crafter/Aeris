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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.runanywhere.kotlin_starter_example.data.SoundType
import com.runanywhere.kotlin_starter_example.services.HapticManager

@Composable
fun HapticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

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
                    text = "Haptic Patterns",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Tap a pattern to feel it",
            fontSize = 13.sp,
            color = Color(0xFF6B7A9A),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Spacer(Modifier.height(4.dp))

        SoundType.values().forEach { sound ->
            HapticCard(sound = sound) {
                HapticManager.trigger(context, sound)
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HapticCard(
    sound: SoundType,
    onTest: () -> Unit
) {
    val meta = hapticMeta(sound)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(meta.color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = meta.icon,
                    contentDescription = null,
                    tint = meta.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meta.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A2340)
                )
                Spacer(Modifier.height(8.dp))
                // Vibration pattern dots
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    meta.patternDots.forEach { size ->
                        Box(
                            modifier = Modifier
                                .size(width = (size * 8).dp, height = 10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(meta.color.copy(alpha = 0.65f))
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Button(
                onClick = onTest,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = meta.color.copy(alpha = 0.12f),
                    contentColor = meta.color
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(
                    Icons.Default.Vibration,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Try", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

private data class HapticMeta(
    val icon: ImageVector,
    val label: String,
    val patternDots: List<Int>,
    val color: Color
)

private fun hapticMeta(sound: SoundType): HapticMeta = when (sound) {
    SoundType.SIREN    -> HapticMeta(Icons.Default.LocalFireDepartment, "Siren",
        listOf(3, 3, 3, 3, 3), Color(0xFFFF6B6B))
    SoundType.HORN     -> HapticMeta(Icons.Default.DirectionsCar, "Car Horn",
        listOf(2, 1, 2, 1, 2), Color(0xFFFFD166))
    SoundType.ALARM    -> HapticMeta(Icons.Default.Alarm, "Alarm",
        listOf(1, 1, 1, 1, 1, 1), Color(0xFFFF9A3C))
    SoundType.DOORBELL -> HapticMeta(Icons.Default.Doorbell, "Doorbell",
        listOf(2, 2), Color(0xFF6FB1FC))
    SoundType.VOICE    -> HapticMeta(Icons.Default.RecordVoiceOver, "Voice",
        listOf(1, 1, 1), Color(0xFF6BCB77))
    else               -> HapticMeta(Icons.Default.VolumeUp, sound.name,
        listOf(1, 1), Color(0xFFB0B0B0))
}