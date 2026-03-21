package com.runanywhere.kotlin_starter_example.data

data class SoundProfile(
    val priority: Int,
    val pattern: LongArray,
    val repeat: Int
)

object SoundProfiles {

    val map = mapOf(

        // 🚨 Strong continuous pattern
        SoundType.SIREN to SoundProfile(
            priority = 5,
            pattern = longArrayOf(0, 400, 150, 400),
            repeat = 0
        ),

        // 🚗 Short bursts
        SoundType.HORN to SoundProfile(
            priority = 4,
            pattern = longArrayOf(0, 120, 80, 120, 80, 120),
            repeat = -1
        ),

        // ⏰ FAST pulses (FIXED)
        SoundType.ALARM to SoundProfile(
            priority = 5,
            pattern = longArrayOf(0, 150, 80, 150, 80, 150, 80, 150),
            repeat = -1
        ),

        // 🔔 DOUBLE TAP (FIXED)
        SoundType.DOORBELL to SoundProfile(
            priority = 3,
            pattern = longArrayOf(0, 200, 150, 200),
            repeat = -1
        ),

        // 🗣️ Soft repeating
        SoundType.VOICE to SoundProfile(
            priority = 1,
            pattern = longArrayOf(0, 250, 250, 250),
            repeat = -1
        )
    )
}