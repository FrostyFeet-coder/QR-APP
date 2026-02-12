package com.example.qrcodegenration.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.qrcodegenration.IncomingCallActivity
import com.example.qrcodegenration.R
import com.example.qrcodegenration.utils.SignalingManager

class CallService : Service(), SignalingManager.SignalingListener {

    private val CHANNEL_ID = "CallServiceChannel"
    private val INCOMING_CHANNEL_ID = "IncomingCallChannel"
    private val NOTIFICATION_ID = 1
    private val INCOMING_CALL_ID = 2

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        SignalingManager.initialize(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userId = intent?.getStringExtra("USER_ID")
        if (userId != null) {
            SignalingManager.login(userId)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, createForegroundNotification("Online and waiting for calls..."), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, createForegroundNotification("Online and waiting for calls..."))
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        SignalingManager.logout()
        SignalingManager.destroy()
    }

    // Signaling Listener Callbacks
    override fun onLoginSuccess() {
        Log.d("CallService", "Logged into RTM successfully")
    }

    override fun onLoginError(errorCode: Int) {
        Log.e("CallService", "RTM Login failed: $errorCode")
    }

    override fun onInvitationReceived(callerId: String, channelName: String) {
        Log.d("CallService", "Invitation received from $callerId for channel $channelName")
        showIncomingCallNotification(channelName)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Background service channel
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Call Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service for receiving calls"
            }

            // Incoming call channel - HIGH importance for heads-up notification
            val incomingChannel = NotificationChannel(
                INCOMING_CHANNEL_ID,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming call notifications"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                enableLights(true)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(incomingChannel)
        }
    }

    private fun createForegroundNotification(contentText: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("QR Parking Assistant")
        .setContentText(contentText)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(true)
        .build()

    private fun showIncomingCallNotification(channelName: String) {
        // Intent for tapping the notification or Accept button
        val acceptIntent = Intent(this, IncomingCallActivity::class.java).apply {
            putExtra("CHANNEL_ID", channelName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val acceptPendingIntent = PendingIntent.getActivity(
            this, 0, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent for Reject button - dismiss notification
        val rejectIntent = Intent(this, CallService::class.java).apply {
            action = "REJECT_CALL"
        }
        val rejectPendingIntent = PendingIntent.getService(
            this, 1, rejectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, INCOMING_CHANNEL_ID)
            .setContentTitle("📞 Incoming Secure Call")
            .setContentText("Someone scanned your QR code and wants to call")
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(acceptPendingIntent) // Tap notification -> open incoming call screen
            .setFullScreenIntent(acceptPendingIntent, true) // Full screen intent for locked devices
            .addAction(android.R.drawable.sym_call_incoming, "Accept", acceptPendingIntent)
            .addAction(android.R.drawable.sym_call_missed, "Reject", rejectPendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(INCOMING_CALL_ID, notification)

        // Also try to launch IncomingCallActivity directly
        try {
            val directLaunchIntent = Intent(this, IncomingCallActivity::class.java).apply {
                putExtra("CHANNEL_ID", channelName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(directLaunchIntent)
        } catch (e: Exception) {
            Log.e("CallService", "Could not directly launch IncomingCallActivity: ${e.message}")
        }
    }
}
