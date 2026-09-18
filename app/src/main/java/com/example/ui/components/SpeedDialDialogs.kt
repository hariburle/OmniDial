package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FavoriteContact

@Composable
fun SpeedDialActionDialog(
    slot: Int,
    fav: FavoriteContact,
    preferredCallingMode: String,
    onSelectContactNumber: (String) -> Unit,
    onPlaceCall: (String, String?) -> Unit,
    onPlaceWhatsAppCall: (String) -> Unit,
    onReassign: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val isFavWaPreferred = preferredCallingMode == "whatsapp"
    val displayName = fav.nickname?.ifBlank { null } ?: fav.name.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: fav.name
    val fullDisplayName = fav.nickname?.ifBlank { null } ?: fav.name

    AlertDialog(
        onDismissRequest = onDismiss,
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
                        Text(
                            text = "#$slot",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Column {
                    Text(
                        text = fullDisplayName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Speed Dial Shortcut",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = fav.phoneNumber,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (fav.label.isNotBlank()) {
                            Text(
                                text = fav.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Prominent Call Button (Channel-Adaptive)
                Button(
                    onClick = {
                        val targetNum = fav.phoneNumber
                        onDismiss()
                        onSelectContactNumber(targetNum)
                        if (isFavWaPreferred) {
                            onPlaceWhatsAppCall(targetNum)
                        } else {
                            onPlaceCall(targetNum, null)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFavWaPreferred) Color(0xFF25D366) else Color(0xFF059669),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("speed_dial_call_button")
                ) {
                    if (isFavWaPreferred) {
                        WhatsAppIcon(
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = "Call",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (isFavWaPreferred) "Call via WhatsApp ($displayName)" else "Call $displayName",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Fallback alternative call channel
                OutlinedButton(
                    onClick = {
                        val targetNum = fav.phoneNumber
                        onDismiss()
                        onSelectContactNumber(targetNum)
                        if (isFavWaPreferred) {
                            onPlaceCall(targetNum, null)
                        } else {
                            onPlaceWhatsAppCall(targetNum)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    if (isFavWaPreferred) {
                        Icon(
                            Icons.Filled.Call,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF059669)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Cellular Call Instead", fontSize = 13.sp)
                    } else {
                        WhatsAppIcon(
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF25D366)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("WhatsApp Voice Instead", fontSize = 13.sp)
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reassign",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .testTag("speed_dial_reassign_action")
                        .clickable { onReassign() }
                        .padding(vertical = 8.dp, horizontal = 6.dp)
                )
                Text(
                    text = "Clear",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .testTag("speed_dial_clear_action")
                        .clickable { onClear() }
                        .padding(vertical = 8.dp, horizontal = 6.dp)
                )
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .testTag("speed_dial_cancel_action")
                        .clickable { onDismiss() }
                        .padding(vertical = 8.dp, horizontal = 6.dp)
                )
            }
        }
    )
}

@Composable
fun SpeedDialAssignPromptDialog(
    slot: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                        Text(
                            text = "#$slot",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Text(
                    text = "Speed Dial #$slot",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Text(
                text = "Key #$slot is not assigned. Would you like to assign a contact to this speed dial shortcut?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("assign_speed_dial_confirm_button")
            ) {
                Text("Assign Contact", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Text(
                text = "Cancel",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .testTag("assign_speed_dial_cancel_action")
                    .clickable { onDismiss() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    )
}
