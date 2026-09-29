package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import com.example.data.TelecomRoutingRule
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
import androidx.compose.material3.HorizontalDivider
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
    routingRules: List<TelecomRoutingRule> = emptyList(),
    onToggleRoutingRule: (TelecomRoutingRule) -> Unit = {},
    onSaveRoutingRule: (TelecomRoutingRule) -> Unit = {},
    onDeleteRoutingRule: (TelecomRoutingRule) -> Unit = {},
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
    onCreateLocalBackup: (((BackupRestoreResult) -> Unit) -> Unit)? = null,
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
    var rulesFilterIndex by rememberSaveable { mutableIntStateOf(0) } // 0: All, 1: Smart Routing, 2: Call Automation
    var showCreateRuleChoiceDialog by rememberSaveable { mutableStateOf(false) }
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var showRecipesModal by rememberSaveable { mutableStateOf(false) }
    var showRoutingDialog by rememberSaveable { mutableStateOf(false) }
    var showRoutingRecipesModal by rememberSaveable { mutableStateOf(false) }
    var editingRule by remember { mutableStateOf<CallerRule?>(null) }
    var editingRoutingRule by remember { mutableStateOf<TelecomRoutingRule?>(null) }

    LaunchedEffect(dismissModalsTrigger) {
        if (dismissModalsTrigger > 0L) {
            showCreateRuleChoiceDialog = false
            showDialog = false
            showHistoryDialog = false
            showRecipesModal = false
            showRoutingDialog = false
            showRoutingRecipesModal = false
            editingRule = null
            editingRoutingRule = null
        }
    }

    BackHandler(enabled = showCreateRuleChoiceDialog || showDialog || showHistoryDialog || showRecipesModal || showRoutingDialog || showRoutingRecipesModal || editingRule != null || editingRoutingRule != null) {
        if (showCreateRuleChoiceDialog) {
            showCreateRuleChoiceDialog = false
        } else if (showDialog) {
            showDialog = false
        } else if (showHistoryDialog) {
            showHistoryDialog = false
        } else if (showRecipesModal) {
            showRecipesModal = false
        } else if (showRoutingDialog) {
            showRoutingDialog = false
        } else if (showRoutingRecipesModal) {
            showRoutingRecipesModal = false
        } else if (editingRule != null) {
            editingRule = null
        } else if (editingRoutingRule != null) {
            editingRoutingRule = null
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val subPagerState = rememberPagerState(initialPage = 0) { 2 }

    LaunchedEffect(initiallyShowAddRuleWithNumber) {
        if (!initiallyShowAddRuleWithNumber.isNullOrBlank()) {
            selectedTab = 0
            rulesFilterIndex = 2
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
                    text = { Text("Rules (${routingRules.size + rules.size})") },
                    icon = { Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null) }
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
                    // Unified Rules Panel (Smart Telecom Routing & Call Automation)
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Category Filter Chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = rulesFilterIndex == 0,
                                onClick = { rulesFilterIndex = 0 },
                                label = {
                                    Text(
                                        "All (${routingRules.size + rules.size})",
                                        fontWeight = if (rulesFilterIndex == 0) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                            FilterChip(
                                selected = rulesFilterIndex == 1,
                                onClick = { rulesFilterIndex = 1 },
                                label = {
                                    Text(
                                        "Smart Routing (${routingRules.size})",
                                        fontWeight = if (rulesFilterIndex == 1) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                            FilterChip(
                                selected = rulesFilterIndex == 2,
                                onClick = { rulesFilterIndex = 2 },
                                label = {
                                    Text(
                                        "Call Automation (${rules.size})",
                                        fontWeight = if (rulesFilterIndex == 2) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }

                        // Unified Scrollable List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 84.dp)
                        ) {
                            // ==========================================
                            // SECTION 1: SMART TELECOM ROUTING
                            // ==========================================
                            if (rulesFilterIndex == 0 || rulesFilterIndex == 1) {
                                item(key = "section_header_routing") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp, bottom = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Smart Telecom Routing",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Dynamic @ category slot rules (Travel & Roaming)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            OutlinedButton(
                                                onClick = { showRoutingRecipesModal = true },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Recipes", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                            Button(
                                                onClick = { editingRoutingRule = null; showRoutingDialog = true },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                if (routingRules.isEmpty()) {
                                    item(key = "empty_routing_rules") {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                                                Text("No Custom Routing Rules Configured", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "Compose rules using @location, @numbers, @channel, and @guard slots to auto-route calls seamlessly while traveling.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                                Button(
                                                    onClick = { editingRoutingRule = null; showRoutingDialog = true },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Create Routing Rule", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    items(routingRules, key = { "routing_${it.id}" }) { rRule ->
                                        val waColor = Color(0xFF25D366)
                                        Card(
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(text = rRule.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                        Text(text = rRule.ruleExpression, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                    Switch(
                                                        checked = rRule.isEnabled,
                                                        onCheckedChange = { onToggleRoutingRule(rRule) }
                                                    )
                                                }

                                                // Visual Category Slot Badges
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                                    ) {
                                                        Text(
                                                            text = "📍 ${rRule.locationPattern}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                                    ) {
                                                        Text(
                                                            text = "📞 ${rRule.destinationPrefix}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = waColor.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "📱 ${rRule.targetChannelId}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    TextButton(
                                                        onClick = {
                                                            editingRoutingRule = rRule
                                                            showRoutingDialog = true
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Edit", fontSize = 12.sp)
                                                    }
                                                    TextButton(
                                                        onClick = { onDeleteRoutingRule(rRule) },
                                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Delete", fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Visual divider between sections when viewing "All"
                            if (rulesFilterIndex == 0) {
                                item(key = "section_divider") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }

                            // ==========================================
                            // SECTION 2: CALL AUTOMATION
                            // ==========================================
                            if (rulesFilterIndex == 0 || rulesFilterIndex == 2) {
                                item(key = "section_header_automation") {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        // Top Action Header
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
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
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        // JIT Call Redirection Warning Banner
                                        if (!isCallRedirectionRoleHeld) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.AltRoute,
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
                                    }
                                }

                                if (rules.isEmpty()) {
                                    item(key = "empty_automation_rules") {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                                                Text("No Call Automation Rules Configured", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "Set up automated answering, in-band DTMF gate codes, or automated SMS responses for specific callers.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    OutlinedButton(
                                                        onClick = { showRecipesModal = true },
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Recipes", fontSize = 12.sp)
                                                    }
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
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Create Automation Rule", fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    items(rules, key = { "caller_${it.id}" }) { rule ->
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

        // Single Unified Floating Action Button on Rules Tab (page 0)
        if (subPagerState.currentPage == 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp, end = 20.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        when (rulesFilterIndex) {
                            1 -> {
                                editingRoutingRule = null
                                showRoutingDialog = true
                            }
                            2 -> {
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
                            }
                            else -> {
                                showCreateRuleChoiceDialog = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = when (rulesFilterIndex) {
                                1 -> Icons.AutoMirrored.Filled.AltRoute
                                2 -> Icons.Default.SmartToy
                                else -> Icons.Default.Add
                            },
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(
                            text = when (rulesFilterIndex) {
                                1 -> "New Routing Rule"
                                2 -> "New Automation Rule"
                                else -> "New Rule"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    },
                    modifier = Modifier.testTag("add_rule_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        // Create Rule Choice Modal Dialog (Choosing rule type instead of auto-picking)
        if (showCreateRuleChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showCreateRuleChoiceDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Create New Rule")
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "What type of rule would you like to create?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Option 1: Smart Telecom Routing Rule
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCreateRuleChoiceDialog = false
                                    editingRoutingRule = null
                                    showRoutingDialog = true
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
                                    imageVector = Icons.AutoMirrored.Filled.AltRoute,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Smart Telecom Routing Rule",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Route calls by roaming status, destination prefix, or calling app channel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Option 2: Call Automation Rule
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCreateRuleChoiceDialog = false
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
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Call Automation Rule",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Auto-answer, play DTMF gate tones, send SMS, or hang up on specific callers",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Option 3: Explore Recipes
                        OutlinedButton(
                            onClick = {
                                showCreateRuleChoiceDialog = false
                                showRoutingRecipesModal = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Browse Recipe Templates")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCreateRuleChoiceDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
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

    // Dynamic @ Slot Rule Composer Dialog
    if (showRoutingDialog) {
        DynamicRuleComposerDialog(
            initialRule = editingRoutingRule,
            availableChannels = discoveredChannels,
            onDismiss = {
                showRoutingDialog = false
                editingRoutingRule = null
            },
            onSave = { rule ->
                onSaveRoutingRule(rule)
                showRoutingDialog = false
                editingRoutingRule = null
            }
        )
    }

    // Pre-Configured Routing Recipes Modal
    if (showRoutingRecipesModal) {
        val recipes = listOf(
            TelecomRoutingRule(
                name = "International Calls via WhatsApp VoIP",
                ruleExpression = "When @location: Any, route @numbers: International via @channel: WhatsApp",
                targetChannelId = "whatsapp",
                locationPattern = "any",
                destinationPrefix = "all_intl",
                guardAction = "warn_roaming",
                isEnabled = true,
                priority = 100
            ),
            TelecomRoutingRule(
                name = "Cross-Border Calls via WhatsApp Business",
                ruleExpression = "When @location: Roaming, route @numbers: International via @channel: WhatsApp Business",
                targetChannelId = "whatsapp_business",
                locationPattern = "roaming",
                destinationPrefix = "all_intl",
                guardAction = "warn_roaming",
                isEnabled = true,
                priority = 90
            ),
            TelecomRoutingRule(
                name = "Domestic Local Calls via SIM 2",
                ruleExpression = "When @location: Abroad, route @numbers: Domestic via @channel: SIM 2",
                targetChannelId = "sim_2",
                locationPattern = "roaming",
                destinationPrefix = "domestic",
                guardAction = "warn_roaming",
                isEnabled = true,
                priority = 80
            )
        )

        AlertDialog(
            onDismissRequest = { showRoutingRecipesModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Travel & Roaming Rule Recipes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Tap a verified template below to install or customize:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    recipes.forEach { recipe ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingRoutingRule = recipe
                                    showRoutingRecipesModal = false
                                    showRoutingDialog = true
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(recipe.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text(recipe.ruleExpression, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoutingRecipesModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Interactive Dynamic @ Category Slot Composer Dialog:
 * Allows user to compose rules naturally by selecting @location, @numbers, @channel, and @guard slots.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DynamicRuleComposerDialog(
    initialRule: TelecomRoutingRule?,
    availableChannels: List<CallingChannel>,
    onSave: (TelecomRoutingRule) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialRule?.name ?: "") }
    var locationPattern by remember { mutableStateOf(initialRule?.locationPattern ?: "any") }
    var customIsoText by remember {
        mutableStateOf(
            if (initialRule?.locationPattern?.startsWith("travel:") == true) initialRule.locationPattern.removePrefix("travel:")
            else if (initialRule?.locationPattern?.startsWith("home:") == true) initialRule.locationPattern.removePrefix("home:")
            else ""
        )
    }
    var destinationPrefix by remember { mutableStateOf(initialRule?.destinationPrefix ?: "all_intl") }
    var customPrefixText by remember {
        mutableStateOf(
            if (initialRule != null && initialRule.destinationPrefix != "all_intl" && initialRule.destinationPrefix != "domestic") {
                initialRule.destinationPrefix
            } else ""
        )
    }
    var targetChannelId by remember {
        mutableStateOf(
            initialRule?.targetChannelId
                ?: availableChannels.firstOrNull()?.id
                ?: "whatsapp"
        )
    }
    var guardAction by remember { mutableStateOf(initialRule?.guardAction ?: "warn_roaming") }
    var priority by remember { mutableIntStateOf(initialRule?.priority ?: 100) }
    var activePickerSlot by remember { mutableStateOf<String?>(null) } // "location", "numbers", "channel", "guard"

    val locationLabel = when {
        locationPattern == "any" -> "Any Location"
        locationPattern == "roaming" -> "Roaming Abroad"
        locationPattern.startsWith("travel:") -> "Traveling in ${locationPattern.removePrefix("travel:").uppercase()}"
        locationPattern.startsWith("home:") -> "Home in ${locationPattern.removePrefix("home:").uppercase()}"
        else -> locationPattern
    }
    val numbersLabel = when {
        destinationPrefix == "all_intl" -> "International Numbers"
        destinationPrefix == "domestic" -> "Domestic Numbers"
        destinationPrefix.startsWith("+") -> destinationPrefix
        else -> destinationPrefix
    }
    val channelLabel = when {
        targetChannelId == "whatsapp" -> "WhatsApp Personal"
        targetChannelId == "whatsapp_business" -> "WhatsApp Business"
        targetChannelId == "sim_1" -> "SIM 1"
        targetChannelId == "sim_2" -> "SIM 2"
        targetChannelId == "google_voice" -> "Google Voice"
        else -> availableChannels.find { it.id == targetChannelId }?.displayName ?: targetChannelId
    }

    val ruleExpressionPreview = "When @location: $locationLabel, route @numbers: $numbersLabel via @channel: $channelLabel"

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.AltRoute, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (initialRule == null) "New Smart Routing Rule" else "Edit Routing Rule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name (Optional)") },
                    placeholder = { Text("Auto-generated if left blank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Active Expression Preview:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(ruleExpressionPreview, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }

                Text("Configure Rule Parameters (@):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = activePickerSlot == "location",
                        onClick = { activePickerSlot = if (activePickerSlot == "location") null else "location" },
                        label = { Text("@location: $locationLabel", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                    )
                    FilterChip(
                        selected = activePickerSlot == "numbers",
                        onClick = { activePickerSlot = if (activePickerSlot == "numbers") null else "numbers" },
                        label = { Text("@numbers: $numbersLabel", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                    )
                    FilterChip(
                        selected = activePickerSlot == "channel",
                        onClick = { activePickerSlot = if (activePickerSlot == "channel") null else "channel" },
                        label = { Text("@channel: $channelLabel", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                    )
                    FilterChip(
                        selected = activePickerSlot == "guard",
                        onClick = { activePickerSlot = if (activePickerSlot == "guard") null else "guard" },
                        label = { Text("@guard: ${if (guardAction == "silent") "Auto" else "Warn"}", fontSize = 11.sp, maxLines = 1, softWrap = false) }
                    )
                }

                when (activePickerSlot) {
                    "location" -> {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("When calling from location:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = locationPattern == "any",
                                        onClick = { locationPattern = "any" },
                                        label = { Text("🌐 Any", maxLines = 1, softWrap = false) }
                                    )
                                    FilterChip(
                                        selected = locationPattern == "roaming",
                                        onClick = { locationPattern = "roaming" },
                                        label = { Text("✈️ Roaming Abroad", maxLines = 1, softWrap = false) }
                                    )
                                    FilterChip(
                                        selected = locationPattern.startsWith("travel:"),
                                        onClick = {
                                            val iso = customIsoText.trim().uppercase().ifBlank { "IN" }
                                            customIsoText = iso
                                            locationPattern = "travel:$iso"
                                        },
                                        label = { Text("🧳 Traveling in Country", maxLines = 1, softWrap = false) }
                                    )
                                    FilterChip(
                                        selected = locationPattern.startsWith("home:"),
                                        onClick = {
                                            val iso = customIsoText.trim().uppercase().ifBlank { "US" }
                                            customIsoText = iso
                                            locationPattern = "home:$iso"
                                        },
                                        label = { Text("🏠 Home Country", maxLines = 1, softWrap = false) }
                                    )
                                }
                                if (locationPattern.startsWith("travel:") || locationPattern.startsWith("home:")) {
                                    OutlinedTextField(
                                        value = customIsoText,
                                        onValueChange = {
                                            customIsoText = it
                                            val prefix = if (locationPattern.startsWith("home:")) "home:" else "travel:"
                                            locationPattern = prefix + it.trim().uppercase()
                                        },
                                        label = { Text("2-Letter Country ISO Code (e.g. IN, US, GB, FR, CA)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    "numbers" -> {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("For destination numbers:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = destinationPrefix == "all_intl",
                                        onClick = { destinationPrefix = "all_intl" },
                                        label = { Text("🌐 International", maxLines = 1, softWrap = false) }
                                    )
                                    FilterChip(
                                        selected = destinationPrefix == "domestic",
                                        onClick = { destinationPrefix = "domestic" },
                                        label = { Text("🏠 Domestic", maxLines = 1, softWrap = false) }
                                    )
                                    FilterChip(
                                        selected = destinationPrefix.startsWith("+"),
                                        onClick = {
                                            val prefix = customPrefixText.trim().ifBlank { "+1" }
                                            customPrefixText = prefix
                                            destinationPrefix = prefix
                                        },
                                        label = { Text("📞 Specific Prefix", maxLines = 1, softWrap = false) }
                                    )
                                }
                                if (destinationPrefix.startsWith("+") || destinationPrefix !in listOf("all_intl", "domestic")) {
                                    OutlinedTextField(
                                        value = customPrefixText,
                                        onValueChange = {
                                            customPrefixText = it
                                            destinationPrefix = it.trim()
                                        },
                                        label = { Text("Country Dial Code / Prefix (e.g. +1, +91, +44, +33)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    "channel" -> {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF25D366).copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Route call through channel:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                                val channelsToDisplay = if (availableChannels.isNotEmpty()) {
                                    availableChannels.map { it.id to it.displayName }
                                } else {
                                    listOf(
                                        "whatsapp" to "WhatsApp Personal",
                                        "whatsapp_business" to "WhatsApp Business",
                                        "sim_1" to "Cellular SIM 1",
                                        "sim_2" to "Cellular SIM 2",
                                        "google_voice" to "Google Voice"
                                    )
                                }

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    channelsToDisplay.forEach { (cId, cName) ->
                                        FilterChip(
                                            selected = targetChannelId == cId,
                                            onClick = { targetChannelId = cId; activePickerSlot = null },
                                            label = { Text(cName, maxLines = 1, softWrap = false) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "guard" -> {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Roaming guard policy:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = guardAction == "warn_roaming",
                                        onClick = { guardAction = "warn_roaming"; activePickerSlot = null },
                                        label = { Text("⚠️ Warn Before Calling", maxLines = 1, softWrap = false) }
                                    )
                                    FilterChip(
                                        selected = guardAction == "silent",
                                        onClick = { guardAction = "silent"; activePickerSlot = null },
                                        label = { Text("⚡ Auto-Dispatch Silently", maxLines = 1, softWrap = false) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = name.trim().ifBlank {
                        "When in $locationLabel route $numbersLabel to $channelLabel"
                    }
                    onSave(
                        TelecomRoutingRule(
                            id = initialRule?.id ?: 0L,
                            name = finalName,
                            ruleExpression = ruleExpressionPreview,
                            targetChannelId = targetChannelId,
                            locationPattern = locationPattern,
                            destinationPrefix = destinationPrefix,
                            guardAction = guardAction,
                            isEnabled = initialRule?.isEnabled ?: true,
                            priority = priority
                        )
                    )
                }
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
