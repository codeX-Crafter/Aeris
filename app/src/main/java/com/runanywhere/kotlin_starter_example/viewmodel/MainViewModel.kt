package com.runanywhere.kotlin_starter_example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runanywhere.kotlin_starter_example.data.SoundRepository
import com.runanywhere.kotlin_starter_example.services.HapticManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    val currentSound = SoundRepository.currentSound

    fun observeSounds(context: Context) {
        viewModelScope.launch {
            currentSound.collectLatest { sound ->
                sound?.let {
                    HapticManager.trigger(context, it)
                }
            }
        }
    }
}