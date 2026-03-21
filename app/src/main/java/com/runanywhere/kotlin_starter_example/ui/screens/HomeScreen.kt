package com.runanywhere.kotlin_starter_example.ui.screens

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.runanywhere.kotlin_starter_example.data.SoundType
import com.runanywhere.kotlin_starter_example.services.AudioForegroundService
import com.runanywhere.kotlin_starter_example.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onLive: () -> Unit,
    onSettings: () -> Unit,
    onHaptics: () -> Unit
) {
    val context = LocalContext.current
    val sound by viewModel.currentSound.collectAsState()
    var isOn by remember { mutableStateOf(false) }

    // Auto-trigger haptics when sound detected
    LaunchedEffect(Unit) {
        viewModel.observeSounds(context)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOn) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val softBlueStart = Color(0xFF6FB1FC)
    val softBlueEnd = Color(0xFFA7C6FF)
    val alertRed = Color(0xFFFF6B6B)

    val statusColor = when {
        sound != null && isOn -> alertRed
        isOn -> softBlueStart
        else -> Color(0xFFB0B0B0)
    }

    val statusText = when {
        !isOn -> "System Off"
        sound != null -> "${sound!!.name} Detected"
        else -> "Listening…"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Header ──────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(listOf(softBlueStart, softBlueEnd))
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Column {
                    Text(
                        text = "Aeris",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Stay aware without looking",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Toggle ───────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(140.dp)
                    .scale(pulseScale)
                    .shadow(
                        elevation = if (isOn) 20.dp else 6.dp,
                        shape = CircleShape,
                        ambientColor = softBlueStart.copy(alpha = 0.4f),
                        spotColor = softBlueStart.copy(alpha = 0.4f)
                    )
                    .clip(CircleShape)
                    .background(
                        if (isOn)
                            Brush.radialGradient(listOf(softBlueStart, softBlueEnd))
                        else
                            Brush.radialGradient(
                                listOf(Color(0xFFDDE3EE), Color(0xFFB8C0D0))
                            )
                    )
                    .clickable {
                        isOn = !isOn
                        val intent = Intent(context, AudioForegroundService::class.java)
                        if (isOn) {
                            context.startForegroundService(intent)
                        } else {
                            context.stopService(intent)
                        }
                    }
            ) {
                Icon(
                    imageVector = if (isOn) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = "Toggle",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Status Card ──────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                !isOn -> Icons.Default.MicOff
                                sound != null -> Icons.Default.Warning
                                else -> Icons.Default.GraphicEq
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = statusText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A2340)
                        )
                        Text(
                            text = if (isOn) "System active" else "Tap toggle to start",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7A9A)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Quick Actions ────────────────────────────────────
            Text(
                text = "Quick Actions",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF6B7A9A),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.GraphicEq,
                    label = "Live",
                    modifier = Modifier.weight(1f),
                    onClick = onLive
                )
                QuickActionCard(
                    icon = Icons.Default.Tune,
                    label = "Sensitivity",
                    modifier = Modifier.weight(1f),
                    onClick = onSettings
                )
                QuickActionCard(
                    icon = Icons.Default.Vibration,
                    label = "Haptics",
                    modifier = Modifier.weight(1f),
                    onClick = onHaptics
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF6FB1FC),
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1A2340)
            )
        }
    }
}