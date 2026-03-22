package com.runanywhere.kotlin_starter_example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SoundRepository {

    private val _currentSound = MutableStateFlow<SoundType?>(null)
    val currentSound: StateFlow<SoundType?> = _currentSound

    private val _confidence = MutableStateFlow(0)
    val confidence: StateFlow<Int> = _confidence

    private val _history = MutableStateFlow<List<SoundEvent>>(emptyList())
    val history: StateFlow<List<SoundEvent>> = _history

    fun updateDetection(predictions: Map<SoundType, Float>) {
        if (predictions.isEmpty()) {
            _currentSound.value = null
            _confidence.value = 0
            return
        }
        val top = predictions.maxByOrNull { it.value }!!
        _currentSound.value = top.key
        _confidence.value = (top.value * 100).toInt()

        // Add to history
        val event = SoundEvent(
            type = top.key,
            confidence = top.value,
            timestamp = System.currentTimeMillis()
        )
        _history.value = listOf(event) + _history.value.take(49)
    }

    fun updateDetectionWithTranscript(
        predictions: Map<SoundType, Float>,
        transcript: String
    ) {
        if (predictions.isEmpty()) return
        val top = predictions.maxByOrNull { it.value }!!
        _currentSound.value = top.key
        _confidence.value = (top.value * 100).toInt()

        val event = SoundEvent(
            type = top.key,
            confidence = top.value,
            timestamp = System.currentTimeMillis(),
            transcript = transcript.ifBlank { null }
        )
        _history.value = listOf(event) + _history.value.take(49)
    }

    fun update(sound: SoundType?, confidenceScore: Float) {
        _currentSound.value = sound
        _confidence.value = (confidenceScore * 100).toInt()
    }
}