package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CallingChannel

/**
 * KeypadChannelDock renders an adaptive horizontal segmented pill bar displaying
 * discovered calling channels (Cellular SIM 1, Cellular SIM 2, WhatsApp, etc.).
 *
 * Tapping a channel pill switches the active transport channel for dialing.
 */
@Composable
fun KeypadChannelDock(
    channels: List<CallingChannel>,
    selectedChannel: CallingChannel?,
    onSelectChannel: (CallingChannel) -> Unit,
    isEmergency: Boolean = false,
    modifier: Modifier = Modifier
) {
    val displayChannels = if (isEmergency) {
        channels.filterIsInstance<CallingChannel.CellularSim>()
    } else {
        channels
    }
    if (displayChannels.isEmpty()) return

    val view = LocalView.current
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        displayChannels.forEach { channel ->
            val isSelected = selectedChannel?.id == channel.id
            val brandColor = Color(channel.brandColorHex)

            val animatedBgColor by animateColorAsState(
                targetValue = when {
                    isSelected -> brandColor.copy(alpha = if (isDark) 0.25f else 0.15f)
                    isDark -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                },
                label = "pill_bg_${channel.id}"
            )

            val animatedBorderColor by animateColorAsState(
                targetValue = when {
                    isSelected -> brandColor
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                },
                label = "pill_border_${channel.id}"
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = animatedBgColor,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = animatedBorderColor
                ),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(32.dp)
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onSelectChannel(channel)
                    }
                    .testTag("keypad_channel_pill_${channel.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    when (channel) {
                        is CallingChannel.CellularSim -> {
                            Icon(
                                imageVector = Icons.Default.SimCard,
                                contentDescription = channel.displayName,
                                tint = if (isSelected) brandColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        is CallingChannel.WhatsApp -> {
                            WhatsAppIcon(
                                modifier = Modifier.size(14.dp),
                                tint = if (isSelected) brandColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is CallingChannel.GoogleVoice -> {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = channel.displayName,
                                tint = if (isSelected) brandColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = channel.displayName,
                                tint = if (isSelected) brandColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = channel.shortLabel,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            if (isDark) brandColor else brandColor.copy(alpha = 0.95f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
