package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChannelConfig
import com.example.domain.model.CallingChannel

/**
 * Interactive Channel Setup & Management Dialog:
 * - Discovers available hardware SIMs and installed VoIP apps
 * - Allows users to choose which channels to leverage in OmniDial
 * - Allows inline renaming of display labels (e.g. "Personal Jio", "Work Airtel")
 * - Used during first-run onboarding and in the Settings screen.
 */
@Composable
fun ChannelSetupDialog(
    discoveredChannels: List<CallingChannel>,
    existingConfigs: List<ChannelConfig> = emptyList(),
    isOnboarding: Boolean = false,
    onSave: (List<ChannelConfig>) -> Unit,
    onDismiss: () -> Unit
) {
    val configMap = remember(existingConfigs) {
        existingConfigs.associateBy { it.channelId }
    }

    // State for enabled channels
    val enabledState = remember(discoveredChannels, existingConfigs) {
        mutableStateMapOf<String, Boolean>().apply {
            discoveredChannels.forEach { ch ->
                val cfg = configMap[ch.id]
                // Default enabled if no existing config, or use persisted value
                put(ch.id, cfg?.isEnabled ?: true)
            }
        }
    }

    // State for custom names
    val customNamesState = remember(discoveredChannels, existingConfigs) {
        mutableStateMapOf<String, String>().apply {
            discoveredChannels.forEach { ch ->
                val cfg = configMap[ch.id]
                put(ch.id, cfg?.customName ?: ch.shortLabel)
            }
        }
    }

    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (!isOnboarding) onDismiss()
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = if (isOnboarding) "Welcome to OmniDial" else "Manage Calling Channels",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (isOnboarding) "Set up your communication channels" else "Customize channels and display labels",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select which channels you want to use in the app, and customize their display labels (e.g., Personal, Work):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (validationError != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = validationError ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                val channelsToDisplay = if (discoveredChannels.isEmpty()) {
                    listOf(CallingChannel.SystemDefault)
                } else {
                    discoveredChannels
                }

                channelsToDisplay.forEachIndexed { index, channel ->
                    val isEnabled = enabledState[channel.id] ?: true
                    val currentName = customNamesState[channel.id] ?: channel.shortLabel
                    val brandColor = Color(channel.brandColorHex)

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEnabled)
                                brandColor.copy(alpha = 0.08f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(
                            width = if (isEnabled) 1.5.dp else 1.dp,
                            color = if (isEnabled) brandColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("channel_config_card_${channel.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Row 1: Icon, Original Name, and Toggle Switch
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isEnabled) brandColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            when (channel) {
                                                is CallingChannel.CellularSim -> {
                                                    Icon(
                                                        imageVector = Icons.Default.SimCard,
                                                        contentDescription = null,
                                                        tint = if (isEnabled) brandColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                is CallingChannel.WhatsApp -> {
                                                    WhatsAppIcon(
                                                        modifier = Modifier.size(18.dp),
                                                        tint = if (isEnabled) brandColor else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                is CallingChannel.GoogleVoice -> {
                                                    Icon(
                                                        imageVector = Icons.Default.Phone,
                                                        contentDescription = null,
                                                        tint = if (isEnabled) brandColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                else -> {
                                                    Icon(
                                                        imageVector = Icons.Default.Phone,
                                                        contentDescription = null,
                                                        tint = if (isEnabled) brandColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = channel.displayName,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = when (channel) {
                                                is CallingChannel.CellularSim -> if (channel.isRoaming) "Cellular (Roaming)" else "Cellular Network"
                                                is CallingChannel.WhatsApp -> if (channel.isBusiness) "WhatsApp Business VoIP" else "Free on Wi-Fi / Data"
                                                is CallingChannel.GoogleVoice -> "Google Voice VoIP"
                                                else -> "Standard Carrier Network"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        enabledState[channel.id] = checked
                                        validationError = null
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = brandColor,
                                        checkedTrackColor = brandColor.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier.testTag("channel_switch_${channel.id}")
                                )
                            }

                            // Row 2: Editable Label (Shown when enabled)
                            if (isEnabled) {
                                OutlinedTextField(
                                    value = currentName,
                                    onValueChange = { customNamesState[channel.id] = it },
                                    label = { Text("Display Name / Label", style = MaterialTheme.typography.labelSmall) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("channel_name_input_${channel.id}"),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val anyEnabled = enabledState.values.any { it }
                    if (!anyEnabled) {
                        validationError = "Please enable at least one channel to place calls."
                        return@Button
                    }

                    val configs = discoveredChannels.mapIndexed { idx, ch ->
                        val isEn = enabledState[ch.id] ?: true
                        val name = customNamesState[ch.id]?.trim()?.takeIf { it.isNotBlank() }
                        ChannelConfig(
                            channelId = ch.id,
                            isEnabled = isEn,
                            customName = name,
                            orderIndex = idx
                        )
                    }
                    onSave(configs)
                },
                modifier = Modifier.testTag("channel_setup_save_button")
            ) {
                Text(if (isOnboarding) "Save & Continue" else "Save Changes")
            }
        },
        dismissButton = {
            if (!isOnboarding) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
