package com.runanywhere.kotlin_starter_example.ui.screens

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
import com.runanywhere.kotlin_starter_example.ui.components.SpectrogramView
import com.runanywhere.kotlin_starter_example.viewmodel.MainViewModel

@Composable
fun LiveDetectionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val sound      by viewModel.currentSound.collectAsState()
    val confidence by viewModel.confidence.collectAsState()

    val isAlert     = sound != null
    val accentColor = if (isAlert) Color(0xFFFF6B6B) else Color(0xFF6FB1FC)

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
                    text = "Live Intelligence",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Detection Display ─────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAlert)
                    Color(0xFFFF6B6B).copy(alpha = 0.08f) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAlert) Icons.Default.Warning
                        else Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = sound?.name?.uppercase() ?: "MONITORING",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAlert) Color(0xFFFF6B6B) else Color(0xFF1A2340)
                )
                Text(
                    text = if (isAlert) "$confidence% match probability" else "Listening for environmental cues…",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7A9A)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── AI Spectrogram (YAMNet Output) ────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A2340)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AI Log-Mel Spectrogram",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                SpectrogramView()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Real-time frequency features extracted by YAMNet",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Waveform ──────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Input Waveform",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A2340)
                )
                Spacer(Modifier.height(10.dp))
                WaveformView(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = accentColor
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── History ──────────────────────────────────────────────
        val history by viewModel.history.collectAsState()
        
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
                    text = "Recent Intelligence",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A2340)
                )
                Spacer(Modifier.height(12.dp))
                
                if (history.isEmpty()) {
                    Text("No detections yet", fontSize = 12.sp, color = Color.Gray)
                }

                history.take(5).forEachIndexed { index, event ->
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
                                    .background(if (index == 0) accentColor else Color(0xFFB0B0B0))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(text = event.type.name, fontSize = 14.sp, color = Color(0xFF1A2340))
                        }
                        Text(
                            text = "${(event.confidence * 100).toInt()}% match", 
                            fontSize = 12.sp, 
                            color = Color(0xFF6B7A9A)
                        )
                    }
                    if (index < 4 && index < history.size - 1) {
                        HorizontalDivider(color = Color(0xFFF0F2F8), thickness = 1.dp)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
