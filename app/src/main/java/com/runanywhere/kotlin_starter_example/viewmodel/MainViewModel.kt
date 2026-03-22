package com.runanywhere.kotlin_starter_example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.kotlin_starter_example.data.SoundEvent
import com.runanywhere.kotlin_starter_example.data.SoundRepository
import com.runanywhere.kotlin_starter_example.data.SoundType
import com.runanywhere.kotlin_starter_example.services.HapticManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    val currentSound: StateFlow<SoundType?> = SoundRepository.currentSound
    val confidence: StateFlow<Int> = SoundRepository.confidence
    val history: StateFlow<List<SoundEvent>> = SoundRepository.history

    fun startDetectionListener(context: Context) {
        viewModelScope.launch {
            SoundRepository.currentSound.collectLatest { sound ->
                sound?.let { HapticManager.trigger(context, it) }
            }
        }
    }
}