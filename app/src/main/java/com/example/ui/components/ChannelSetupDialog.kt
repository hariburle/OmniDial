package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChannelConfig
import com.example.domain.model.CallingChannel

/**
 * Interactive Channel Setup & Management Dialog.
 * Used during first-run onboarding and in Settings screen.
 */
@Composable
fun ChannelSetupDialog(
    discoveredChannels: List<CallingChannel>,
    existingConfigs: List<ChannelConfig> = emptyList(),
    isOnboarding: Boolean = false,
    onSave: (List<ChannelConfig>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val configMap = remember(existingConfigs) { existingConfigs.associateBy { it.channelId } }

    val enabledState = remember(discoveredChannels, existingConfigs) {
        mutableStateMapOf<String, Boolean>().apply {
            discoveredChannels.forEach { ch -> put(ch.id, configMap[ch.id]?.isEnabled ?: true) }
        }
    }

    val customNamesState = remember(discoveredChannels, existingConfigs) {
        mutableStateMapOf<String, String>().apply {
            discoveredChannels.forEach { ch -> put(ch.id, configMap[ch.id]?.customName ?: ch.shortLabel) }
        }
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    val showScrollFade by remember { derivedStateOf { scrollState.canScrollForward } }

    AlertDialog(
        onDismissRequest = { if (!isOnboarding) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
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
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Select which channels to use, and customize their display labels:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (validationError != null) {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer) {
                            Text(
                                text = validationError ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (isOnboarding) {
                        HomeRegionCard(context = context)
                    }

                    val channelsToDisplay = if (discoveredChannels.isEmpty()) listOf(CallingChannel.SystemDefault) else discoveredChannels

                    channelsToDisplay.forEachIndexed { index, channel ->
                        val isEnabled = enabledState[channel.id] ?: true
                        val currentName = customNamesState[channel.id] ?: channel.shortLabel
                        val brandColor = Color(channel.brandColorHex)

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isEnabled) brandColor.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(
                                width = if (isEnabled) 1.5.dp else 1.dp,
                                color = if (isEnabled) brandColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("channel_config_card_${channel.id}")
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                                    is CallingChannel.CellularSim -> Icon(Icons.Default.SimCard, null, tint = if (isEnabled) brandColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                                    is CallingChannel.WhatsApp -> WhatsAppIcon(modifier = Modifier.size(18.dp), tint = if (isEnabled) brandColor else MaterialTheme.colorScheme.onSurfaceVariant)
                                                    else -> Icon(Icons.Default.Phone, null, tint = if (isEnabled) brandColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                        Column {
                                            Text(channel.displayName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                        onCheckedChange = { checked -> enabledState[channel.id] = checked; validationError = null },
                                        colors = SwitchDefaults.colors(checkedThumbColor = brandColor, checkedTrackColor = brandColor.copy(alpha = 0.35f)),
                                        modifier = Modifier.testTag("channel_switch_${channel.id}")
                                    )
                                }

                                if (isEnabled) {
                                    OutlinedTextField(
                                        value = currentName,
                                        onValueChange = { customNamesState[channel.id] = it },
                                        label = { Text("Display Name / Label", style = MaterialTheme.typography.labelSmall) },
                                        supportingText = if (channel is CallingChannel.CellularSim && !channel.deviceSimName.isNullOrBlank()) {
                                            { Text("Device SIM: ${channel.deviceSimName} (${channel.carrierName})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        } else null,
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("channel_name_input_${channel.id}"),
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Scroll-to-see-more fade — shown when content extends below viewport
                if (showScrollFade) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
                                )
                            )
                    ) {
                        Text(
                            text = "↓  scroll to see more",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!enabledState.values.any { it }) { validationError = "Please enable at least one channel to place calls."; return@Button }
                    onSave(discoveredChannels.mapIndexed { idx, ch ->
                        ChannelConfig(ch.id, enabledState[ch.id] ?: true, customNamesState[ch.id]?.trim()?.takeIf { it.isNotBlank() }, idx)
                    })
                },
                modifier = Modifier.testTag("channel_setup_save_button")
            ) {
                Text(if (isOnboarding) "Save & Continue" else "Save Changes")
            }
        },
        dismissButton = {
            if (!isOnboarding) TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Home region selector for the onboarding flow.
 * Auto-detects country from SIM via TravelRoamingManager.
 * Supports any ISO 3166-1 alpha-2 code via inline edit — no hardcoded country list.
 * Flag emoji is computed from the ISO, covering every country in the world.
 */
@Composable
private fun HomeRegionCard(context: android.content.Context) {
    val travelManager = remember { com.example.telecom.TravelRoamingManager.getInstance(context) }
    var selectedIso by remember { mutableStateOf(travelManager.getHomeCountryIso().lowercase().take(2)) }
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(selectedIso.uppercase()) }

    val displayCountry = remember(selectedIso) {
        try {
            java.util.Locale("", selectedIso.uppercase()).displayCountry
                .takeIf { it.isNotBlank() && it != selectedIso.uppercase() } ?: selectedIso.uppercase()
        } catch (_: Exception) { selectedIso.uppercase() }
    }

    val flagEmoji = remember(selectedIso) {
        try {
            val iso = selectedIso.uppercase()
            if (iso.length == 2) {
                val offset = 0x1F1E6 - 0x41
                String(Character.toChars(iso[0].code + offset)) + String(Character.toChars(iso[1].code + offset))
            } else "🌍"
        } catch (_: Exception) { "🌍" }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text("Confirm Home Region", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                "Smart routing uses this to detect when you're traveling.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Country card — solid background + strong border for dark mode clarity
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text(flagEmoji, fontSize = 22.sp) }
                        }
                        Column {
                            Text(displayCountry, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("ISO: ${selectedIso.uppercase()} · Home country", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                        }
                    }

                    // Change / Done — solid fill, unambiguous in dark mode
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.clickable { isEditing = !isEditing }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                contentDescription = null,
                                tint = if (isEditing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isEditing) "Done" else "Change",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isEditing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ISO edit field — slides in when user taps Change
            AnimatedVisibility(visible = isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { raw ->
                            val v = raw.uppercase().filter { it.isLetter() }.take(2)
                            editText = v
                            if (v.length == 2) {
                                selectedIso = v.lowercase()
                                travelManager.setHomeCountryIso(v.lowercase())
                            }
                        },
                        label = { Text("2-letter country code") },
                        placeholder = { Text("e.g. US, GB, AU, CA, DE…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Type any ISO 3166-1 alpha-2 country code",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
