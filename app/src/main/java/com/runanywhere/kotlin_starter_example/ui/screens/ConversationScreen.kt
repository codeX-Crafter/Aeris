package com.runanywhere.kotlin_starter_example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.runanywhere.sdk.public.extensions.TTS.TTSOptions
import com.runanywhere.sdk.public.extensions.synthesize
import com.runanywhere.sdk.public.extensions.transcribe
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import com.runanywhere.kotlin_starter_example.services.playWavBytes

data class ConversationMessage(
    val text: String,
    val isFromOther: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ConversationScreen(
    modelService: ModelService,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf(listOf<ConversationMessage>()) }
    var myReply by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var listenJob by remember { mutableStateOf<Job?>(null) }
    var hasPermission by remember { mutableStateOf(false) }

    val softBlue = Color(0xFF6FB1FC)
    val purple = Color(0xFF9C6FFC)

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val modelsReady = modelService.isSTTLoaded && modelService.isTTSLoaded

    fun listenForOther() {
        isListening = true
        listenJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            try {
                val record = AudioRecord(
                    MediaRecorder.AudioSource.MIC, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 4
                )
                record.startRecording()
                val out = ByteArrayOutputStream()
                val buf = ByteArray(bufferSize)
                val target = sampleRate * 2 * 4 // 4 seconds

                while (out.size() < target && isActive) {
                    val read = record.read(buf, 0, buf.size)
                    if (read > 0) out.write(buf, 0, read)
                }

                record.stop(); record.release()

                val transcript = RunAnywhere.transcribe(out.toByteArray()).trim()
                if (transcript.isNotBlank()) {
                    withContext(Dispatchers.Main) {
                        messages = messages + ConversationMessage(
                            text = transcript,
                            isFromOther = true
                        )
                        isListening = false
                    }
                } else {
                    withContext(Dispatchers.Main) { isListening = false }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isListening = false }
            }
        }
    }

    fun speakMyReply() {
        if (myReply.isBlank()) return
        val text = myReply
        messages = messages + ConversationMessage(text = text, isFromOther = false)
        myReply = ""

        scope.launch {
            isSpeaking = true
            try {
                val output = withContext(Dispatchers.IO) {
                    RunAnywhere.synthesize(text, TTSOptions())
                }
                withContext(Dispatchers.IO) { playWavBytes(output.audioData) }
            } catch (e: Exception) {
                // handle silently
            } finally {
                isSpeaking = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { listenJob?.cancel() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(softBlue, Color(0xFFA7C6FF))))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Two-Way Conversation",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Listen → Read → Reply via voice",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Model warning
        if (!modelsReady) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFD166).copy(alpha = 0.15f)
                )
            ) {
                Text(
                    "STT + TTS models required. Download from Home screen.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 13.sp,
                    color = Color(0xFF1A2340)
                )
            }
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Forum,
                                contentDescription = null,
                                tint = Color(0xFFB0B0B0),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Tap 'Listen' to capture what someone says",
                                fontSize = 14.sp,
                                color = Color(0xFF6B7A9A)
                            )
                            Text(
                                "Then type and speak your reply",
                                fontSize = 13.sp,
                                color = Color(0xFFB0B0B0)
                            )
                        }
                    }
                }
            }
            items(messages) { msg ->
                ConversationBubble(message = msg)
            }
        }

        // Bottom input area
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Listen button
                Button(
                    onClick = {
                        if (!isListening) listenForOther()
                        else { listenJob?.cancel(); isListening = false }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) Color(0xFFFF6B6B) else softBlue
                    ),
                    enabled = modelsReady && hasPermission && !isSpeaking
                ) {
                    if (isListening) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Listening… tap to stop")
                    } else {
                        Icon(Icons.Default.Hearing, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Listen to other person", fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // My reply input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = myReply,
                        onValueChange = { myReply = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type your reply…") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,

                            focusedBorderColor = purple,
                            unfocusedBorderColor = Color(0xFFEEF0F5)
                        ),
                        maxLines = 3
                    )
                    FloatingActionButton(
                        onClick = { speakMyReply() },
                        modifier = Modifier.size(52.dp),
                        containerColor = purple,
                        contentColor = Color.White
                    ) {
                        if (isSpeaking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Speak")
                        }
                    }
                }

                if (!hasPermission) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Grant mic permission to listen", color = Color(0xFFFF6B6B))
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationBubble(message: ConversationMessage) {
    val isOther = message.isFromOther
    val softBlue = Color(0xFF6FB1FC)
    val purple = Color(0xFF9C6FFC)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOther) Arrangement.Start else Arrangement.End
    ) {
        if (isOther) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(softBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = softBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = if (isOther) 4.dp else 16.dp,
                topEnd = if (isOther) 16.dp else 4.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isOther) Color.White else purple
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isOther) "They said:" else "You said:",
                    fontSize = 11.sp,
                    color = if (isOther) Color(0xFF6B7A9A) else Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    color = if (isOther) Color(0xFF1A2340) else Color.White
                )
            }
        }

        if (!isOther) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(purple.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = purple,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}