package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.DeviceTask
import com.example.data.UiState
import com.example.ui.theme.JinwoBlue
import com.example.ui.theme.JinwoBlueBorder
import com.example.ui.theme.JinwoBlueSoft
import com.example.ui.theme.JinwoBodyText
import com.example.ui.theme.JinwoDenyBg
import com.example.ui.theme.JinwoError
import com.example.ui.theme.JinwoInk
import com.example.ui.theme.JinwoMuted
import com.example.ui.theme.JinwoOk
import com.example.ui.theme.JinwoOkSoft

@Composable
fun ActiveScreen(
    uiState: EmiUiState,
    onTaskClick: (DeviceTask) -> Unit
) {
    val companyName = uiState.config.companyName.ifBlank { stringResource(R.string.default_company_name) }
    val activeMessage = uiState.config.activeMessage.ifBlank {
        stringResource(R.string.default_active_message)
    }

    val liveTransition = rememberInfiniteTransition(label = "liveDot")
    val dotScale by liveTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotScale"
    )

    JinwoScreenScaffold {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                CompanyLogo(
                    logoUrl = uiState.config.logoUrl,
                    companyName = companyName,
                    large = false
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = companyName,
                    style = MaterialTheme.typography.titleSmall,
                    color = JinwoInk,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(22.dp))
                ShieldCircleIcon {
                    Icon(
                        imageVector = Icons.Rounded.VerifiedUser,
                        contentDescription = stringResource(R.string.active_badge),
                        tint = JinwoBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(R.string.active_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = JinwoInk,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.active_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = JinwoMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(99.dp),
                    color = JinwoOkSoft
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .scale(dotScale)
                                .clip(CircleShape)
                                .background(JinwoOk)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(
                            text = stringResource(R.string.active_badge),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = JinwoOk
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                WhiteCard {
                    Text(
                        text = activeMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = JinwoBodyText
                    )
                }

                if (!uiState.isBackendConfigured) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = JinwoBlueSoft,
                        border = BorderStroke(1.dp, JinwoBlueBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.firebase_not_configured),
                            style = MaterialTheme.typography.bodySmall,
                            color = JinwoInk,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            when (val tasksState = uiState.tasksState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = JinwoBlue,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }

                is UiState.Error -> {
                    item {
                        Text(
                            text = tasksState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = JinwoError,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }

                is UiState.Success -> {
                    val tasks = tasksState.data
                    if (tasks.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.tasks_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = JinwoMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 18.dp)
                            )
                        }
                    } else {
                        items(tasks, key = { it.id }) { task ->
                            TaskRowCard(
                                task = task,
                                onClick = { onTaskClick(task) }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRowCard(
    task: DeviceTask,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = JinwoBlue.copy(alpha = 0.08f),
                spotColor = JinwoBlue.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("task_item_card_${task.id}"),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, JinwoBlueBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(JinwoBlueSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckBox,
                    contentDescription = task.name,
                    tint = JinwoBlue,
                    modifier = Modifier.size(22.dp)
                )
                if (task.iconUrl.isNotBlank()) {
                    AsyncImage(
                        model = task.iconUrl,
                        contentDescription = task.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.5.sp),
                    color = JinwoInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = JinwoMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = if (task.isDone) JinwoOkSoft else JinwoBlueSoft
            ) {
                Text(
                    text = stringResource(
                        if (task.isDone) R.string.task_status_done else R.string.task_status_todo
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.isDone) JinwoOk else JinwoBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TaskDetailScreen(
    task: DeviceTask,
    isWorking: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onCompleteTask: () -> Unit
) {
    val context = LocalContext.current
    var showPermissionSheet by remember { mutableStateOf(false) }
    var permissionDeniedNote by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = true) {
        if (showPermissionSheet) {
            showPermissionSheet = false
        } else {
            onBack()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNote = null
            onCompleteTask()
        } else {
            permissionDeniedNote = "অনুমতি ছাড়া এই টাস্ক সম্পন্ন হবে না। আবার চেষ্টা করুন।"
        }
    }

    // Play Policy compliant zero-permission Android Photo Picker for gallery tasks
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            permissionDeniedNote = null
            onCompleteTask()
        } else {
            permissionDeniedNote = "ছবি বাছাই বাতিল করা হয়েছে। আবার চেষ্টা করুন।"
        }
    }

    JinwoScreenScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(JinwoBlueSoft)
                            .testTag("task_detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = JinwoBlue
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = task.name,
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp),
                        color = JinwoInk,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 12.dp)
                ) {
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = JinwoMuted
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        task.steps.forEachIndexed { index, stepText ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, JinwoBlueBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (task.isDone) JinwoOk else JinwoBlueSoft),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (task.isDone) "✓" else "${index + 1}",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (task.isDone) Color.White else JinwoBlue
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = stepText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = JinwoBodyText,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    val note = permissionDeniedNote ?: errorMessage
                    if (!note.isNullOrBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = JinwoError
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                PrimaryGradientButton(
                    text = when {
                        task.isDone -> stringResource(R.string.task_done_btn)
                        isWorking -> stringResource(R.string.task_processing)
                        else -> stringResource(R.string.start_button)
                    },
                    enabled = !task.isDone && !isWorking,
                    modifier = Modifier.testTag("task_start_button"),
                    leadingIcon = {
                        Icon(
                            imageVector = if (task.isDone) Icons.Rounded.Check else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    onClick = {
                        permissionDeniedNote = null
                        val perm = task.requiresPermission?.lowercase()
                        when (perm) {
                            "camera" -> {
                                val alreadyGranted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (alreadyGranted) {
                                    onCompleteTask()
                                } else {
                                    showPermissionSheet = true
                                }
                            }

                            "gallery" -> {
                                showPermissionSheet = true
                            }

                            else -> {
                                onCompleteTask()
                            }
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = showPermissionSheet,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                val isCamera = task.requiresPermission.equals("camera", ignoreCase = true)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x8C0A0F1E))
                        .clickable(onClick = { showPermissionSheet = false }),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = false) {}
                    ) {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 22.dp, vertical = 22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(JinwoBlueSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isCamera) Icons.Rounded.CameraAlt else Icons.Rounded.PhotoLibrary,
                                    contentDescription = null,
                                    tint = JinwoBlue,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = stringResource(
                                    if (isCamera) R.string.camera_perm_title else R.string.gallery_perm_title
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = JinwoInk,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = stringResource(
                                    if (isCamera) R.string.camera_perm_desc else R.string.gallery_perm_desc
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = JinwoMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(18.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showPermissionSheet = false },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = JinwoDenyBg,
                                        contentColor = JinwoMuted
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("task_perm_deny_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.perm_deny_action),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Button(
                                    onClick = {
                                        showPermissionSheet = false
                                        if (isCamera) {
                                            cameraLauncher.launch(Manifest.permission.CAMERA)
                                        } else {
                                            galleryPickerLauncher.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = JinwoBlue,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("task_perm_allow_button")
                                ) {
                                    Text(
                                        text = stringResource(R.string.perm_allow_action),
                                        fontWeight = FontWeight.Bold
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
