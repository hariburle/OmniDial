package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * First-run setup wizard: one guided flow for every role and permission
 * OmniDial needs, instead of scattered popups.
 *
 * Android requires a separate system screen for each role grant
 * (default dialer, call redirection) and each special permission
 * (overlay, full-screen intent), so they cannot be merged into a single
 * system popup. This wizard lists them once, in plain language, and walks
 * through each system screen back-to-back with one tap each.
 */
enum class SetupStep {
    PERMISSIONS,
    DEFAULT_DIALER,
    CALL_REDIRECTION,
    OVERLAY,
    FULL_SCREEN_INTENT
}

enum class SetupStepStatus {
    PENDING,
    DONE,
    SKIPPED
}

data class SetupStepState(
    val step: SetupStep,
    val status: SetupStepStatus = SetupStepStatus.PENDING
)

private data class SetupStepInfo(
    val title: String,
    val description: String,
    val icon: ImageVector
)

private fun setupStepInfo(step: SetupStep): SetupStepInfo = when (step) {
    SetupStep.PERMISSIONS -> SetupStepInfo(
        title = "Phone, contacts & notifications",
        description = "Make calls, see your contacts and never miss a call alert.",
        icon = Icons.Default.Phone
    )
    SetupStep.DEFAULT_DIALER -> SetupStepInfo(
        title = "Default phone app",
        description = "Let OmniDial handle all your calls.",
        icon = Icons.Default.Call
    )
    SetupStep.CALL_REDIRECTION -> SetupStepInfo(
        title = "Car & watch call forwarding",
        description = "Route calls from your car through your WhatsApp rules — no surprise roaming charges.",
        icon = Icons.Default.Bluetooth
    )
    SetupStep.OVERLAY -> SetupStepInfo(
        title = "Appear on top",
        description = "Show the call screen instantly, even over other apps.",
        icon = Icons.Default.Layers
    )
    SetupStep.FULL_SCREEN_INTENT -> SetupStepInfo(
        title = "Wake screen for calls",
        description = "Light up your screen for incoming calls when the phone is locked.",
        icon = Icons.Default.NotificationsActive
    )
}

@Composable
fun SetupWizardDialog(
    steps: List<SetupStepState>,
    isRunning: Boolean,
    onStartSetup: () -> Unit,
    onStepClick: (SetupStep) -> Unit,
    onSkipStep: (SetupStep) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!isRunning) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isRunning,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Finish setting up OmniDial",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A few quick steps so every call — including from your car — follows your rules.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (isRunning) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                }

                steps.forEachIndexed { index, state ->
                    SetupStepRow(
                        state = state,
                        stepNumber = index + 1,
                        enabled = !isRunning,
                        onClick = { onStepClick(state.step) },
                        onSkip = { onSkipStep(state.step) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onStartSetup,
                    enabled = !isRunning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRunning) "Setting up…" else "Set up")
                }
                TextButton(
                    onClick = onDismiss,
                    enabled = !isRunning,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Not now")
                }
            }
        }
    }
}

@Composable
private fun SetupStepRow(
    state: SetupStepState,
    stepNumber: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    onSkip: () -> Unit
) {
    val info = setupStepInfo(state.step)
    val clickable = enabled && state.status != SetupStepStatus.DONE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = clickable, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (state.status) {
            SetupStepStatus.DONE -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Done",
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(28.dp)
                )
            }
            SetupStepStatus.SKIPPED -> {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Skipped",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
            SetupStepStatus.PENDING -> {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(28.dp)
                ) {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stepNumber.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = info.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = info.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (state.status == SetupStepStatus.SKIPPED) {
                Text(
                    text = "Skipped — tap to try again",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (state.status == SetupStepStatus.PENDING && enabled) {
            TextButton(onClick = onSkip) {
                Text("Skip")
            }
        }
    }
}
