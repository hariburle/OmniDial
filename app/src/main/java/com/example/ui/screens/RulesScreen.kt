package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.AutomationLog
import com.example.data.CallerRule
import com.example.data.ChannelConfig
import com.example.data.FavoriteContact
import com.example.data.SpamNumber
import com.example.domain.model.CallingChannel
import com.example.ui.components.AutomationLogItem
import com.example.ui.components.RuleCard
import com.example.ui.components.RuleEditDialog
import com.example.util.BackupRestoreResult
import com.example.util.DeviceContact
import kotlinx.coroutines.launch

data class AutomationTemplate(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val defaultRule: CallerRule
)

val standardAutomationTemplates = listOf(
    AutomationTemplate(
        title = "Apartment Gate Buzzer",
        description = "Auto-answer intercom & send 9# unlock tone",
        icon = Icons.Default.Lock,
        defaultRule = CallerRule(
            name = "Gate Buzzer",
            phoneNumberPattern = "",
            isEnabled = true,
            autoAnswer = true,
            answerDelaySec = 1,
            dtmfSequence = "9#",
            dtmfDelayMs = 800,
            sendSms = false,
            smsMessage = "",
            autoHangup = true,
            hangupDelaySec = 2,
            autoSpeakerphone = true,
            autoMuteMic = true
        )
    ),
    AutomationTemplate(
        title = "Office Extension IVR",
        description = "Auto-dial department or room extension 104#",
        icon = Icons.Default.Business,
        defaultRule = CallerRule(
            name = "Office Extension",
            phoneNumberPattern = "",
            isEnabled = true,
            autoAnswer = true,
            answerDelaySec = 2,
            dtmfSequence = "104#",
            dtmfDelayMs = 1000,
            sendSms = false,
            smsMessage = "",
            autoHangup = false
        )
    ),
    AutomationTemplate(
        title = "SMS Auto-Responder",
        description = "Auto-reply when busy and drop the call",
        icon = Icons.AutoMirrored.Filled.Chat,
        defaultRule = CallerRule(
            name = "Auto SMS Responder",
            phoneNumberPattern = "",
            isEnabled = true,
            autoAnswer = false,
            answerDelaySec = 0,
            dtmfSequence = "",
            dtmfDelayMs = 0,
            sendSms = true,
            smsMessage = "I am currently in a meeting. I will call you back shortly.",
            autoHangup = true,
            hangupDelaySec = 1
        )
    ),
    AutomationTemplate(
        title = "Delivery Gate Access",
        description = "Send buzzer 4# and delivery confirmation SMS",
        icon = Icons.Default.Home,
        defaultRule = CallerRule(
            name = "Delivery Gate",
            phoneNumberPattern = "",
            isEnabled = true,
            autoAnswer = true,
            answerDelaySec = 1,
            dtmfSequence = "4#",
            dtmfDelayMs = 800,
            sendSms = true,
            smsMessage = "Lobby gate opened automatically.",
            autoHangup = true,
            hangupDelaySec = 2,
            autoSpeakerphone = true,
            autoMuteMic = true
        )
    ),
    AutomationTemplate(
        title = "Voicemail PIN",
        description = "Auto-enter keypad PIN touch-tones",
        icon = Icons.Default.Voicemail,
        defaultRule = CallerRule(
            name = "Voicemail PIN",
            phoneNumberPattern = "",
            isEnabled = true,
            autoAnswer = false,
            answerDelaySec = 0,
            dtmfSequence = "1234#",
            dtmfDelayMs = 500,
            sendSms = false,
            smsMessage = "",
            autoHangup = false
        )
    )
)

