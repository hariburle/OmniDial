package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.AutomationLog
import com.example.data.CallerRule
import com.example.data.FavoriteContact
import com.example.data.SpamNumber
import com.example.ui.components.AutomationLogItem
import com.example.ui.components.RuleCard
import com.example.ui.components.RuleEditDialog
import com.example.util.BackupRestoreResult
import com.example.util.DeviceContact
import kotlinx.coroutines.launch

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
    initiallyShowAddRuleWithNumber: String? = null,
    onConsumeAddRuleNumber: () -> Unit = {},
    deviceContacts: List<DeviceContact> = emptyList(),
    onExportBackup: ((android.net.Uri, (Boolean) -> Unit) -> Unit)? = null,
    onImportBackup: ((android.net.Uri, (BackupRestoreResult) -> Unit) -> Unit)? = null,
    localBackups: List<java.io.File> = emptyList(),
    onCreateLocalBackup: (((Boolean) -> Unit) -> Unit)? = null,
    onRestoreLocalBackup: ((java.io.File, (BackupRestoreResult) -> Unit) -> Unit)? = null,
    onDeleteLocalBackup: ((java.io.File) -> Unit)? = null,
    globalSimPreferenceMode: String = "system",
    onSetGlobalSimPreferenceMode: (String) -> Unit = {},
    activeSims: List<com.example.telecom.SimInfo> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CallerRule?>(null) }

    BackHandler(enabled = showDialog || showHistoryDialog || editingRule != null) {
        if (showDialog) {
            showDialog = false
        } else if (showHistoryDialog) {
            showHistoryDialog = false
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
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        if (rules.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SmartToy,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "No Automation Rules Created Yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Create rules to auto-answer intercoms, dial DTMF extension codes, or auto-reply with SMS.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
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
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Create First Rule", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
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
                                        onDelete = { onDeleteRule(rule) }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(72.dp))
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
                        activeSims = activeSims
                    )
                }
            }
        }

        // Bottom Action Row for Add Rule and Execution History in Rules Page (Page 0)
        if (subPagerState.currentPage == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp, end = 16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showHistoryDialog = true },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        text = { Text("History (${automationLogs.size})") },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
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
                        text = { Text("Create Rule", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("add_rule_fab"),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
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
                                Text("Recent logs", style = MaterialTheme.typography.titleSmall)
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
