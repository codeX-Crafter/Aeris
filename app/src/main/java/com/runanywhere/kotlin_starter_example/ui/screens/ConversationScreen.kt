package com.runanywhere.kotlin_starter_example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import com.runanywhere.kotlin_starter_example.data.HistoryContentLine
import com.runanywhere.kotlin_starter_example.data.HistoryType
import com.runanywhere.kotlin_starter_example.data.SyncedHistoryItem
import com.runanywhere.kotlin_starter_example.services.ModelService
import com.runanywhere.kotlin_starter_example.services.playWavBytes
import com.runanywhere.kotlin_starter_example.viewmodel.HistoryViewModel
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.TTS.TTSOptions
import com.runanywhere.sdk.public.extensions.chat
import com.runanywhere.sdk.public.extensions.synthesize
import com.runanywhere.sdk.public.extensions.transcribe
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.navigationBarsPadding

// ── Data models ──────────────────────────────────────────────────────────────

data class ConversationMessage(
    val text: String,
    val isFromOther: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ConversationState {
    IDLE,
    LISTENING,
    TRANSCRIBING,
    GENERATING_SUGGESTIONS,
    SPEAKING
}

// ── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun ConversationScreen(
    modelService: ModelService,
    historyViewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var messages by remember { mutableStateOf(listOf<ConversationMessage>()) }
    var myReply by remember { mutableStateOf("") }
    var conversationState by remember { mutableStateOf(ConversationState.IDLE) }
    var listenJob by remember { mutableStateOf<Job?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf(listOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val softBlue = Color(0xFF6FB1FC)
    val purple   = Color(0xFF9C6FFC)
    val green    = Color(0xFF6BCB77)
    val red      = Color(0xFFFF6B6B)

    // ── Permission ────────────────────────────────────────────
    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        historyViewModel.loadHistory(HistoryType.CONVERSATION)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasPermission = it }

    // ── Auto scroll ───────────────────────────────────────────
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // ── Model readiness ───────────────────────────────────────
    val sttReady = modelService.isSTTLoaded
    val ttsReady = modelService.isTTSLoaded
    val llmReady = modelService.isLLMLoaded

    // ── Generate suggestions via LLM ─────────────────────────
    fun generateSuggestions(lastTranscript: String) {
        if (!llmReady) return

        scope.launch {
            conversationState = ConversationState.GENERATING_SUGGESTIONS

            try {
                // ✅ Build context from last 4 messages for token efficiency
                val context = buildString {
                    appendLine("You are helping a deaf person reply in a conversation.")
                    appendLine("Recent conversation:")
                    messages.takeLast(4).forEach { msg ->
                        appendLine(
                            if (msg.isFromOther) "Other person: ${msg.text}"
                            else "Me: ${msg.text}"
                        )
                    }
                    appendLine("\nSuggest exactly 3 short natural replies (max 8 words each).")
                    appendLine("Format: one reply per line, no numbering, no punctuation,NEVER include labels like 'Me:', 'Other:', 'Person:', 'Suggestion:','Myself:', '1.', 'A.', or 'Reply:'., NEVER include quotes around the text.")
                }

                // ✅ Use chat() not generate() — better for instruction following
                val response = withContext(Dispatchers.IO) {
                    RunAnywhere.chat(context)
                }

                // ✅ Clean parsing — split by newline, filter blanks
                suggestions = response
                    .trim()
                    .split("\n")
                    .map { line ->
                        line
                            .trim()
                            .removePrefix("-")
                            .removePrefix("•")
                            .removePrefix("*")
                            .trim()
                            .trimEnd('.', ',', ';')
                    }
                    .filter { it.isNotBlank() && it.length > 2 }
                    .take(3)

            } catch (e: Exception) {
                suggestions = listOf(
                    "I understand",
                    "Can you repeat that?",
                    "One moment please"
                )
            } finally {
                if (conversationState == ConversationState.GENERATING_SUGGESTIONS) {
                    conversationState = ConversationState.IDLE
                }
            }
        }
    }

    // ── Listen for other person ───────────────────────────────
    fun listenForOther() {
        conversationState = ConversationState.LISTENING
        errorMessage = null
        suggestions = emptyList()

        listenJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            if (bufferSize <= 0) {
                withContext(Dispatchers.Main) {
                    errorMessage = "AudioRecord not available"
                    conversationState = ConversationState.IDLE
                }
                return@launch
            }

            try {
                val record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize * 4
                )

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Microphone unavailable"
                        conversationState = ConversationState.IDLE
                    }
                    return@launch
                }

                record.startRecording()

                val out = ByteArrayOutputStream()
                val buf = ByteArray(bufferSize)
                // ✅ 5 seconds — more natural conversation length
                val targetBytes = sampleRate * 2 * 5

                while (out.size() < targetBytes && isActive &&
                    conversationState == ConversationState.LISTENING
                ) {
                    val read = record.read(buf, 0, buf.size)
                    if (read > 0) out.write(buf, 0, read)
                }

                record.stop()
                record.release()

                if (!isActive) return@launch

                // ✅ Switch to transcribing state
                withContext(Dispatchers.Main) {
                    conversationState = ConversationState.TRANSCRIBING
                }

                val audioBytes = out.toByteArray()

                // ✅ Skip if too short — likely no speech
                if (audioBytes.size < sampleRate * 2) {
                    withContext(Dispatchers.Main) {
                        errorMessage = "Recording too short — try again"
                        conversationState = ConversationState.IDLE
                    }
                    return@launch
                }

                val transcript = RunAnywhere.transcribe(audioBytes).trim()

                withContext(Dispatchers.Main) {
                    if (transcript.isNotBlank()) {
                        messages = messages + ConversationMessage(
                            text = transcript,
                            isFromOther = true
                        )
                        conversationState = ConversationState.IDLE
                        // ✅ Auto-generate suggestions after transcription
                        generateSuggestions(transcript)
                    } else {
                        errorMessage = "No speech detected — try again"
                        conversationState = ConversationState.IDLE
                    }
                }

            } catch (e: SecurityException) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Microphone permission denied"
                    conversationState = ConversationState.IDLE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error: ${e.message}"
                    conversationState = ConversationState.IDLE
                }
            }
        }
    }

    // ── Speak reply via TTS ───────────────────────────────────
    fun speakMyReply(text: String = myReply) {
        if (text.isBlank() || !ttsReady) return

        val replyText = text.trim()
        messages = messages + ConversationMessage(
            text = replyText,
            isFromOther = false
        )
        if (text == myReply) myReply = ""
        suggestions = emptyList()

        scope.launch {
            conversationState = ConversationState.SPEAKING
            try {
                val output = withContext(Dispatchers.IO) {
                    RunAnywhere.synthesize(replyText, TTSOptions())
                }
                withContext(Dispatchers.IO) {
                    playWavBytes(output.audioData)
                }
            } catch (e: Exception) {
                errorMessage = "TTS failed: ${e.message}"
            } finally {
                conversationState = ConversationState.IDLE
            }
        }
    }

    // ── Save History Logic ────────────────────────────────────
    fun saveToHistoryAndExit() {
        if (messages.isNotEmpty()) {
            val historyContent = messages.map { 
                HistoryContentLine(it.text, fromOther = it.isFromOther, it.timestamp) 
            }
            historyViewModel.saveSession(HistoryType.CONVERSATION, historyContent)
        }
        onBack()
    }

    // ── Sidebar History Drawer ────────────────────────────────
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.85f),
                drawerContainerColor = Color.White
            ) {
                HistoryDrawerContent(
                    historyViewModel = historyViewModel,
                    type = HistoryType.CONVERSATION,
                    onItemSelected = { item ->
                        messages = item.content.map { 
                            ConversationMessage(it.text, it.fromOther, it.timestamp) 
                        }
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        // ── Main UI ───────────────────────────────────────────
        Scaffold(
            containerColor = Color(0xFFF8F9FA),
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(softBlue, Color(0xFFA7C6FF))))
                        .padding(top = 12.dp, bottom = 12.dp, start = 8.dp, end = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "History", tint = Color.White)
                        }
                        IconButton(onClick = { saveToHistoryAndExit() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Conversation",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (conversationState != ConversationState.IDLE) {
                                ConversationStateBadge(state = conversationState)
                            }
                        }
                        if (messages.isNotEmpty()) {
                            IconButton(onClick = { 
                                val historyContent = messages.map { HistoryContentLine(it.text, fromOther = it.isFromOther, it.timestamp) }
                                historyViewModel.saveSession(HistoryType.CONVERSATION, historyContent)
                                messages = emptyList() 
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color.White)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF8F9FA))
            ) {
                // ── Model status bar ──────────────────────────
                if (!sttReady || !ttsReady) {
                    ModelStatusBar(
                        sttReady = sttReady,
                        ttsReady = ttsReady,
                        llmReady = llmReady
                    )
                }

                // ── Error message ─────────────────────────────
                errorMessage?.let { error ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(red.copy(alpha = 0.08f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = red,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(error, fontSize = 12.sp, color = red, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { errorMessage = null },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = red,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // ── Messages ──────────────────────────────────
                Box(modifier = Modifier.weight(1f)) {
                    if (messages.isEmpty()) {
                        ConversationEmptyState()
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = messages,
                                key = { it.timestamp }
                            ) { msg ->
                                ConversationBubble(message = msg)
                            }
                        }
                    }

                    // ── Generating indicator ───────────────────
                    if (conversationState == ConversationState.GENERATING_SUGGESTIONS) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .clip(RoundedCornerShape(50.dp))
                                .background(purple.copy(alpha = 0.1f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    color = purple,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Generating replies…",
                                    fontSize = 12.sp,
                                    color = purple
                                )
                            }
                        }
                    }
                }

                // ── Bottom panel ──────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        // ── Suggestions ───────────────────────
                        if (suggestions.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(items = suggestions) { suggestion: String ->
                                    SuggestionChip(
                                        text = suggestion,
                                        onTap = { myReply = suggestion },
                                        onSpeak = { speakMyReply(suggestion) },
                                        purple = purple
                                    )
                                }
                            }
                        }

                        // ── Listen button ─────────────────────
                        Button(
                            onClick = {
                                if (conversationState == ConversationState.LISTENING) {
                                    listenJob?.cancel()
                                    conversationState = ConversationState.IDLE
                                } else if (hasPermission) {
                                    listenForOther()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (conversationState == ConversationState.LISTENING) red else softBlue
                            )
                        ) {
                            Icon(
                                if (conversationState == ConversationState.LISTENING) Icons.Default.Stop else Icons.Default.Hearing,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (conversationState == ConversationState.LISTENING) "Stop Listening" else "Listen",
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── Reply input + speak ───────────────
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = myReply,
                                onValueChange = { myReply = it },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        "Type your reply…",
                                        color = Color(0xFFB0B0B0)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF1A2340),
                                    unfocusedTextColor = Color(0xFF1A2340),
                                    focusedBorderColor = purple,
                                    unfocusedBorderColor = Color(0xFFEEF0F5)
                                )
                            )

                            FloatingActionButton(
                                onClick = { speakMyReply() },
                                modifier = Modifier.size(52.dp),
                                containerColor = if (myReply.isBlank()) Color(0xFFEEF0F5) else purple,
                                contentColor = if (myReply.isBlank()) Color(0xFFB0B0B0) else Color.White,
                                elevation = FloatingActionButtonDefaults.elevation(0.dp)
                            ) {
                                if (conversationState == ConversationState.SPEAKING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Speak")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationStateBadge(state: ConversationState) {
    val (label, color) = when (state) {
        ConversationState.IDLE                  -> return
        ConversationState.LISTENING             -> "Listening" to Color(0xFFFF6B6B)
        ConversationState.TRANSCRIBING          -> "Transcribing" to Color(0xFFFFD166)
        ConversationState.GENERATING_SUGGESTIONS -> "Thinking" to Color(0xFF9C6FFC)
        ConversationState.SPEAKING              -> "Speaking" to Color(0xFF6BCB77)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            label,
            fontSize = 11.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ModelStatusBar(
    sttReady: Boolean,
    ttsReady: Boolean,
    llmReady: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF8E1))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFFFD166),
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(
                "Some models not loaded:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A2340)
            )
            Text(
                buildString {
                    if (!sttReady) append("STT (listening) ")
                    if (!ttsReady) append("TTS (speaking) ")
                    if (!llmReady) append("LLM (suggestions) ")
                    append("— download from Home")
                },
                fontSize = 11.sp,
                color = Color(0xFF6B7A9A)
            )
        }
    }
}

