package com.example.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppConfig
import com.example.data.AppScreen
import com.example.data.DevicePreferences
import com.example.data.DeviceState
import com.example.data.DeviceTask
import com.example.data.EmiRepository
import com.example.data.FirebaseConfigHelper
import com.example.data.PaymentOrder
import com.example.data.UiState
import com.example.system.JinwoDeviceAdminReceiver
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class EmiUiState(
    val screen: AppScreen = AppScreen.Start,
    val config: AppConfig = AppConfig(),
    val deviceState: DeviceState? = null,
    val tasksState: UiState<List<DeviceTask>> = UiState.Loading,
    val isBackendConfigured: Boolean = false,
    val deviceId: String = "",
    val startProgress: Float = 0f,
    val startStatusResIndex: Int = 0,
    val startReady: Boolean = false,
    val deviceAdminGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val internetGranted: Boolean = false,
    val registerStepIndex: Int = 0,
    val isWorking: Boolean = false,
    val actionError: String? = null,
    val pinDigits: String = "",
    val pinError: Boolean = false,
    val pinVerifying: Boolean = false
) {
    val allEnrollPermissionsGranted: Boolean
        get() = deviceAdminGranted && notificationsGranted && internetGranted

    val isInLockFlow: Boolean
        get() = screen is AppScreen.Locked ||
            screen is AppScreen.PaymentWebView ||
            screen is AppScreen.PaymentVerify ||
            screen is AppScreen.PaymentSuccess ||
            screen is AppScreen.PinEntry
}

