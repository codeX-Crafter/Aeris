package com.runanywhere.kotlin_starter_example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import com.runanywhere.kotlin_starter_example.services.ModelService
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.transcribe
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class CaptionLine(
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 0.9f
)

@Composable
fun LiveCaptionScreen(
    modelService: ModelService,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var isLive by remember { mutableStateOf(false) }
    var captions by remember { mutableStateOf(listOf<CaptionLine>()) }
    var hasPermission by remember { mutableStateOf(false) }
    var captureJob by remember { mutableStateOf<Job?>(null) }

    // ✅ Defined as a val outside any composable scope
    val quickReplies = listOf(
        "Repeat please",
        "Speak slower",
        "I am deaf",
        "Can you write?",
        "One moment"
    )

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    LaunchedEffect(captions.size) {
        if (captions.isNotEmpty()) {
            listState.animateScrollToItem(captions.size - 1)
        }
    }

    fun startCaption() {
        isLive = true
        captureJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            try {
                val audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize * 4
                )
                audioRecord.startRecording()

                while (isActive && isLive) {
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(bufferSize)
                    val targetBytes = sampleRate * 2 * 3

                    while (out.size() < targetBytes && isActive && isLive) {
                        val read = audioRecord.read(buffer, 0, buffer.size)
                        if (read > 0) out.write(buffer, 0, read)
                    }

                    if (!isActive || !isLive) break

                    val audioBytes = out.toByteArray()
                    if (audioBytes.isNotEmpty()) {
                        try {
                            val transcript = RunAnywhere.transcribe(audioBytes).trim()
                            if (transcript.isNotBlank()) {
                                withContext(Dispatchers.Main) {
                                    captions = captions + CaptionLine(text = transcript)
                                }
                            }
                        } catch (e: Exception) {
                            // Continue on error
                        }
                    }
                }

                audioRecord.stop()
                audioRecord.release()

            } catch (e: SecurityException) {
                withContext(Dispatchers.Main) { isLive = false }
            }
        }
    }

    fun stopCaption() {
        isLive = false
        captureJob?.cancel()
        captureJob = null
    }

    DisposableEffect(Unit) {
        onDispose { captureJob?.cancel() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Live Captions",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (isLive) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color.Red.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "LIVE",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Model warning ─────────────────────────────────────────
        if (!modelService.isSTTLoaded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFD166).copy(alpha = 0.15f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFD166),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "STT model not loaded",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A2340)
                        )
                        Text(
                            "Download it from the Home screen first",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7A9A)
                        )
                    }
                }
            }
        }

        // ── Permission warning ────────────────────────────────────
        if (!hasPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF6B6B).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color(0xFFFF6B6B)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Mic permission needed",
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        color = Color(0xFF1A2340)
                    )
                    TextButton(onClick = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }) {
                        Text("Grant", color = Color(0xFF6FB1FC))
                    }
                }
            }
        }

        // ── Captions area ─────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            if (captions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ClosedCaption,
                            contentDescription = null,
                            tint = Color(0xFFB0B0B0),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Captions will appear here",
                            fontSize = 16.sp,
                            color = Color(0xFF6B7A9A)
                        )
                        Text(
                            "Tap the mic button to start",
                            fontSize = 13.sp,
                            color = Color(0xFFB0B0B0)
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ✅ explicit type annotation fixes "cannot infer type" error
                    items(items = captions, key = { it.timestamp }) { caption ->
                        CaptionLineCard(caption = caption)
                    }
                }
            }
        }

        // ── Quick replies ─────────────────────────────────────────
        // ✅ LazyRow with explicit String type + itemContent lambda
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items = quickReplies) { reply: String ->
                OutlinedButton(
                    onClick = { },
                    shape = RoundedCornerShape(50.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    border = BorderStroke(1.dp, Color(0xFF6FB1FC))
                ) {
                    Text(reply, fontSize = 12.sp, color = Color(0xFF6FB1FC))
                }
            }
        }

        // ── Start/Stop button ─────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = { if (isLive) stopCaption() else startCaption() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLive) Color(0xFFFF6B6B) else Color(0xFF6FB1FC)
                ),
                enabled = modelService.isSTTLoaded && hasPermission
            ) {
                Icon(
                    imageVector = if (isLive) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isLive) "Stop Captions" else "Start Live Captions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CaptionLineCard(caption: CaptionLine) {
    val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
    val confidenceColor = when {
        caption.confidence >= 0.8f -> Color(0xFF6BCB77)
        caption.confidence >= 0.5f -> Color(0xFFFFD166)
        else                       -> Color(0xFFFF6B6B)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeFormat.format(Date(caption.timestamp)),
                    fontSize = 11.sp,
                    color = Color(0xFF6B7A9A)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(confidenceColor)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = caption.text,
                fontSize = 16.sp,
                color = Color(0xFF1A2340),
                lineHeight = 24.sp
            )
        }
    }
}