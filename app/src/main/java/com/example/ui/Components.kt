package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.JinwoBlue
import com.example.ui.theme.JinwoBlueBorder
import com.example.ui.theme.JinwoBlueDark
import com.example.ui.theme.JinwoBlueGradientStart
import com.example.ui.theme.JinwoBlueLight
import com.example.ui.theme.JinwoBlueSoft
import com.example.ui.theme.JinwoLockNavy

@Composable
fun JinwoScreenScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to Color.White,
                    0.35f to Color.White,
                    1.0f to JinwoBlueSoft
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 460.dp),
            content = content
        )
    }
}

@Composable
fun CompanyLogo(
    logoUrl: String,
    companyName: String,
    large: Boolean = false,
    withHalo: Boolean = false,
    modifier: Modifier = Modifier
) {
    val size: Dp = if (large) 92.dp else 46.dp
    val cornerRadius: Dp = if (large) 28.dp else 15.dp
    var remoteLoaded by remember(logoUrl) { mutableStateOf(false) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (withHalo) {
            HaloRings(baseSize = size)
        }
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = if (large) 14.dp else 6.dp,
                    shape = RoundedCornerShape(cornerRadius),
                    ambientColor = JinwoBlue.copy(alpha = 0.45f),
                    spotColor = JinwoBlue.copy(alpha = 0.45f)
                )
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    Brush.linearGradient(
                        listOf(JinwoBlueGradientStart, JinwoBlueDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (logoUrl.isBlank() || !remoteLoaded) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_art_1794895848088),
                    contentDescription = companyName.ifBlank { "Company Logo" },
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (logoUrl.isNotBlank()) {
                AsyncImage(
                    model = logoUrl,
                    contentDescription = companyName.ifBlank { "Company Logo" },
                    contentScale = ContentScale.Crop,
                    onSuccess = { remoteLoaded = true },
                    onError = { remoteLoaded = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun HaloRings(baseSize: Dp) {
    val transition = rememberInfiniteTransition(label = "halo")
    val p1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p1"
    )
    val p2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "p2"
    )

    listOf(p1, p2).forEach { progress ->
        val scale = 0.9f + (0.45f * progress)
        val alpha = (1f - progress) * 0.35f
        Box(
            modifier = Modifier
                .size(baseSize + 28.dp)
                .scale(scale)
                .alpha(alpha)
                .border(1.5.dp, JinwoBlue, RoundedCornerShape(38.dp))
        )
    }
}

@Composable
fun PrimaryGradientButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    val alpha = if (enabled) 1f else 0.42f
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .alpha(alpha)
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = JinwoBlue.copy(alpha = 0.4f),
                        spotColor = JinwoBlue.copy(alpha = 0.4f)
                    )
                } else Modifier
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    listOf(JinwoBlueLight, JinwoBlueDark)
                )
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(Modifier.width(9.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun PhotoBackgroundContainer(
    backgroundUrl: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JinwoLockNavy),
        contentAlignment = Alignment.TopCenter
    ) {
        if (backgroundUrl.isNotBlank()) {
            AsyncImage(
                model = backgroundUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x590A1432),
                            Color(0xB80A1432)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 460.dp),
            content = content
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
fun WhiteCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = JinwoBlue.copy(alpha = 0.12f),
                spotColor = JinwoBlue.copy(alpha = 0.12f)
            ),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, JinwoBlueBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun ShieldCircleIcon(
    darkSurface: Boolean = false,
    icon: @Composable () -> Unit
) {
    val bgBrush = if (darkSurface) {
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.16f))
        )
    } else {
        Brush.linearGradient(
            listOf(Color.White, JinwoBlueSoft)
        )
    }
    val borderColor = if (darkSurface) Color.White.copy(alpha = 0.4f) else Color.White

    Box(
        modifier = Modifier
            .size(68.dp)
            .then(
                if (!darkSurface) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = JinwoBlue.copy(alpha = 0.24f),
                        spotColor = JinwoBlue.copy(alpha = 0.24f)
                    )
                } else Modifier
            )
            .clip(CircleShape)
            .background(bgBrush)
            .border(1.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}
