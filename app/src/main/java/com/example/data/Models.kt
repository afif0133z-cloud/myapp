package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val DEFAULT_LOGO_URL =
    "https://i.postimg.cc/Xqss1GKX/file-00000000c6f0821191367557d1089f92.png"
const val DEFAULT_LOCK_BG_URL =
    "https://i.postimg.cc/CxThLRpL/images-(1).jpg"

@Keep
@IgnoreExtraProperties
data class AppConfig(
    val companyName: String = "",
    val logoUrl: String = DEFAULT_LOGO_URL,
    val lockBackgroundUrl: String = DEFAULT_LOCK_BG_URL,
    val warningHeading: String = "",
    val warningMessage: String = "",
    val activeMessage: String = "",
    val paymentGatewayUrl: String = "",
    val paymentInstructions: String = ""
)

@Keep
@IgnoreExtraProperties
data class DeviceState(
    @DocumentId val id: String = "",
    val deviceId: String = "",
    val status: String = "active",
    val amountDue: Double = 0.0,
    val fcmToken: String = "",
    @ServerTimestamp val lastSeen: Timestamp? = null
) {
    val isLocked: Boolean
        get() = status.equals("locked", ignoreCase = true)
}

@Keep
@IgnoreExtraProperties
data class DeviceTask(
    @DocumentId val id: String = "",
    val deviceId: String = "",
    val name: String = "",
    val iconUrl: String = "",
    val description: String = "",
    val steps: List<String> = emptyList(),
    val status: String = "pending",
    val requiresPermission: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val isDone: Boolean
        get() = status.equals("done", ignoreCase = true)
}

@Keep
@IgnoreExtraProperties
data class PaymentOrder(
    @DocumentId val id: String = "",
    val deviceId: String = "",
    val amount: Double = 0.0,
    val status: String = "pending",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val isPaid: Boolean
        get() = status.equals("paid", ignoreCase = true)
}

data class CreateOrderResult(
    val orderId: String,
    val amount: Double,
    val gatewayUrl: String
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AppScreen {
    data object Start : AppScreen
    data object Enrollment : AppScreen
    data object Registering : AppScreen
    data object Active : AppScreen
    data class TaskDetail(val taskId: String) : AppScreen
    data object Locked : AppScreen
    data class PaymentWebView(
        val orderId: String,
        val amount: Double,
        val gatewayUrl: String
    ) : AppScreen
    data class PaymentVerify(val orderId: String) : AppScreen
    data class PaymentSuccess(val orderId: String) : AppScreen
    data class PinEntry(val orderId: String?) : AppScreen
    data object Unlocked : AppScreen
}