class EmiViewModel(
    private val repository: EmiRepository,
    private val prefs: DevicePreferences,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EmiUiState(
            isBackendConfigured = repository.isBackendConfigured,
            deviceId = prefs.getOrCreateDeviceId(),
            config = prefs.loadCachedConfig()
        )
    )
    val uiState: StateFlow<EmiUiState> = _uiState.asStateFlow()

    private var configListenerJob: Job? = null
    private var deviceListenerJob: Job? = null
    private var tasksListenerJob: Job? = null
    private var orderListenerJob: Job? = null

    init {
        runStartLoadingSequence()
        startConfigListener()
        if (prefs.isEnrolled()) {
            startDeviceAndTasksListeners()
        }
        refreshEnrollmentStatuses()
    }

    private fun runStartLoadingSequence() {
        viewModelScope.launch {
            _uiState.update { it.copy(startStatusResIndex = 0, startProgress = 0.25f) }
            delay(700)
            _uiState.update { it.copy(startStatusResIndex = 1, startProgress = 0.72f) }
            delay(700)
            _uiState.update { it.copy(startStatusResIndex = 2, startProgress = 1f, startReady = true) }
        }
    }

    private fun startConfigListener() {
        configListenerJob?.cancel()
        configListenerJob = viewModelScope.launch {
            repository.observeAppConfig()
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Config listener error", firestoreError ?: error)
                }
                .collect { cfg ->
                    _uiState.update { it.copy(config = cfg) }
                }
        }
    }

    private fun startDeviceAndTasksListeners() {
        val deviceId = _uiState.value.deviceId
        if (deviceId.isBlank()) return

        deviceListenerJob?.cancel()
        deviceListenerJob = viewModelScope.launch {
            repository.observeDeviceState(deviceId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Device listener error", firestoreError ?: error)
                }
                .collect { deviceState ->
                    _uiState.update { it.copy(deviceState = deviceState) }
                    if (deviceState != null) {
                        handleLiveLockStateChange(deviceState.isLocked, deviceState.amountDue)
                    }
                }
        }

        tasksListenerJob?.cancel()
        tasksListenerJob = viewModelScope.launch {
            repository.observeTasks(deviceId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val root = firestoreError ?: error
                    Log.w(TAG, "Tasks listener error", root)
                    val uiMsg = if (firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        "Firestore পারমিশন নেই। Firebase Console-এ নতুন firestore.rules পাবলিশ করুন।"
                    } else {
                        root.message ?: "টাস্ক লোড করা যায়নি"
                    }
                    _uiState.update {
                        it.copy(tasksState = UiState.Error(uiMsg))
                    }
                }
                .collect { tasks ->
                    _uiState.update { it.copy(tasksState = UiState.Success(tasks)) }
                }
        }
    }

    private fun handleLiveLockStateChange(isLocked: Boolean, amountDue: Double) {
        prefs.saveCachedLockState(isLocked, amountDue)
        val current = _uiState.value.screen
        if (isLocked) {
            if (current !is AppScreen.Start && !isLockOrPaymentScreen(current)) {
                _uiState.update { it.copy(screen = AppScreen.Locked, actionError = null) }
            }
        } else {
            if (current is AppScreen.Locked || current is AppScreen.PaymentWebView || current is AppScreen.PaymentVerify) {
                viewModelScope.launch {
                    _uiState.update { it.copy(screen = AppScreen.Unlocked, pinDigits = "", pinError = false) }
                    delay(2200)
                    _uiState.update { it.copy(screen = AppScreen.Active) }
                }
            }
        }
    }

    private fun isLockOrPaymentScreen(screen: AppScreen): Boolean =
        screen is AppScreen.Locked ||
            screen is AppScreen.PaymentWebView ||
            screen is AppScreen.PaymentVerify ||
            screen is AppScreen.PaymentSuccess ||
            screen is AppScreen.PinEntry ||
            screen is AppScreen.Unlocked

    fun onStartClicked() {
        val state = _uiState.value
        if (!state.startReady) return

        if (prefs.isDeviceLocked() || state.deviceState?.isLocked == true) {
            _uiState.update { it.copy(screen = AppScreen.Locked, actionError = null) }
            return
        }

        if (prefs.isEnrolled()) {
            startDeviceAndTasksListeners()
            _uiState.update { it.copy(screen = AppScreen.Active, actionError = null) }
        } else {
            refreshEnrollmentStatuses()
            _uiState.update { it.copy(screen = AppScreen.Enrollment, actionError = null) }
        }
    }

    fun refreshEnrollmentStatuses() {
        val adminActive = JinwoDeviceAdminReceiver.isAdminOrOwner(appContext)
        val online = hasInternetConnection(appContext)
        _uiState.update {
            it.copy(
                deviceAdminGranted = it.deviceAdminGranted || adminActive,
                internetGranted = it.internetGranted || online
            )
        }
    }

    fun onDeviceAdminGranted(granted: Boolean) {
        if (granted) {
            _uiState.update { it.copy(deviceAdminGranted = true, actionError = null) }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update {
            it.copy(
                notificationsGranted = granted,
                actionError = if (!granted) "নোটিফিকেশন অনুমতি প্রয়োজন" else null
            )
        }
    }

    fun checkAndGrantInternet() {
        val online = hasInternetConnection(appContext)
        _uiState.update {
            it.copy(
                internetGranted = online,
                actionError = if (!online) "অনুগ্রহ করে ইন্টারনেট সংযোগ চালু করুন" else null
            )
        }
    }

    fun onEnrollContinue() {
        val state = _uiState.value
        if (!state.allEnrollPermissionsGranted) return

        _uiState.update { it.copy(screen = AppScreen.Registering, registerStepIndex = 0, actionError = null) }
        viewModelScope.launch {
            _uiState.update { it.copy(registerStepIndex = 0) }
            delay(750)
            _uiState.update { it.copy(registerStepIndex = 1) }

            val fcmToken = if (repository.isBackendConfigured) {
                runCatching { FirebaseMessaging.getInstance().token.await() }
                    .getOrElse { prefs.getFcmToken() }
            } else {
                ""
            }
            if (fcmToken.isNotBlank()) prefs.saveFcmToken(fcmToken)

            if (repository.isBackendConfigured) {
                val res = repository.registerDevice(state.deviceId, fcmToken)
                if (res.isFailure) {
                    val err = res.exceptionOrNull()
                    val fsErr = err?.let {
                        generateSequence(it) { c -> c.cause }
                            .filterIsInstance<FirebaseFirestoreException>()
                            .firstOrNull()
                    }
                    val msg = if (fsErr?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        "Firestore পারমিশন নেই। Firebase Console-এ নতুন firestore.rules পাবলিশ করুন।"
                    } else {
                        err?.localizedMessage ?: "সার্ভারে নিবন্ধন ব্যর্থ হয়েছে"
                    }
                    _uiState.update {
                        it.copy(
                            screen = AppScreen.Enrollment,
                            actionError = msg
                        )
                    }
                    return@launch
                }
            }

            _uiState.update { it.copy(registerStepIndex = 2) }
            delay(750)
            prefs.setEnrolled(true)
            startDeviceAndTasksListeners()
            val target = if (prefs.isDeviceLocked() || _uiState.value.deviceState?.isLocked == true) {
                AppScreen.Locked
            } else {
                AppScreen.Active
            }
            _uiState.update { it.copy(screen = target) }
        }
    }

    fun openTaskDetail(taskId: String) {
        _uiState.update { it.copy(screen = AppScreen.TaskDetail(taskId), actionError = null) }
    }

    fun backToActive() {
        _uiState.update { it.copy(screen = AppScreen.Active, actionError = null) }
    }

    fun completeTask(task: DeviceTask) {
        val deviceId = _uiState.value.deviceId
        _uiState.update { it.copy(isWorking = true, actionError = null) }
        viewModelScope.launch {
            delay(550)
            if (repository.isBackendConfigured) {
                val result = repository.markTaskDone(deviceId, task)
                if (result.isFailure) {
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            actionError = result.exceptionOrNull()?.localizedMessage
                                ?: "টাস্ক আপডেট ব্যর্থ হয়েছে"
                        )
                    }
                    return@launch
                }
            }
            _uiState.update { it.copy(isWorking = false, screen = AppScreen.Active) }
        }
    }

    fun onPayNowClicked() {
        val state = _uiState.value
        if (!repository.isBackendConfigured) {
            _uiState.update {
                it.copy(actionError = "Firebase এবং পেমেন্ট সার্ভার কনফিগার করা হয়নি")
            }
            return
        }
        _uiState.update { it.copy(isWorking = true, actionError = null) }
        viewModelScope.launch {
            val result = repository.createPaymentOrder(state.deviceId)
            result.fold(
                onSuccess = { order ->
                    prefs.saveActiveOrderId(order.orderId)
                    startObservingOrder(order.orderId)
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            screen = AppScreen.PaymentWebView(
                                orderId = order.orderId,
                                amount = order.amount,
                                gatewayUrl = order.gatewayUrl
                            )
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isWorking = false,
                            actionError = err.localizedMessage ?: "পেমেন্ট অর্ডার তৈরি করা যায়নি"
                        )
                    }
                }
            )
        }
    }

    private fun startObservingOrder(orderId: String) {
        orderListenerJob?.cancel()
        orderListenerJob = viewModelScope.launch {
            repository.observeOrder(orderId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Order listener error", firestoreError ?: error)
                }
                .collect { order: PaymentOrder? ->
                    if (order?.isPaid == true) {
                        val current = _uiState.value.screen
                        if (current is AppScreen.PaymentWebView || current is AppScreen.PaymentVerify) {
                            _uiState.update { it.copy(screen = AppScreen.PaymentVerify(order.id)) }
                            delay(1000)
                            _uiState.update { it.copy(screen = AppScreen.PaymentSuccess(order.id)) }
                        }
                    }
                }
        }
    }

    fun onWebViewBackToLocked() {
        _uiState.update { it.copy(screen = AppScreen.Locked, actionError = null) }
    }

    fun onCheckPaymentStatus(orderId: String) {
        _uiState.update { it.copy(screen = AppScreen.PaymentVerify(orderId), actionError = null) }
        startObservingOrder(orderId)
    }

    fun onProceedToPin(orderId: String) {
        _uiState.update {
            it.copy(
                screen = AppScreen.PinEntry(orderId),
                pinDigits = "",
                pinError = false,
                pinVerifying = false,
                actionError = null
            )
        }
    }

    fun onPinKeyPressed(key: String) {
        val state = _uiState.value
        if (state.pinVerifying) return
        val orderId = (state.screen as? AppScreen.PinEntry)?.orderId ?: prefs.getActiveOrderId()

        if (key == "⌫") {
            _uiState.update { it.copy(pinDigits = it.pinDigits.dropLast(1), pinError = false) }
            return
        }
        if (key.isEmpty() || state.pinDigits.length >= 6) return

        val nextPin = state.pinDigits + key
        _uiState.update { it.copy(pinDigits = nextPin, pinError = false) }

        if (nextPin.length == 6) {
            submitPinForVerification(state.deviceId, orderId, nextPin)
        }
    }

    private fun submitPinForVerification(deviceId: String, orderId: String?, pin: String) {
        _uiState.update { it.copy(pinVerifying = true, pinError = false) }
        viewModelScope.launch {
            val result = repository.verifyUnlockPin(deviceId, orderId, pin)
            val verified = result.getOrDefault(false)
            if (verified) {
                prefs.saveCachedLockState(isLocked = false, amountDue = 0.0)
                prefs.saveActiveOrderId(null)
                _uiState.update {
                    it.copy(
                        pinVerifying = false,
                        pinDigits = "",
                        screen = AppScreen.Unlocked
                    )
                }
                delay(2200)
                _uiState.update { it.copy(screen = AppScreen.Active) }
            } else {
                _uiState.update {
                    it.copy(
                        pinVerifying = false,
                        pinError = true
                    )
                }
                delay(450)
                _uiState.update { it.copy(pinDigits = "") }
            }
        }
    }

    fun onFcmStatusPush(status: String, amountDue: Double) {
        handleLiveLockStateChange(status == "locked", amountDue)
    }

    private fun hasInternetConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onCleared() {
        super.onCleared()
        configListenerJob?.cancel()
        deviceListenerJob?.cancel()
        tasksListenerJob?.cancel()
        orderListenerJob?.cancel()
    }

    companion object {
        private const val TAG = "EmiViewModel"

        fun provideFactory(appContext: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val context = appContext.applicationContext
                    val prefs = DevicePreferences(context)
                    val db = FirebaseConfigHelper.getFirestoreOrNull(context)
                    val repository = EmiRepository(db, prefs)
                    return EmiViewModel(repository, prefs, context) as T
                }
            }
    }
}
