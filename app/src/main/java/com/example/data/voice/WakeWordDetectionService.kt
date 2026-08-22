package com.example.data.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class WakeWordDetectionService : Service() {

    private val binder = LocalBinder()
    private var wakeWordEngine: AcousticWakeWordEngine? = null

    inner class LocalBinder : Binder() {
        fun getService(): WakeWordDetectionService = this@WakeWordDetectionService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d("WakeWordService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_SERVICE) {
            stopForegroundService()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        startAcousticEngine()

        return START_STICKY
    }

    private fun startAcousticEngine() {
        if (wakeWordEngine == null) {
            wakeWordEngine = AcousticWakeWordEngine(this) {
                // Broadcast wake word detection event
                val broadcastIntent = Intent(ACTION_WAKE_WORD_DETECTED).apply {
                    setPackage(packageName)
                }
                sendBroadcast(broadcastIntent)
                listener?.onWakeWordTriggered()
            }
        }
        wakeWordEngine?.start()
    }

    private fun stopForegroundService() {
        wakeWordEngine?.stop()
        wakeWordEngine = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeWordEngine?.stop()
        wakeWordEngine = null
        Log.d("WakeWordService", "Service destroyed")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Z-AI Wake Word Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background listening service for 'Hey Z-AI' wake word"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Z-AI Wake Word Active")
            .setContentText("Listening locally for 'Hey Z-AI' • 100% On-Device")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "z_ai_wakeword_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_SERVICE = "com.example.ACTION_START_WAKEWORD"
        const val ACTION_STOP_SERVICE = "com.example.ACTION_STOP_WAKEWORD"
        const val ACTION_WAKE_WORD_DETECTED = "com.example.ACTION_WAKE_WORD_DETECTED"

        interface WakeWordServiceListener {
            fun onWakeWordTriggered()
        }

        var listener: WakeWordServiceListener? = null

        fun startService(context: Context) {
            val intent = Intent(context, WakeWordDetectionService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, WakeWordDetectionService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.stopService(intent)
        }
    }
}
