package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.BackupManager
import com.example.util.BackupRestoreResult
import kotlinx.coroutines.launch
import java.io.File

sealed interface RestoreDialogState {
    data class Progress(
        val title: String,
        val step: String,
        val progress: Float
    ) : RestoreDialogState

    data class Complete(
        val result: BackupRestoreResult
    ) : RestoreDialogState
}

@Composable
fun BackupManagementCard(
    localBackups: List<File>,
    onCreateLocalBackup: (((Boolean) -> Unit) -> Unit)?,
    onRestoreLocalBackup: ((File, ((String, Float) -> Unit)?, (BackupRestoreResult) -> Unit) -> Unit)?,
    onDeleteLocalBackup: ((File) -> Unit)?,
    onExportBackup: ((Uri, (Boolean) -> Unit) -> Unit)?,
    onImportBackup: ((Uri, ((String, Float) -> Unit)?, (BackupRestoreResult) -> Unit) -> Unit)?,
    onStatusMessage: (String) -> Unit = {},
    onLoadingChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var backupToRestore by remember { mutableStateOf<File?>(null) }
    var backupToDelete by remember { mutableStateOf<File?>(null) }
    var restoreDialogState by remember { mutableStateOf<RestoreDialogState?>(null) }

    // External file picker launcher for restoring backups transferred from another device or downloaded
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onLoadingChanged(true)
            restoreDialogState = RestoreDialogState.Progress(
                title = "Restoring Backup",
                step = "Opening backup file…",
                progress = 0.05f
            )
            if (onImportBackup != null) {
                onImportBackup(
                    uri,
                    { step, progress ->
                        restoreDialogState = RestoreDialogState.Progress("Restoring Backup", step, progress)
                    }
                ) { result ->
                    onLoadingChanged(false)
                    restoreDialogState = RestoreDialogState.Complete(result)
                }
            } else {
                coroutineScope.launch {
                    val result = BackupManager.restoreBackupFromUri(context, uri) { step, progress ->
                        restoreDialogState = RestoreDialogState.Progress("Restoring Backup", step, progress)
                    }
                    onLoadingChanged(false)
                    restoreDialogState = RestoreDialogState.Complete(result)
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Automatic Backups & Restore",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Saves your rules, contacts, speed dials, and preferences to persistent device storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Primary single-tap "Create Backup" action
            Button(
                onClick = {
                    onLoadingChanged(true)
                    if (onCreateLocalBackup != null) {
                        onCreateLocalBackup { success ->
                            onLoadingChanged(false)
                            android.widget.Toast.makeText(
                                context,
                                if (success) "Backup saved to device storage" else "Failed to create backup",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("button_create_local_backup"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Backup Now", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Saved Backups (${localBackups.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = {
                        importLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Browse Files", fontSize = 12.sp)
                }
            }

            if (localBackups.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No backups found in app storage.\nTap 'Backup Now' to create one, or 'Browse files' to restore from Documents or Downloads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    localBackups.take(6).forEach { file ->
                        val dateStr = try {
                            val parts = file.name.substringAfter("omnidial_backup_").substringBefore(".bak").substringBefore(".json").split("_")
                            if (parts.size == 2) {
                                val ymd = parts[0]
                                val hms = parts[1]
                                "${ymd.take(4)}-${ymd.substring(4, 6)}-${ymd.substring(6, 8)} ${hms.take(2)}:${hms.substring(2, 4)}:${hms.substring(4, 6)}"
                            } else {
                                file.name
                            }
                        } catch (e: Exception) {
                            file.name
                        }
                        val sizeStr = "${(file.length() / 1024.0).let { "%.1f".format(it) }} KB"

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.SettingsBackupRestore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = dateStr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$sizeStr • Auto-Saved Backup",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Restore Button
                                FilledTonalButton(
                                    onClick = { backupToRestore = file },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFFDCFCE7),
                                        contentColor = Color(0xFF15803D)
                                    ),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = "Restore",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Delete Button
                                IconButton(
                                    onClick = { backupToDelete = file },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete this backup",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Restore
    if (backupToRestore != null) {
        val targetFile = backupToRestore!!
        AlertDialog(
            onDismissRequest = { backupToRestore = null },
            title = {
                Text(
                    text = "Restore Backup?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "This will restore your settings, routing rules, favorites, and speed dials from this backup file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = targetFile
                        backupToRestore = null
                        onLoadingChanged(true)
                        restoreDialogState = RestoreDialogState.Progress(
                            title = "Restoring Backup",
                            step = "Opening backup file…",
                            progress = 0.05f
                        )
                        if (onRestoreLocalBackup != null) {
                            onRestoreLocalBackup(
                                file,
                                { step, progress ->
                                    restoreDialogState = RestoreDialogState.Progress("Restoring Backup", step, progress)
                                }
                            ) { result ->
                                onLoadingChanged(false)
                                restoreDialogState = RestoreDialogState.Complete(result)
                            }
                        } else {
                            coroutineScope.launch {
                                val result = BackupManager.restoreBackupFromFile(context, file) { step, progress ->
                                    restoreDialogState = RestoreDialogState.Progress("Restoring Backup", step, progress)
                                }
                                onLoadingChanged(false)
                                restoreDialogState = RestoreDialogState.Complete(result)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Restore Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToRestore = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Active Restoration Progress & Completion Summary Dialog
    if (restoreDialogState != null) {
        val currentState = restoreDialogState!!
        AlertDialog(
            onDismissRequest = {
                if (currentState is RestoreDialogState.Complete) {
                    restoreDialogState = null
                }
            },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = currentState is RestoreDialogState.Complete,
                dismissOnClickOutside = currentState is RestoreDialogState.Complete
            ),
            icon = {
                when (currentState) {
                    is RestoreDialogState.Progress -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    is RestoreDialogState.Complete -> {
                        if (currentState.result.success) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            },
            title = {
                Text(
                    text = when (currentState) {
                        is RestoreDialogState.Progress -> currentState.title
                        is RestoreDialogState.Complete -> if (currentState.result.success) "Restore Completed" else "Restore Failed"
                    },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                when (currentState) {
                    is RestoreDialogState.Progress -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = currentState.step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = { currentState.progress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Restoring data…",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "${(currentState.progress.coerceIn(0f, 1f) * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    is RestoreDialogState.Complete -> {
                        val result = currentState.result
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = result.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (result.success) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (result.rulesCount > 0) Text("• ${result.rulesCount} call routing rules", style = MaterialTheme.typography.bodySmall)
                                        if (result.favoritesCount > 0) Text("• ${result.favoritesCount} favorite contacts", style = MaterialTheme.typography.bodySmall)
                                        if (result.contactsCount > 0) Text("• ${result.contactsCount} local contacts", style = MaterialTheme.typography.bodySmall)
                                        if (result.recentCallsCount > 0) Text("• ${result.recentCallsCount} call logs", style = MaterialTheme.typography.bodySmall)
                                        if (result.spamCount > 0) Text("• ${result.spamCount} blocked spam numbers", style = MaterialTheme.typography.bodySmall)
                                        if (result.channelPreferencesCount > 0) Text("• ${result.channelPreferencesCount} SIM / channel preferences", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (currentState is RestoreDialogState.Complete) {
                    Button(
                        onClick = { restoreDialogState = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = null
        )
    }

    // Confirmation Dialog for Delete
    if (backupToDelete != null) {
        val targetFile = backupToDelete!!
        AlertDialog(
            onDismissRequest = { backupToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Delete Backup?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently remove this backup file from your device storage?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        backupToDelete = null
                        if (onDeleteLocalBackup != null) {
                            onDeleteLocalBackup(targetFile)
                            android.widget.Toast.makeText(context, "Backup deleted", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { backupToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
