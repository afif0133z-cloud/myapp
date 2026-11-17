package com.example.ui

import android.Manifest
import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R
import com.example.system.JinwoDeviceAdminReceiver
import com.example.ui.theme.JinwoBlue
import com.example.ui.theme.JinwoBlueBorder
import com.example.ui.theme.JinwoBlueSoft
import com.example.ui.theme.JinwoError
import com.example.ui.theme.JinwoInk
import com.example.ui.theme.JinwoMuted
import com.example.ui.theme.JinwoOk
import com.example.ui.theme.JinwoOkBorder
import com.example.ui.theme.JinwoOkIconBg

@Composable
fun EnrollmentScreen(
    uiState: EmiUiState,
    onDeviceAdminResult: (Boolean) -> Unit,
    onNotificationResult: (Boolean) -> Unit,
    onInternetCheck: () -> Unit,
    onContinueClick: () -> Unit
) {
    val context = LocalContext.current
    val companyName = uiState.config.companyName.ifBlank { stringResource(R.string.default_company_name) }
    val rationaleText = stringResource(R.string.perm_device_admin_rationale)

    val adminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val active = result.resultCode == Activity.RESULT_OK ||
            JinwoDeviceAdminReceiver.isAdminOrOwner(context)
        onDeviceAdminResult(active)
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onNotificationResult(granted)
    }

    LaunchedEffect(Unit) {
        if (JinwoDeviceAdminReceiver.isAdminOrOwner(context)) {
            onDeviceAdminResult(true)
        }
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        if (notifGranted) {
            onNotificationResult(true)
        }
        onInternetCheck()
    }

    JinwoScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompanyLogo(
                logoUrl = uiState.config.logoUrl,
                companyName = companyName,
                large = false
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.enroll_title),
                style = MaterialTheme.typography.headlineSmall,
                color = JinwoInk,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.enroll_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = JinwoMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PermissionEnrollRow(
                    icon = Icons.Rounded.Security,
                    title = stringResource(R.string.perm_device_admin_title),
                    subtitle = stringResource(R.string.perm_device_admin_desc),
                    granted = uiState.deviceAdminGranted,
                    testTag = "perm_device_admin_button",
                    onAllowClick = {
                        if (JinwoDeviceAdminReceiver.isAdminOrOwner(context)) {
                            onDeviceAdminResult(true)
                        } else {
                            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                putExtra(
                                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                    JinwoDeviceAdminReceiver.componentName(context)
                                )
                                putExtra(
                                    DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    rationaleText
                                )
                            }
                            adminLauncher.launch(intent)
                        }
                    }
                )

                PermissionEnrollRow(
                    icon = Icons.Rounded.Notifications,
                    title = stringResource(R.string.perm_notif_title),
                    subtitle = stringResource(R.string.perm_notif_desc),
                    granted = uiState.notificationsGranted,
                    testTag = "perm_notifications_button",
                    onAllowClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onNotificationResult(true)
                        }
                    }
                )

                PermissionEnrollRow(
                    icon = Icons.Rounded.Wifi,
                    title = stringResource(R.string.perm_internet_title),
                    subtitle = stringResource(R.string.perm_internet_desc),
                    granted = uiState.internetGranted,
                    testTag = "perm_internet_button",
                    onAllowClick = onInternetCheck
                )
            }

            if (!uiState.actionError.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = uiState.actionError,
                    style = MaterialTheme.typography.bodySmall,
                    color = JinwoError,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(18.dp))

            PrimaryGradientButton(
                text = stringResource(R.string.continue_btn),
                enabled = uiState.allEnrollPermissionsGranted,
                onClick = onContinueClick,
                modifier = Modifier.testTag("enroll_continue_button"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun PermissionEnrollRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    testTag: String,
    onAllowClick: () -> Unit
) {
    val bgColor = if (granted) Color(0xFFF4FBF7) else Color.White
    val borderColor = if (granted) JinwoOkBorder else JinwoBlueBorder
    val iconBg = if (granted) JinwoOkIconBg else JinwoBlueSoft
    val iconTint = if (granted) JinwoOk else JinwoBlue

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = JinwoBlue.copy(alpha = 0.08f),
                spotColor = JinwoBlue.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.5.sp),
                    color = JinwoInk
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = JinwoMuted
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onAllowClick,
                enabled = !granted,
                shape = RoundedCornerShape(99.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = JinwoBlue,
                    contentColor = Color.White,
                    disabledContainerColor = JinwoOkIconBg,
                    disabledContentColor = JinwoOk
                ),
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag(testTag)
            ) {
                Text(
                    text = stringResource(if (granted) R.string.granted_btn else R.string.allow_btn),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun RegisteringScreen(stepIndex: Int) {
    val stepText = when (stepIndex) {
        0 -> stringResource(R.string.reg_step_1)
        1 -> stringResource(R.string.reg_step_2)
        else -> stringResource(R.string.reg_step_3)
    }

    JinwoScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(44.dp),
                color = JinwoBlue,
                trackColor = JinwoBlueSoft,
                strokeWidth = 4.dp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.reg_title),
                style = MaterialTheme.typography.headlineSmall,
                color = JinwoInk,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stepText,
                style = MaterialTheme.typography.bodyMedium,
                color = JinwoMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
