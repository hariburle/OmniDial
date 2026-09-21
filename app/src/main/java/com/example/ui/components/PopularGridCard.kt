package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.domain.model.CallingChannel
import com.example.telecom.ChannelDiscoveryManager
import com.example.ui.models.PopularContactItem

@Composable
fun PopularGridCard(
    item: PopularContactItem,
    isConfigureMode: Boolean = false,
    preferredCallingMode: String = "cellular",
    onCall: () -> Unit,
    onCallWhatsApp: () -> Unit = {},
    onAddFavorite: () -> Unit,
    onIgnore: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val discoveryManager = remember(context) { ChannelDiscoveryManager.getInstance(context) }
    val availableChannels by discoveryManager.availableChannels.collectAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("popular_card_${item.phoneNumber}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top Row: Avatar + Name / Phone / Label + Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    if (!item.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = item.photoUri,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            val initialChar = (item.nickname?.takeIf { it.isNotBlank() } ?: item.name).take(1).uppercase()
                            Text(
                                text = initialChar,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Name & Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    val hasNickname = !item.nickname.isNullOrBlank()
                    val mainDisplayName = if (hasNickname) item.nickname!! else item.name
                    val isPhoneOnly = mainDisplayName.filter { it.isDigit() } == item.phoneNumber.filter { it.isDigit() } ||
                            (mainDisplayName.length >= 7 && mainDisplayName.filter { it.isDigit() }.takeLast(10) == item.phoneNumber.filter { it.isDigit() }.takeLast(10))

                    Text(
                        text = mainDisplayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subtitleText = when {
                        isPhoneOnly -> {
                            if (!item.label.isNullOrBlank() && item.label != "Frequent" && item.label != "Recent" && item.label != "Mobile") {
                                item.label!!
                            } else {
                                ""
                            }
                        }
                        else -> {
                            if (!item.label.isNullOrBlank() && item.label != "Frequent" && item.label != "Recent") {
                                "${item.phoneNumber} • ${item.label}"
                            } else {
                                item.phoneNumber
                            }
                        }
                    }

                    if (subtitleText.isNotBlank()) {
                        Text(
                            text = subtitleText,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Call count chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = "${item.callCount}x",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            // Bottom Action Row (Height: 28.dp matching FavoriteGridCard)
            if (isConfigureMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = onAddFavorite,
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.height(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 7.dp)) {
                            Text(
                                text = "+ Favorite",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Surface(
                        onClick = onIgnore,
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Ignore Contact",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } else {
                val resolvedChannel = remember(preferredCallingMode, availableChannels) {
                    if (preferredCallingMode.isBlank() || preferredCallingMode.equals("ask", ignoreCase = true) || preferredCallingMode.equals("ask_always", ignoreCase = true)) {
                        null
                    } else {
                        availableChannels.firstOrNull { it.id.equals(preferredCallingMode, ignoreCase = true) }
                            ?: when (preferredCallingMode.lowercase()) {
                                "cellular", "phone" -> availableChannels.firstOrNull { it.id == "sim_1" } ?: availableChannels.firstOrNull { it is CallingChannel.CellularSim }
                                else -> null
                            }
                    }
                }
                val isUnknown = (resolvedChannel == null)
                val buttonText = if (resolvedChannel != null) "Call - ${resolvedChannel.shortLabel}" else "Call"
                val brandColor = resolvedChannel?.let { Color(it.brandColorHex) } ?: MaterialTheme.colorScheme.primary
                val isWhatsApp = (resolvedChannel is CallingChannel.WhatsApp)
                val buttonAction = when {
                    isUnknown -> onCall
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
                        .testTag("pop_call_btn_${item.phoneNumber}")
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
