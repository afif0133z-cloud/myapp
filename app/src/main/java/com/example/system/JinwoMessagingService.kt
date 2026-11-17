package com.example.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.DevicePreferences
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class JinwoMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "FCM token refreshed")
        DevicePreferences(applicationContext).saveFcmToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        if (data["type"] == "DEVICE_STATUS") {
            val status = data["status"] ?: return
            val amountDue = data["amountDue"]?.toDoubleOrNull() ?: 0.0
            val prefs = DevicePreferences(applicationContext)
            val isLocked = status == "locked"
            prefs.saveCachedLockState(isLocked = isLocked, amountDue = amountDue)

            val statusIntent = Intent(ACTION_DEVICE_STATUS_PUSH).apply {
                setPackage(packageName)
                putExtra(EXTRA_STATUS, status)
                putExtra(EXTRA_AMOUNT_DUE, amountDue)
            }
            sendBroadcast(statusIntent)

            if (isLocked) {
                showLockNotification(amountDue)
                val activityIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
                try {
                    startActivity(activityIntent)
                } catch (e: Exception) {
                    Log.w(TAG, "Background launch deferred to notification: ${e.message}")
                }
            }
        }
    }

    private fun showLockNotification(amountDue: Double) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.default_warning_heading))
            .setContentText("${getString(R.string.amount_due_label)}: ৳ ${amountDue.toLong()}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(LOCK_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "JinwoFCM"
        private const val CHANNEL_ID = "jinwo_emi_status"
        private const val LOCK_NOTIFICATION_ID = 4041
        const val ACTION_DEVICE_STATUS_PUSH = "com.example.ACTION_DEVICE_STATUS_PUSH"
        const val EXTRA_STATUS = "status"
        const val EXTRA_AMOUNT_DUE = "amountDue"
    }
}
