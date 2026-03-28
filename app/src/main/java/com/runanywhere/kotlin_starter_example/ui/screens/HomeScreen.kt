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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.runanywhere.kotlin_starter_example.services.ModelService
import com.runanywhere.kotlin_starter_example.viewmodel.MainViewModel

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modelService: ModelService,         // ✅ comes from NavHost — no second viewModel() call
    onLive: () -> Unit,
    onSettings: () -> Unit,
    onHaptics: () -> Unit,
    onCaptions: () -> Unit,
    onHistory: () -> Unit,
    onVoiceProxy: () -> Unit,
    onConversation: () -> Unit
) {
    val context = LocalContext.current

    val sound: SoundType? by viewModel.currentSound.collectAsState(initial = null)
    val confidence by viewModel.confidence.collectAsState()
    
    var isOn by remember { mutableStateOf(AudioForegroundService.isRunning) }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isOn) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val softBlueStart = Color(0xFF6FB1FC)
    val softBlueEnd   = Color(0xFFA7C6FF)
    val alertRed      = Color(0xFFFF6B6B)
    val currentSound  = sound

    val statusColor = when {
        currentSound != null && isOn -> alertRed
        isOn                         -> softBlueStart
        else                         -> Color(0xFFB0B0B0)
    }

    val statusText = when {
        !isOn                -> "System Off"
        currentSound != null -> "${currentSound.name} Detected"
        else                 -> "Listening…"
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
                    .background(Brush.linearGradient(listOf(softBlueStart, softBlueEnd)))
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
                        if (isOn) {
                            context.startForegroundService(
                                Intent(context, AudioForegroundService::class.java)
                            )
                            viewModel.startDetectionListener(context)
                        } else {
                            context.stopService(
                                Intent(context, AudioForegroundService::class.java)
                            )
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
                                !isOn                -> Icons.Default.MicOff
                                currentSound != null -> Icons.Default.Warning
                                else                 -> Icons.Default.GraphicEq
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
                            text = if (isOn) {
                                if (currentSound != null) "$confidence% confidence" else "System active"
                            } else "Tap toggle to start",
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

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        icon = Icons.Default.ClosedCaption,
                        label = "Captions",
                        modifier = Modifier.weight(1f),
                        onClick = onCaptions
                    )
                    QuickActionCard(
                        icon = Icons.Default.History,
                        label = "History",
                        modifier = Modifier.weight(1f),
                        onClick = onHistory
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                    QuickActionCard(
                        icon = Icons.Default.VolumeUp,
                        label = "Voice Proxy",
                        modifier = Modifier.weight(1f),
                        onClick = onVoiceProxy
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Forum,
                        label = "Converse",
                        modifier = Modifier.weight(1f),
                        onClick = onConversation
                    )
                    Box(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(28.dp))

            // ── AI Models Section ────────────────────────────────
            ModelDownloadSection(modelService = modelService)

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Model Download Section ───────────────────────────────────────────────────

@Composable
private fun ModelDownloadSection(modelService: ModelService) {

    Text(
        text = "AI Models",
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF6B7A9A),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )

    // Sound detection — YAMNet is bundled in assets now
    ModelCard(
        title = "Smart Classification",
        subtitle = "YAMNet — bundled local model",
        fileSize = "Built-in",
        accentColor = Color(0xFF6FB1FC),
        isLoaded = true,
        isLoading = false,
        isDownloading = false,
        progress = 1f,
        readyHint = "YAMNet model active and ready",
        onDownload = { }
    )

    Spacer(Modifier.height(12.dp))

    // STT — required for sound detection + live captions + conversation listening
    ModelCard(
        title = "Live Captions",
        subtitle = "Whisper Tiny — speech recognition",
        fileSize = "~40MB",
        accentColor = Color(0xFF6FB1FC),
        isLoaded = modelService.isSTTLoaded,
        isLoading = modelService.isSTTLoading,
        isDownloading = modelService.isSTTDownloading,
        progress = modelService.sttDownloadProgress,
        readyHint = "Speech-to-text ready for captions",
        onDownload = { modelService.downloadAndLoadSTT() }
    )

    Spacer(Modifier.height(12.dp))

    // TTS — required for Voice Proxy + Conversation speaking
    ModelCard(
        title = "Voice Proxy & Conversation",
        subtitle = "Piper TTS — text to speech",
        fileSize = "~60MB",
        accentColor = Color(0xFF9C6FFC),
        isLoaded = modelService.isTTSLoaded,
        isLoading = modelService.isTTSLoading,
        isDownloading = modelService.isTTSDownloading,
        progress = modelService.ttsDownloadProgress,
        readyHint = "Voice Proxy and Conversation speaking ready",
        onDownload = { modelService.downloadAndLoadTTS() }
    )

    Spacer(Modifier.height(12.dp))

    // LLM — required for smart replies + summarization
    ModelCard(
        title = "AI Smart Replies",
        subtitle = "SmolLM2 360M — on-device AI",
        fileSize = "~400MB",
        accentColor = Color(0xFFFF9A3C),
        isLoaded = modelService.isLLMLoaded,
        isLoading = modelService.isLLMLoading,
        isDownloading = modelService.isLLMDownloading,
        progress = modelService.llmDownloadProgress,
        readyHint = "Smart replies and summarization active",
        onDownload = { modelService.downloadAndLoadLLM() }
    )

    Spacer(Modifier.height(16.dp))

    // Download All — only when something is missing and nothing is downloading
    val allLoaded = modelService.isSTTLoaded &&
            modelService.isTTSLoaded &&
            modelService.isLLMLoaded
    val anyDownloading = modelService.isSTTDownloading ||
            modelService.isTTSDownloading ||
            modelService.isLLMDownloading

    if (!allLoaded && !anyDownloading) {
        Button(
            onClick = { modelService.downloadAndLoadAllModels() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6FB1FC)
            )
        ) {
            Icon(
                Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Download All Models",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "~500MB total • Required for all features",
            fontSize = 11.sp,
            color = Color(0xFFB0B0B0),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ── Reusable Model Card ──────────────────────────────────────────────────────

@Composable
private fun ModelCard(
    title: String,
    subtitle: String,
    fileSize: String,
    accentColor: Color,
    isLoaded: Boolean,
    isLoading: Boolean,
    isDownloading: Boolean,
    progress: Float,
    readyHint: String,
    onDownload: () -> Unit
) {
    val green = Color(0xFF6BCB77)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Title row ────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (isLoaded) green.copy(alpha = 0.12f)
                            else accentColor.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isLoaded) Icons.Default.CheckCircle
                        else Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = if (isLoaded) green else accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A2340)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF6B7A9A)
                    )
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(
                            when {
                                isLoaded      -> green.copy(alpha = 0.12f)
                                isDownloading -> accentColor.copy(alpha = 0.12f)
                                isLoading     -> accentColor.copy(alpha = 0.12f)
                                else          -> Color(0xFFEEF0F5)
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            isLoaded      -> "Ready"
                            isDownloading -> "Downloading"
                            isLoading     -> "Loading"
                            else          -> "Not loaded"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = when {
                            isLoaded      -> green
                            isDownloading -> accentColor
                            isLoading     -> accentColor
                            else          -> Color(0xFF6B7A9A)
                        }
                    )
                }
            }

            // ── Download progress ─────────────────────────────────
            if (isDownloading) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Downloading…", fontSize = 12.sp, color = Color(0xFF6B7A9A))
                    Text(
                        "${(progress * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = accentColor
                    )
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = accentColor,
                    trackColor = Color(0xFFEEF0F5)
                )
            }

            // ── Loading spinner ───────────────────────────────────
            if (isLoading) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = accentColor,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Loading into memory…",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7A9A)
                    )
                }
            }

            // ── Download button ───────────────────────────────────
            if (!isLoaded && !isDownloading && !isLoading) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(fileSize, fontSize = 12.sp, color = Color(0xFFB0B0B0))
                    Button(
                        onClick = onDownload,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Download",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Ready hint ────────────────────────────────────────
            if (isLoaded) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(green.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(green)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(readyHint, fontSize = 11.sp, color = green)
                    }
                }
            }
        }
    }
}

// ── Quick Action Card ────────────────────────────────────────────────────────

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
        modifier = modifier
            .heightIn(min = 76.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFF6FB1FC),
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1A2340),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
