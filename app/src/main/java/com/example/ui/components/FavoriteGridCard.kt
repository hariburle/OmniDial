package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FavoriteContact
import com.example.domain.model.CallingChannel
import com.example.telecom.ChannelDiscoveryManager

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FavoriteGridCard(
    contact: FavoriteContact,
    isCompact: Boolean,
    isConfigureMode: Boolean = false,
    isFloatingOverlay: Boolean = false,
    isDraggingActive: Boolean = false,
    preferredCallingMode: String = "cellular",
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragDelta: (Offset) -> Unit = {},
    onCall: () -> Unit,
    onCallWhatsApp: () -> Unit = {},
    onCallUnknown: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onSelect: () -> Unit,
    onCreateRule: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    onSpeedDialClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val discoveryManager = remember(context) { ChannelDiscoveryManager.getInstance(context) }
    val waLabel = remember(context) {
        try {
            com.example.data.ChannelConfigRepository.getInstance(context).getCustomNameSync("whatsapp") ?: "WhatsApp"
        } catch (_: Exception) {
            "WhatsApp"
        }
    }
    val phoneLabel = remember(context) {
        try {
            com.example.data.ChannelConfigRepository.getInstance(context).getCustomNameSync("sim_1") ?: "Phone"
        } catch (_: Exception) {
            "Phone"
        }
    }

    val shadowElevation by animateDpAsState(
        targetValue = if (isFloatingOverlay) 16.dp else 1.dp,
        label = "drag_shadow"
    )

    val cardModifier = if (!isFloatingOverlay) {
        modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    } else {
        modifier.fillMaxWidth()
    }

    val cardShape = RoundedCornerShape(18.dp)
    val cardBorder = if (!isFloatingOverlay) {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    } else null

    val cardContainerColor = if (isFloatingOverlay) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = cardModifier.testTag("fav_grid_card_${contact.phoneNumber}"),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = shadowElevation),
        shape = cardShape,
        border = cardBorder
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Main Top Row: Avatar + Name & Label + #number Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar (Squircle Bento style)
                    val avatarShape = RoundedCornerShape(12.dp)
                    val avatarSize = 40.dp

                    Surface(
                        shape = avatarShape,
                        color = Color(contact.avatarColor),
                        modifier = Modifier.size(avatarSize)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            val initialChar = (contact.nickname?.takeIf { it.isNotBlank() } ?: contact.name).take(1).uppercase()
                            Text(
                                text = initialChar,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (!contact.photoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = contact.photoUri,
                                    contentDescription = contact.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Name, Nickname, Phone Number & Label
                    Column(modifier = Modifier.weight(1f)) {
                        val displayName = if (!contact.nickname.isNullOrBlank()) contact.nickname!! else contact.name
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = contact.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Speed Dial #slot Badge (#number instead of Key #number)
                    if (contact.speedDialSlot != null && !isConfigureMode) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "#${contact.speedDialSlot}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Bottom Action Row
                if (isConfigureMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            onClick = onSpeedDialClick,
                            shape = RoundedCornerShape(6.dp),
                            color = if (contact.speedDialSlot != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.height(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 7.dp)) {
                                Text(
                                    text = if (contact.speedDialSlot != null) "#${contact.speedDialSlot}" else "+ Speed",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (contact.speedDialSlot != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Surface(
                            onClick = onDelete,
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            modifier = Modifier
                                .size(26.dp)
                                .testTag("fav_delete_${contact.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Dedicated Drag Handle in Configure Mode
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(26.dp)
                                .pointerInput(contact.id) {
                                    detectDragGestures(
                                        onDragStart = { onDragStart() },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            onDragDelta(dragAmount)
                                        },
                                        onDragEnd = { onDragEnd() },
                                        onDragCancel = { onDragEnd() }
                                    )
                                }
                                .testTag("fav_drag_handle_${contact.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Normal Mode: Voice-first single Call action (Option B)
                    // If channel preference is unknown/ask, button reads "Call" and triggers onCallUnknown
                    // If preference is known (e.g. Jio, Airtel, WhatsApp, etc.), button reads "Call - <Channel Name>"
                    val resolvedChannel = remember(preferredCallingMode, discoveryManager) {
                        if (preferredCallingMode.isBlank() || preferredCallingMode.equals("ask", ignoreCase = true) || preferredCallingMode.equals("ask_always", ignoreCase = true)) {
                            null
                        } else {
                            discoveryManager.getChannelById(preferredCallingMode) ?: when (preferredCallingMode.lowercase()) {
                                "cellular", "phone" -> discoveryManager.getChannelById("sim_1") ?: CallingChannel.SystemDefault
                                else -> null
                            }
                        }
                    }
                    val isUnknown = (resolvedChannel == null)
                    val buttonText = if (resolvedChannel != null) "Call - ${resolvedChannel.shortLabel}" else "Call"
                    val brandColor = resolvedChannel?.let { Color(it.brandColorHex) } ?: MaterialTheme.colorScheme.primary
                    val isWhatsApp = (resolvedChannel is CallingChannel.WhatsApp)
                    val buttonAction = when {
                        isUnknown -> onCallUnknown
                        isWhatsApp -> onCallWhatsApp
                        else -> onCall
                    }

                    Surface(
                        onClick = buttonAction,
                        shape = RoundedCornerShape(10.dp),
                        color = if (resolvedChannel != null) brandColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .testTag("fav_call_btn_${contact.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isWhatsApp) {
                                WhatsAppIcon(modifier = Modifier.size(14.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = brandColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = buttonText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = brandColor
                            )
                        }
                    }
                }
            }
        }
    }
}
