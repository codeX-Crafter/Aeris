package com.runanywhere.kotlin_starter_example.ui.screens

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.runanywhere.kotlin_starter_example.ui.components.WaveformView
import com.runanywhere.kotlin_starter_example.viewmodel.MainViewModel

@Composable
fun LiveDetectionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val sound by viewModel.currentSound.collectAsState()

    val isAlert = sound != null
    val accentColor = if (isAlert) Color(0xFFFF6B6B) else Color(0xFF6FB1FC)
    val confidence = if (isAlert) 92 else 0

    val recentHistory = listOf(
        "Siren"    to "Just now",
        "Car Horn" to "2 min ago",
        "Voice"    to "5 min ago"
    )

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
                    text = "Live Detection",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Detection Display ─────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAlert) Color(0xFFFF6B6B).copy(alpha = 0.08f) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAlert) Icons.Default.Warning else Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = sound?.name?.uppercase() ?: "NO SOUND",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAlert) Color(0xFFFF6B6B) else Color(0xFF1A2340)
                )
                Text(
                    text = if (isAlert) "Sound detected nearby" else "Monitoring environment…",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7A9A)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Confidence Bar ────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Confidence",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1A2340)
                    )
                    Text(
                        text = "$confidence%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            confidence >= 80 -> Color(0xFF6BCB77)
                            confidence >= 50 -> Color(0xFFFFD166)
                            else            -> Color(0xFFB0B0B0)
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { confidence / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        confidence >= 80 -> Color(0xFF6BCB77)
                        confidence >= 50 -> Color(0xFFFFD166)
                        else            -> Color(0xFFB0B0B0)
                    },
                    trackColor = Color(0xFFEEF0F5)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("LOW", "BALANCED", "HIGH").forEach { label ->
                        Text(text = label, fontSize = 10.sp, color = Color(0xFFB0B0B0))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Waveform ──────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Live Waveform",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A2340)
                )
                Spacer(Modifier.height(12.dp))
                // ✅ Replaced WaveformBars with WaveformView
                WaveformView(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = accentColor
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Recent History ────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Recent Detections",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A2340)
                )
                Spacer(Modifier.height(12.dp))
                recentHistory.forEachIndexed { index, (label, time) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == 0) Color(0xFFFF6B6B) else Color(0xFFB0B0B0)
                                    )
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(text = label, fontSize = 14.sp, color = Color(0xFF1A2340))
                        }
                        Text(text = time, fontSize = 12.sp, color = Color(0xFF6B7A9A))
                    }
                    if (index < recentHistory.lastIndex) {
                        HorizontalDivider(color = Color(0xFFF0F2F8), thickness = 1.dp)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}