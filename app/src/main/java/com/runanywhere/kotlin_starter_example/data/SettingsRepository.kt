package com.runanywhere.kotlin_starter_example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object SettingsRepository {
    private const val PREFS_NAME = "aeris_settings"
    private const val KEY_ADAPTIVE = "adaptive_mode"
    private const val PREFIX_SENSITIVITY = "sensitivity_"
    private const val KEY_NOTIFICATION_ENABLED = "notifications_enabled"

    private lateinit var prefs: SharedPreferences

    private val _sensitivities = MutableStateFlow<Map<SoundType, Float>>(emptyMap())
    val sensitivities: StateFlow<Map<SoundType, Float>> = _sensitivities

    private val _adaptiveMode = MutableStateFlow(false)
    val adaptiveMode: StateFlow<Boolean> = _adaptiveMode

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _adaptiveMode.value = prefs.getBoolean(KEY_ADAPTIVE, false)
        _notificationsEnabled.value = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true)
        
        val map = SoundType.values().associateWith { type ->
            prefs.getFloat(PREFIX_SENSITIVITY + type.name, 0.5f)
        }
        _sensitivities.value = map
    }

    fun setSensitivity(type: SoundType, value: Float) {
        prefs.edit().putFloat(PREFIX_SENSITIVITY + type.name, value).apply()
        val current = _sensitivities.value.toMutableMap()
        current[type] = value
        _sensitivities.value = current
    }

    fun setAdaptiveMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ADAPTIVE, enabled).apply()
        _adaptiveMode.value = enabled
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }
}
