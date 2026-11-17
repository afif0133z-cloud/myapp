package com.example.data

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

class DevicePreferences(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @SuppressLint("HardwareIds")
    fun getOrCreateDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val androidId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        )?.replace(Regex("[^a-zA-Z0-9_-]"), "")

        val resolved = if (!androidId.isNullOrBlank()) {
            androidId
        } else {
            "dev_${System.currentTimeMillis()}"
        }
        prefs.edit().putString(KEY_DEVICE_ID, resolved).apply()
        return resolved
    }

    fun isEnrolled(): Boolean = prefs.getBoolean(KEY_ENROLLED, false)

    fun setEnrolled(enrolled: Boolean) {
        prefs.edit().putBoolean(KEY_ENROLLED, enrolled).apply()
    }

    fun isDeviceLocked(): Boolean = prefs.getBoolean(KEY_LOCKED, false)

    fun getCachedAmountDue(): Double =
        java.lang.Double.longBitsToDouble(prefs.getLong(KEY_AMOUNT_DUE, 0L))

    fun saveCachedLockState(isLocked: Boolean, amountDue: Double) {
        prefs.edit()
            .putBoolean(KEY_LOCKED, isLocked)
            .putLong(KEY_AMOUNT_DUE, java.lang.Double.doubleToRawLongBits(amountDue))
            .apply()
    }

    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFcmToken(): String = prefs.getString(KEY_FCM_TOKEN, "") ?: ""

    fun saveActiveOrderId(orderId: String?) {
        prefs.edit().putString(KEY_ACTIVE_ORDER_ID, orderId).apply()
    }

    fun getActiveOrderId(): String? = prefs.getString(KEY_ACTIVE_ORDER_ID, null)

    fun saveCachedConfig(config: AppConfig) {
        prefs.edit()
            .putString(KEY_COMPANY_NAME, config.companyName)
            .putString(KEY_LOGO_URL, config.logoUrl)
            .putString(KEY_LOCK_BG_URL, config.lockBackgroundUrl)
            .putString(KEY_WARNING_HEADING, config.warningHeading)
            .putString(KEY_WARNING_MSG, config.warningMessage)
            .putString(KEY_ACTIVE_MSG, config.activeMessage)
            .putString(KEY_GATEWAY_URL, config.paymentGatewayUrl)
            .putString(KEY_PAYMENT_INSTR, config.paymentInstructions)
            .apply()
    }

    fun loadCachedConfig(): AppConfig = AppConfig(
        companyName = prefs.getString(KEY_COMPANY_NAME, "") ?: "",
        logoUrl = (prefs.getString(KEY_LOGO_URL, null) ?: "").ifBlank { DEFAULT_LOGO_URL },
        lockBackgroundUrl = (prefs.getString(KEY_LOCK_BG_URL, null) ?: "").ifBlank { DEFAULT_LOCK_BG_URL },
        warningHeading = prefs.getString(KEY_WARNING_HEADING, "") ?: "",
        warningMessage = prefs.getString(KEY_WARNING_MSG, "") ?: "",
        activeMessage = prefs.getString(KEY_ACTIVE_MSG, "") ?: "",
        paymentGatewayUrl = prefs.getString(KEY_GATEWAY_URL, "") ?: "",
        paymentInstructions = prefs.getString(KEY_PAYMENT_INSTR, "") ?: ""
    )

    companion object {
        private const val PREFS_NAME = "jinwo_emi_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ENROLLED = "is_enrolled"
        private const val KEY_LOCKED = "is_locked"
        private const val KEY_AMOUNT_DUE = "amount_due_bits"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_ACTIVE_ORDER_ID = "active_order_id"
        private const val KEY_COMPANY_NAME = "cfg_company_name"
        private const val KEY_LOGO_URL = "cfg_logo_url"
        private const val KEY_LOCK_BG_URL = "cfg_lock_bg"
        private const val KEY_WARNING_HEADING = "cfg_warn_head"
        private const val KEY_WARNING_MSG = "cfg_warn_msg"
        private const val KEY_ACTIVE_MSG = "cfg_active_msg"
        private const val KEY_GATEWAY_URL = "cfg_gateway_url"
        private const val KEY_PAYMENT_INSTR = "cfg_pay_instr"
    }
}
