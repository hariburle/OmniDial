package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.DialerSuggestionsList
import com.example.ui.components.SpeedDialActionDialog
import com.example.ui.components.SpeedDialAssignPromptDialog
import com.example.ui.components.WhatsAppIcon
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.data.ChannelPreferenceRepository
import com.example.domain.model.CallingChannel
import com.example.telecom.ChannelDiscoveryManager
import com.example.ui.components.KeypadChannelDock
import com.example.ui.components.MultiChannelChoiceDialog
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FavoriteContact
import com.example.data.RecentCall
import com.example.telecom.SimInfo
import com.example.ui.components.AddFavoriteDialog
import com.example.ui.components.ContactSaveDestination
import com.example.ui.components.CreateContactDialog
import com.example.ui.components.ContactPickerDialog
import com.example.ui.components.Keypad
import com.example.ui.components.MultiNumberCallDialog
import com.example.ui.components.RoleBanner

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AssistChipDefaults
import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import com.example.util.T9Helper
import com.example.util.T9SearchResult
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerScreen(
    number: String,
    favorites: List<FavoriteContact>,
    recentCalls: List<RecentCall> = emptyList(),
    isDefaultDialer: Boolean,
    context: Context,
    simSlot: Int = 1,
    activeSims: List<SimInfo> = emptyList(),
    onToggleSim: () -> Unit = {},
    onRoleChanged: () -> Unit,
    onDigitPress: (Char) -> Unit,
    onDeleteDigit: () -> Unit,
    onClearDigits: () -> Unit,
    onSelectContactNumber: (String) -> Unit,
    onPlaceCall: (String, String?) -> Unit,
    onPlaceWhatsAppCall: (String) -> Unit = { ContactHelper.launchWhatsAppCall(context, it) },
    onPlaceWhatsAppCallWithBusiness: ((String, Boolean) -> Unit)? = null,
    onPlaceGoogleVoiceCall: ((String) -> Unit)? = null,
    onSimulateCall: (String, String) -> Unit,
    onCreateRuleForNumber: (String) -> Unit,
    onAddFavorite: (String, String, String, String?) -> Unit,
    onAddNewContact: (name: String, number: String, label: String, destination: ContactSaveDestination, addToFavorites: Boolean) -> Unit = { _, _, _, _, _ -> },
    onDeleteFavorite: (FavoriteContact) -> Unit,
    onAssignSpeedDial: (FavoriteContact, Int) -> Unit = { _, _ -> },
    onAssignSpeedDialSlot: (Int, String, String, String?) -> Unit = { _, _, _, _ -> },
    onClearSpeedDialSlot: (Int) -> Unit = {},
    confirmSpeedDialCall: Boolean = true,
    askToAssignUnassignedSpeedDial: Boolean = true,
    speedDialKeypadDisplay: String = "speed_dial_above",
    showDialerQuickActions: Boolean = true,
    deviceContacts: List<DeviceContact> = emptyList(),
    precomputedSearchContacts: List<DeviceContact>? = null,
    getPreferredCallingMode: (String) -> String = { "cellular" },
    whatsAppCallMode: String = "ask_learn",
    onPlaceCallDirect: ((String, Int?) -> Unit)? = null,
    channelPreferenceRepository: ChannelPreferenceRepository = remember(context) { ChannelPreferenceRepository.getInstance(context) },
    channelDiscoveryManager: ChannelDiscoveryManager = remember(context) { ChannelDiscoveryManager.getInstance(context) },
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val availableChannels by channelDiscoveryManager.availableChannels.collectAsState()
    var activeChannel by remember { mutableStateOf<CallingChannel?>(null) }
    var userSelectedChannel by remember { mutableStateOf<CallingChannel?>(null) }
    var showChannelPickerSheet by remember { mutableStateOf(false) }
    var pickerTargetNumber by remember { mutableStateOf("") }

    var localNumber by remember(number) { mutableStateOf(number) }
    var selectionState by remember { mutableStateOf(TextRange(number.length)) }

    LaunchedEffect(localNumber) {
        if (localNumber.isBlank()) {
            userSelectedChannel = null
        }
        delay(200)
        if (localNumber != number) {
            onSelectContactNumber(localNumber)
        }
    }

    LaunchedEffect(number) {
        if (number != localNumber) {
            localNumber = number
            selectionState = TextRange(number.length)
        }
    }

    var showContactPicker by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showAddFavoriteDialog by remember { mutableStateOf(false) }
    var assignSpeedDialSlotTarget by remember { mutableStateOf<Int?>(null) }
    var promptAssignSlotTarget by remember { mutableStateOf<Int?>(null) }
    var speedDialActionSlotTarget by remember { mutableStateOf<Pair<Int, FavoriteContact>?>(null) }
    var matchedContact by remember { mutableStateOf<DeviceContact?>(null) }
    val effectiveContacts = deviceContacts
    var speedDialToast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(speedDialToast) {
        if (speedDialToast != null) {
            kotlinx.coroutines.delay(2000)
            speedDialToast = null
        }
    }
    var selectedCallReason by remember { mutableStateOf<String?>(null) }
    var showCallReasonMenu by remember { mutableStateOf(false) }
    var multiNumberContactToCall by remember { mutableStateOf<DeviceContact?>(null) }
    var multiNumberSpeedDialSlot by remember { mutableStateOf<Int?>(null) }
    var multiNumberFavoriteTarget by remember { mutableStateOf<FavoriteContact?>(null) }

    BackHandler(
        enabled = number.isNotBlank() ||
                  assignSpeedDialSlotTarget != null ||
                  promptAssignSlotTarget != null ||
                  speedDialActionSlotTarget != null ||
                  showAddFavoriteDialog ||
                  showContactPicker ||
                  multiNumberContactToCall != null ||
                  multiNumberFavoriteTarget != null ||
                  showCallReasonMenu ||
                  showChannelPickerSheet
    ) {
        if (showChannelPickerSheet) {
            showChannelPickerSheet = false
        } else if (assignSpeedDialSlotTarget != null) {
            assignSpeedDialSlotTarget = null
        } else if (promptAssignSlotTarget != null) {
            promptAssignSlotTarget = null
        } else if (speedDialActionSlotTarget != null) {
            speedDialActionSlotTarget = null
        } else if (showAddFavoriteDialog) {
            showAddFavoriteDialog = false
        } else if (showContactPicker) {
            showContactPicker = false
        } else if (multiNumberContactToCall != null) {
            multiNumberContactToCall = null
        } else if (multiNumberFavoriteTarget != null) {
            multiNumberFavoriteTarget = null
        } else if (showCallReasonMenu) {
            showCallReasonMenu = false
        } else if (number.isNotBlank()) {
            onClearDigits()
        }
    }

    // Use background pre-computed contacts if provided, otherwise fallback to local computation
    val allSearchContacts = remember(precomputedSearchContacts, favorites, effectiveContacts) {
        if (precomputedSearchContacts != null) {
            precomputedSearchContacts
        } else {
            fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)
            val favByDigits = favorites.associateBy { normDigits(it.phoneNumber) }
            val favByName = favorites.associateBy { it.name.trim().lowercase() }
            val list = mutableListOf<DeviceContact>()

            effectiveContacts.forEach { dc ->
                val dcDigits = normDigits(dc.phoneNumber)
                val matchingFav = favByDigits[dcDigits] ?: favByName[dc.name.trim().lowercase()]
                val effectiveNickname = matchingFav?.nickname?.ifBlank { null } ?: dc.nickname?.ifBlank { null }
                if (effectiveNickname != null && effectiveNickname != dc.nickname) {
                    list.add(dc.copy(nickname = effectiveNickname))
                } else {
                    list.add(dc)
                }
            }
            val knownDigits = effectiveContacts.flatMap { dc ->
                dc.phoneNumbers.map { normDigits(it.number) } + listOf(normDigits(dc.phoneNumber))
            }.filter { it.isNotBlank() }.toSet()

            favorites.forEach { fav ->
                val fDigits = normDigits(fav.phoneNumber)
                if (fDigits.isBlank() || !knownDigits.contains(fDigits)) {
                    list.add(DeviceContact(fav.name, fav.phoneNumber, fav.label, fav.photoUri, nickname = fav.nickname, isStarred = true))
                }
            }
            list.distinctBy { dc ->
                (if (dc.isAppOnly) "app_" else "dev_") + (dc.contactId ?: "") + "_" + dc.name.trim().lowercase() + "_" + normDigits(dc.phoneNumber)
            }
        }
    }

    // T9 search results (Asynchronous background search so keypad input is lightning fast)
    var t9Matches by remember { mutableStateOf<List<T9SearchResult>>(emptyList()) }

    LaunchedEffect(localNumber, allSearchContacts, favorites) {
        if (localNumber.isNotBlank()) {
            delay(80)
            val cleanNum = localNumber.filter { it.isDigit() }
            val (resolvedContact, resolvedT9) = withContext(Dispatchers.Default) {
                // 1. Direct match check (Favorites first, then all search contacts)
                val fav = favorites.firstOrNull { f ->
                    val fClean = f.phoneNumber.filter { it.isDigit() }
                    fClean == cleanNum || (cleanNum.length >= 7 && (fClean.endsWith(cleanNum) || cleanNum.endsWith(fClean)))
                }
                val directContact: DeviceContact? = if (fav != null) {
                    DeviceContact(fav.name, fav.phoneNumber, fav.label, fav.photoUri, nickname = fav.nickname)
                } else {
                    val directMatch = allSearchContacts.firstOrNull { dc ->
                        val dcClean = dc.phoneNumber.filter { it.isDigit() }
                        dcClean == cleanNum || (cleanNum.length >= 7 && (dcClean.endsWith(cleanNum) || cleanNum.endsWith(dcClean))) ||
                        dc.phoneNumbers.any { pn ->
                            val pnClean = pn.number.filter { it.isDigit() }
                            pnClean == cleanNum || (cleanNum.length >= 7 && (pnClean.endsWith(cleanNum) || cleanNum.endsWith(pnClean)))
                        }
                    }
                    if (directMatch != null) {
                        // Find the EXACT matching phone number and label so we don't display the default/wrong number
                        val matchedPn = directMatch.phoneNumbers.firstOrNull { pn ->
                            val pnClean = pn.number.filter { it.isDigit() }
                            pnClean == cleanNum || (cleanNum.length >= 7 && (pnClean.endsWith(cleanNum) || cleanNum.endsWith(pnClean)))
                        } ?: if (directMatch.phoneNumber.filter { it.isDigit() }.let { dcClean ->
                                dcClean == cleanNum || (cleanNum.length >= 7 && (dcClean.endsWith(cleanNum) || cleanNum.endsWith(dcClean)))
                            }) {
                            ContactPhoneNumber(directMatch.phoneNumber, directMatch.label)
                        } else null

                        if (matchedPn != null) {
                            directMatch.copy(phoneNumber = matchedPn.number, label = matchedPn.label)
                        } else {
                            directMatch
                        }
                    } else if (cleanNum.length >= 7) {
                        // Only fallback to ContentResolver query for sufficiently long numbers to avoid typing latency
                        try {
                            ContactHelper.lookupContactByNumber(context, localNumber)
                        } catch (_: Exception) {
                            null
                        }
                    } else {
                        null
                    }
                }

                // 2. T9 Search runs in background thread
                val matches = T9Helper.search(allSearchContacts, localNumber)
                Pair(directContact, matches)
            }

            matchedContact = resolvedContact
            t9Matches = resolvedT9
        } else {
            matchedContact = null
            t9Matches = emptyList()
        }
    }

    // Filter out only the exact contact already displayed in the top matchedContact banner to prevent identical duplicates
    val filteredT9Matches = remember(t9Matches, matchedContact, localNumber) {
        if (matchedContact != null) {
            val cleanNum = localNumber.filter { it.isDigit() }
            t9Matches.filter { match ->
                val matchClean = match.phoneNumber.filter { it.isDigit() }
                val isSameNum = matchClean.isNotEmpty() && (matchClean == cleanNum || (cleanNum.length >= 7 && (matchClean.endsWith(cleanNum) || cleanNum.endsWith(matchClean))))
                val isSameName = match.name.equals(matchedContact?.name, ignoreCase = true)
                !(isSameNum && isSameName)
            }
        } else {
            t9Matches
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("dialer_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Default Dialer prompt banner
        RoleBanner(
            isDefaultDialer = isDefaultDialer,
            context = context,
            onRoleChanged = onRoleChanged
        )

        // Flexible top container absorbs all dynamic sizing so the edit box, keypad, and buttons stay strictly fixed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.BottomCenter
        ) {
            DialerSuggestionsList(
                number = localNumber,
                recentCalls = recentCalls,
                t9Matches = filteredT9Matches,
                matchedContact = matchedContact,
                onSelectContactNumber = { chosenNum ->
                    localNumber = chosenNum
                    selectionState = TextRange(chosenNum.length)
                    userSelectedChannel = null
                    onSelectContactNumber(chosenNum)
                },
                contacts = allSearchContacts,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Fixed-Position Keypad and Dialing Controls (zero bouncing/shifting)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Resolve full international contact number from matched contact or local number without blocking composition
            val effectiveNumber = remember(localNumber, matchedContact) {
                if (localNumber.isBlank()) ""
                else {
                    val target = matchedContact?.phoneNumber
                    if (target != null && target.trim().startsWith("+")) target
                    else matchedContact?.phoneNumbers?.firstOrNull { it.number.trim().startsWith("+") }?.number
                        ?: target?.ifBlank { null }
                        ?: localNumber
                }
            }

            val isEmergency = remember(localNumber, effectiveNumber) {
                val numToCheck = effectiveNumber.ifBlank { localNumber }
                channelDiscoveryManager.isEmergencyNumber(numToCheck)
            }

            LaunchedEffect(effectiveNumber, localNumber, availableChannels, simSlot, userSelectedChannel) {
                if (userSelectedChannel != null) {
                    val matching = availableChannels.firstOrNull { it.id == userSelectedChannel?.id }
                    if (matching != null) {
                        userSelectedChannel = matching
                        activeChannel = matching
                        return@LaunchedEffect
                    } else {
                        userSelectedChannel = null
                    }
                }

                val numToCheck = effectiveNumber.ifBlank { localNumber }
                if (numToCheck.isNotBlank()) {
                    // 1. Safety Guardrail: Emergency number discovery locked to domestic cellular SIM
                    if (channelDiscoveryManager.isEmergencyNumber(numToCheck)) {
                        val emergencySim = channelDiscoveryManager.getEmergencyCellularChannel()
                            ?: availableChannels.filterIsInstance<CallingChannel.CellularSim>().firstOrNull()
                        if (emergencySim != null) {
                            activeChannel = emergencySim
                            return@LaunchedEffect
                        }
                    }

                    // 2. Explicit contact preference
                    val pref = channelPreferenceRepository.getPreferenceForNumber(numToCheck)
                    if (pref != null && !pref.preferredChannelId.equals("ask", ignoreCase = true)) {
                        val matched = availableChannels.firstOrNull { it.id.equals(pref.preferredChannelId, ignoreCase = true) }
                        if (matched != null) {
                            activeChannel = matched
                            return@LaunchedEffect
                        }
                    }

                    // 3. International number auto-recommendation: if number starts with +, 011, 00, auto-switch to WhatsApp
                    if (channelDiscoveryManager.isInternationalNumber(numToCheck)) {
                        val waChannel = availableChannels.firstOrNull { it is CallingChannel.WhatsApp }
                        if (waChannel != null) {
                            activeChannel = waChannel
                            return@LaunchedEffect
                        }
                    }
                }
                val currentSlotSim = availableChannels.filterIsInstance<CallingChannel.CellularSim>()
                    .firstOrNull { it.slotIndex + 1 == simSlot }
                val refreshedActive = availableChannels.firstOrNull { it.id == activeChannel?.id }
                if (refreshedActive != null && userSelectedChannel == null) {
                    if (activeChannel is CallingChannel.CellularSim && currentSlotSim != null && refreshedActive != currentSlotSim) {
                        activeChannel = currentSlotSim
                    } else {
                        activeChannel = refreshedActive
                    }
                } else {
                    activeChannel = currentSlotSim ?: availableChannels.firstOrNull()
                }
            }

            val updateLocalNumber: (String) -> Unit = { newText ->
                localNumber = newText
            }

            val localOnDigitPress: (Char) -> Unit = { digit ->
                val current = localNumber
                val start = selectionState.start.coerceIn(0, current.length)
                val end = selectionState.end.coerceIn(0, current.length)
                val minSel = minOf(start, end)
                val maxSel = maxOf(start, end)
                val newText = current.substring(0, minSel) + digit + current.substring(maxSel)
                selectionState = TextRange(minSel + 1)
                updateLocalNumber(newText)
            }

            val localOnDigitReplace: (Char, Char) -> Unit = { originalDigit, replacementChar ->
                val current = localNumber
                val cursorPos = selectionState.start.coerceIn(0, current.length)
                val targetIndex = cursorPos - 1
                if (targetIndex in current.indices && current[targetIndex] == originalDigit) {
                    val newText = current.substring(0, targetIndex) + replacementChar + current.substring(targetIndex + 1)
                    selectionState = TextRange(cursorPos)
                    updateLocalNumber(newText)
                } else {
                    localOnDigitPress(replacementChar)
                }
            }

            val localOnRemoveLastTypedDigit: (Char) -> Unit = { digit ->
                val current = localNumber
                val cursorPos = selectionState.start.coerceIn(0, current.length)
                val targetIndex = cursorPos - 1
                if (targetIndex in current.indices && current[targetIndex] == digit) {
                    val newText = current.substring(0, targetIndex) + current.substring(targetIndex + 1)
                    selectionState = TextRange(targetIndex)
                    updateLocalNumber(newText)
                }
            }

            val localOnDeleteDigit: () -> Unit = {
                val current = localNumber
                val start = selectionState.start.coerceIn(0, current.length)
                val end = selectionState.end.coerceIn(0, current.length)
                if (start == end) {
                    if (start > 0) {
                        val newText = current.substring(0, start - 1) + current.substring(start)
                        selectionState = TextRange(start - 1)
                        updateLocalNumber(newText)
                    }
                } else {
                    val minSel = minOf(start, end)
                    val maxSel = maxOf(start, end)
                    val newText = current.substring(0, minSel) + current.substring(maxSel)
                    selectionState = TextRange(minSel)
                    updateLocalNumber(newText)
                }
            }

            val localOnClearDigits: () -> Unit = {
                selectionState = TextRange.Zero
                userSelectedChannel = null
                updateLocalNumber("")
                onClearDigits()
            }

            // Fixed Row Just Above Number Text Field (38.dp height)
            // Left: Add Reason (active tonal styling, clear icon, dismissal button)
            // Right: Add Contact (contextual for unsaved numbers; no duplicate name for saved contacts)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT HALF (weight 1f): Reason Selector (fixed location, never jumps)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val hasReason = selectedCallReason != null
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasReason)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                        border = if (hasReason)
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        else
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .clickable { showCallReasonMenu = true }
                            .testTag("dialer_add_reason_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (hasReason) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (hasReason) "Reason: $selectedCallReason" else "Add Reason",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (hasReason) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            if (hasReason) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .clickable { selectedCallReason = null },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Call Reason",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Call Reason",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showCallReasonMenu,
                        onDismissRequest = { showCallReasonMenu = false }
                    ) {
                        val reasons = listOf(null, "Urgent", "Quick Question", "Work", "Personal", "Delivery")
                        reasons.forEach { reason ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = reason ?: "None (Clear)",
                                        color = if (reason == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    selectedCallReason = reason
                                    showCallReasonMenu = false
                                }
                            )
                        }
                    }
                }

                // RIGHT HALF (weight 1f): Add Contact for unsaved numbers (clean empty state for saved contacts)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (number.isNotEmpty() && matchedContact == null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .clickable { showAddFavoriteDialog = true }
                                .testTag("dialer_add_contact_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = "Add Contact",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Add Contact",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Dialed Number Display Area with Contact Picker / Overflow Menu & Backspace (Fixed 56.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box {
                        if (number.isEmpty()) {
                            IconButton(
                                onClick = { showContactPicker = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("pick_contact_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContactPhone,
                                    contentDescription = "Select Contact",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        } else {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("dialer_overflow_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Options",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add to Contacts") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.PersonAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showAddFavoriteDialog = true
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add 2-sec pause (,)") },
                                    onClick = {
                                        localOnDigitPress(',')
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add wait (;)") },
                                    onClick = {
                                        localOnDigitPress(';')
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Send Text Message (SMS)") },
                                    onClick = {
                                        ContactHelper.launchSms(context, effectiveNumber.ifBlank { number })
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Send WhatsApp Message") },
                                    onClick = {
                                        ContactHelper.launchWhatsAppMessage(context, effectiveNumber.ifBlank { number }, isBusiness = false)
                                        showOverflowMenu = false
                                    }
                                )
                                val hasWaBizChannel = availableChannels.any { it is CallingChannel.WhatsApp && it.isBusiness }
                                if (hasWaBizChannel) {
                                    DropdownMenuItem(
                                        text = { Text("Send WhatsApp Business Message") },
                                        onClick = {
                                            ContactHelper.launchWhatsAppMessage(context, effectiveNumber.ifBlank { number }, isBusiness = true)
                                            showOverflowMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (localNumber.isEmpty()) {
                        Text(
                            text = "Enter number or pick contact",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        val tFV = TextFieldValue(text = localNumber, selection = selectionState)
                        val keyboardController = LocalSoftwareKeyboardController.current
                        @Suppress("DEPRECATION")
                        CompositionLocalProvider(
                            LocalTextInputService provides null
                        ) {
                            @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
                            androidx.compose.ui.platform.InterceptPlatformTextInput(
                                interceptor = { _, _ ->
                                    kotlinx.coroutines.awaitCancellation()
                                }
                            ) {
                                BasicTextField(
                                    value = tFV,
                                    onValueChange = { newValue ->
                                        selectionState = newValue.selection
                                        keyboardController?.hide()
                                        if (newValue.text != localNumber) {
                                            val sanitized = newValue.text.filter { it.isDigit() || it == '+' || it == '*' || it == '#' || it == ',' || it == ';' }
                                            updateLocalNumber(sanitized)
                                        }
                                    },
                                    readOnly = false,
                                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center
                                    ),
                                    singleLine = true,
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("dialer_number_display"),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            innerTextField()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Backspace button with click to delete single digit & long-press to clear entire field
                    val view = LocalView.current
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .then(
                                if (localNumber.isNotEmpty()) {
                                    Modifier.combinedClickable(
                                        onClick = localOnDeleteDigit,
                                        onLongClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                            localOnClearDigits()
                                        }
                                    )
                                } else Modifier
                            )
                            .testTag("dialer_backspace_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Delete Digit",
                            tint = if (localNumber.isNotEmpty())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                    }
                }
            }

            // Speed dial toast / feedback message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (speedDialToast != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.inverseSurface
                    ) {
                        Text(
                            text = speedDialToast ?: "",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            val speedDialMap = remember(favorites) {
                val map = mutableMapOf<Char, String>()
                map['1'] = "VM"
                (2..9).forEach { slot ->
                    val fav = favorites.firstOrNull { it.speedDialSlot == slot }
                    if (fav != null) {
                        val shortName = fav.nickname?.takeIf { it.isNotBlank() }
                            ?: fav.name.trim().split(" ").firstOrNull()
                            ?: fav.name
                        map[slot.digitToChar()] = shortName.take(8)
                    }
                }
                map
            }

            // MCCE Adaptive Keypad Channel Dock (SIM 1, SIM 2, WhatsApp, etc.)
            KeypadChannelDock(
                channels = availableChannels,
                selectedChannel = activeChannel,
                onSelectChannel = { channel ->
                    userSelectedChannel = channel
                    activeChannel = channel
                    if (channel is CallingChannel.CellularSim) {
                        if (simSlot != channel.slotIndex + 1) {
                            onToggleSim()
                        }
                    }
                },
                isEmergency = isEmergency,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Main Telephone Keypad with Speed Dial Long-Press
            Keypad(
                compact = false,
                speedDialMap = speedDialMap,
                speedDialDisplayMode = speedDialKeypadDisplay,
                onDigitPress = localOnDigitPress,
                onDigitLongPress = { digit ->
                    when (digit) {
                        '0' -> localOnDigitReplace('0', '+')
                        '*' -> localOnDigitReplace('*', ',')
                        '#' -> localOnDigitReplace('#', ';')
                        '1' -> {
                            localOnRemoveLastTypedDigit('1')
                            val vmNumber = ContactHelper.getVoicemailNumber(context)
                            speedDialToast = "Voicemail ($vmNumber)"
                            onSelectContactNumber(vmNumber)
                            onPlaceCall(vmNumber, null)
                        }
                        in '2'..'9' -> {
                            localOnRemoveLastTypedDigit(digit)
                            val slotNum = digit.digitToInt()
                            val fav = favorites.firstOrNull { it.speedDialSlot == slotNum }
                            if (fav != null) {
                                if (confirmSpeedDialCall) {
                                    speedDialActionSlotTarget = Pair(slotNum, fav)
                                } else {
                                    val targetNum = fav.phoneNumber
                                    speedDialToast = "Calling ${fav.name} (#$slotNum)..."
                                    onSelectContactNumber(targetNum)
                                    onPlaceCall(targetNum, null)
                                }
                            } else {
                                if (askToAssignUnassignedSpeedDial) {
                                    promptAssignSlotTarget = slotNum
                                } else {
                                    speedDialToast = "Speed dial #$slotNum is unassigned"
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Channel-Adaptive Hero Call Action + Auxiliary Secondary Actions Bar
            val callingMode = if (localNumber.isNotBlank()) getPreferredCallingMode(localNumber) else "none"
            val isWaBizPreferred = callingMode == "whatsapp_business"
            val isWaStandardPreferred = callingMode == "whatsapp"
            val isWaPreferred = isWaStandardPreferred || isWaBizPreferred
            val isGvPreferred = callingMode == "google_voice"
            val isGsmPreferred = callingMode == "cellular"
            val isDark = isSystemInDarkTheme()
            val isNumEmpty = localNumber.isBlank()
            val isWaBizActive = (activeChannel as? CallingChannel.WhatsApp)?.isBusiness == true || (isWaBizPreferred && activeChannel !is CallingChannel.CellularSim)
            val isWaActive = activeChannel is CallingChannel.WhatsApp || (isWaPreferred && activeChannel !is CallingChannel.CellularSim)
            val isGvActive = activeChannel is CallingChannel.GoogleVoice || (isGvPreferred && activeChannel !is CallingChannel.CellularSim)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. PRIMARY ACTION ROW (Aligned with 3 Keypad Columns: [SIM] [CALL] [BACKSPACE])
                val heroContainerColor = if (isWaBizActive && !isNumEmpty) {
                    Color(0xFF128C7E) // WhatsApp Business teal
                } else if (isWaActive && !isNumEmpty) {
                    Color(0xFF25D366) // WhatsApp brand green
                } else if (isGvActive && !isNumEmpty) {
                    Color(0xFF0F9D58) // Google Voice green
                } else {
                    Color(0xFF059669) // Cellular / Phone Emerald green
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(68.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // PART 1 (Left 1/3, under column 1): Message Button (SMS, WhatsApp Chat, or Google Voice based on active channel)
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        val isWaMessage = activeChannel is CallingChannel.WhatsApp
                        val isWaBizMessage = (activeChannel as? CallingChannel.WhatsApp)?.isBusiness == true
                        val isGvMessage = activeChannel is CallingChannel.GoogleVoice
                        val msgColor = when {
                            isWaBizMessage -> Color(0xFF128C7E)
                            isWaMessage -> Color(0xFF25D366)
                            isGvMessage -> Color(0xFF0F9D58)
                            else -> Color(0xFF0284C7)
                        }
                        val hasNumber = !isNumEmpty
                        Surface(
                            onClick = {
                                if (hasNumber) {
                                    if (isWaMessage) {
                                        ContactHelper.launchWhatsAppMessage(context, effectiveNumber, isBusiness = isWaBizMessage)
                                    } else if (isGvMessage) {
                                        ContactHelper.launchGoogleVoiceMessage(context, effectiveNumber)
                                    } else {
                                        ContactHelper.launchSms(context, effectiveNumber)
                                    }
                                } else if (recentCalls.isNotEmpty()) {
                                    val firstUnique = recentCalls.distinctBy { it.phoneNumber.filter { c -> c.isDigit() || c == '+' } }.firstOrNull()
                                    if (firstUnique != null) {
                                        if (isWaMessage) {
                                            ContactHelper.launchWhatsAppMessage(context, firstUnique.phoneNumber, isBusiness = isWaBizMessage)
                                        } else if (isGvMessage) {
                                            ContactHelper.launchGoogleVoiceMessage(context, firstUnique.phoneNumber)
                                        } else {
                                            ContactHelper.launchSms(context, firstUnique.phoneNumber)
                                        }
                                    }
                                } else {
                                    speedDialToast = when {
                                        isWaBizMessage -> "Enter number to chat on WhatsApp Business"
                                        isWaMessage -> "Enter number to chat on WhatsApp"
                                        isGvMessage -> "Enter number to message on Google Voice"
                                        else -> "Enter number to send SMS"
                                    }
                                }
                            },
                            shape = CircleShape,
                            color = if (hasNumber) {
                                if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                            } else Color.Transparent,
                            border = if (hasNumber) BorderStroke(1.dp, msgColor.copy(alpha = 0.35f)) else null,
                            modifier = Modifier
                                .size(56.dp)
                                .testTag("hero_message_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isWaMessage) {
                                    WhatsAppIcon(
                                        modifier = Modifier.size(24.dp),
                                        tint = if (hasNumber) msgColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = "Message",
                                        tint = if (hasNumber) msgColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }

                    // PART 2 (Center 1/3, under column 2): Call Button (Hero Green Button)
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        val view = LocalView.current
                        Surface(
                            shape = CircleShape,
                            color = heroContainerColor,
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .combinedClickable(
                                    onClick = {
                                        if (isNumEmpty) {
                                            if (recentCalls.isNotEmpty()) {
                                                val firstUnique = recentCalls.distinctBy { it.phoneNumber.filter { c -> c.isDigit() || c == '+' } }.firstOrNull()
                                                if (firstUnique != null) {
                                                    localNumber = firstUnique.phoneNumber
                                                    selectionState = TextRange(firstUnique.phoneNumber.length)
                                                    userSelectedChannel = null
                                                    onSelectContactNumber(firstUnique.phoneNumber)
                                                }
                                            }
                                        } else {
                                            when (val ch = activeChannel) {
                                                is CallingChannel.WhatsApp -> {
                                                    if (onPlaceWhatsAppCallWithBusiness != null) {
                                                        onPlaceWhatsAppCallWithBusiness(effectiveNumber, ch.isBusiness)
                                                    } else {
                                                        ContactHelper.launchWhatsAppCall(context, effectiveNumber, isBusiness = ch.isBusiness)
                                                    }
                                                }
                                                is CallingChannel.GoogleVoice -> {
                                                    if (onPlaceGoogleVoiceCall != null) {
                                                        onPlaceGoogleVoiceCall(effectiveNumber)
                                                    } else {
                                                        ContactHelper.launchGoogleVoiceCall(context, effectiveNumber, accountHandle = ch.phoneAccountHandle)
                                                    }
                                                }
                                                is CallingChannel.CellularSim -> {
                                                    if (simSlot != ch.slotIndex + 1) {
                                                        onToggleSim()
                                                    }
                                                    onPlaceCall(effectiveNumber, selectedCallReason)
                                                }
                                                is CallingChannel.AskAlways -> {
                                                    pickerTargetNumber = effectiveNumber.ifBlank { localNumber }
                                                    showChannelPickerSheet = true
                                                }
                                                else -> {
                                                    if (isWaPreferred) {
                                                        if (onPlaceWhatsAppCallWithBusiness != null) {
                                                            onPlaceWhatsAppCallWithBusiness(effectiveNumber, isWaBizPreferred)
                                                        } else {
                                                            ContactHelper.launchWhatsAppCall(context, effectiveNumber, isBusiness = isWaBizPreferred)
                                                        }
                                                    } else if (isGvPreferred) {
                                                        if (onPlaceGoogleVoiceCall != null) {
                                                            onPlaceGoogleVoiceCall(effectiveNumber)
                                                        } else {
                                                            ContactHelper.launchGoogleVoiceCall(context, effectiveNumber)
                                                        }
                                                    } else {
                                                        onPlaceCall(effectiveNumber, selectedCallReason)
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        if (effectiveNumber.isNotBlank()) {
                                            pickerTargetNumber = effectiveNumber.ifBlank { localNumber }
                                            showChannelPickerSheet = true
                                        }
                                    }
                                )
                                .testTag("hero_call_button")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isWaActive && !isNumEmpty) {
                                    WhatsAppIcon(
                                        modifier = Modifier.size(28.dp),
                                        tint = Color.White
                                    )
                                } else if (isGvActive && !isNumEmpty) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = "Call via Google Voice",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    // PART 3 (Right 1/3, under column 3): Backspace Button
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        val view = LocalView.current
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    if (!isNumEmpty) {
                                        if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                    } else Color.Transparent
                                )
                                .then(
                                    if (!isNumEmpty) {
                                        Modifier.combinedClickable(
                                            onClick = localOnDeleteDigit,
                                            onLongClick = {
                                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                localOnClearDigits()
                                            }
                                        )
                                    } else Modifier
                                )
                                .testTag("dialer_bottom_backspace"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete Digit",
                                tint = if (!isNumEmpty) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Contact Picker Dialog
    if (showContactPicker) {
        ContactPickerDialog(
            favorites = favorites,
            deviceContacts = deviceContacts,
            onContactSelected = { _, selectedNumber, _ ->
                onSelectContactNumber(selectedNumber)
            },
            onDismiss = { showContactPicker = false }
        )
    }

    // Add Contact Dialog
    if (showAddFavoriteDialog) {
        CreateContactDialog(
            initialNumber = number,
            initialName = matchedContact?.name ?: "",
            dialogTitle = "Add Contact",
            initialAddToFavorites = false,
            onDismiss = { showAddFavoriteDialog = false },
            onSave = { name, favNumber, label, destination, addToFavs ->
                onAddNewContact(name, favNumber, label, destination, addToFavs)
                showAddFavoriteDialog = false
            }
        )
    }

    // Multi-Number Confirmation Dialog for Speed Dial and Favorites
    if (multiNumberContactToCall != null) {
        val contact = multiNumberContactToCall!!
        MultiNumberCallDialog(
            contactName = contact.name,
            phoneNumbers = contact.phoneNumbers,
            defaultNumber = multiNumberFavoriteTarget?.phoneNumber ?: contact.phoneNumber,
            titlePrefix = if (multiNumberSpeedDialSlot != null) "Speed Dial #$multiNumberSpeedDialSlot" else "Favorite",
            onSelectNumberToCall = { chosenNumber ->
                onSelectContactNumber(chosenNumber)
                onPlaceCall(chosenNumber, null)
            },
            onSearchOtherContacts = {
                showContactPicker = true
            },
            onDismiss = {
                multiNumberContactToCall = null
                multiNumberSpeedDialSlot = null
                multiNumberFavoriteTarget = null
            }
        )
    }

    if (speedDialActionSlotTarget != null) {
        val (slot, fav) = speedDialActionSlotTarget!!
        SpeedDialActionDialog(
            slot = slot,
            fav = fav,
            preferredCallingMode = getPreferredCallingMode(fav.phoneNumber),
            onSelectContactNumber = onSelectContactNumber,
            onPlaceCall = onPlaceCall,
            onPlaceWhatsAppCall = onPlaceWhatsAppCall,
            onPlaceGoogleVoiceCall = onPlaceGoogleVoiceCall,
            onReassign = {
                val targetSlot = slot
                speedDialActionSlotTarget = null
                assignSpeedDialSlotTarget = targetSlot
            },
            onClear = {
                val targetSlot = slot
                speedDialActionSlotTarget = null
                onClearSpeedDialSlot(targetSlot)
                speedDialToast = "Cleared Speed Dial #$targetSlot"
            },
            onDismiss = { speedDialActionSlotTarget = null }
        )
    }

    if (promptAssignSlotTarget != null) {
        val slot = promptAssignSlotTarget!!
        SpeedDialAssignPromptDialog(
            slot = slot,
            onConfirm = {
                val targetSlot = slot
                promptAssignSlotTarget = null
                assignSpeedDialSlotTarget = targetSlot
            },
            onDismiss = { promptAssignSlotTarget = null }
        )
    }

    if (assignSpeedDialSlotTarget != null) {
        val targetSlot = assignSpeedDialSlotTarget!!
        ContactPickerDialog(
            favorites = favorites,
            deviceContacts = deviceContacts,
            onContactSelected = { name, number, photoUri ->
                onAssignSpeedDialSlot(targetSlot, name, number, photoUri)
                val displayName = name.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: name
                speedDialToast = "Assigned $displayName to #$targetSlot"
                assignSpeedDialSlotTarget = null
            },
            onDismiss = { assignSpeedDialSlotTarget = null },
            title = "Assign Speed Dial #$targetSlot"
        )
    }

    if (showChannelPickerSheet) {
        val targetNum = pickerTargetNumber.ifBlank { number }
        MultiChannelChoiceDialog(
            phoneNumber = targetNum,
            contactName = matchedContact?.name,
            channels = availableChannels,
            initialRememberChoice = (whatsAppCallMode == "ask_learn"),
            showRememberChoice = (whatsAppCallMode != "ask_always"),
            onSelectChannel = { chosenChannel, rememberChoice ->
                activeChannel = chosenChannel
                showChannelPickerSheet = false
                if (rememberChoice && targetNum.isNotBlank()) {
                    coroutineScope.launch {
                        channelPreferenceRepository.setPreferredChannel(targetNum, chosenChannel)
                    }
                }
                when (chosenChannel) {
                    is CallingChannel.WhatsApp -> {
                        if (onPlaceWhatsAppCallWithBusiness != null) {
                            onPlaceWhatsAppCallWithBusiness(targetNum, chosenChannel.isBusiness)
                        } else {
                            ContactHelper.launchWhatsAppCall(context, targetNum, isBusiness = chosenChannel.isBusiness)
                        }
                    }
                    is CallingChannel.GoogleVoice -> {
                        if (onPlaceGoogleVoiceCall != null) {
                            onPlaceGoogleVoiceCall(targetNum)
                        } else {
                            ContactHelper.launchGoogleVoiceCall(context, targetNum, accountHandle = chosenChannel.phoneAccountHandle)
                        }
                    }
                    is CallingChannel.CellularSim -> {
                        val slot = chosenChannel.slotIndex + 1
                        if (simSlot != slot) {
                            onToggleSim()
                        }
                        if (onPlaceCallDirect != null) {
                            onPlaceCallDirect(targetNum, slot)
                        } else {
                            onPlaceCall(targetNum, selectedCallReason)
                        }
                    }
                    else -> {
                        if (onPlaceCallDirect != null) {
                            onPlaceCallDirect(targetNum, null)
                        } else {
                            onPlaceCall(targetNum, selectedCallReason)
                        }
                    }
                }
            },
            onDismiss = { showChannelPickerSheet = false }
        )
    }
}
