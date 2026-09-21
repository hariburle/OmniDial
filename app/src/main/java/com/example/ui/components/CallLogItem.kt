package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.RecentCall
import com.example.telecom.SimInfo
import com.example.ui.screens.GroupedCallLog
import com.example.util.ContactHelper
import com.example.util.DeviceContact
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallLogItem(
    group: GroupedCallLog,
    isFavorite: Boolean,
    highlightNumber: String? = null,
    hasMultipleNumbers: Boolean = false,
    numberLabel: String = "Mobile",
    onCallBack: () -> Unit,
    onCreateRule: () -> Unit,
    onOpenNoteDialog: (RecentCall) -> Unit,
    onToggleSpam: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenDetails: () -> Unit,
    onDeleteCall: () -> Unit = {},
    onDeleteCallsForNumber: () -> Unit = {},
    matchedDc: DeviceContact? = null,
    activeSims: List<SimInfo> = emptyList(),
    modifier: Modifier = Modifier
) {
    val call = group.primaryCall
    val context = LocalContext.current
    val isVoicemail = remember(call.phoneNumber) {
        ContactHelper.isVoicemailNumber(context, call.phoneNumber)
    }
    val photoToUse = if (isVoicemail) null else (matchedDc?.photoUri?.ifBlank { null } ?: call.photoUri?.ifBlank { null })
    val formalName = if (isVoicemail) "Voicemail" else (matchedDc?.name?.ifBlank { null } ?: call.callerName?.ifBlank { null })
    val effectiveNickname = if (isVoicemail) null else matchedDc?.nickname?.ifBlank { null }
    val nameToUse = effectiveNickname ?: formalName
    val isWaBiz = call.callReason?.contains("WhatsApp Business", ignoreCase = true) == true
    val isWhatsApp = call.callReason?.contains("WhatsApp", ignoreCase = true) == true
    val isGoogleVoice = call.callReason?.contains("Google Voice", ignoreCase = true) == true
    val waColor = if (isWaBiz) Color(0xFF128C7E) else Color(0xFF25D366)
    val gvColor = Color(0xFF0F9D58)
    val isCarrierAutoDropped = call.isSpam && (
        call.note?.contains("auto-dropped", ignoreCase = true) == true ||
        call.ruleMatched?.contains("Carrier", ignoreCase = true) == true ||
        call.callReason?.contains("auto-dropped", ignoreCase = true) == true
    )
    val noteToShow = if (isCarrierAutoDropped) null else call.note
    val reminderToShow = call.reminderTime
    val discoveryManager = remember(context) { com.example.telecom.ChannelDiscoveryManager.getInstance(context) }
    val availableChannels by discoveryManager.availableChannels.collectAsState()
    val waLabel = remember(availableChannels, isWaBiz) {
        val targetId = if (isWaBiz) "whatsapp_business" else "whatsapp"
        availableChannels.firstOrNull { it.id == targetId }?.displayName
            ?: try {
                com.example.data.ChannelConfigRepository.getInstance(context).getCustomNameSync(targetId)
                    ?: if (isWaBiz) "WA Business" else "WhatsApp"
            } catch (_: Exception) {
                if (isWaBiz) "WA Business" else "WhatsApp"
            }
    }
    val gvLabel = remember(availableChannels) {
        availableChannels.firstOrNull { it.id == "google_voice" }?.displayName
            ?: try {
                com.example.data.ChannelConfigRepository.getInstance(context).getCustomNameSync("google_voice") ?: "Google Voice"
            } catch (_: Exception) {
                "Google Voice"
            }
    }
    val (typeIcon, typeColor, typeLabel) = when {
        isGoogleVoice -> Triple(Icons.AutoMirrored.Filled.CallMade, gvColor, "$gvLabel Call")
        isWhatsApp -> Triple(Icons.AutoMirrored.Filled.CallMade, waColor, "$waLabel Call")
        call.callType == 1 -> Triple(Icons.AutoMirrored.Filled.CallReceived, Color(0xFF16A34A), "Incoming")
        call.callType == 2 -> Triple(Icons.AutoMirrored.Filled.CallMade, Color(0xFF2563EB), "Outgoing")
        else -> Triple(Icons.AutoMirrored.Filled.CallMissed, Color(0xFFDC2626), "Missed")
    }

    val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(call.timestamp))

    val isHighlighted = remember(highlightNumber, call.phoneNumber) {
        if (highlightNumber.isNullOrBlank()) false
        else {
            val targetDigits = highlightNumber.filter { it.isDigit() }.takeLast(10)
            val callDigits = call.phoneNumber.filter { it.isDigit() }.takeLast(10)
            (targetDigits.isNotBlank() && callDigits == targetDigits) || call.phoneNumber == highlightNumber
        }
    }

    val animatedContainerColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isHighlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        } else if (isGoogleVoice) {
            gvColor.copy(alpha = 0.08f)
        } else if (isWhatsApp) {
            waColor.copy(alpha = 0.08f)
        } else if (group.isSpam) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 600),
        label = "call_item_container_color"
    )

    val animatedBorderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isHighlighted) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 600),
        label = "call_item_border_color"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() }
            .testTag("call_item_${call.id}"),
        colors = CardDefaults.cardColors(
            containerColor = animatedContainerColor
        ),
        border = if (isHighlighted || animatedBorderColor != Color.Transparent) BorderStroke(2.dp, animatedBorderColor) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Avatar with contact photo or call type indicator or Spam warning
                Box(modifier = Modifier.size(36.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (group.isSpam) Color(0xFFDC2626).copy(alpha = 0.15f) else if (isGoogleVoice) gvColor.copy(alpha = 0.18f) else if (isWhatsApp) waColor.copy(alpha = 0.18f) else typeColor.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (group.isSpam) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Report,
                                    contentDescription = "Spam Caller",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else if (!photoToUse.isNullOrBlank()) {
                            AsyncImage(
                                model = photoToUse,
                                contentDescription = nameToUse ?: call.phoneNumber,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (isVoicemail) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Voicemail,
                                    contentDescription = "Voicemail",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else if (!nameToUse.isNullOrBlank()) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = nameToUse.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGoogleVoice) gvColor else if (isWhatsApp) Color(0xFF15803D) else typeColor,
                                    fontSize = 16.sp
                                )
                            }
                        } else if (isWhatsApp) {
                            Box(contentAlignment = Alignment.Center) {
                                WhatsAppIcon(
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = typeLabel,
                                    tint = typeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (isGoogleVoice) {
                        Surface(
                            shape = CircleShape,
                            color = gvColor,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = gvLabel,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    } else if (isWhatsApp) {
                        Surface(
                            shape = CircleShape,
                            color = waColor,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                WhatsAppIcon(
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    } else if (!photoToUse.isNullOrBlank() || !nameToUse.isNullOrBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = typeLabel,
                                    tint = typeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Line 1: Name / Number + Count Badge + WhatsApp Badge + Missed Call Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = nameToUse ?: call.phoneNumber,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (group.isSpam) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (group.count > 1) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "(${group.count})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (isGoogleVoice) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = gvColor.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = gvColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = gvLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = gvColor,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (isWhatsApp) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = waColor.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    WhatsAppIcon(modifier = Modifier.size(11.dp))
                                    Text(
                                        text = waLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWaBiz) Color(0xFF0F766E) else Color(0xFF15803D),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (isHighlighted) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDC2626)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallMissed,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Missed",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (group.isSpam) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "Spam",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        } else {
                            val community = remember(call.phoneNumber) {
                                com.example.util.CommunityCallerIdService.lookup(call.phoneNumber)
                            }
                            if (community != null && (community.category.contains("Delivery", ignoreCase = true) || community.verificationType.contains("Delivery", ignoreCase = true))) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFFF3E0)
                                ) {
                                    Text(
                                        text = "Delivery",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE65100),
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            } else if (community != null && community.isVerified && (community.verificationType.contains("Verified", ignoreCase = true) || community.verificationType.contains("Financial", ignoreCase = true))) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = "Verified",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Line 1.5: Specific Phone Number and Label if contact has multiple numbers
                    if (hasMultipleNumbers && (formalName != null || !call.callerName.isNullOrBlank())) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    text = numberLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                            Text(
                                text = call.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Line 2: Time + Duration + SIM Badge + Rules
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        if (isGoogleVoice) {
                            Text(
                                text = "• $gvLabel Call",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = gvColor
                            )
                        } else if (isWhatsApp) {
                            Text(
                                text = "• $waLabel Call",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF15803D)
                            )
                        } else {
                            if (call.durationSeconds > 0) {
                                val mins = call.durationSeconds / 60
                                val secs = call.durationSeconds % 60
                                val durText = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                                Text(
                                    text = "• $durText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // SIM Badge with Custom Name (shown only on multi-SIM devices)
                            if (activeSims.size > 1) {
                                val slot = if (call.simSlot > 0) call.simSlot else 1
                                val customName = remember(slot, activeSims, availableChannels) {
                                    availableChannels.filterIsInstance<com.example.domain.model.CallingChannel.CellularSim>()
                                        .firstOrNull { it.slotIndex + 1 == slot }?.displayName
                                        ?: try {
                                            com.example.data.ChannelConfigRepository.getInstance(context).getCustomNameSync("sim_$slot")
                                        } catch (_: Exception) {
                                            null
                                        }
                                }
                                val matchedSim = activeSims.firstOrNull { it.slotIndex + 1 == slot }
                                val simLabel = customName ?: if (matchedSim != null && matchedSim.displayName.isNotBlank()) {
                                    matchedSim.displayName.take(14)
                                } else {
                                    "SIM $slot"
                                }
                                val simColor = if (slot == 2) Color(0xFF16A34A) else Color(0xFF2563EB)

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = simColor.copy(alpha = 0.12f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SimCard,
                                            contentDescription = simLabel,
                                            tint = simColor,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = simLabel,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = simColor,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                        if (!call.ruleMatched.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = "🤖 ${call.ruleMatched}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Carrier Spam Security Badge (compact single-line badge, replaces wrapping note)
                    if (isCarrierAutoDropped) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                            modifier = Modifier.padding(top = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Spam Blocked",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Carrier Auto-Dropped",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Note Display in Recents
                    if (!noteToShow.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .padding(top = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = "Note",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = noteToShow,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Scheduled Reminder Badge
                    if (reminderToShow != null && reminderToShow > System.currentTimeMillis()) {
                        val minsLeft = ((reminderToShow - System.currentTimeMillis()) / (60 * 1000)).coerceAtLeast(1)
                        val reminderText = if (minsLeft < 60) "Reminder in ${minsLeft}m" else "Reminder in ${minsLeft / 60}h ${minsLeft % 60}m"
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = "Follow-up Reminder",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = reminderText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            var showOverflowMenu by remember { mutableStateOf(false) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // One-tap Call Back Button
                FilledIconButton(
                    onClick = onCallBack,
                    modifier = Modifier.size(38.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call Back",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // One-tap Favorite Star
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorite) "Remove Favorite" else "Add Favorite",
                        tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // More Options Menu (Note, Spam, Rule)
                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        val hasUserNote = !noteToShow.isNullOrBlank() || call.reminderTime != null
                        DropdownMenuItem(
                            text = { Text(if (hasUserNote) "Edit Note / Reminder" else "Add Note / Reminder") },
                            leadingIcon = {
                                Icon(Icons.Default.EditNote, contentDescription = null)
                            },
                            onClick = {
                                showOverflowMenu = false
                                onOpenNoteDialog(if (isCarrierAutoDropped && call.note?.contains("auto-dropped", ignoreCase = true) == true) call.copy(note = null) else call)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (group.isSpam) "Unmark as Spam" else "Report as Spam") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (group.isSpam) Icons.Default.Security else Icons.Default.Block,
                                    contentDescription = null,
                                    tint = if (group.isSpam) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onToggleSpam()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Create Automation Rule") },
                            leadingIcon = {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                showOverflowMenu = false
                                onCreateRule()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Entry") },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                showOverflowMenu = false
                                onDeleteCall()
                            }
                        )
                        if (group.count > 1) {
                            DropdownMenuItem(
                                text = { Text("Clear All (${group.count}) for Number") },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onDeleteCallsForNumber()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
