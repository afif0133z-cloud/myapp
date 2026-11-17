package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AppConfig
import com.example.ui.theme.JinwoBlueDark
import com.example.ui.theme.JinwoBlueGradientStart
import com.example.ui.theme.JinwoBlueSoft
import com.example.ui.theme.JinwoInk
import com.example.ui.theme.JinwoMuted

@Composable
fun StartScreen(
    config: AppConfig,
    progressTarget: Float,
    statusIndex: Int,
    startEnabled: Boolean,
    onStartClick: () -> Unit
) {
    val companyName = config.companyName.ifBlank { stringResource(R.string.default_company_name) }
    val statusText = when (statusIndex) {
        0 -> stringResource(R.string.loading_step_1)
        1 -> stringResource(R.string.loading_step_2)
        else -> stringResource(R.string.loading_step_3)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 650),
        label = "startProgress"
    )

    JinwoScreenScaffold {
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
                CompanyLogo(
                    logoUrl = config.logoUrl,
                    companyName = companyName,
                    large = true,
                    withHalo = true
                )
                Spacer(Modifier.height(22.dp))
                Text(
                    text = companyName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = JinwoInk,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(30.dp))
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(JinwoBlueSoft)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(9.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(JinwoBlueGradientStart, JinwoBlueDark)
                                )
                            )
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = JinwoMuted,
                    textAlign = TextAlign.Center
                )
            }

            PrimaryGradientButton(
                text = stringResource(R.string.start_button),
                enabled = startEnabled,
                onClick = onStartClick,
                modifier = Modifier.testTag("start_button"),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            )
        }
    }
}
