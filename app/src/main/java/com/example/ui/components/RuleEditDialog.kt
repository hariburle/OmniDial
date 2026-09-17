package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CallerRule
import com.example.data.FavoriteContact
import com.example.util.DeviceContact
import com.example.ui.components.ContactPickerDialog

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialRule.id == 0L) "New Caller Automation Rule" else "Edit Automation Rule")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Quick Presets:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
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
                        label = { Text("Gate DTMF (9#)") }
                    )
                    AssistChip(
                        onClick = {
                            name = "IVR Office Extension"
                            autoAnswer = true
                            answerDelaySec = "2"
                            dtmfSequence = "104#"
                            autoHangup = false
                        },
                        label = { Text("Extension DTMF") }
                    )
                    AssistChip(
                        onClick = {
                            name = "Auto SMS Responder"
                            autoAnswer = false
                            sendSms = true
                            smsMessage = "I am currently busy. I will call you back shortly."
                            autoHangup = true
                            hangupDelaySec = "1"
                        },
                        label = { Text("SMS Auto-Reply") }
                    )
                    AssistChip(
                        onClick = {
                            name = "Delivery Access Auto-Grant"
                            autoAnswer = true
                            answerDelaySec = "1"
                            dtmfSequence = "4#"
                            sendSms = true
                            smsMessage = "Lobby gate opened automatically."
                            autoHangup = true
                            hangupDelaySec = "2"
                        },
                        label = { Text("Delivery Gate (4#)") }
                    )
                    AssistChip(
                        onClick = {
                            name = "Voicemail Auto PIN"
                            autoAnswer = false
                            answerDelaySec = "0"
                            dtmfSequence = "1234#"
                            sendSms = false
                            autoHangup = false
                        },
                        label = { Text("Voicemail PIN") }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rule_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text("Number Pattern (or * for any)") },
                        placeholder = { Text("e.g. 5550199 or 18005550100") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("rule_pattern_input")
                    )
                    IconButton(
                        onClick = { showContactPicker = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("rule_pick_contact_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactPhone,
                            contentDescription = "Pick Contact",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-Answer via Call.answer(0)")
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
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = dtmfSequence,
                    onValueChange = { dtmfSequence = it },
                    label = { Text("In-Band DTMF Key Sequence") },
                    placeholder = { Text("e.g. 9# or 104# or 1*23") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rule_dtmf_input")
                )

                if (dtmfSequence.isNotEmpty()) {
                    OutlinedTextField(
                        value = dtmfDelayMs,
                        onValueChange = { dtmfDelayMs = it },
                        label = { Text("DTMF Start Delay (milliseconds)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Send Auto-Reply SMS")
                    Switch(
                        checked = sendSms,
                        onCheckedChange = { sendSms = it }
                    )
                }

                if (sendSms) {
                    OutlinedTextField(
                        value = smsMessage,
                        onValueChange = { smsMessage = it },
                        label = { Text("SMS Message Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-Hangup via Call.disconnect()")
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
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Auto-Route to Speakerphone")
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
                    Text("Mute Mic during DTMF Tones")
                    Switch(
                        checked = autoMuteMic,
                        onCheckedChange = { autoMuteMic = it }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                Text(
                    text = "Ambient Geofence Guards (Optional):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = requiredWifiSsid,
                    onValueChange = { requiredWifiSsid = it },
                    label = { Text("Required Wi-Fi SSID (Zero-Battery Guard)") },
                    placeholder = { Text("e.g. Home_5G or Office_Wi-Fi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = requiredBluetoothDevice,
                    onValueChange = { requiredBluetoothDevice = it },
                    label = { Text("Required Bluetooth Device (e.g. Car)") },
                    placeholder = { Text("e.g. Tesla or Car Bluetooth") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
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
                modifier = Modifier.testTag("rule_save_button")
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showContactPicker) {
        ContactPickerDialog(
            favorites = favorites,
            deviceContacts = deviceContacts,
            onContactSelected = { contactName, contactNumber, _ ->
                pattern = contactNumber
                if (name.isBlank() || name == "New Automation Rule") {
                    name = "$contactName Rule"
                }
            },
            onDismiss = { showContactPicker = false },
            title = "Select Contact for Rule"
        )
    }
}
