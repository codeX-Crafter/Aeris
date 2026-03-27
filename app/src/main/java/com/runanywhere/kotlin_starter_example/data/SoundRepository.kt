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

    private val _audioRMS = MutableStateFlow(0f)
    val audioRMS: StateFlow<Float> = _audioRMS

    // Holds the log-mel spectrogram data (typically 96x64 for YAMNet)
    private val _spectrogram = MutableStateFlow<Array<FloatArray>?>(null)
    val spectrogram: StateFlow<Array<FloatArray>?> = _spectrogram

    fun updateRMS(rms: Float) {
        _audioRMS.value = rms
    }

    fun updateSpectrogram(data: Array<FloatArray>) {
        _spectrogram.value = data
    }

    fun updateDetection(predictions: Map<SoundType, Float>) {
        if (predictions.isEmpty()) {
            _currentSound.value = null
            _confidence.value = 0
            return
        }
        val top = predictions.maxByOrNull { it.value }!!
        _currentSound.value = top.key
        _confidence.value = (top.value * 100).toInt()

        val event = SoundEvent(
            type = top.key,
            confidence = top.value,
            timestamp = System.currentTimeMillis()
        )
        _history.value = listOf(event) + _history.value.take(49)
    }

    // ... keeping existing methods for compatibility
}
