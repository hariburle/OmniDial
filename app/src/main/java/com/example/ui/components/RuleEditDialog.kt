package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CallerRule
import com.example.data.FavoriteContact
import com.example.util.DeviceContact

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RuleEditDialog(
    initialRule: CallerRule,
    favorites: List<FavoriteContact> = emptyList(),
    deviceContacts: List<DeviceContact> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (CallerRule) -> Unit
) {
    var name by remember { mutableStateOf(initialRule.name) }
    var pattern by remember { mutableStateOf(initialRule.phoneNumberPattern) }
    var autoAnswer by remember { mutableStateOf(initialRule.autoAnswer) }
    var answerDelaySec by remember { mutableStateOf(initialRule.answerDelaySec.toString()) }
    var dtmfSequence by remember { mutableStateOf(initialRule.dtmfSequence) }
    var dtmfDelayMs by remember { mutableStateOf(initialRule.dtmfDelayMs.toString()) }
    var sendSms by remember { mutableStateOf(initialRule.sendSms) }
    var smsMessage by remember { mutableStateOf(initialRule.smsMessage) }
    var autoHangup by remember { mutableStateOf(initialRule.autoHangup) }
    var hangupDelaySec by remember { mutableStateOf(initialRule.hangupDelaySec.toString()) }
    var autoSpeakerphone by remember { mutableStateOf(initialRule.autoSpeakerphone) }
    var autoMuteMic by remember { mutableStateOf(initialRule.autoMuteMic) }
    var requiredWifiSsid by remember { mutableStateOf(initialRule.requiredWifiSsid) }
    var requiredBluetoothDevice by remember { mutableStateOf(initialRule.requiredBluetoothDevice) }
    var showContactPicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
                .systemBarsPadding()
                .imePadding(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Header Bar with Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (initialRule.id == 0L) Icons.Default.AddCircle else Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (initialRule.id == 0L) "New Caller Rule" else "Edit Caller Rule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Configure automatic in-call workflow",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Scrollable Content Area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Live Interactive Pipeline Preview
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Route,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Workflow Pipeline Preview",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PipelineStepChip(label = "Ringing", icon = Icons.Default.RingVolume, color = MaterialTheme.colorScheme.primary)
                                if (autoAnswer) {
                                    PipelineStepChip(label = "Answer (${answerDelaySec.ifBlank { "0" }}s)", icon = Icons.Default.Phone, color = Color(0xFF10B981))
                                }
                                if (autoMuteMic) {
                                    PipelineStepChip(label = "Mute Mic", icon = Icons.Default.MicOff, color = MaterialTheme.colorScheme.error)
                                }
                                if (dtmfSequence.isNotBlank()) {
                                    PipelineStepChip(label = "DTMF '$dtmfSequence'", icon = Icons.Default.Dialpad, color = Color(0xFF3B82F6))
                                }
                                if (sendSms) {
                                    PipelineStepChip(label = "Auto SMS", icon = Icons.AutoMirrored.Filled.Chat, color = Color(0xFF8B5CF6))
                                }
                                if (autoHangup) {
                                    PipelineStepChip(label = "Hangup (${hangupDelaySec.ifBlank { "0" }}s)", icon = Icons.Default.CallEnd, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }

                    // 2. Quick Presets
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Quick Presets",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SuggestionChip(
                                onClick = {
                                    name = "Gate Buzzer"
                                    autoAnswer = true
                                    answerDelaySec = "1"
                                    dtmfSequence = "9#"
                                    autoSpeakerphone = true
                                    autoMuteMic = true
                                    autoHangup = true
                                    hangupDelaySec = "2"
                                },
                                label = { Text("Gate DTMF (9#)") },
                                icon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                            )
                            SuggestionChip(
                                onClick = {
                                    name = "Office Extension"
                                    autoAnswer = true
                                    answerDelaySec = "2"
                                    dtmfSequence = "104#"
                                    autoHangup = false
                                },
                                label = { Text("Extension DTMF") },
                                icon = {
                                    Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                            )
                            SuggestionChip(
                                onClick = {
                                    name = "Auto SMS Responder"
                                    autoAnswer = false
                                    sendSms = true
                                    smsMessage = "I am currently busy. I will call you back shortly."
                                    autoHangup = true
                                    hangupDelaySec = "1"
                                },
                                label = { Text("SMS Auto-Reply") },
                                icon = {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                            )
                            SuggestionChip(
                                onClick = {
                                    name = "Delivery Gate Access"
                                    autoAnswer = true
                                    answerDelaySec = "1"
                                    dtmfSequence = "4#"
                                    sendSms = true
                                    smsMessage = "Lobby gate opened automatically."
                                    autoHangup = true
                                    hangupDelaySec = "2"
                                    autoSpeakerphone = true
                                    autoMuteMic = true
                                },
                                label = { Text("Delivery Gate (4#)") },
                                icon = {
                                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                            )
                            SuggestionChip(
                                onClick = {
                                    name = "Voicemail PIN"
                                    autoAnswer = false
                                    answerDelaySec = "0"
                                    dtmfSequence = "1234#"
                                    sendSms = false
                                    autoHangup = false
                                },
                                label = { Text("Voicemail PIN") },
                                icon = {
                                    Icon(Icons.Default.Voicemail, contentDescription = null, modifier = Modifier.size(15.dp))
                                }
                            )
                        }
                    }

                    // 3. Identification Section Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Rule Identification & Target",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Rule Name") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rule_name_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = pattern,
                                    onValueChange = { pattern = it },
                                    label = { Text("Phone Number Pattern (* for any)") },
                                    placeholder = { Text("e.g. 5550199 or 1800*") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp))
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("rule_pattern_input")
                                )
                                FilledTonalIconButton(
                                    onClick = { showContactPicker = true },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .size(52.dp)
                                        .testTag("rule_pick_contact_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonSearch,
                                        contentDescription = "Pick Contact",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // 4. Automated Actions Section Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Call Answering & DTMF Touch-Tones",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            // Auto Answer Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Answer Incoming Call", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Picks up the line automatically",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoAnswer,
                                    onCheckedChange = { autoAnswer = it }
                                )
                            }

                            if (autoAnswer) {
                                OutlinedTextField(
                                    value = answerDelaySec,
                                    onValueChange = { answerDelaySec = it },
                                    label = { Text("Answer Delay (seconds)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = {
                                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // DTMF Sequence
                            OutlinedTextField(
                                value = dtmfSequence,
                                onValueChange = { dtmfSequence = it },
                                label = { Text("In-Band DTMF Key Sequence") },
                                placeholder = { Text("e.g. 9# or 104# or 1234#") },
                                leadingIcon = {
                                    Icon(Icons.Default.Dialpad, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rule_dtmf_input")
                            )

                            if (dtmfSequence.isNotEmpty()) {
                                OutlinedTextField(
                                    value = dtmfDelayMs,
                                    onValueChange = { dtmfDelayMs = it },
                                    label = { Text("DTMF Start Delay (milliseconds)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = {
                                        Icon(Icons.Default.HourglassBottom, contentDescription = null, modifier = Modifier.size(20.dp))
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Auto Hangup Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Hangup Call", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Disconnects automatically after actions execute",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoHangup,
                                    onCheckedChange = { autoHangup = it }
                                )
                            }

                            if (autoHangup) {
                                OutlinedTextField(
                                    value = hangupDelaySec,
                                    onValueChange = { hangupDelaySec = it },
                                    label = { Text("Hangup Delay (seconds after actions)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    leadingIcon = {
                                        Icon(Icons.Default.TimerOff, contentDescription = null, modifier = Modifier.size(20.dp))
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 5. Audio & Microphone Controls
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Audio & Mic Controls",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-Route to Speakerphone", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Enables hands-free speaker on connect",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoSpeakerphone,
                                    onCheckedChange = { autoSpeakerphone = it }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Mute Mic During DTMF Tones", fontWeight = FontWeight.Medium)
                                    Text(
                                        "Prevents background noise during buzzer tones",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = autoMuteMic,
                                    onCheckedChange = { autoMuteMic = it }
                                )
                            }
                        }
                    }

                    // 6. SMS Auto-Reply Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Send Auto-Reply SMS", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Dispatches a text message to caller",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = sendSms,
                                    onCheckedChange = { sendSms = it }
                                )
                            }

                            if (sendSms) {
                                OutlinedTextField(
                                    value = smsMessage,
                                    onValueChange = { smsMessage = it },
                                    label = { Text("SMS Message Content") },
                                    placeholder = { Text("e.g. I am in a meeting, will call you back.") },
                                    leadingIcon = {
                                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // 7. Ambient Geofence & Device Guards
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Ambient Geofence Guards (Optional)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Execute only when connected to designated Wi-Fi or Bluetooth (0% battery drain).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = requiredWifiSsid,
                                onValueChange = { requiredWifiSsid = it },
                                label = { Text("Required Wi-Fi SSID") },
                                placeholder = { Text("e.g. Home_5G or Office_Network") },
                                leadingIcon = {
                                    Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = requiredBluetoothDevice,
                                onValueChange = { requiredBluetoothDevice = it },
                                label = { Text("Required Bluetooth Device") },
                                placeholder = { Text("e.g. Tesla Model 3 or Car BT") },
                                leadingIcon = {
                                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(20.dp))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Bottom Sticky Action Bar with Prominent Save & Cancel Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            val finalRule = initialRule.copy(
                                name = name.ifBlank { "Caller Rule" },
                                phoneNumberPattern = pattern.trim(),
                                autoAnswer = autoAnswer,
                                answerDelaySec = answerDelaySec.toIntOrNull() ?: 1,
                                dtmfSequence = dtmfSequence.trim(),
                                dtmfDelayMs = dtmfDelayMs.toLongOrNull() ?: 800L,
                                sendSms = sendSms,
                                smsMessage = smsMessage.trim(),
                                autoHangup = autoHangup,
                                hangupDelaySec = hangupDelaySec.toIntOrNull() ?: 2,
                                autoSpeakerphone = autoSpeakerphone,
                                autoMuteMic = autoMuteMic,
                                requiredWifiSsid = requiredWifiSsid.trim(),
                                requiredBluetoothDevice = requiredBluetoothDevice.trim()
                            )
                            onSave(finalRule)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("rule_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (initialRule.id == 0L) "Create Rule" else "Save Changes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    if (showContactPicker) {
        ContactPickerDialog(
            favorites = favorites,
            deviceContacts = deviceContacts,
            onContactSelected = { contactName, contactNumber, _ ->
                pattern = contactNumber
                if (name.isBlank() || name == "New Automation Rule" || name == "New Caller Rule") {
                    name = "$contactName Rule"
                }
            },
            onDismiss = { showContactPicker = false },
            title = "Select Contact for Rule"
        )
    }
}

@Composable
private fun PipelineStepChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