@Composable
private fun ConversationEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6FB1FC).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Forum,
                    contentDescription = null,
                    tint = Color(0xFF6FB1FC),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Start a conversation",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A2340)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap Listen and let the other person speak.\nTheir words will appear here.\nType or tap a suggestion to reply.",
                fontSize = 13.sp,
                color = Color(0xFF6B7A9A),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StepHint(number = "1", label = "Listen")
                StepHint(number = "2", label = "Read")
                StepHint(number = "3", label = "Reply")
            }
        }
    }
}

@Composable
private fun StepHint(number: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF6FB1FC).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6FB1FC)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = Color(0xFF6B7A9A))
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onTap: () -> Unit,
    onSpeak: () -> Unit,
    purple: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(purple.copy(alpha = 0.08f))
            .border(
                width = 1.dp,
                color = purple.copy(alpha = 0.25f),
                shape = RoundedCornerShape(50.dp)
            )
    ) {
        // Tap to fill text field
        TextButton(
            onClick = onTap,
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 6.dp,
                top = 6.dp,
                bottom = 6.dp
            )
        ) {
            Text(
                text,
                fontSize = 12.sp,
                color = Color(0xFF1A2340),
                maxLines = 1
            )
        }
        // Tap speaker icon to speak directly
        IconButton(
            onClick = onSpeak,
            modifier = Modifier
                .size(32.dp)
                .padding(end = 6.dp)
        ) {
            Icon(
                Icons.Default.VolumeUp,
                contentDescription = "Speak",
                tint = purple,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun PulsingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            tween(500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "dotScale"
    )
    Box(
        modifier = Modifier
            .size(10.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun ConversationBubble(message: ConversationMessage) {
    val isOther = message.isFromOther
    val softBlue = Color(0xFF6FB1FC)
    val purple   = Color(0xFF9C6FFC)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOther) Arrangement.Start else Arrangement.End
    ) {
        if (isOther) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(softBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = softBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isOther) Alignment.Start else Alignment.End,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Card(
                shape = RoundedCornerShape(
                    topStart = if (isOther) 4.dp else 18.dp,
                    topEnd = if (isOther) 18.dp else 4.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOther) Color.White else purple
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    color = if (isOther) Color(0xFF1A2340) else Color.White,
                    modifier = Modifier.padding(
                        horizontal = 14.dp,
                        vertical = 10.dp
                    ),
                    lineHeight = 22.sp
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = timeFormat.format(Date(message.timestamp)),
                fontSize = 10.sp,
                color = Color(0xFFB0B0B0),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        if (!isOther) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(purple.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = purple,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
