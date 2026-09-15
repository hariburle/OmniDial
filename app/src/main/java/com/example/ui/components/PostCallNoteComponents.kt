package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentCall

data class ReminderPreset(val minutes: Long?, val label: String)

val DEFAULT_REMINDER_PRESETS = listOf(
    ReminderPreset(15L, "15m"),
    ReminderPreset(30L, "30m"),
    ReminderPreset(60L, "1h"),
    ReminderPreset(120L, "2h"),
    ReminderPreset(240L, "4h"),
    ReminderPreset(1440L, "Tomorrow"),
    ReminderPreset(2880L, "2 Days"),
    ReminderPreset(10080L, "1 Week")
)

fun formatReminderRelativeTime(minutes: Long): String {
    return when {
        minutes < 60 -> "In ${minutes}m"
        minutes < 1440 -> "In ${minutes / 60}h"
        minutes == 1440L -> "Tomorrow"
        else -> "In ${minutes / 1440} days"
    }
}

/**
 * Reusable scrollable reminder presets selector with relative time display.
 */
@Composable
fun ReminderPresetChips(
    selectedMinutes: Long?,
    onSelectMinutes: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    presets: List<ReminderPreset> = DEFAULT_REMINDER_PRESETS,
    includeNoneOption: Boolean = false,
    headerText: String = "Set Reminder:"
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headerText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            if (selectedMinutes != null) {
                Text(
                    text = formatReminderRelativeTime(selectedMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val allPresets = remember(includeNoneOption, presets) {
            if (includeNoneOption) listOf(ReminderPreset(null, "None")) + presets else presets
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            allPresets.forEach { preset ->
                val isSelected = (selectedMinutes == preset.minutes)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSelectMinutes(if (isSelected && !includeNoneOption) null else preset.minutes)
                    },
                    label = { Text(preset.label, fontSize = 11.5.sp) },
                    leadingIcon = if (isSelected && preset.minutes != null) {
                        {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

/**
 * Reusable dialog for adding, editing, or clearing notes and follow-up reminders.
 */
@Composable
fun PostCallNoteReminderDialog(
    call: RecentCall,
    onDismiss: () -> Unit,
    onSave: (note: String?, reminderEpoch: Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    var noteText by remember(call) { mutableStateOf(call.note ?: "") }
    var reminderMinutes by remember(call) {
        mutableStateOf(
            call.reminderTime?.let {
                val diff = (it - System.currentTimeMillis()) / (60 * 1000)
                if (diff > 0) diff else null
            }
        )
    }
    val hasExistingContent = !call.note.isNullOrBlank() || call.reminderTime != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Post-Call Note & Reminder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = call.callerName ?: call.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = { Text("Add call summary, action items...") },
                    trailingIcon = {
                        if (noteText.isNotEmpty()) {
                            IconButton(onClick = { noteText = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear note text",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_call_note_input")
                )

                ReminderPresetChips(
                    selectedMinutes = reminderMinutes,
                    onSelectMinutes = { reminderMinutes = it },
                    includeNoneOption = true,
                    headerText = "Follow-up Reminder:"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reminderEpoch = reminderMinutes?.let { System.currentTimeMillis() + it * 60 * 1000 }
                    onSave(noteText.ifBlank { null }, reminderEpoch)
                    onDismiss()
                },
                modifier = Modifier.testTag("dialog_save_note_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (hasExistingContent) {
                    TextButton(
                        onClick = {
                            onSave(null, null)
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
        modifier = modifier
    )
}
