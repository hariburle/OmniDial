package com.example.ui.components

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneForwarded
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.telecom.RoleHelper
import com.example.telecom.RoleReminderNotification

/**
 * Inline banner shown on the dialer when OmniDial doesn't hold the
 * Call Redirection role. Mirrors [RoleBanner]: one tap fires the system
 * role-grant dialog, and the banner animates away once granted.
 *
 * No install/update detection is needed — after a reinstall the role is
 * simply not held, so this appears on first launch by itself. When a car
 * Bluetooth system connected recently, the copy turns car-specific.
 */
@Composable
fun CallRedirectionBanner(
    isRoleHeld: Boolean,
    carRecentlyConnected: Boolean,
    context: Context,
    onRoleChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!RoleHelper.isCallRedirectionRoleAvailable(context)) return

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // Role may now be held — dismiss any connect-time reminder notification.
        RoleReminderNotification.cancel(context)
        onRoleChanged()
    }

    AnimatedVisibility(
        visible = !isRoleHeld,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("redirection_banner_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PhoneForwarded,
                    contentDescription = "Call Redirection Role",
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Set as Call Redirection App",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (carRecentlyConnected)
                            "Your car connected — its calls will go as cellular. Enable routing to keep WhatsApp working."
                        else
                            "Let OmniDial route calls from your car, watch or voice assistant through WhatsApp",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Button(
                    onClick = {
                        val intent = RoleHelper.createCallRedirectionRoleIntent(context)
                        if (intent != null) {
                            try {
                                roleLauncher.launch(intent)
                            } catch (_: Exception) {
                                // Fallback: user can grant it from Settings instead
                            }
                        }
                    },
                    modifier = Modifier.testTag("set_redirection_button")
                ) {
                    Text("Enable")
                }
            }
        }
    }
}
