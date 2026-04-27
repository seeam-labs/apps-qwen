package com.zendroid.nmapgui.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zendroid.nmapgui.R
import com.zendroid.nmapgui.ui.activity.MainActivity

class ScanForegroundService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "zendroid_scan_channel"
        private const val CHANNEL_NAME = "Active Scans"
        
        const val ACTION_UPDATE_PROGRESS = "com.zendroid.nmapgui.UPDATE_PROGRESS"
        const val ACTION_STOP_SCAN = "com.zendroid.nmapgui.STOP_SCAN"
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_STATUS = "status"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_PROGRESS -> {
                val progress = intent.getIntExtra(EXTRA_PROGRESS, 0)
                val status = intent.getStringExtra(EXTRA_STATUS) ?: "Scanning..."
                updateNotification(progress, status)
            }
            ACTION_STOP_SCAN -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                startForeground(NOTIFICATION_ID, createNotification(0, "Initializing scan..."))
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of active network scans"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(progress: Int, status: String) = 
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ZenDroid Scan")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .addAction(createStopAction())
            .setContentIntent(createContentIntent())
            .build()

    private fun createStopAction(): NotificationCompat.Action {
        val stopIntent = Intent(this, ScanForegroundService::class.java).apply {
            action = ACTION_STOP_SCAN
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop",
            stopPendingIntent
        ).build()
    }

    private fun createContentIntent(): PendingIntent {
        val mainIntent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun updateNotification(progress: Int, status: String) {
        val notification = createNotification(progress, status)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun updateProgress(progress: Int, status: String) {
        val intent = Intent(this, ScanForegroundService::class.java).apply {
            action = ACTION_UPDATE_PROGRESS
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_STATUS, status)
        }
        onStartCommand(intent, 0, 0)
    }
}
