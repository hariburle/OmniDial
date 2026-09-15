package com.example.ui.screens
import com.example.ui.components.CallDurationStatusChip
import com.example.ui.components.InCallControlButton
import com.example.ui.components.SwipeUpAnswerView
import com.example.ui.components.ButtonTapAnswerView
import com.example.ui.components.SwipeSliderAnswerView

import android.telecom.Call
import android.telecom.CallAudioState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.telecom.ActiveCallInfo
import com.example.telecom.AutomationStep
import com.example.ui.components.Keypad
import com.example.ui.components.ReminderPresetChips
import com.example.util.ContactHelper
import kotlinx.coroutines.delay

@Composable
fun InCallScreen(
    callInfo: ActiveCallInfo,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    automationStep: AutomationStep?,
    lastDtmfKey: Char?,
    showKeypad: Boolean,
    onToggleKeypad: () -> Unit,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onDisconnect: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    audioRoute: Int = CallAudioState.ROUTE_EARPIECE,
    supportedAudioRoutes: Int = CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER,
    bluetoothDeviceName: String? = null,
    availableBluetoothDevices: List<com.example.telecom.BluetoothDeviceItem> = emptyList(),
    activeBluetoothDeviceAddress: String? = null,
    onSelectAudioRoute: (Int) -> Unit = {},
    onSelectBluetoothDevice: (String) -> Unit = {},
    onPlayDtmf: (Char) -> Unit,
    onStopDtmf: (Char) -> Unit,
    onDeclineWithSms: (String) -> Unit = {},
    onSavePostCallNote: ((note: String?, reminderMinutes: Long?) -> Unit)? = null,
    onMarkSpam: ((String) -> Unit)? = null,
    onUnblockSpam: ((String) -> Unit)? = null,
    onDismiss: () -> Unit = {},
    onClosePostCall: () -> Unit = onDismiss,
    callAnswerStyle: String = "swipe_slider",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(callInfo.id) {
        keyboardController?.hide()
        focusManager.clearFocus()
    }
    var enteredDtmfHistory by remember { mutableStateOf("") }
    var postCallNote by remember { mutableStateOf("") }
    var postCallReminderMins by remember { mutableStateOf<Long?>(null) }
    var noteSaved by remember { mutableStateOf(false) }
    var isUserInteractingWithNote by remember { mutableStateOf(false) }
    var autoCloseRemainingSeconds by remember { mutableIntStateOf(5) }
    var autoCloseTimerActive by remember { mutableStateOf(true) }
    var spamActionConfirmed by remember { mutableStateOf(false) }
    var showReportSpamDialog by remember { mutableStateOf(false) }
    var showAudioRouteSelector by remember { mutableStateOf(false) }

    LaunchedEffect(callInfo.state, isUserInteractingWithNote, noteSaved, autoCloseTimerActive) {
        if (callInfo.state == Call.STATE_DISCONNECTED && !isUserInteractingWithNote && !noteSaved && autoCloseTimerActive) {
            autoCloseRemainingSeconds = 5
            while (autoCloseRemainingSeconds > 0) {
                delay(1000)
                if (isUserInteractingWithNote || noteSaved || !autoCloseTimerActive) break
                autoCloseRemainingSeconds--
            }
            if (!isUserInteractingWithNote && !noteSaved && autoCloseTimerActive) {
                onClosePostCall()
            }
        }
    }

    LaunchedEffect(noteSaved) {
        if (noteSaved) {
            delay(1000)
            onClosePostCall()
        }
    }

    val isVoicemail = callInfo.displayName.equals("Voicemail", ignoreCase = true) ||
        ContactHelper.isVoicemailNumber(context, callInfo.phoneNumber)

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("in_call_screen")
            .let { baseModifier ->
                if (callInfo.state == Call.STATE_DISCONNECTED) {
                    baseModifier.pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                onClosePostCall()
                            }
                        )
                    }
                } else {
                    baseModifier
                }
            },
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Minimize button, Status, Caller details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                // Top row with minimize button and status chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("minimize_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Minimize call",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Isolated Call Duration Status Chip (prevents whole-screen recomposition)
                    CallDurationStatusChip(
                        callState = callInfo.state,
                        connectTimeMillis = callInfo.connectTimeMillis,
                        isAutomationRunning = automationStep?.isRunning == true,
                        onDismiss = onDismiss
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Caller Avatar (shows contact photo if available)
                Surface(
                    modifier = Modifier
                        .size(112.dp)
                        .testTag("in_call_avatar"),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shadowElevation = 4.dp
                ) {
                    if (!callInfo.photoUri.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = callInfo.photoUri,
                            contentDescription = "Caller Photo",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            if (isVoicemail) {
                                Icon(
                                    imageVector = Icons.Default.Voicemail,
                                    contentDescription = "Voicemail",
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else if (callInfo.displayName.isNotBlank() && callInfo.displayName != "Incoming Caller" && callInfo.displayName != "Calling...") {
                                Text(
                                    text = callInfo.displayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Caller",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Caller Name, Nickname, Label & Number
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = callInfo.displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    // Nickname badge if available and distinct from displayName
                    if (!callInfo.nickname.isNullOrBlank() && !callInfo.nickname.equals(callInfo.displayName, ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Nickname",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "\"${callInfo.nickname}\"",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Label and Number
                    val numberSubtitle = buildString {
                        if (!callInfo.numberLabel.isNullOrBlank()) {
                            append(callInfo.numberLabel)
                            if (callInfo.phoneNumber.isNotBlank()) {
                                append(" • ")
                            }
                        }
                        if (callInfo.phoneNumber.isNotBlank()) {
                            append(callInfo.phoneNumber)
                        }
                    }

                    if (numberSubtitle.isNotBlank()) {
                        Text(
                            text = numberSubtitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Contextual Caller ID ("Call Reason")
                    if (!callInfo.callReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Call Reason",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Reason: ${callInfo.callReason}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    // Live Community Caller ID Info
                    callInfo.communityInfo?.let { comm ->
                        Spacer(modifier = Modifier.height(6.dp))
                        val isSpam = comm.spamScore > 50
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSpam) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSpam) Icons.Default.Warning else Icons.Default.Verified,
                                    contentDescription = "Community Verified",
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSpam) Color(0xFFDC2626) else Color(0xFF0284C7)
                                )
                                Text(
                                    text = "${comm.verificationType} • ${comm.category}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpam) Color(0xFF991B1B) else Color(0xFF0369A1)
                                )
                            }
                        }
                    }
                }

                // Automation Step Banner (if rule matched and running)
                if (automationStep != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("automation_step_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "Automation",
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Auto-Engine: ${automationStep.ruleName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = automationStep.stepDescription,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (automationStep.isRunning) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }

                // DTMF feedback badge
                if (lastDtmfKey != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "DTMF Tone: $lastDtmfKey",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Middle Section: Optional DTMF Keypad
            AnimatedVisibility(
                visible = showKeypad,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    if (enteredDtmfHistory.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .testTag("incall_dtmf_history_badge")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Dialpad,
                                    contentDescription = "Keypad input",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = enteredDtmfHistory,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }
                    Keypad(
                        compact = true,
                        onDigitPress = { char ->
                            enteredDtmfHistory += char
                            onPlayDtmf(char)
                        },
                        onDigitRelease = { char ->
                            onStopDtmf(char)
                        }
                    )
                }
            }

            // Bottom Section: Audio controls & Call End/Answer buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                if (callInfo.state == Call.STATE_RINGING) {
                    // Incoming Call: Explicit Audio Route Selection (No Mute / Keypad before answering)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Audio Output",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Explicit Audio Route Options (Handset, Speaker, Car Bluetooth, Bluetooth Headset, etc.)
                        ExplicitAudioRoutesBar(
                            currentRoute = audioRoute,
                            supportedRoutes = supportedAudioRoutes,
                            bluetoothDeviceName = bluetoothDeviceName,
                            availableBluetoothDevices = availableBluetoothDevices,
                            activeBluetoothDeviceAddress = activeBluetoothDeviceAddress,
                            onSelectRoute = onSelectAudioRoute,
                            onSelectBluetoothDevice = onSelectBluetoothDevice
                        )
                    }

                    // Quick-Decline SMS Chips
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Quick Decline with SMS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val quickDeclineChips = listOf(
                            "Can't talk now. What's up?",
                            "In a meeting, text me.",
                            "I'll call you right back.",
                            "On my way."
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(quickDeclineChips) { chipText ->
                                SuggestionChip(
                                    onClick = { onDeclineWithSms(chipText) },
                                    label = {
                                        Text(
                                            text = chipText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.Message,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("quick_decline_chip")
                                )
                            }
                        }
                    }

                    // Incoming call Answering UI based on selected callAnswerStyle
                    when (callAnswerStyle) {
                        "swipe_up" -> {
                            SwipeUpAnswerView(
                                onAnswer = onAnswer,
                                onDecline = onDecline
                            )
                        }
                        "button_tap" -> {
                            ButtonTapAnswerView(
                                onAnswer = onAnswer,
                                onDecline = onDecline
                            )
                        }
                        else -> { // Default: "swipe_slider" / "horizontal_slide"
                            SwipeSliderAnswerView(
                                onAnswer = onAnswer,
                                onDecline = onDecline
                            )
                        }
                    }
                } else if (callInfo.state == Call.STATE_DISCONNECTED) {
                    // Post-Call Follow-up Card (Beautifully styled)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    // Consume tap inside the card
                                }
                            }
                            .testTag("post_call_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (noteSaved) Color(0xFF16A34A).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (noteSaved) Icons.Default.CheckCircle else Icons.Default.NoteAdd,
                                                contentDescription = null,
                                                tint = if (noteSaved) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = if (noteSaved) "Note & Reminder Saved!" else "Post-Call Note & Reminder",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!noteSaved) {
                                            Text(
                                                text = "Capture details while fresh in mind",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (!noteSaved && !isUserInteractingWithNote) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                progress = { autoCloseRemainingSeconds / 10f },
                                                modifier = Modifier.size(10.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "${autoCloseRemainingSeconds}s",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            if (!noteSaved) {
                                // Smart After-Call Quick Actions (Spam Defense & Quick Response)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Block & Report Spam Action
                                    OutlinedButton(
                                        onClick = {
                                            autoCloseTimerActive = false
                                            if (callInfo.phoneNumber.isNotBlank()) {
                                                if (spamActionConfirmed) {
                                                    onUnblockSpam?.invoke(callInfo.phoneNumber)
                                                    spamActionConfirmed = false
                                                } else {
                                                    onMarkSpam?.invoke(callInfo.phoneNumber)
                                                    spamActionConfirmed = true
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = if (spamActionConfirmed) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (spamActionConfirmed) Color(0xFF16A34A).copy(alpha = 0.5f) else MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.weight(1f).testTag("post_call_spam_block_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (spamActionConfirmed) Icons.Default.Check else Icons.Default.Block,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (spamActionConfirmed) "Blocked (Unblock)" else "Block Spam",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }

                                    // Detailed Report Tag Option
                                    OutlinedButton(
                                        onClick = {
                                            autoCloseTimerActive = false
                                            showReportSpamDialog = true
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.weight(1f).testTag("post_call_report_tag_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Tag / Report",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // Text Input Field
                                OutlinedTextField(
                                    value = postCallNote,
                                    onValueChange = {
                                        postCallNote = it
                                        isUserInteractingWithNote = true
                                        autoCloseTimerActive = false
                                    },
                                    placeholder = {
                                        Text(
                                            "e.g., Send proposal by tomorrow, follow up on pricing...",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    trailingIcon = {
                                        if (postCallNote.isNotEmpty()) {
                                            IconButton(onClick = { postCallNote = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear note text",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focusState ->
                                            if (focusState.isFocused) {
                                                isUserInteractingWithNote = true
                                                autoCloseTimerActive = false
                                            }
                                        }
                                        .testTag("post_call_note_input"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    singleLine = false,
                                    maxLines = 3
                                )

                                // Reminder Chips Section
                                ReminderPresetChips(
                                    selectedMinutes = postCallReminderMins,
                                    onSelectMinutes = { mins ->
                                        postCallReminderMins = mins
                                        isUserInteractingWithNote = true
                                        autoCloseTimerActive = false
                                    }
                                )

                                // Action Buttons Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            if (postCallNote.isNotBlank()) {
                                                onSavePostCallNote?.invoke(postCallNote, postCallReminderMins)
                                            }
                                            onClosePostCall()
                                        }
                                    ) {
                                        Text(
                                            text = if (isUserInteractingWithNote) "Discard" else "Skip",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            noteSaved = true
                                            onSavePostCallNote?.invoke(postCallNote, postCallReminderMins)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("save_post_call_note_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save & Close", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Active or Outgoing Call:
                    // 1. Controls Row: Mute, Keypad
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mute
                        InCallControlButton(
                            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (isMuted) "Unmute" else "Mute",
                            isActive = isMuted,
                            onClick = onToggleMute,
                            testTag = "incall_mute_button"
                        )

                        // Keypad
                        InCallControlButton(
                            icon = Icons.Default.Dialpad,
                            label = if (showKeypad) "Hide Keypad" else "Keypad",
                            isActive = showKeypad,
                            onClick = onToggleKeypad,
                            testTag = "incall_keypad_button"
                        )
                    }

                    // 2. Explicit Audio Routes Bar (Direct one-tap switching)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ExplicitAudioRoutesBar(
                            currentRoute = audioRoute,
                            supportedRoutes = supportedAudioRoutes,
                            bluetoothDeviceName = bluetoothDeviceName,
                            availableBluetoothDevices = availableBluetoothDevices,
                            activeBluetoothDeviceAddress = activeBluetoothDeviceAddress,
                            onSelectRoute = onSelectAudioRoute,
                            onSelectBluetoothDevice = onSelectBluetoothDevice
                        )
                    }

                    // 3. End Call Button (Red)
                    FilledIconButton(
                        onClick = onDisconnect,
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("incall_end_call_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }

        if (showReportSpamDialog) {
            AlertDialog(
                onDismissRequest = { showReportSpamDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        text = "Report & Block Number",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Add ${callInfo.phoneNumber.ifBlank { "this number" }} to spam database and block future calls?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "This will immediately silence and block incoming calls from this caller using E.164 smart matching.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showReportSpamDialog = false
                            spamActionConfirmed = true
                            if (callInfo.phoneNumber.isNotBlank()) {
                                onMarkSpam?.invoke(callInfo.phoneNumber)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Block & Report")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportSpamDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun ExplicitAudioRoutesBar(
    currentRoute: Int,
    supportedRoutes: Int,
    bluetoothDeviceName: String?,
    availableBluetoothDevices: List<com.example.telecom.BluetoothDeviceItem>,
    activeBluetoothDeviceAddress: String?,
    onSelectRoute: (Int) -> Unit,
    onSelectBluetoothDevice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    data class DisplayRoute(
        val key: String,
        val label: String,
        val subLabel: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val isActive: Boolean,
        val onClick: () -> Unit
    )

    val items = mutableListOf<DisplayRoute>()

    // 1. Phone / Handset (Earpiece)
    val isEarpieceActive = (currentRoute == CallAudioState.ROUTE_EARPIECE)
    items.add(
        DisplayRoute(
            key = "earpiece",
            label = "Handset",
            subLabel = "Earpiece",
            icon = Icons.Default.PhoneAndroid,
            isActive = isEarpieceActive,
            onClick = { onSelectRoute(CallAudioState.ROUTE_EARPIECE) }
        )
    )

    // 2. Speakerphone
    val isSpeakerActive = (currentRoute == CallAudioState.ROUTE_SPEAKER)
    items.add(
        DisplayRoute(
            key = "speaker",
            label = "Speaker",
            subLabel = "Loudspeaker",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            isActive = isSpeakerActive,
            onClick = { onSelectRoute(CallAudioState.ROUTE_SPEAKER) }
        )
    )

    // 3. Bluetooth devices (all connected devices listed individually with specific icons)
    if (availableBluetoothDevices.isNotEmpty()) {
        availableBluetoothDevices.forEach { dev ->
            val isDevActive = currentRoute == CallAudioState.ROUTE_BLUETOOTH &&
                    (activeBluetoothDeviceAddress == null || activeBluetoothDeviceAddress == dev.address || availableBluetoothDevices.size == 1)
            val icon = when {
                dev.isCar -> Icons.Default.DirectionsCar
                dev.isHeadphone -> Icons.Default.Headphones
                else -> Icons.Default.BluetoothAudio
            }
            val subLabel = when {
                dev.isCar -> "Car Audio"
                dev.isHeadphone -> "Headphones"
                else -> "Bluetooth"
            }
            items.add(
                DisplayRoute(
                    key = "bt_${dev.address}",
                    label = dev.name,
                    subLabel = subLabel,
                    icon = icon,
                    isActive = isDevActive,
                    onClick = { onSelectBluetoothDevice(dev.address) }
                )
            )
        }
    } else if ((supportedRoutes and CallAudioState.ROUTE_BLUETOOTH) != 0 || bluetoothDeviceName != null) {
        val isBtActive = (currentRoute == CallAudioState.ROUTE_BLUETOOTH)
        val name = bluetoothDeviceName ?: "Bluetooth Device"
        val isCar = name.lowercase().contains("car") || name.lowercase().contains("vehicle") || name.lowercase().contains("handsfree")
        val isHeadphone = name.lowercase().contains("buds") || name.lowercase().contains("pod") || name.lowercase().contains("head")
        val icon = when {
            isCar -> Icons.Default.DirectionsCar
            isHeadphone -> Icons.Default.Headphones
            else -> Icons.Default.BluetoothAudio
        }
        val subLabel = when {
            isCar -> "Car Audio"
            isHeadphone -> "Headphones"
            else -> "Bluetooth"
        }
        items.add(
            DisplayRoute(
                key = "bluetooth",
                label = name,
                subLabel = subLabel,
                icon = icon,
                isActive = isBtActive,
                onClick = { onSelectRoute(CallAudioState.ROUTE_BLUETOOTH) }
            )
        )
    }

    // 4. Wired Headset (if connected)
    if ((supportedRoutes and CallAudioState.ROUTE_WIRED_HEADSET) != 0) {
        val isWiredActive = (currentRoute == CallAudioState.ROUTE_WIRED_HEADSET)
        items.add(
            DisplayRoute(
                key = "wired",
                label = "Headset",
                subLabel = "Wired",
                icon = Icons.Default.Headphones,
                isActive = isWiredActive,
                onClick = { onSelectRoute(CallAudioState.ROUTE_WIRED_HEADSET) }
            )
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { route ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (route.isActive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                },
                border = if (route.isActive) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                },
                shadowElevation = if (route.isActive) 3.dp else 0.dp,
                modifier = Modifier
                    .clickable(onClick = route.onClick)
                    .testTag("audio_route_${route.key}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Icon Container
                    Surface(
                        shape = CircleShape,
                        color = if (route.isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = route.icon,
                                contentDescription = route.label,
                                tint = if (route.isActive) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Text Info
                    Column {
                        Text(
                            text = route.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (route.isActive) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (route.isActive) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1
                        )
                        Text(
                            text = route.subLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (route.isActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                    }

                    if (route.isActive) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

