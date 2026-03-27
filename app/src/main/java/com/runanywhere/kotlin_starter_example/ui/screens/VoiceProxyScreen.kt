package com.runanywhere.kotlin_starter_example.ui.screens

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.runanywhere.kotlin_starter_example.services.ModelService
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.TTS.TTSOptions
import com.runanywhere.sdk.public.extensions.synthesize
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.runanywhere.kotlin_starter_example.services.playWavBytes

@Composable
fun VoiceProxyScreen(
    modelService: ModelService,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isSpeaking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val recentPhrases = remember {
        mutableStateListOf(
            "I am deaf — please write or type",
            "Can you repeat that please?",
            "Speak slower please",
            "One moment, I am reading"
        )
    }

    val softBlue = Color(0xFF6FB1FC)
    val softBlueEnd = Color(0xFFA7C6FF)

    suspend fun speak(text: String) {
        if (text.isBlank()) return
        isSpeaking = true
        errorMessage = null
        try {
            val output = withContext(Dispatchers.IO) {
                RunAnywhere.synthesize(text, TTSOptions())
            }
            withContext(Dispatchers.IO) {
                playWavBytes(output.audioData)
            }
            // Add to recent if not already there
            if (!recentPhrases.contains(text)) {
                recentPhrases.add(0, text)
                if (recentPhrases.size > 6) recentPhrases.removeLastOrNull()
            }
        } catch (e: Exception) {
            errorMessage = "TTS failed: ${e.message}"
        } finally {
            isSpeaking = false
        }
    }

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
                .background(Brush.linearGradient(listOf(softBlue, softBlueEnd)))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Voice Proxy",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Type — your phone speaks for you",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Model not loaded
        if (!modelService.isTTSLoaded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
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
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = Color(0xFFFFD166)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "TTS model required",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A2340)
                        )
                        Text(
                            "Download Piper TTS from Home screen",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7A9A)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Text input card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "What do you want to say?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF6B7A9A)
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type your message here…") },
                    minLines = 4,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,     // 🔥 FIX
                        unfocusedTextColor = Color.Black,   // 🔥 FIX

                        focusedBorderColor = softBlue,
                        unfocusedBorderColor = Color(0xFFEEF0F5)
                    )
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { scope.launch { speak(inputText) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = softBlue
                    ),
                    enabled = modelService.isTTSLoaded && !isSpeaking && inputText.isNotBlank()
                ) {
                    if (isSpeaking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Speaking…", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Speak Aloud",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Quick phrases
        Text(
            "Quick Phrases",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7A9A),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))

        recentPhrases.forEach { phrase ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clickable {
                        if (modelService.isTTSLoaded && !isSpeaking) {
                            scope.launch { speak(phrase) }
                        }
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = softBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        phrase,
                        fontSize = 14.sp,
                        color = Color(0xFF1A2340),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFFB0B0B0),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        errorMessage?.let {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFF6B6B).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    it,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 13.sp,
                    color = Color(0xFFFF6B6B)
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

