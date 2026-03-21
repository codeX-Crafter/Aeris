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
import kotlin.math.sin

@Composable
fun WaveformView(
    modifier: Modifier = Modifier,
    barCount: Int = 24,
    color: Color = Color(0xFF6FB1FC)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val barWidth = size.width / barCount

        for (i in 0 until barCount) {
            val normalized = i.toFloat() / barCount

            // ✅ .toFloat() keeps math within Float range, avoids Double promotion
            val wave = (sin((normalized * 2 * Math.PI + phase).toFloat()) + 1f) / 2f

            val barHeight = size.height * (0.2f + wave * 0.8f)

            drawRect(
                color = color.copy(alpha = 0.6f + wave * 0.4f),
                topLeft = Offset(
                    x = i * barWidth,   // Float * Float → Float ✅
                    y = size.height - barHeight
                ),
                size = Size(
                    width = barWidth * 0.6f,
                    height = barHeight
                )
            )
        }
    }
}