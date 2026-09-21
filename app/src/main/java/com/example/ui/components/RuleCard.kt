package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AutomationLog
import com.example.data.CallerRule

@Composable
fun RuleCard(
    rule: CallerRule,
    automationLogs: List<AutomationLog> = emptyList(),
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTest: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null
) {
    val runCount = automationLogs.count { it.ruleName.equals(rule.name, ignoreCase = true) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("rule_card_${rule.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Title, Pattern, Run Count & Toggle Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = rule.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (runCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Run $runCount×",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "Matches: ${rule.phoneNumberPattern.ifBlank { "Any Inbound Caller (*)" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (rule.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (rule.requiredWifiSsid.isNotBlank() || rule.requiredBluetoothDevice.isNotBlank()) {
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (rule.requiredWifiSsid.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "📶 Wi-Fi: ${rule.requiredWifiSsid}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (rule.requiredBluetoothDevice.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        text = "🚗 BT: ${rule.requiredBluetoothDevice}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = rule.isEnabled,
                        onCheckedChange = { onToggle() },
                        modifier = Modifier.testTag("rule_switch_${rule.id}")
                    )
                }
            }

            // Visual Execution Pipeline
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "EXECUTION PIPELINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
                RuleExecutionPipeline(rule = rule, isEnabled = rule.isEnabled)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            // Bottom Action Bar: Test Run Dry-Run + Quick Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Simulation Test Button
                OutlinedButton(
                    onClick = { onTest?.invoke() },
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("test_rule_btn_${rule.id}"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Test Rule",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onDuplicate?.invoke() },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("duplicate_rule_btn_${rule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Duplicate Rule",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("edit_rule_btn_${rule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Rule",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("delete_rule_btn_${rule.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Rule",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleExecutionPipeline(rule: CallerRule, isEnabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Step 1: Inbound Call Ringing
        PipelineStepNode(
            icon = Icons.Default.Phone,
            label = "Ring",
            isEnabled = isEnabled
        )
        PipelineArrow(isEnabled = isEnabled)

        // Step 2: Auto Answer & Delay
        if (rule.autoAnswer) {
            if (rule.answerDelaySec > 0) {
                PipelineStepNode(
                    icon = Icons.Default.NotificationsActive,
                    label = "Wait ${rule.answerDelaySec}s",
                    isEnabled = isEnabled
                )
                PipelineArrow(isEnabled = isEnabled)
            }
            PipelineStepNode(
                icon = Icons.Default.Call,
                label = "Auto-Answer",
                isEnabled = isEnabled
            )
            PipelineArrow(isEnabled = isEnabled)

            if (rule.autoSpeakerphone) {
                PipelineStepNode(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    label = "Speaker",
                    isEnabled = isEnabled
                )
                PipelineArrow(isEnabled = isEnabled)
            }
        }

        // Step 3: In-band DTMF transmission & Mute
        if (rule.dtmfSequence.isNotBlank()) {
            if (rule.autoMuteMic) {
                PipelineStepNode(
                    icon = Icons.Default.MicOff,
                    label = "Mute Mic",
                    isEnabled = isEnabled
                )
                PipelineArrow(isEnabled = isEnabled)
            }
            PipelineStepNode(
                icon = Icons.Default.Dialpad,
                label = "DTMF '${rule.dtmfSequence}'",
                isEnabled = isEnabled
            )
            PipelineArrow(isEnabled = isEnabled)
        }

        // Step 4: Out-of-band SMS
        if (rule.sendSms) {
            PipelineStepNode(
                icon = Icons.AutoMirrored.Filled.Chat,
                label = "SMS Reply",
                isEnabled = isEnabled
            )
            PipelineArrow(isEnabled = isEnabled)
        }

        // Step 5: Hangup or stay connected
        if (rule.autoHangup) {
            PipelineStepNode(
                icon = Icons.Default.CallEnd,
                label = "Hangup (${rule.hangupDelaySec}s)",
                isEnabled = isEnabled,
                isTerminal = true
            )
        } else {
            PipelineStepNode(
                icon = Icons.Default.Call,
                label = "Connected",
                isEnabled = isEnabled,
                isTerminal = true
            )
        }
    }
}

@Composable
private fun PipelineStepNode(
    icon: ImageVector,
    label: String,
    isEnabled: Boolean,
    isTerminal: Boolean = false
) {
    val containerColor = when {
        !isEnabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        isTerminal -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
        else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
    }
    val contentColor = when {
        !isEnabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        isTerminal -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PipelineArrow(isEnabled: Boolean) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        modifier = Modifier.size(11.dp),
        tint = if (isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
    )
}