@Composable
fun RulesScreen(
    rules: List<CallerRule>,
    automationLogs: List<AutomationLog>,
    favorites: List<FavoriteContact> = emptyList(),
    themeMode: String = "system",
    onSetThemeMode: (String) -> Unit = {},
    whatsAppCallMode: String = "ask_learn",
    onSetWhatsAppCallMode: (String) -> Unit = {},
    onResetWhatsAppChoices: () -> Unit = {},
    learnedChoicesCount: Int = 0,
    spamNumbers: List<SpamNumber> = emptyList(),
    onAddSpam: (String, String) -> Unit = { _, _ -> },
    onRemoveSpam: (String) -> Unit = {},
    confirmFavoritesCall: Boolean = false,
    onSetConfirmFavoritesCall: (Boolean) -> Unit = {},
    confirmSpeedDialCall: Boolean = true,
    onSetConfirmSpeedDialCall: (Boolean) -> Unit = {},
    askToAssignUnassignedSpeedDial: Boolean = true,
    onSetAskToAssignUnassignedSpeedDial: (Boolean) -> Unit = {},
    speedDialKeypadDisplay: String = "speed_dial_above",
    onSetSpeedDialKeypadDisplay: (String) -> Unit = {},
    showDialerQuickActions: Boolean = true,
    onSetShowDialerQuickActions: (Boolean) -> Unit = {},
    defaultStartTab: Int = 0,
    onSetDefaultStartTab: (Int) -> Unit = {},
    swipeToSwitchPanels: Boolean = true,
    onSetSwipeToSwitchPanels: (Boolean) -> Unit = {},
    navBarStyle: String = "full",
    onSetNavBarStyle: (String) -> Unit = {},
    callAnswerStyle: String = "swipe_slider",
    onSetCallAnswerStyle: (String) -> Unit = {},
    onToggleRule: (CallerRule) -> Unit,
    onSaveRule: (CallerRule) -> Unit,
    onDeleteRule: (CallerRule) -> Unit,
    onClearLogs: () -> Unit,
    onTestRule: (CallerRule) -> Unit = {},
    onDuplicateRule: (CallerRule) -> Unit = {},
    initiallyShowAddRuleWithNumber: String? = null,
    onConsumeAddRuleNumber: () -> Unit = {},
    deviceContacts: List<DeviceContact> = emptyList(),
    onExportBackup: ((android.net.Uri, (Boolean) -> Unit) -> Unit)? = null,
    onImportBackup: ((android.net.Uri, ((String, Float) -> Unit)?, (BackupRestoreResult) -> Unit) -> Unit)? = null,
    localBackups: List<java.io.File> = emptyList(),
    onCreateLocalBackup: (((Boolean) -> Unit) -> Unit)? = null,
    onRestoreLocalBackup: ((java.io.File, ((String, Float) -> Unit)?, (BackupRestoreResult) -> Unit) -> Unit)? = null,
    onDeleteLocalBackup: ((java.io.File) -> Unit)? = null,
    globalSimPreferenceMode: String = "system",
    onSetGlobalSimPreferenceMode: (String) -> Unit = {},
    activeSims: List<com.example.telecom.SimInfo> = emptyList(),
    channelConfigs: List<ChannelConfig> = emptyList(),
    discoveredChannels: List<CallingChannel> = emptyList(),
    onSaveChannelConfigs: ((List<ChannelConfig>) -> Unit)? = null,
    isCallRedirectionRoleHeld: Boolean = true,
    onRequestCallRedirectionRole: () -> Unit = {},
    setupStepStates: List<com.example.ui.components.SetupStepState> = emptyList(),
    onLaunchSetupStep: (com.example.ui.components.SetupStep) -> Unit = {},
    onRerunSetupWizard: () -> Unit = {},
    onOpenAppSettings: () -> Unit = {},
    dismissModalsTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var showRecipesModal by rememberSaveable { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CallerRule?>(null) }

    LaunchedEffect(dismissModalsTrigger) {
        if (dismissModalsTrigger > 0L) {
            showDialog = false
            showHistoryDialog = false
            showRecipesModal = false
            editingRule = null
        }
    }

    BackHandler(enabled = showDialog || showHistoryDialog || showRecipesModal || editingRule != null) {
        if (showDialog) {
            showDialog = false
        } else if (showHistoryDialog) {
            showHistoryDialog = false
        } else if (showRecipesModal) {
            showRecipesModal = false
        } else if (editingRule != null) {
            editingRule = null
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val subPagerState = rememberPagerState(initialPage = 0) { 2 }

    LaunchedEffect(initiallyShowAddRuleWithNumber) {
        if (!initiallyShowAddRuleWithNumber.isNullOrBlank()) {
            selectedTab = 0
            subPagerState.scrollToPage(0)
            editingRule = CallerRule(
                name = "Custom Rule",
                phoneNumberPattern = initiallyShowAddRuleWithNumber,
                isEnabled = true,
                autoAnswer = true,
                answerDelaySec = 1,
                dtmfSequence = "9#",
                dtmfDelayMs = 800,
                sendSms = false,
                smsMessage = "",
                autoHangup = true,
                hangupDelaySec = 2
            )
            showDialog = true
            onConsumeAddRuleNumber()
        }
    }

    LaunchedEffect(subPagerState.currentPage) {
        if (selectedTab != subPagerState.currentPage) {
            selectedTab = subPagerState.currentPage
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("rules_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        coroutineScope.launch { subPagerState.animateScrollToPage(0) }
                    },
                    text = { Text("Caller Rules (${rules.size})") },
                    icon = { Icon(Icons.Default.SmartToy, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        coroutineScope.launch { subPagerState.animateScrollToPage(1) }
                    },
                    text = { Text("Settings") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
            }

            HorizontalPager(
                state = subPagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                if (page == 0) {
                    // Rules Tab Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        // Top Action Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Call Automation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "In-band DTMF buzzer & auto-actions",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = { showRecipesModal = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("rules_recipes_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Recipes",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                OutlinedButton(
                                    onClick = { showHistoryDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .height(34.dp)
                                        .testTag("rules_history_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "History (${automationLogs.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // JIT Call Redirection Warning Banner
                        if (!isCallRedirectionRoleHeld) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AltRoute,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Call Redirection Not Active",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = "Outgoing rules & car routing require Call Redirection to intercept calls.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = onRequestCallRedirectionRole,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary,
                                            contentColor = MaterialTheme.colorScheme.onTertiary
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (rules.isEmpty()) {
                            // Zero-state: Display Quick-Start Recipe Templates Gallery
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Quick-Start Automation Recipes",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Select a pre-configured template below to set up in 1-tap, or create a custom recipe.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                standardAutomationTemplates.forEach { template ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                editingRule = template.defaultRule.copy()
                                                showDialog = true
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = template.icon,
                                                contentDescription = null,
                                                modifier = Modifier.size(24.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = template.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = template.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = "+ Use",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Button(
                                    onClick = {
                                        editingRule = CallerRule(
                                            name = "New Automation Rule",
                                            phoneNumberPattern = "",
                                            isEnabled = true,
                                            autoAnswer = true,
                                            answerDelaySec = 1,
                                            dtmfSequence = "9#",
                                            dtmfDelayMs = 800,
                                            sendSms = false,
                                            smsMessage = "Automated reply sent.",
                                            autoHangup = true,
                                            hangupDelaySec = 2
                                        )
                                        showDialog = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create Custom Rule", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(72.dp))
                            }
                        } else {
                            // Rules List with enhanced visual cards
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(rules, key = { it.id }) { rule ->
                                    RuleCard(
                                        rule = rule,
                                        automationLogs = automationLogs,
                                        onToggle = { onToggleRule(rule) },
                                        onEdit = {
                                            editingRule = rule
                                            showDialog = true
                                        },
                                        onDelete = { onDeleteRule(rule) },
                                        onTest = { onTestRule(rule) },
                                        onDuplicate = { onDuplicateRule(rule) }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(84.dp))
                                }
                            }
                        }
                    }
                } else {
                    // Settings Tab Content
                    SettingsScreen(
                        themeMode = themeMode,
                        onSetThemeMode = onSetThemeMode,
                        whatsAppCallMode = whatsAppCallMode,
                        onSetWhatsAppCallMode = onSetWhatsAppCallMode,
                        onResetWhatsAppChoices = onResetWhatsAppChoices,
                        learnedChoicesCount = learnedChoicesCount,
                        spamNumbers = spamNumbers,
                        onAddSpam = onAddSpam,
                        onRemoveSpam = onRemoveSpam,
                        confirmFavoritesCall = confirmFavoritesCall,
                        onSetConfirmFavoritesCall = onSetConfirmFavoritesCall,
                        confirmSpeedDialCall = confirmSpeedDialCall,
                        onSetConfirmSpeedDialCall = onSetConfirmSpeedDialCall,
                        askToAssignUnassignedSpeedDial = askToAssignUnassignedSpeedDial,
                        onSetAskToAssignUnassignedSpeedDial = onSetAskToAssignUnassignedSpeedDial,
                        speedDialKeypadDisplay = speedDialKeypadDisplay,
                        onSetSpeedDialKeypadDisplay = onSetSpeedDialKeypadDisplay,
                        showDialerQuickActions = showDialerQuickActions,
                        onSetShowDialerQuickActions = onSetShowDialerQuickActions,
                        defaultStartTab = defaultStartTab,
                        onSetDefaultStartTab = onSetDefaultStartTab,
                        swipeToSwitchPanels = swipeToSwitchPanels,
                        onSetSwipeToSwitchPanels = onSetSwipeToSwitchPanels,
                        navBarStyle = navBarStyle,
                        onSetNavBarStyle = onSetNavBarStyle,
                        callAnswerStyle = callAnswerStyle,
                        onSetCallAnswerStyle = onSetCallAnswerStyle,
                        onExportBackup = onExportBackup,
                        onImportBackup = onImportBackup,
                        localBackups = localBackups,
                        onCreateLocalBackup = onCreateLocalBackup,
                        onRestoreLocalBackup = onRestoreLocalBackup,
                        onDeleteLocalBackup = onDeleteLocalBackup,
                        globalSimPreferenceMode = globalSimPreferenceMode,
                        onSetGlobalSimPreferenceMode = onSetGlobalSimPreferenceMode,
                        activeSims = activeSims,
                        channelConfigs = channelConfigs,
                        discoveredChannels = discoveredChannels,
                        onSaveChannelConfigs = onSaveChannelConfigs,
                        setupStepStates = setupStepStates,
                        onLaunchSetupStep = onLaunchSetupStep,
                        onRerunSetupWizard = onRerunSetupWizard,
                        onOpenAppSettings = onOpenAppSettings
                    )
                }
            }
        }

        // Clean, Anchored Single Extended Floating Action Button on Rules Page
        if (subPagerState.currentPage == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp, end = 20.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingRule = CallerRule(
                            name = "New Automation Rule",
                            phoneNumberPattern = "",
                            isEnabled = true,
                            autoAnswer = true,
                            answerDelaySec = 1,
                            dtmfSequence = "9#",
                            dtmfDelayMs = 800,
                            sendSms = false,
                            smsMessage = "Automated reply sent.",
                            autoHangup = true,
                            hangupDelaySec = 2
                        )
                        showDialog = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Rule", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("add_rule_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Recipe Templates Modal Dialog
        if (showRecipesModal) {
            AlertDialog(
                onDismissRequest = { showRecipesModal = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                ),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(vertical = 16.dp)
                    .systemBarsPadding(),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Automation Recipe Templates")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Tap a pre-configured template to load and customize it:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        standardAutomationTemplates.forEach { template ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editingRule = template.defaultRule.copy()
                                        showRecipesModal = false
                                        showDialog = true
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = template.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = template.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = template.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "+ Use",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRecipesModal = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Execution History Dialog
        if (showHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showHistoryDialog = false },
                title = { Text("Execution History (${automationLogs.size})") },
                text = {
                    if (automationLogs.isEmpty()) {
                        Text("No automation history yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recent trigger logs", style = MaterialTheme.typography.titleSmall)
                                TextButton(onClick = onClearLogs) {
                                    Text("Clear")
                                }
                            }
                            automationLogs.forEach { log ->
                                AutomationLogItem(log = log)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHistoryDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }

    // Add / Edit Rule Dialog
    if (showDialog && editingRule != null) {
        RuleEditDialog(
            initialRule = editingRule!!,
            favorites = favorites,
            deviceContacts = deviceContacts,
            onDismiss = {
                showDialog = false
                editingRule = null
                onConsumeAddRuleNumber()
            },
            onSave = { updatedRule ->
                onSaveRule(updatedRule)
                showDialog = false
                editingRule = null
                onConsumeAddRuleNumber()
            }
        )
    }
}
