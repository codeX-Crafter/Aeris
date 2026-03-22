package com.runanywhere.kotlin_starter_example.data

data class SoundEvent(
    val type: SoundType,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val transcript: String? = null
)