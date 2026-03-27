package com.runanywhere.kotlin_starter_example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.runanywhere.kotlin_starter_example.data.SoundRepository
import kotlin.math.max

@Composable
fun WaveformView(
    modifier: Modifier = Modifier,
    barCount: Int = 32,
    color: Color = Color(0xFF6FB1FC)
) {
    val rms by SoundRepository.audioRMS.collectAsState()
    
    // Create a list of heights that we'll animate/shift
    val barHeights = remember { mutableStateListOf<Float>().apply { 
        repeat(barCount) { add(0.1f) } 
    } }

    // Update heights based on real-time RMS
    LaunchedEffect(rms) {
        barHeights.removeAt(0)
        // Scale RMS (usually 0.0 to 0.1) to a visible bar height (0.1 to 1.0)
        val newHeight = (rms * 10f).coerceIn(0.05f, 1f)
        barHeights.add(newHeight)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val barWidth = canvasWidth / barCount
        val gap = barWidth * 0.2f

        barHeights.forEachIndexed { index, heightMultiplier ->
            val x = index * barWidth
            val h = canvasHeight * heightMultiplier
            
            drawRect(
                color = color.copy(alpha = 0.4f + (heightMultiplier * 0.6f)),
                topLeft = Offset(x + gap, (canvasHeight - h) / 2),
                size = Size(barWidth - (gap * 2), h)
            )
        }
    }
}
