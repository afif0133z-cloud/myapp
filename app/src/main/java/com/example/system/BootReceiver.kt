package com.example.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import com.example.data.DevicePreferences

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            val prefs = DevicePreferences(context.applicationContext)
            if (prefs.isDeviceLocked()) {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
