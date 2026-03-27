package com.runanywhere.kotlin_starter_example.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.runanywhere.kotlin_starter_example.R
import com.runanywhere.kotlin_starter_example.data.SoundRepository
import com.runanywhere.kotlin_starter_example.data.SettingsRepository
import com.runanywhere.kotlin_starter_example.data.SoundType
import kotlinx.coroutines.*

class AudioForegroundService : Service() {

    companion object {
        var isRunning = false
            private set
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var audioProcessor: AudioProcessor
    private lateinit var classifier: SoundClassifier
    
    private val lastNotificationTimes = mutableMapOf<SoundType, Long>()
    private val NOTIFICATION_COOLDOWN = 15000L // 15 seconds per sound type

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        audioProcessor = AudioProcessor(applicationContext)
        classifier = SoundClassifier(applicationContext)
        
        // Start foreground with proper type for Android 14+
        val notification = createNotification("Aeris Active", "Monitoring for sounds...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }
        
        startListening()
    }

    private fun startListening() {
        scope.launch {
            try {
                audioProcessor.start { audioChunk ->
                    scope.launch {
                        try {
                            val predictions = classifier.classify(audioChunk)
                            if (predictions.isNotEmpty()) {
                                val sensitivities = SettingsRepository.sensitivities.value
                                val filtered = predictions.filter { (type, conf) ->
                                    // Logic: Sensitivity slider 0.0 (High Sens) -> 1.0 (Low Sens)
                                    // Threshold = value. 
                                    // If slider is 0.2, it triggers at 20% confidence.
                                    val threshold = sensitivities[type] ?: 0.5f
                                    conf >= threshold
                                }

                                if (filtered.isNotEmpty()) {
                                    SoundRepository.updateDetection(filtered)
                                    
                                    val top = filtered.maxByOrNull { it.value }!!
                                    // Trigger haptics directly from service to ensure it works even if screen is off
                                    HapticManager.trigger(applicationContext, top.key)
                                    
                                    checkAndSendNotification(filtered)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("AudioForegroundService", "Detection error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioForegroundService", "startListening error: ${e.message}")
            }
        }
    }

    private fun checkAndSendNotification(predictions: Map<SoundType, Float>) {
        if (!SettingsRepository.notificationsEnabled.value) return

        val top = predictions.maxByOrNull { it.value } ?: return
        val soundType = top.key
        val now = System.currentTimeMillis()
        
        val lastTime = lastNotificationTimes[soundType] ?: 0L
        if (now - lastTime > NOTIFICATION_COOLDOWN) {
            sendSoundNotification(soundType, top.value)
            lastNotificationTimes[soundType] = now
        }
    }

    private fun sendSoundNotification(type: SoundType, confidence: Float) {
        val manager = getSystemService(NotificationManager::class.java)
        val title = "${type.name} Detected!"
        val text = "Confidence: ${(confidence * 100).toInt()}%"

        val notification = NotificationCompat.Builder(this, "aeris_alerts")
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .build()

        manager.notify(type.ordinal + 100, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        scope.cancel()
        audioProcessor.stop()
        classifier.close()
        SoundRepository.updateDetection(emptyMap())
    }

    private fun createNotification(title: String, text: String): Notification {
        val channelId = "aeris_channel"
        val alertChannelId = "aeris_alerts"
        val manager = getSystemService(NotificationManager::class.java)
        
        val channel = NotificationChannel(
            channelId, "Aeris Status",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)

        val alertChannel = NotificationChannel(
            alertChannelId, "Sound Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Detection alerts"
            enableLights(true)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(alertChannel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
