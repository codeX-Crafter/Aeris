package com.runanywhere.kotlin_starter_example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.kotlin_starter_example.data.CaptionLine
import com.runanywhere.kotlin_starter_example.data.HistoryContentLine
import com.runanywhere.kotlin_starter_example.data.HistoryType
import com.runanywhere.kotlin_starter_example.services.ModelService
import com.runanywhere.kotlin_starter_example.viewmodel.ExportState
import com.runanywhere.kotlin_starter_example.viewmodel.HistoryViewModel
import com.runanywhere.kotlin_starter_example.viewmodel.MainViewModel
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.transcribe
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LiveCaptionScreen(
    modelService: ModelService,
    historyViewModel: HistoryViewModel,
    onBack: () -> Unit,
    mainViewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var isLive by remember { mutableStateOf(false) }
    var captions by remember { mutableStateOf(listOf<CaptionLine>()) }
    var hasPermission by remember { mutableStateOf(false) }
    var captureJob by remember { mutableStateOf<Job?>(null) }

    // ── PDF Export Observation ────────────────────────────────
    val exportState by mainViewModel.exportState.collectAsState(ExportState.Idle)
    
    LaunchedEffect(exportState) {
        when (val state = exportState) {
            is ExportState.Success -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, state.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Transcript"))
            }
            is ExportState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        historyViewModel.loadHistory(HistoryType.CAPTION)
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
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            try {
                val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize * 4)
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
                    val transcript = RunAnywhere.transcribe(out.toByteArray()).trim()
                    if (transcript.isNotBlank()) {
                        withContext(Dispatchers.Main) { captions = captions + CaptionLine(text = transcript) }
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

    fun saveToHistoryAndExit() {
        if (captions.isNotEmpty()) {
            val historyContent = captions.map { HistoryContentLine(it.text, fromOther = true, it.timestamp) }
            historyViewModel.saveSession(HistoryType.CAPTION, historyContent)
        }
        onBack()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.85f),
                drawerContainerColor = Color.White
            ) {
                HistoryDrawerContent(
                    historyViewModel = historyViewModel,
                    type = HistoryType.CAPTION,
                    onItemSelected = { item ->
                        captions = item.content.map { CaptionLine(text = it.text, timestamp = it.timestamp) }
                        scope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(Color(0xFF6FB1FC), Color(0xFFA7C6FF))))
                        .padding(top = 12.dp, bottom = 12.dp, start = 8.dp, end = 16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "History", tint = Color.White)
                        }
                        IconButton(onClick = { saveToHistoryAndExit() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Text("Live Captions", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                        
                        if (captions.isNotEmpty()) {
                            IconButton(onClick = {
                                val historyContent = captions.map { HistoryContentLine(it.text, fromOther = true, it.timestamp) }
                                historyViewModel.saveSession(HistoryType.CAPTION, historyContent)
                                captions = emptyList()
                            }) {
                                Icon(Icons.Default.Add, "New Session", tint = Color.White)
                            }
                        }

                        if (isLive) {
                            LiveBadge()
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFFF8F9FA))
            ) {
                // Captions list
                Box(modifier = Modifier.weight(1f)) {
                    if (captions.isEmpty()) {
                        EmptyCaptionsState()
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items = captions, key = { it.timestamp }) { caption ->
                                CaptionLineCard(caption = caption)
                            }
                        }
                    }
                }

                // Control bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { if (isLive) stopCaption() else startCaption() },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLive) Color(0xFFFF6B6B) else Color(0xFF6FB1FC)
                            ),
                            enabled = modelService.isSTTLoaded && hasPermission
                        ) {
                            Icon(if (isLive) Icons.Default.Stop else Icons.Default.Mic, null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isLive) "Stop Captions" else "Start Live Captions")
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        
                        IconButton(
                            onClick = { mainViewModel.exportCaptions(context, captions) },
                            enabled = captions.isNotEmpty(),
                            modifier = Modifier.size(56.dp).background(Color(0xFFF0F2F8), CircleShape)
                        ) {
                            Icon(Icons.Default.Share, "Export", tint = Color(0xFF6FB1FC))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(50.dp)).background(Color.Red.copy(alpha = 0.2f)).padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Red))
        Spacer(Modifier.width(4.dp))
        Text("LIVE", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyCaptionsState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ClosedCaption, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
            Text("Captions will appear here", color = Color.Gray)
        }
    }
}

@Composable
private fun CaptionLineCard(caption: CaptionLine) {
    val timeFormat = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(timeFormat.format(Date(caption.timestamp)), fontSize = 11.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(caption.text, fontSize = 16.sp, color = Color.Black, lineHeight = 24.sp)
        }
    }
}
