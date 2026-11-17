package com.example.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.data.AppConfig
import com.example.ui.theme.JinwoBlue
import com.example.ui.theme.JinwoBlueDark
import com.example.ui.theme.JinwoBlueSoft
import com.example.ui.theme.JinwoInk
import com.example.ui.theme.JinwoMuted
import com.example.ui.theme.JinwoOk
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

private fun formatTaka(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("bn", "BD"))
    formatter.maximumFractionDigits = 0
    return "৳ ${formatter.format(amount)}"
}

@Composable
fun LockedScreen(
    config: AppConfig,
    amountDue: Double,
    isWorking: Boolean,
    errorMessage: String?,
    onPayNowClick: () -> Unit
) {
    // Prevent leaving the lock screen via the system Back button
    BackHandler(enabled = true) {}

    val context = LocalContext.current
    val companyName = config.companyName.ifBlank { stringResource(R.string.default_company_name) }
    val heading = config.warningHeading.ifBlank { stringResource(R.string.default_warning_heading) }
    val lockMessage = config.warningMessage.ifBlank { stringResource(R.string.default_warning_message) }
    val instructions = config.paymentInstructions.ifBlank {
        stringResource(R.string.default_payment_instructions)
    }

    PhotoBackgroundContainer(backgroundUrl = config.lockBackgroundUrl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompanyLogo(
                logoUrl = config.logoUrl,
                companyName = companyName,
                large = false
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = companyName,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ShieldCircleIcon(darkSurface = true) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = heading,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = heading,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))

                GlassCard {
                    Text(
                        text = lockMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.amount_due_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = formatTaka(amountDue),
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = instructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFFD6D6),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onPayNowClick,
                enabled = !isWorking,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1C4FD1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(12.dp, RoundedCornerShape(18.dp))
                    .testTag("pay_now_button")
            ) {
                if (isWorking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF1C4FD1),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = stringResource(R.string.pay_now_btn),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:999")
                        }
                        runCatching { context.startActivity(dialIntent) }
                    }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .testTag("emergency_call_button"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = stringResource(R.string.emergency_call_label),
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.emergency_call_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PaymentWebViewScreen(
    orderId: String,
    amount: Double,
    gatewayUrl: String,
    onBack: () -> Unit,
    onCheckStatus: () -> Unit
) {
    BackHandler(enabled = true, onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8FF))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(JinwoBlueSoft)
                    .testTag("payment_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = JinwoBlue
                )
            }
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = JinwoOk,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.gateway_header),
                    style = MaterialTheme.typography.titleSmall,
                    color = JinwoInk
                )
                Text(
                    text = "${formatTaka(amount)} • $orderId",
                    style = MaterialTheme.typography.bodySmall,
                    color = JinwoMuted
                )
            }
        }

        AndroidView(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean = false
                    }
                    loadUrl(gatewayUrl)
                }
            }
        )

        Surface(
            color = Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                PrimaryGradientButton(
                    text = stringResource(R.string.verify_subtitle),
                    onClick = onCheckStatus,
                    modifier = Modifier.testTag("check_payment_status_button")
                )
            }
        }
    }
}

@Composable
fun PaymentVerifyScreen(
    config: AppConfig,
    onBackToLocked: () -> Unit
) {
    BackHandler(enabled = true, onBack = onBackToLocked)

    PhotoBackgroundContainer(backgroundUrl = config.lockBackgroundUrl) {
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
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
                strokeWidth = 4.dp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.verify_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.verify_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE5ECFF),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PaymentSuccessScreen(
    config: AppConfig,
    onProceedToPin: () -> Unit
) {
    BackHandler(enabled = true) {}

    PhotoBackgroundContainer(backgroundUrl = config.lockBackgroundUrl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SuccessTickCircle {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.payment_success_title),
                        tint = JinwoOk,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.payment_success_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                GlassCard {
                    Text(
                        text = stringResource(R.string.payment_success_msg),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = onProceedToPin,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1C4FD1)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(12.dp, RoundedCornerShape(18.dp))
                    .testTag("to_pin_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Key,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = stringResource(R.string.enter_pin_btn),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun PinEntryScreen(
    config: AppConfig,
    pinDigits: String,
    pinError: Boolean,
    pinVerifying: Boolean,
    onKeyPress: (String) -> Unit,
    onBackToLocked: () -> Unit
) {
    BackHandler(enabled = true, onBack = onBackToLocked)

    val companyName = config.companyName.ifBlank { stringResource(R.string.default_company_name) }
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(pinError) {
        if (pinError) {
            shakeOffset.animateTo(-14f, tween(70))
            shakeOffset.animateTo(14f, tween(70))
            shakeOffset.animateTo(-10f, tween(70))
            shakeOffset.animateTo(0f, tween(70))
        }
    }

    val hintText = when {
        pinVerifying -> stringResource(R.string.pin_verifying)
        pinError -> stringResource(R.string.pin_wrong)
        else -> stringResource(R.string.pin_hint)
    }

    PhotoBackgroundContainer(backgroundUrl = config.lockBackgroundUrl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CompanyLogo(
                logoUrl = config.logoUrl,
                companyName = companyName,
                large = false
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ShieldCircleIcon(darkSurface = true) {
                    Icon(
                        imageVector = Icons.Rounded.Key,
                        contentDescription = stringResource(R.string.pin_title),
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.pin_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(6) { index ->
                        val filled = index < pinDigits.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .scale(if (filled) 1.15f else 1f)
                                .clip(CircleShape)
                                .background(if (filled) Color.White else Color.Transparent)
                                .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    text = hintText,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (pinError) Color(0xFFFFD6D6) else Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))

                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "⌫")
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(11.dp)
                ) {
                    keys.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            rowKeys.forEach { key ->
                                if (key.isEmpty()) {
                                    Spacer(Modifier.weight(1f).height(54.dp))
                                } else {
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(54.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable(enabled = !pinVerifying) { onKeyPress(key) }
                                            .testTag("pin_key_$key"),
                                        shape = RoundedCornerShape(18.dp),
                                        color = Color.White.copy(alpha = 0.17f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (key == "⌫") {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                                    contentDescription = "Backspace",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = key,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnlockedScreen(config: AppConfig) {
    PhotoBackgroundContainer(backgroundUrl = config.lockBackgroundUrl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SuccessTickCircle {
                Icon(
                    imageVector = Icons.Rounded.LockOpen,
                    contentDescription = stringResource(R.string.unlocked_title),
                    tint = JinwoOk,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.unlocked_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.unlocked_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE5ECFF),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SuccessTickCircle(icon: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .size(88.dp)
            .shadow(16.dp, CircleShape),
        shape = CircleShape,
        color = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
