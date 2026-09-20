package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.example.domain.model.CallingChannel
import com.example.telecom.SimInfo
import com.example.ui.CallMethodChoicePrompt
import com.example.ui.CloudContactConfirmation
import com.example.ui.SimChoicePrompt

/**
 * Dialog prompting user to confirm cloud modifications to Google Contacts.
 */
@Composable
fun CloudContactSyncDialog(
    confirmation: CloudContactConfirmation,
    onConfirm: () -> Unit,
    onSecondary: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = confirmation.title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = confirmation.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(confirmation.confirmButtonText)
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (confirmation.secondaryButtonText != null && onSecondary != null) {
                    FilledTonalButton(onClick = onSecondary) {
                        Text(confirmation.secondaryButtonText)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(confirmation.dismissButtonText)
                }
            }
        }
    )
}

/**
 * Dialog prompting the user to choose between Cellular and WhatsApp call channels.
 */
@Composable
fun WhatsAppChoiceDialog(
    prompt: CallMethodChoicePrompt,
    onChooseMethod: (method: String, remember: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var rememberChoice by remember(prompt) { mutableStateOf(prompt.isLearnMode) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val discoveryManager = remember(context) { com.example.telecom.ChannelDiscoveryManager.getInstance(context) }
    val availableChannels by discoveryManager.availableChannels.collectAsState()

    val cellularChannel = availableChannels.firstOrNull { it is CallingChannel.CellularSim }
    val cellularLabel = cellularChannel?.shortLabel ?: "Cellular"
    val isCellularAvailable = cellularChannel != null

    val waChannel = availableChannels.firstOrNull { it is CallingChannel.WhatsApp }
    val waLabel = waChannel?.shortLabel ?: "WhatsApp"
    val isWaAvailable = waChannel != null

    val gvChannel = availableChannels.firstOrNull { it is CallingChannel.GoogleVoice }
    val gvLabel = gvChannel?.shortLabel ?: "Google Voice"
    val isGvAvailable = gvChannel != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Calling Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    if (!prompt.contactName.isNullOrBlank()) {
                        Text(
                            text = prompt.contactName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = prompt.number,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isCellularAvailable && isWaAvailable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onChooseMethod("cellular", rememberChoice) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) {
                                Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(cellularLabel)
                            }

                            Button(
                                onClick = { onChooseMethod("whatsapp", rememberChoice) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                WhatsAppIcon(modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(waLabel, color = Color.White)
                            }
                        }
                    } else if (isCellularAvailable) {
                        Button(
                            onClick = { onChooseMethod("cellular", rememberChoice) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(cellularLabel)
                        }
                    } else if (isWaAvailable) {
                        Button(
                            onClick = { onChooseMethod("whatsapp", rememberChoice) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            WhatsAppIcon(modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(waLabel, color = Color.White)
                        }
                    }

                    if (isGvAvailable) {
                        Button(
                            onClick = { onChooseMethod("google_voice", rememberChoice) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F9D58))
                        ) {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(gvLabel, color = Color.White)
                        }
                    }
                }

                if (prompt.isLearnMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { rememberChoice = !rememberChoice }
                    ) {
                        Checkbox(
                            checked = rememberChoice,
                            onCheckedChange = { rememberChoice = it }
                        )
                        Text(
                            text = "Remember choice for this contact",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog prompting user to select which SIM card to place an outgoing call from.
 */
@Composable
fun SimChoiceDialog(
    prompt: SimChoicePrompt,
    activeSims: List<SimInfo>,
    onSelectSim: (slot: Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SimCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Choose SIM for Call",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val nameOrNum = prompt.contactName ?: prompt.number
                Text(
                    text = "Call $nameOrNum (${prompt.number}) using:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val sim1 = activeSims.firstOrNull { it.slotIndex == 0 }
                    val sim2 = activeSims.firstOrNull { it.slotIndex == 1 }

                    val sim1Title = sim1?.displayName?.takeIf { it.isNotBlank() } ?: "SIM 1"
                    val sim2Title = sim2?.displayName?.takeIf { it.isNotBlank() } ?: "SIM 2"

                    if (sim1 != null) {
                        Button(
                            onClick = { onSelectSim(1) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(sim1Title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (sim1.carrierName.isNotBlank() && sim1.carrierName != sim1Title) {
                                    Text(
                                        text = sim1.carrierName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (sim2 != null) {
                        Button(
                            onClick = { onSelectSim(2) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(sim2Title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (sim2.carrierName.isNotBlank() && sim2.carrierName != sim2Title) {
                                    Text(
                                        text = sim2.carrierName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Smart Multi-Channel Call Dialog ("Always Ask" Mode or Keypad Long-Press)
 * Displays discovered channels with branded cards, icons, and optional "Remember choice" checkbox.
 */
@Composable
fun MultiChannelChoiceDialog(
    phoneNumber: String,
    contactName: String? = null,
    channels: List<CallingChannel>,
    initialRememberChoice: Boolean = false,
    showRememberChoice: Boolean = true,
    onSelectChannel: (channel: CallingChannel, remember: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var rememberChoice by remember(phoneNumber, initialRememberChoice) { mutableStateOf(initialRememberChoice) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Choose Calling Channel",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val displayName = contactName ?: phoneNumber
                Text(
                    text = "Call $displayName using:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                val displayChannels = if (channels.isEmpty()) listOf(CallingChannel.SystemDefault) else channels

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    displayChannels.forEach { channel ->
                        val brandColor = Color(channel.brandColorHex)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = brandColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, brandColor.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectChannel(channel, if (showRememberChoice) rememberChoice else false) }
                                .testTag("channel_dialog_option_${channel.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                when (channel) {
                                    is CallingChannel.CellularSim -> {
                                        Icon(
                                            imageVector = Icons.Default.SimCard,
                                            contentDescription = null,
                                            tint = brandColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    is CallingChannel.WhatsApp -> {
                                        WhatsAppIcon(
                                            modifier = Modifier.size(20.dp),
                                            tint = brandColor
                                        )
                                    }
                                    is CallingChannel.GoogleVoice -> {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = brandColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = brandColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = channel.displayName,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val subtitle = when (channel) {
                                        is CallingChannel.CellularSim -> if (channel.isRoaming) "Cellular (Roaming)" else "Cellular Network"
                                        is CallingChannel.WhatsApp -> if (channel.isBusiness) "WhatsApp Business VoIP" else "Free on Wi-Fi / Data"
                                        is CallingChannel.GoogleVoice -> "Google Voice VoIP / Carrier"
                                        else -> "Standard Carrier Network"
                                    }
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (showRememberChoice) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { rememberChoice = !rememberChoice }
                            .padding(top = 4.dp)
                    ) {
                        Checkbox(
                            checked = rememberChoice,
                            onCheckedChange = { rememberChoice = it }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Remember choice for this number",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Unified Call Confirmation Dialog for accidental touch protection (Favorites & Speed Dial).
 * Matches the exact styling, iconography, and colors of MultiChannelChoiceDialog.
 */
@Composable
fun CallConfirmationDialog(
    phoneNumber: String,
    contactName: String? = null,
    channel: CallingChannel? = null,
    isWhatsApp: Boolean = false,
    channelLabel: String? = null,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val displayName = contactName?.ifBlank { null } ?: phoneNumber
    val effectiveLabel = channelLabel ?: channel?.displayName ?: if (isWhatsApp) "WhatsApp" else "Cellular"
    val brandColor = channel?.let { Color(it.brandColorHex) } ?: if (isWhatsApp) Color(0xFF25D366) else Color(0xFF16A34A)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = brandColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Confirm Call",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Call $displayName?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = brandColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, brandColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isWhatsApp) {
                            WhatsAppIcon(modifier = Modifier.size(18.dp), tint = brandColor)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = brandColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Calling via $effectiveLabel",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = brandColor)
            ) {
                Text(if (isWhatsApp) "Call on WhatsApp" else "Call")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

