package com.example.ui.components

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.telecom.OmniCallScreeningService
import com.example.telecom.RoleHelper

/**
 * Settings card for Android's "Caller ID & spam app" default
 * (Settings → Apps → Default apps → Caller ID & spam app).
 *
 * When held, OmniDial screens incoming calls: suspected spam is silenced
 * and logged — never silently dropped — unless the user explicitly enables
 * automatic blocking below.
 */
@Composable
fun CallScreeningCard(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var screeningRoleHeld by remember {
        mutableStateOf(RoleHelper.isCallScreeningRoleHeld(context))
    }
    val prefs = remember {
        context.getSharedPreferences(
            OmniCallScreeningService.PREFS,
            android.content.Context.MODE_PRIVATE
        )
    }
    var autoBlock by remember {
        mutableStateOf(prefs.getBoolean(OmniCallScreeningService.KEY_SPAM_AUTO_BLOCK, false))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                screeningRoleHeld = RoleHelper.isCallScreeningRoleHeld(context)
                autoBlock = prefs.getBoolean(OmniCallScreeningService.KEY_SPAM_AUTO_BLOCK, false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val screeningLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        screeningRoleHeld = RoleHelper.isCallScreeningRoleHeld(context)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Caller ID & Spam",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (screeningRoleHeld) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (screeningRoleHeld) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = if (screeningRoleHeld) Color(0xFF15803D) else Color(0xFFB45309),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (screeningRoleHeld) "Active" else "Action Needed",
                            color = if (screeningRoleHeld) Color(0xFF15803D) else Color(0xFFB45309),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            if (screeningRoleHeld) {
                Text(
                    text = "OmniDial is your Caller ID & spam app. Suspected spam calls are silenced and logged as missed calls — never dropped — so you can always call back.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Block spam automatically",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Off: silence and log. On: reject outright.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoBlock,
                        onCheckedChange = { checked ->
                            autoBlock = checked
                            prefs.edit()
                                .putBoolean(OmniCallScreeningService.KEY_SPAM_AUTO_BLOCK, checked)
                                .apply()
                        }
                    )
                }
                OutlinedButton(
                    onClick = {
                        val intent = RoleHelper.createDefaultAppsSettingsIntent(context)
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Verify in Phone Settings", fontSize = 12.sp)
                }
            } else {
                Text(
                    text = "No app is handling caller ID & spam on this phone. Set OmniDial to silence suspected spam instead of dropping it, so a flagged call you wanted is never lost.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        val intent = RoleHelper.createCallScreeningRoleIntent(context)
                        if (intent != null) {
                            try {
                                screeningLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open role settings", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Not available on this device", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set as Caller ID & Spam App", fontSize = 13.sp)
                }
                // Temporary diagnostic: shows exactly what the system sees.
                // Tells us whether the manifest change made it into this build
                // (service listed below) or the role layer is the problem.
                val screeningPkgs = remember {
                    context.packageManager.queryIntentServices(
                        Intent("android.telecom.CallScreeningService"),
                        PackageManager.MATCH_ALL
                    ).map { it.serviceInfo.packageName }.distinct().sorted()
                }
                val roleAvailable = remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val rm = context.getSystemService(RoleManager::class.java)
                        "Role available: ${rm?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)}"
                    } else "Role API n/a (pre-Q)"
                }
                Text(
                    text = "Diag — $roleAvailable\nScreening services seen: " +
                        screeningPkgs.joinToString().ifBlank { "(none)" },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }
    }
}
