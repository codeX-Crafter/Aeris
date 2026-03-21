package com.runanywhere.kotlin_starter_example.services

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.runanywhere.kotlin_starter_example.data.SoundProfiles
import com.runanywhere.kotlin_starter_example.data.SoundType

object HapticManager {

    fun trigger(context: Context, sound: SoundType) {

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val profile = SoundProfiles.map[sound] ?: return

        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createWaveform(
                profile.pattern,
                profile.repeat
            )
            vibrator.cancel()
            vibrator.vibrate(effect)
        } else {
            // fallback for older devices
            vibrator.vibrate(profile.pattern, profile.repeat)
        }
    }
}