package com.runanywhere.kotlin_starter_example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.runanywhere.kotlin_starter_example.data.SoundEvent
import com.runanywhere.kotlin_starter_example.data.SoundType
import com.runanywhere.kotlin_starter_example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val history by viewModel.history.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }

    val filters = listOf("All", "Siren", "Horn", "Alarm", "Doorbell", "Voice")

    val filtered = when (selectedFilter) {
        "All"      -> history
        "Siren"    -> history.filter { it.type == SoundType.SIREN }
        "Horn"     -> history.filter { it.type == SoundType.HORN }
        "Alarm"    -> history.filter { it.type == SoundType.ALARM }
        "Doorbell" -> history.filter { it.type == SoundType.DOORBELL }
        "Voice"    -> history.filter { it.type == SoundType.VOICE }
        else       -> history
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
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF6FB1FC), Color(0xFFA7C6FF))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Detection History",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "${history.size} events recorded",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Filter chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF6FB1FC),
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        if (filtered.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFFB0B0B0),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No detections yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A2340)
                    )
                    Text(
                        "Turn on detection to start monitoring",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7A9A)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { event ->
                    HistoryEventCard(event = event)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryEventCard(event: SoundEvent) {
    val (color, icon) = when (event.type) {
        SoundType.SIREN    -> Color(0xFFFF6B6B) to Icons.Default.LocalFireDepartment
        SoundType.HORN     -> Color(0xFFFFD166) to Icons.Default.DirectionsCar
        SoundType.ALARM    -> Color(0xFFFF9A3C) to Icons.Default.Alarm
        SoundType.DOORBELL -> Color(0xFF6FB1FC) to Icons.Default.Doorbell
        SoundType.VOICE    -> Color(0xFF6BCB77) to Icons.Default.RecordVoiceOver
        else               -> Color(0xFFB0B0B0) to Icons.Default.VolumeUp
    }

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = timeFormat.format(Date(event.timestamp))
    val confidence = (event.confidence * 100).toInt()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = event.type.name
                            .lowercase()
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A2340)
                    )
                    Text(
                        text = timeString,
                        fontSize = 12.sp,
                        color = Color(0xFF6B7A9A)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Confidence bar
                LinearProgressIndicator(
                    progress = { event.confidence },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = color,
                    trackColor = Color(0xFFEEF0F5)
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Transcript if available
                    event.transcript?.let { transcript ->
                        Text(
                            text = "\"$transcript\"",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7A9A),
                            modifier = Modifier.weight(1f)
                        )
                    } ?: Text(
                        text = "Sound detected",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7A9A)
                    )

                    Text(
                        text = "$confidence%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = color
                    )
                }
            }
        }
    }
}