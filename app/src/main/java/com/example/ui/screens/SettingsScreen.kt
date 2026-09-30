package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.ChannelConfig
import com.example.data.SpamNumber
import com.example.domain.model.CallingChannel
import com.example.telecom.ChannelDiscoveryManager
import com.example.telecom.OmniCallScreeningService
import com.example.telecom.RoleHelper
import com.example.ui.components.BackupManagementCard
import com.example.ui.components.ChannelSetupDialog
import com.example.ui.components.SpamManagementDialog
import com.example.util.BackupRestoreResult
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    themeMode: String,
    onSetThemeMode: (String) -> Unit,
    whatsAppCallMode: String,
    onSetWhatsAppCallMode: (String) -> Unit,
    onResetWhatsAppChoices: () -> Unit,
    learnedChoicesCount: Int,
    spamNumbers: List<SpamNumber>,
    onAddSpam: (String, String) -> Unit,
    onRemoveSpam: (String) -> Unit,
    confirmFavoritesCall: Boolean,
    onSetConfirmFavoritesCall: (Boolean) -> Unit,
    confirmSpeedDialCall: Boolean = true,
    onSetConfirmSpeedDialCall: (Boolean) -> Unit = {},
    askToAssignUnassignedSpeedDial: Boolean = true,
    onSetAskToAssignUnassignedSpeedDial: (Boolean) -> Unit = {},
    speedDialKeypadDisplay: String = "speed_dial_above",
    onSetSpeedDialKeypadDisplay: (String) -> Unit = {},
    showDialerQuickActions: Boolean = true,
    onSetShowDialerQuickActions: (Boolean) -> Unit = {},
    defaultStartTab: Int,
    onSetDefaultStartTab: (Int) -> Unit,
    swipeToSwitchPanels: Boolean,
    onSetSwipeToSwitchPanels: (Boolean) -> Unit,
    navBarStyle: String = "full",
    onSetNavBarStyle: (String) -> Unit = {},
    callAnswerStyle: String,
    onSetCallAnswerStyle: (String) -> Unit,
    onExportBackup: ((android.net.Uri, (Boolean) -> Unit) -> Unit)? = null,
    onImportBackup: ((android.net.Uri, ((String, Float) -> Unit)?, (BackupRestoreResult) -> Unit) -> Unit)? = null,
    localBackups: List<java.io.File> = emptyList(),
    onCreateLocalBackup: (((BackupRestoreResult) -> Unit) -> Unit)? = null,
    onRestoreLocalBackup: ((java.io.File, ((String, Float) -> Unit)?, (BackupRestoreResult) -> Unit) -> Unit)? = null,
    onDeleteLocalBackup: ((java.io.File) -> Unit)? = null,
    globalSimPreferenceMode: String = "system",
    onSetGlobalSimPreferenceMode: (String) -> Unit = {},
    activeSims: List<com.example.telecom.SimInfo> = emptyList(),
    channelConfigs: List<ChannelConfig> = emptyList(),
    discoveredChannels: List<CallingChannel> = emptyList(),
    onSaveChannelConfigs: ((List<ChannelConfig>) -> Unit)? = null,
    setupStepStates: List<com.example.ui.components.SetupStepState> = emptyList(),
    onLaunchSetupStep: (com.example.ui.components.SetupStep) -> Unit = {},
    onRerunSetupWizard: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var showSpamDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showChannelConfigDialog by remember { mutableStateOf(false) }
    var backupStatusMessage by remember { mutableStateOf<String?>(null) }

    val discoveryManager = remember(context) { ChannelDiscoveryManager.getInstance(context) }
    val effectiveDiscoveredChannels = if (discoveredChannels.isNotEmpty()) discoveredChannels else discoveryManager.allDiscoveredChannels.collectAsState().value

    // Caller ID & Spam screening role status and auto-block preference
    var screeningRoleHeld by remember {
        mutableStateOf(RoleHelper.isCallScreeningRoleHeld(context))
    }
    val screeningPrefs = remember {
        context.getSharedPreferences(OmniCallScreeningService.PREFS, android.content.Context.MODE_PRIVATE)
    }
    var autoBlockSpam by remember {
        mutableStateOf(screeningPrefs.getBoolean(OmniCallScreeningService.KEY_SPAM_AUTO_BLOCK, false))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                screeningRoleHeld = RoleHelper.isCallScreeningRoleHeld(context)
                autoBlockSpam = screeningPrefs.getBoolean(OmniCallScreeningService.KEY_SPAM_AUTO_BLOCK, false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val screeningLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        screeningRoleHeld = RoleHelper.isCallScreeningRoleHeld(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ---------------------------------------------------------------------------------
        // 1. SYSTEM HEALTH & ROLES (TOP)
        // ---------------------------------------------------------------------------------
        if (setupStepStates.isNotEmpty()) {
            Text(
                text = "App Permissions & Setup",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            PermissionsHubCard(
                setupStepStates = setupStepStates,
                onLaunchSetupStep = onLaunchSetupStep,
                onRerunSetupWizard = onRerunSetupWizard,
                onOpenAppSettings = onOpenAppSettings
            )
        }

        // ---------------------------------------------------------------------------------
        // 2. CALLING CHANNELS & DISPATCH POLICIES
        // ---------------------------------------------------------------------------------
        Text(
            text = "Manage Channels",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Channel Selection & Custom Labels",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Choose which channels appear in OmniDial, and customize their names",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            discoveryManager.refreshChannels()
                            showChannelConfigDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("manage_channels_button")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Manage", fontSize = 12.sp)
                    }
                }

                // Summary chips of configured channels
                if (effectiveDiscoveredChannels.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val configMap = channelConfigs.associateBy { it.channelId }
                        effectiveDiscoveredChannels.forEach { ch ->
                            val cfg = configMap[ch.id]
                            val isEnabled = cfg?.isEnabled ?: true
                            val displayName = cfg?.customName?.takeIf { it.isNotBlank() } ?: ch.shortLabel
                            val brandColor = Color(ch.brandColorHex)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isEnabled) brandColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(1.dp, if (isEnabled) brandColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (isEnabled) brandColor else Color.Gray, CircleShape)
                                    )
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    if (!isEnabled) {
                                        Text(
                                            text = "(Off)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Channel Preferences",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val options = listOf(
                    Triple("ask_learn", "Smart Routing", "Rules auto-fire. Asks once if no rule matches, then remembers") to Icons.Default.AutoAwesome,
                    Triple("ask_always", "Always Ask", "Show channel picker on every call") to Icons.AutoMirrored.Filled.HelpOutline,
                    Triple("never", "Cellular Only", "Standard carrier calls only. Rules ignored") to Icons.Default.PhoneDisabled
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    options.forEach { (info, icon) ->
                        val (mode, label, desc) = info
                        val isSelected = whatsAppCallMode == mode || (mode == "ask_learn" && whatsAppCallMode == "all_international")
                        Card(
                            onClick = { onSetWhatsAppCallMode(mode) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("whatsapp_mode_$mode")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
                if (whatsAppCallMode == "ask_learn" || whatsAppCallMode == "all_international") {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "$learnedChoicesCount contact choice(s) remembered",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Choices and Learn Memory", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Smart Travel & Roaming Card
        val travelRoamingManager = remember(context) { com.example.telecom.TravelRoamingManager.getInstance(context) }
        var isSmartRoaming by remember { mutableStateOf(travelRoamingManager.isSmartRoamingEnabled()) }
        val currentCountryIso = remember { travelRoamingManager.getCurrentCountryIso().uppercase() }
        val isCurrentlyTraveling = remember { travelRoamingManager.isTraveling() }

        Text(
            text = "Smart Travel & Roaming",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = Icons.Default.Flight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Zero-Touch Smart Roaming",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Auto-detects travel abroad. Routes home (+1) calls via WhatsApp to eliminate roaming fees, and domestic calls via local SIM. Saved preferences remain 100% preserved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isSmartRoaming,
                        onCheckedChange = { checked ->
                            isSmartRoaming = checked
                            travelRoamingManager.setSmartRoamingEnabled(checked)
                        },
                        modifier = Modifier.testTag("smart_roaming_switch")
                    )
                }

                // Current location status badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCurrentlyTraveling) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyTraveling) Icons.Default.Public else Icons.Default.Home,
                            contentDescription = null,
                            tint = if (isCurrentlyTraveling) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isCurrentlyTraveling) "Status: Traveling abroad ($currentCountryIso) • Roaming Protection Active"
                                   else "Status: Home Region ($currentCountryIso) • Roaming Protection Ready",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (isSmartRoaming) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Home Region & Permanent Relocation Assistant
                    var showRelocationDialog by remember { mutableStateOf(false) }
                    var homeCountryIso by remember { mutableStateOf(travelRoamingManager.getHomeCountryIso()) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Home Region & Primary Profile",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Current Home: ${if (homeCountryIso == "us") "🇺🇸 United States (US)" else if (homeCountryIso == "in") "🇮🇳 India (IN)" else homeCountryIso.uppercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { showRelocationDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Relocate", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (showRelocationDialog) {
                        var targetIso by remember { mutableStateOf(if (homeCountryIso == "us") "in" else "us") }
                        var promotePrefs by remember { mutableStateOf(true) }

                        AlertDialog(
                            onDismissRequest = { showRelocationDialog = false },
                            icon = {
                                Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            title = {
                                Text("Permanent Relocation Assistant", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "Moving to a new country permanently? Update your Home Region to switch your baseline calling profile.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text("Select New Home Country:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = targetIso == "in",
                                            onClick = { targetIso = "in" },
                                            label = { Text("🇮🇳 India (+91)") }
                                        )
                                        FilterChip(
                                            selected = targetIso == "us",
                                            onClick = { targetIso = "us" },
                                            label = { Text("🇺🇸 United States (+1)") }
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = promotePrefs,
                                                onCheckedChange = { promotePrefs = it }
                                            )
                                            Column {
                                                Text("Promote Travel to Home Profile", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                                Text("Promote contacts learned in $targetIso to become your permanent home baseline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val repo = com.example.data.AppRepository(com.example.data.AppDatabase.getInstance(context).appDao())
                                            travelRoamingManager.relocateHomeCountry(targetIso, promotePrefs, repo)
                                            homeCountryIso = targetIso
                                            showRelocationDialog = false
                                            Toast.makeText(context, "★ Home region relocated to ${targetIso.uppercase()}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Text("Confirm Relocation")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showRelocationDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    // Roaming Guard Intercept toggle
                    var roamingGuard by remember { mutableStateOf(travelRoamingManager.isRoamingGuardEnabled()) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Pre-Call Roaming Tariff Warning",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Confirms before placing any cellular call on a roaming carrier SIM",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = roamingGuard,
                            onCheckedChange = { checked ->
                                roamingGuard = checked
                                travelRoamingManager.setRoamingGuardEnabled(checked)
                            },
                            modifier = Modifier.testTag("roaming_guard_switch")
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ---------------------------------------------------------------------------------
        // 3. KEYPAD & SPEED DIAL
        // ---------------------------------------------------------------------------------
        Text(
            text = "Keypad & Speed Dial",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Confirm Before Calling Favorites", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Displays a confirmation dialog to prevent accidental calls when tapping favorites",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = confirmFavoritesCall,
                        onCheckedChange = onSetConfirmFavoritesCall,
                        modifier = Modifier.testTag("confirm_favorites_call_switch")
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Confirm Before Speed-Dialing", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Show confirmation dialog before calling when long-pressing keys (2–9)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = confirmSpeedDialCall,
                        onCheckedChange = onSetConfirmSpeedDialCall,
                        modifier = Modifier.testTag("confirm_speed_dial_call_switch")
                    )
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Ask to Assign Unassigned Keys", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Prompt to assign a contact when long-pressing an unassigned keypad key (2–9)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = askToAssignUnassignedSpeedDial,
                        onCheckedChange = onSetAskToAssignUnassignedSpeedDial,
                        modifier = Modifier.testTag("ask_assign_speed_dial_switch")
                    )
                }

                HorizontalDivider()

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dialpad,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Speed Dial Keypad Display",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = "Control whether speed dial contact names, T9 letters, or both appear on the keypad buttons",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val speedDialOptions = listOf(
                            "speed_dial_above",
                            "t9_only",
                            "speed_dial_only"
                        )

                        speedDialOptions.forEach { modeKey ->
                            val isSelected = speedDialKeypadDisplay == modeKey
                            val label = when (modeKey) {
                                "speed_dial_above" -> "Fav & T9"
                                "t9_only" -> "T9 Only"
                                else -> "Fav Only"
                            }
                            Card(
                                onClick = { onSetSpeedDialKeypadDisplay(modeKey) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                border = BorderStroke(
                                    if (isSelected) 2.5.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(98.dp)
                                    .testTag("speed_dial_display_$modeKey")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "2",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                when (modeKey) {
                                                    "speed_dial_above" -> {
                                                        Text(
                                                            text = "Mom",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            maxLines = 1
                                                        )
                                                        Text(
                                                            text = "ABC",
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    "t9_only" -> {
                                                        Text(
                                                            text = "ABC",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    "speed_dial_only" -> {
                                                        Text(
                                                            text = "Mom",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Show Quick Action Buttons",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Keep SMS, WhatsApp Chat, and Secondary Call buttons beneath the dialpad without layout jumping",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = showDialerQuickActions,
                        onCheckedChange = onSetShowDialerQuickActions,
                        modifier = Modifier.testTag("show_dialer_quick_actions_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ---------------------------------------------------------------------------------
        // 4. SPAM & CALL PROTECTION (UNIFIED)
        // ---------------------------------------------------------------------------------
        Text(
            text = "Spam & Call Protection",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Screening status header
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
                            text = "Caller ID & Spam Filtering",
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

                if (!screeningRoleHeld) {
                    Text(
                        text = "Android requires granting the Caller ID & Spam app role so OmniDial can screen incoming calls and detect suspected spam.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            val intent = RoleHelper.createCallScreeningRoleIntent(context)
                            if (intent != null) {
                                try {
                                    screeningLauncher.launch(intent)
                                } catch (_: Exception) {
                                    screeningLauncher.launch(RoleHelper.createDefaultAppsSettingsIntent(context))
                                }
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enable Spam Screening", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "OmniDial screens incoming calls: suspected spam is silenced and logged as a missed call so you can always check back.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Auto-block toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Block Suspected Spam",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (autoBlockSpam) "On: reject outright (caller never rings)" else "Off: silence and log as missed call",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoBlockSpam,
                        onCheckedChange = { checked ->
                            autoBlockSpam = checked
                            screeningPrefs.edit().putBoolean(OmniCallScreeningService.KEY_SPAM_AUTO_BLOCK, checked).apply()
                        },
                        modifier = Modifier.testTag("auto_block_spam_switch")
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // Blocked numbers list entry
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSpamDialog = true }
                        .testTag("entry_spam_management")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Manage Blocked Numbers & Rules",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (spamNumbers.isNotEmpty()) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "${spamNumbers.size} blocked",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (spamNumbers.isNotEmpty()) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open Spam Manager",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ---------------------------------------------------------------------------------
        // 5. APPEARANCE & NAVIGATION
        // ---------------------------------------------------------------------------------
        Text(
            text = "Appearance & Navigation",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 1. Theme Mode
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Theme Mode",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themeOptions = listOf(
                            Triple("system", "System", Icons.Default.BrightnessAuto),
                            Triple("light", "Light", Icons.Default.LightMode),
                            Triple("dark", "Dark", Icons.Default.DarkMode)
                        )
                        themeOptions.forEach { (mode, label, icon) ->
                            val selected = themeMode == mode
                            Card(
                                onClick = { onSetThemeMode(mode) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected)
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("theme_option_$mode")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 2. Incoming Call Answering Style
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Incoming Call Answering Style",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val answerStyles = listOf(
                            Triple("swipe_slider", "Slide", "Slider") to Icons.Default.Swipe,
                            Triple("swipe_up", "Swipe Up", "Gesture") to Icons.Default.KeyboardArrowUp,
                            Triple("button_tap", "Buttons", "Direct tap") to Icons.Default.TouchApp
                        )
                        answerStyles.forEach { (info, icon) ->
                            val (style, label, desc) = info
                            val isSelected = callAnswerStyle == style
                            Card(
                                onClick = { onSetCallAnswerStyle(style) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("call_answer_style_$style")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 3. Navigation Bar Style
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Navigation Bar Style",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val navOptions = listOf(
                            Triple("full", "Standard", "Icons & text"),
                            Triple("compact", "Compact", "Icons only"),
                            Triple("indicator", "Minimal", "Gesture bar")
                        )
                        navOptions.forEach { (styleKey, title, subtitle) ->
                            val isSelected = navBarStyle == styleKey
                            Card(
                                onClick = { onSetNavBarStyle(styleKey) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("nav_bar_style_$styleKey")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 4. Default Startup Screen
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Default Startup Screen",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tabs = listOf(
                            0 to ("Favorites" to Icons.Default.Star),
                            1 to ("Recents" to Icons.Default.History),
                            2 to ("Keypad" to Icons.Default.Dialpad),
                            3 to ("Contacts" to Icons.Default.Contacts)
                        )
                        tabs.forEach { (index, tabData) ->
                            val (label, icon) = tabData
                            val isSelected = defaultStartTab == index
                            Card(
                                onClick = { onSetDefaultStartTab(index) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("nav_tab_button_$index")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 5. Swipe to switch panels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Swipe to switch panels",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Swipe horizontally across main screens (Favorites <-> Recents <-> Keypad <-> Contacts <-> Rules)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = swipeToSwitchPanels,
                        onCheckedChange = onSetSwipeToSwitchPanels,
                        modifier = Modifier.testTag("swipe_to_switch_panels_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ---------------------------------------------------------------------------------
        // 6. BACKUP & MAINTENANCE
        // ---------------------------------------------------------------------------------
        BackupManagementCard(
            localBackups = localBackups,
            onCreateLocalBackup = onCreateLocalBackup,
            onRestoreLocalBackup = onRestoreLocalBackup,
            onDeleteLocalBackup = onDeleteLocalBackup,
            onExportBackup = onExportBackup,
            onImportBackup = onImportBackup,
            onStatusMessage = { msg -> backupStatusMessage = msg },
            onLoadingChanged = { /* handled */ }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Footer credit
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Developed by Hari Burle with Google AI Studio",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(72.dp))
    }

    if (showSpamDialog) {
        SpamManagementDialog(
            spamNumbers = spamNumbers,
            onAddSpam = onAddSpam,
            onRemoveSpam = onRemoveSpam,
            onDismiss = { showSpamDialog = false }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = "Reset All Learned Channels?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will clear all learned Phone vs WhatsApp calling choices for all contacts ($learnedChoicesCount contacts remembered). You can set preferences per contact again at any time.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResetWhatsAppChoices()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "★ Reset all learned choices", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (backupStatusMessage != null) {
        AlertDialog(
            onDismissRequest = { backupStatusMessage = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Backup,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Backup & Restore",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = backupStatusMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { backupStatusMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (showChannelConfigDialog) {
        ChannelSetupDialog(
            discoveredChannels = effectiveDiscoveredChannels,
            existingConfigs = channelConfigs,
            isOnboarding = false,
            onSave = { configs ->
                onSaveChannelConfigs?.invoke(configs)
                showChannelConfigDialog = false
            },
            onDismiss = { showChannelConfigDialog = false }
        )
    }
}

@Composable
private fun PermissionsHubCard(
    setupStepStates: List<com.example.ui.components.SetupStepState>,
    onLaunchSetupStep: (com.example.ui.components.SetupStep) -> Unit,
    onRerunSetupWizard: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val anyAttentionNeeded =
        setupStepStates.any { it.status != com.example.ui.components.SetupStepStatus.DONE }
    // Start collapsed when everything is done; expanded when something needs attention.
    var expanded by remember(setupStepStates) { mutableStateOf(anyAttentionNeeded) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Permissions & System Roles",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Live status of permissions and system roles needed by OmniDial",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse setup status" else "Expand setup status",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!expanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    setupStepStates.forEach { state ->
                        val info = com.example.ui.components.setupStepInfo(state.step)
                        val isDone = state.status == com.example.ui.components.SetupStepStatus.DONE
                        val isSkipped = state.status == com.example.ui.components.SetupStepStatus.SKIPPED
                        val statusColor = when {
                            isDone -> Color(0xFF166534)
                            isSkipped -> Color.Gray
                            else -> MaterialTheme.colorScheme.error
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = statusColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .clickable(enabled = !isDone) { onLaunchSetupStep(state.step) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDone) Icons.Default.Check else info.icon,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = info.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isDone) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (expanded) {
                setupStepStates.forEach { state ->
                    val info = com.example.ui.components.setupStepInfo(state.step)
                    val isDone = state.status == com.example.ui.components.SetupStepStatus.DONE

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDone) Color(0xFF166534).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = info.icon,
                                        contentDescription = null,
                                        tint = if (isDone) Color(0xFF166534) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = info.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = info.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (isDone) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF166534).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFF166534).copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF15803D),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { onLaunchSetupStep(state.step) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onRerunSetupWizard,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Setup Wizard", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onOpenAppSettings,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("App Settings", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
