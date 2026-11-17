package com.example.system

import android.app.Activity
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

class JinwoDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "Device admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.w(TAG, "Device admin disabled")
    }

    companion object {
        private const val TAG = "JinwoAdmin"

        fun componentName(context: Context): ComponentName =
            ComponentName(context.applicationContext, JinwoDeviceAdminReceiver::class.java)

        fun isAdminOrOwner(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false
            val admin = componentName(context)
            return dpm.isDeviceOwnerApp(context.packageName) || dpm.isAdminActive(admin)
        }

        fun isDeviceOwner(context: Context): Boolean {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                ?: return false
            return dpm.isDeviceOwnerApp(context.packageName)
        }

        fun enterLockTaskIfPermitted(activity: Activity) {
            try {
                val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
                    ?: return
                val admin = componentName(activity)
                if (dpm.isDeviceOwnerApp(activity.packageName)) {
                    dpm.setLockTaskPackages(admin, arrayOf(activity.packageName))
                }
                if (dpm.isLockTaskPermitted(activity.packageName)) {
                    activity.startLockTask()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to enter lock task mode: ${e.message}")
            }
        }

        fun exitLockTaskSafely(activity: Activity) {
            try {
                activity.stopLockTask()
            } catch (e: Exception) {
                Log.w(TAG, "Unable to exit lock task mode: ${e.message}")
            }
        }
    }
}
