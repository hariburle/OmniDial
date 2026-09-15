package com.example.ui.components

import android.telecom.Call
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Isolated call duration timer composable to prevent unnecessary parent recompositions
 * on high-frequency 1Hz timer updates.
 */
@Composable
fun CallDurationStatusChip(
    callState: Int,
    connectTimeMillis: Long,
    isAutomationRunning: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var callSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(callState, connectTimeMillis) {
        if (callState == Call.STATE_ACTIVE) {
            while (true) {
                if (connectTimeMillis > 0) {
                    callSeconds = (System.currentTimeMillis() - connectTimeMillis) / 1000
                }
                delay(1000)
            }
        } else {
            callSeconds = 0L
        }
    }

    val stateLabel = when (callState) {
        Call.STATE_RINGING -> "Incoming Call..."
        Call.STATE_DIALING, Call.STATE_CONNECTING -> "Connecting..."
        Call.STATE_ACTIVE -> {
            val mins = callSeconds / 60
            val secs = callSeconds % 60
            String.format("%02d:%02d", mins, secs)
        }
        Call.STATE_DISCONNECTED, Call.STATE_DISCONNECTING -> "Call Ended"
        else -> "In Call"
    }

    AssistChip(
        onClick = onDismiss,
        modifier = modifier,
        label = {
            Text(
                text = stateLabel,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (callState == Call.STATE_ACTIVE)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.secondaryContainer
        ),
        leadingIcon = {
            if (isAutomationRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
