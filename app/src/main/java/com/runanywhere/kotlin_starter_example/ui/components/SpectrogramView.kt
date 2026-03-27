package com.runanywhere.kotlin_starter_example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.runanywhere.kotlin_starter_example.data.SoundRepository

@Composable
fun SpectrogramView(
    modifier: Modifier = Modifier
) {
    val spectrogram by SoundRepository.spectrogram.collectAsState()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val data = spectrogram ?: return@Canvas
        
        // YAMNet spectrogram is typically 96 frames x 64 frequency bins
        val numFrames = data.size
        val numBins = data[0].size
        
        val cellWidth = size.width / numFrames
        val cellHeight = size.height / numBins

        // Define a color map (Viridis-like: Dark Blue -> Green -> Yellow)
        val colorLow = Color(0xFF1A2340)
        val colorMid = Color(0xFF6BCB77)
        val colorHigh = Color(0xFFFFD166)

        for (f in 0 until numFrames) {
            for (b in 0 until numBins) {
                // Log-mel values are typically negative, we normalize roughly for visualization
                // Higher values (closer to 0 or positive) = louder/more activity
                val rawVal = data[f][b]
                val normalized = ((rawVal + 10f) / 10f).coerceIn(0f, 1f)
                
                val color = if (normalized < 0.5f) {
                    lerp(colorLow, colorMid, normalized * 2)
                } else {
                    lerp(colorMid, colorHigh, (normalized - 0.5f) * 2)
                }

                drawRect(
                    color = color,
                    // origin is bottom-left for frequency
                    topLeft = Offset(f * cellWidth, size.height - (b + 1) * cellHeight),
                    size = Size(cellWidth + 1f, cellHeight + 1f) // +1 to avoid gaps
                )
            }
        }
    }
}
