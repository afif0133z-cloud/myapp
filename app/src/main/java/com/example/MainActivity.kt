package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppScreen
import com.example.data.UiState
import com.example.system.JinwoDeviceAdminReceiver
import com.example.system.JinwoMessagingService
import com.example.ui.ActiveScreen
import com.example.ui.EmiUiState
import com.example.ui.EmiViewModel
import com.example.ui.EnrollmentScreen
import com.example.ui.LockedScreen
import com.example.ui.PaymentSuccessScreen
import com.example.ui.PaymentVerifyScreen
import com.example.ui.PaymentWebViewScreen
import com.example.ui.PinEntryScreen
import com.example.ui.RegisteringScreen
import com.example.ui.StartScreen
import com.example.ui.TaskDetailScreen
import com.example.ui.UnlockedScreen
import com.example.ui.theme.JinwoEmiTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JinwoEmiTheme {
                val vm: EmiViewModel = viewModel(
                    factory = EmiViewModel.provideFactory(applicationContext)
                )
                val uiState by vm.uiState.collectAsStateWithLifecycle()

                DisposableEffect(this) {
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            val status = intent?.getStringExtra(JinwoMessagingService.EXTRA_STATUS) ?: return
                            val amountDue = intent.getDoubleExtra(JinwoMessagingService.EXTRA_AMOUNT_DUE, 0.0)
                            vm.onFcmStatusPush(status, amountDue)
                        }
                    }
                    val filter = IntentFilter(JinwoMessagingService.ACTION_DEVICE_STATUS_PUSH)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                    } else {
                        @Suppress("UnspecifiedRegisterReceiverFlag")
                        registerReceiver(receiver, filter)
                    }
                    onDispose {
                        runCatching { unregisterReceiver(receiver) }
                    }
                }

                LaunchedEffect(uiState.isInLockFlow) {
                    if (uiState.isInLockFlow) {
                        JinwoDeviceAdminReceiver.enterLockTaskIfPermitted(this@MainActivity)
                    } else if (uiState.screen is AppScreen.Active || uiState.screen is AppScreen.Unlocked) {
                        JinwoDeviceAdminReceiver.exitLockTaskSafely(this@MainActivity)
                    }
                }

                JinwoAppRouter(
                    uiState = uiState,
                    vm = vm
                )
            }
        }
    }
}

@Composable
private fun JinwoAppRouter(
    uiState: EmiUiState,
    vm: EmiViewModel
) {
    when (val screen = uiState.screen) {
        is AppScreen.Start -> StartScreen(
            config = uiState.config,
            progressTarget = uiState.startProgress,
            statusIndex = uiState.startStatusResIndex,
            startEnabled = uiState.startReady,
            onStartClick = vm::onStartClicked
        )

        is AppScreen.Enrollment -> EnrollmentScreen(
            uiState = uiState,
            onDeviceAdminResult = vm::onDeviceAdminGranted,
            onNotificationResult = vm::onNotificationPermissionResult,
            onInternetCheck = vm::checkAndGrantInternet,
            onContinueClick = vm::onEnrollContinue
        )

        is AppScreen.Registering -> RegisteringScreen(
            stepIndex = uiState.registerStepIndex
        )

        is AppScreen.Active -> ActiveScreen(
            uiState = uiState,
            onTaskClick = { task -> vm.openTaskDetail(task.id) }
        )

        is AppScreen.TaskDetail -> {
            val tasks = (uiState.tasksState as? UiState.Success)?.data.orEmpty()
            val task = tasks.firstOrNull { it.id == screen.taskId }
            if (task == null) {
                ActiveScreen(
                    uiState = uiState,
                    onTaskClick = { t -> vm.openTaskDetail(t.id) }
                )
            } else {
                TaskDetailScreen(
                    task = task,
                    isWorking = uiState.isWorking,
                    errorMessage = uiState.actionError,
                    onBack = vm::backToActive,
                    onCompleteTask = { vm.completeTask(task) }
                )
            }
        }

        is AppScreen.Locked -> LockedScreen(
            config = uiState.config,
            amountDue = uiState.deviceState?.amountDue ?: 0.0,
            isWorking = uiState.isWorking,
            errorMessage = uiState.actionError,
            onPayNowClick = vm::onPayNowClicked
        )

        is AppScreen.PaymentWebView -> PaymentWebViewScreen(
            orderId = screen.orderId,
            amount = screen.amount,
            gatewayUrl = screen.gatewayUrl,
            onBack = vm::onWebViewBackToLocked,
            onCheckStatus = { vm.onCheckPaymentStatus(screen.orderId) }
        )

        is AppScreen.PaymentVerify -> PaymentVerifyScreen(
            config = uiState.config,
            onBackToLocked = vm::onWebViewBackToLocked
        )

        is AppScreen.PaymentSuccess -> PaymentSuccessScreen(
            config = uiState.config,
            onProceedToPin = { vm.onProceedToPin(screen.orderId) }
        )

        is AppScreen.PinEntry -> PinEntryScreen(
            config = uiState.config,
            pinDigits = uiState.pinDigits,
            pinError = uiState.pinError,
            pinVerifying = uiState.pinVerifying,
            onKeyPress = vm::onPinKeyPressed,
            onBackToLocked = vm::onWebViewBackToLocked
        )

        is AppScreen.Unlocked -> UnlockedScreen(
            config = uiState.config
        )
    }
}
