package com.example.ui.screens

import androidx.compose.runtime.LaunchedEffect
import com.example.util.ContactHelper
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RecentCall
import com.example.ui.components.CallLogItem
import com.example.ui.components.CompactSearchBar
import com.example.ui.components.ContactDetailsBottomSheet
import com.example.ui.components.PostCallNoteReminderDialog
import com.example.ui.components.WhatsAppIcon
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.AssistChipDefaults
import com.example.data.FavoriteContact
import com.example.data.SpamNumber
import com.example.ui.components.ContactSaveDestination
import com.example.ui.components.CreateContactDialog

data class FilterOptionData(
    val key: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val activeColor: Color? = null
)

data class GroupedCallLog(
    val primaryCall: RecentCall,
    val count: Int,
    val isSpam: Boolean,
    val spamDetails: SpamNumber?
)

@Composable
fun CallLogScreen(
    recentCalls: List<RecentCall>,
    spamNumbers: List<SpamNumber> = emptyList(),
    favorites: List<FavoriteContact> = emptyList(),
    highlightNumber: String? = null,
    isSpamNumber: ((String) -> Boolean)? = null,
    onCallBack: (RecentCall) -> Unit,
    onCallNumber: (String) -> Unit = { num -> onCallBack(RecentCall(phoneNumber = num, callType = 2)) },
    onCreateRuleForNumber: (String) -> Unit,
    onMarkSpam: (String) -> Unit = {},
    onRemoveSpam: (String) -> Unit = {},
    onToggleFavorite: (name: String, number: String, label: String, photoUri: String?) -> Unit = { _, _, _, _ -> },
    onUpdateFavoriteNumber: (FavoriteContact, String, String) -> Unit = { _, _, _ -> },
    onAddNewContact: (name: String, number: String, label: String, saveToDevice: Boolean, addToFavorites: Boolean) -> Unit = { _, _, _, _, _ -> },
    onUpdateNoteAndReminder: (RecentCall, String?, Long?) -> Unit = { _, _, _ -> },
    onUpdateContact: (oldNum: String, name: String, number: String, label: String, nickname: String?) -> Unit = { _, _, _, _, _ -> },
    onSetDefaultContactNumber: ((contact: DeviceContact, number: String, label: String) -> Unit)? = null,
    onDeleteContact: ((DeviceContact) -> Unit)? = null,
    onDeleteCall: (RecentCall) -> Unit = {},
    onDeleteCallsForNumber: (String) -> Unit = {},
    rules: List<com.example.data.CallerRule> = emptyList(),
    deviceContacts: List<DeviceContact> = emptyList(),
    activeSims: List<com.example.telecom.SimInfo> = emptyList(),
    getPreferredCallingMode: (String) -> String = { "cellular" },
    onSaveLearnedCallMode: (String, String) -> Unit = { _, _ -> },
    getPreferredSimSlot: (String) -> Int = { -1 },
    onSetPreferredSimSlot: (String, Int) -> Unit = { _, _ -> },
    globalSimPreferenceMode: String = "always_ask",
    dismissModalsTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var noteDialogCall by remember { mutableStateOf<RecentCall?>(null) }
    var contactDetailsTarget by remember { mutableStateOf<Pair<DeviceContact, FavoriteContact?>?>(null) }

    LaunchedEffect(dismissModalsTrigger) {
        if (dismissModalsTrigger > 0L) {
            noteDialogCall = null
            contactDetailsTarget = null
        }
    }

    if (contactDetailsTarget != null) {
        val (matchedContact, favContactInitial) = contactDetailsTarget!!
        val favContact = remember(matchedContact, favorites) {
            favorites.firstOrNull { fav ->
                val favDigits = fav.phoneNumber.filter { it.isDigit() }.takeLast(10)
                val phoneMatch = if (favDigits.length >= 7) {
                    matchedContact.phoneNumbers.any { it.number.filter { c -> c.isDigit() }.takeLast(10) == favDigits } ||
                    matchedContact.phoneNumber.filter { c -> c.isDigit() }.takeLast(10) == favDigits
                } else false
                phoneMatch ||
                fav.name.equals(matchedContact.name.trim(), ignoreCase = true) ||
                (!matchedContact.nickname.isNullOrBlank() && fav.name.equals(matchedContact.nickname!!.trim(), ignoreCase = true))
            }
        }
        ContactDetailsBottomSheet(
            contact = matchedContact,
            favoriteContact = favContact,
            isFavorite = favContact != null,
            onCallNumber = { num ->
                onCallNumber(num)
            },
            onSelectInDialer = { num ->
                onCallNumber(num)
            },
            onToggleFavorite = {
                onToggleFavorite(matchedContact.name, favContact?.phoneNumber ?: matchedContact.phoneNumber, favContact?.label ?: matchedContact.label, matchedContact.photoUri)
            },
            onSetAsDefaultNumber = { newNum, newLabel ->
                if (onSetDefaultContactNumber != null) {
                    onSetDefaultContactNumber(matchedContact, newNum, newLabel)
                }
                if (favContact != null) {
                    onUpdateFavoriteNumber(favContact, newNum, newLabel)
                    contactDetailsTarget = Pair(matchedContact.copy(phoneNumber = newNum, label = newLabel), favContact.copy(phoneNumber = newNum, label = newLabel))
                } else {
                    contactDetailsTarget = Pair(matchedContact.copy(phoneNumber = newNum, label = newLabel), null)
                }
            },
            onClearDefaultNumber = {
                val firstNum = matchedContact.phoneNumbers.firstOrNull()?.number ?: matchedContact.phoneNumber
                val firstLabel = matchedContact.phoneNumbers.firstOrNull()?.label ?: matchedContact.label
                if (onSetDefaultContactNumber != null) {
                    onSetDefaultContactNumber(matchedContact, firstNum, firstLabel)
                }
                if (favContact != null) {
                    onUpdateFavoriteNumber(favContact, firstNum, firstLabel)
                    contactDetailsTarget = Pair(matchedContact.copy(phoneNumber = firstNum, label = firstLabel), favContact.copy(phoneNumber = firstNum, label = firstLabel))
                }
            },
            onCreateRule = { num ->
                onCreateRuleForNumber(num)
            },
            onAddNewContact = { name, number, label, saveToDevice, addToFav ->
                onAddNewContact(name, number, label, saveToDevice, addToFav)
                contactDetailsTarget = null
            },
            onDeleteContact = if (onDeleteContact != null) {
                { contactToDelete ->
                    val numsToDelete = (contactToDelete.phoneNumbers.map { it.number } + listOf(contactToDelete.phoneNumber))
                        .filter { it.isNotBlank() }
                        .distinct()
                    numsToDelete.forEach { num ->
                        onDeleteCallsForNumber(num)
                    }
                    onDeleteContact(contactToDelete)
                    contactDetailsTarget = null
                }
            } else null,
            onDeleteCallLog = {
                val numsToDelete = (matchedContact.phoneNumbers.map { it.number } + listOf(matchedContact.phoneNumber))
                    .filter { it.isNotBlank() }
                    .distinct()
                numsToDelete.forEach { num ->
                    onDeleteCallsForNumber(num)
                }
                contactDetailsTarget = null
            },
            getPreferredCallingMode = getPreferredCallingMode,
            onSaveLearnedCallMode = onSaveLearnedCallMode,
            activeSims = activeSims,
            getPreferredSimSlot = getPreferredSimSlot,
            onSetPreferredSimSlot = onSetPreferredSimSlot,
            globalSimPreferenceMode = globalSimPreferenceMode,
            onEditContact = { name, number, label, nickname ->
                onUpdateContact(matchedContact.phoneNumber, name, number, label, nickname)
                contactDetailsTarget = null
            },
            onDismiss = {
                contactDetailsTarget = null
            }
        )
    }

    if (noteDialogCall != null) {
        val targetCall = noteDialogCall!!
        PostCallNoteReminderDialog(
            call = targetCall,
            onDismiss = { noteDialogCall = null },
            onSave = { note, reminderEpoch ->
                onUpdateNoteAndReminder(targetCall, note, reminderEpoch)
            }
        )
    }
    // Group consecutive calls from the same phone number
    val groupedCalls = androidx.compose.runtime.remember(recentCalls, spamNumbers, isSpamNumber) {
        val groups = mutableListOf<GroupedCallLog>()
        if (recentCalls.isEmpty()) return@remember groups

        var currentGroupCall = recentCalls[0]
        var currentCount = 1

        for (i in 1 until recentCalls.size) {
            val call = recentCalls[i]
            val isSameNumber = ContactHelper.isSamePhoneNumber(call.phoneNumber, currentGroupCall.phoneNumber)
            if (isSameNumber && call.callType == currentGroupCall.callType) {
                currentCount++
                if (currentGroupCall.note.isNullOrBlank() && !call.note.isNullOrBlank()) {
                    currentGroupCall = currentGroupCall.copy(
                        note = call.note,
                        reminderTime = call.reminderTime
                    )
                }
            } else {
                val spam = spamNumbers.firstOrNull { s ->
                    ContactHelper.isSamePhoneNumber(s.phoneNumber, currentGroupCall.phoneNumber)
                }
                val isSpam = isSpamNumber?.invoke(currentGroupCall.phoneNumber) ?: (spam != null)
                groups.add(GroupedCallLog(currentGroupCall, currentCount, isSpam, spam))
                currentGroupCall = call
                currentCount = 1
            }
        }
        val lastSpam = spamNumbers.firstOrNull { s ->
            ContactHelper.isSamePhoneNumber(s.phoneNumber, currentGroupCall.phoneNumber)
        }
        val isSpam = isSpamNumber?.invoke(currentGroupCall.phoneNumber) ?: (lastSpam != null)
        groups.add(GroupedCallLog(currentGroupCall, currentCount, isSpam, lastSpam))
        groups
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredGroupedCalls = remember(groupedCalls, searchQuery, selectedFilter) {
        groupedCalls.filter { group ->
            val call = group.primaryCall

            // 1. Check Category Filter
            val matchesFilter = when (selectedFilter) {
                "MISSED" -> call.callType == 3
                "INCOMING" -> call.callType == 1
                "OUTGOING" -> call.callType == 2 && call.callReason?.contains("WhatsApp", ignoreCase = true) != true
                "WHATSAPP" -> call.callReason?.contains("WhatsApp", ignoreCase = true) == true
                "SPAM" -> group.isSpam
                "RULES" -> {
                    val normCallNum = call.phoneNumber.filter { it.isDigit() }.takeLast(10)
                    !call.ruleMatched.isNullOrBlank() || rules.any { rule ->
                        if (!rule.isEnabled) return@any false
                        val normPattern = rule.phoneNumberPattern.filter { it.isDigit() }.takeLast(10)
                        (normPattern.isNotBlank() && normCallNum.contains(normPattern)) ||
                        (rule.phoneNumberPattern.isNotBlank() && call.phoneNumber.contains(rule.phoneNumberPattern))
                    }
                }
                "NOTES" -> !call.note.isNullOrBlank()
                else -> true
            }

            if (!matchesFilter) return@filter false

            // 2. Check Search Query
            if (searchQuery.isBlank()) true
            else {
                val q = searchQuery.trim().lowercase()
                call.phoneNumber.contains(q) ||
                (call.callerName != null && call.callerName.lowercase().contains(q)) ||
                (call.callReason != null && call.callReason.lowercase().contains(q)) ||
                (call.note != null && call.note.lowercase().contains(q)) ||
                (call.ruleMatched != null && call.ruleMatched.lowercase().contains(q)) ||
                (call.communityTag != null && call.communityTag.lowercase().contains(q))
            }
        }
    }

    // Stable per-item keys. Deriving the key from the item itself (rather than its position) means
    // prepending a new call no longer re-keys the whole list. The occurrence suffix only ever
    // applies to genuinely duplicated ids, so it cannot reintroduce that instability.
    val callLogItemKeys = remember(filteredGroupedCalls) {
        val seen = HashMap<String, Int>()
        filteredGroupedCalls.map { group ->
            val base = "${group.primaryCall.id}_${group.primaryCall.timestamp}"
            val occurrences = seen[base]
            if (occurrences == null) {
                seen[base] = 1
                base
            } else {
                seen[base] = occurrences + 1
                "${base}_$occurrences"
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(highlightNumber) {
        if (!highlightNumber.isNullOrBlank()) {
            searchQuery = ""
            isSearchExpanded = false
            selectedFilter = "ALL"
        }
    }

    BackHandler(enabled = searchQuery.isNotBlank() || isSearchExpanded || selectedFilter != "ALL") {
        if (searchQuery.isNotBlank()) {
            searchQuery = ""
        } else if (isSearchExpanded) {
            isSearchExpanded = false
        } else if (selectedFilter != "ALL") {
            selectedFilter = "ALL"
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Precomputed lookups for performance optimization during scrolling
    val fastFavoritesNormalizedSet = remember(favorites) {
        favorites.map { ContactHelper.normalizeToLocalDigits(it.phoneNumber) }.filter { it.isNotBlank() }.toSet()
    }
    val fastFavoritesMap = remember(favorites) {
        val map = mutableMapOf<String, FavoriteContact>()
        favorites.forEach { fav ->
            val norm = ContactHelper.normalizeToLocalDigits(fav.phoneNumber)
            if (norm.isNotBlank() && !map.containsKey(norm)) {
                map[norm] = fav
            }
        }
        map
    }

    // Pre-indexed device contacts by phone numbers (normalized)
    val fastDeviceContactsMap = remember(deviceContacts) {
        val map = mutableMapOf<String, DeviceContact>()
        deviceContacts.forEach { dc ->
            val normPrimary = ContactHelper.normalizeToLocalDigits(dc.phoneNumber)
            if (normPrimary.isNotBlank() && !map.containsKey(normPrimary)) {
                map[normPrimary] = dc
            }
            dc.phoneNumbers.forEach { pn ->
                val normNum = ContactHelper.normalizeToLocalDigits(pn.number)
                if (normNum.isNotBlank() && !map.containsKey(normNum)) {
                    map[normNum] = dc
                }
            }
        }
        map
    }

    // Precomputed distinct caller name map
    val fastRecentCallerDistinctNumbersCount = remember(recentCalls) {
        val result = mutableMapOf<String, Int>()
        val groupedByName = recentCalls.filter { !it.callerName.isNullOrBlank() }.groupBy { it.callerName!!.lowercase() }
        groupedByName.forEach { (name, calls) ->
            result[name] = calls.map { ContactHelper.normalizeToLocalDigits(it.phoneNumber) }.distinct().size
        }
        result
    }

    var activeHighlightedCallId by remember(highlightNumber) { mutableStateOf<Long?>(null) }
    var hasScrolledToHighlight by remember(highlightNumber) { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(highlightNumber, filteredGroupedCalls) {
        if (!highlightNumber.isNullOrBlank() && filteredGroupedCalls.isNotEmpty() && !hasScrolledToHighlight) {
            val targetDigits = highlightNumber.filter { it.isDigit() }.takeLast(10)
            val index = filteredGroupedCalls.indexOfFirst {
                val callDigits = it.primaryCall.phoneNumber.filter { c -> c.isDigit() }.takeLast(10)
                (targetDigits.isNotBlank() && callDigits == targetDigits) || it.primaryCall.phoneNumber == highlightNumber
            }
            if (index >= 0) {
                activeHighlightedCallId = filteredGroupedCalls[index].primaryCall.id
                hasScrolledToHighlight = true
                listState.animateScrollToItem(index)
                kotlinx.coroutines.delay(3500L)
                activeHighlightedCallId = null
            }
        }
    }

    if (recentCalls.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp)
                .testTag("call_log_empty"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Text(
                    text = "No Recent Calls",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Calls made or received will appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // Top Filter Buttons & Search Toggle Row (Stationary at the very top)
            val filterOptions = listOf(
                FilterOptionData("ALL", "All Calls", Icons.AutoMirrored.Filled.List, null),
                FilterOptionData("MISSED", "Missed Calls", Icons.AutoMirrored.Filled.CallMissed, MaterialTheme.colorScheme.error),
                FilterOptionData("INCOMING", "Incoming Calls", Icons.AutoMirrored.Filled.CallReceived, Color(0xFF2E7D32)),
                FilterOptionData("OUTGOING", "Outgoing Calls", Icons.AutoMirrored.Filled.CallMade, MaterialTheme.colorScheme.primary),
                FilterOptionData("WHATSAPP", "WhatsApp Calls", Icons.Default.Chat, Color(0xFF25D366)),
                FilterOptionData("SPAM", "Spam Calls", Icons.Default.Shield, MaterialTheme.colorScheme.error),
                FilterOptionData("RULES", "Rules & Automation", Icons.Default.Bolt, Color(0xFFE65100)),
                FilterOptionData("NOTES", "Notes & Reminders", Icons.Default.EditNote, Color(0xFF673AB7))
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                filterOptions.forEach { item ->
                    val isSelected = (selectedFilter == item.key)
                    val accentColor = item.activeColor ?: MaterialTheme.colorScheme.primary

                    @OptIn(ExperimentalMaterial3Api::class)
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text(item.label)
                            }
                        },
                        state = rememberTooltipState()
                    ) {
                        Surface(
                            onClick = {
                                selectedFilter = if (isSelected && item.key != "ALL") "ALL" else item.key
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                accentColor.copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("recents_filter_${item.key.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                    }
                }

                // Search Toggle Button with Tooltip
                val isSearchActive = isSearchExpanded || searchQuery.isNotBlank()
                @OptIn(ExperimentalMaterial3Api::class)
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = {
                        PlainTooltip {
                            Text(if (isSearchExpanded) "Close search" else "Search recents")
                        }
                    },
                    state = rememberTooltipState()
                ) {
                    Surface(
                        onClick = {
                            isSearchExpanded = !isSearchExpanded
                            if (!isSearchExpanded) {
                                searchQuery = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSearchActive) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        },
                        border = BorderStroke(
                            width = if (isSearchActive) 1.5.dp else 0.5.dp,
                            color = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("recents_search_toggle_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = if (isSearchExpanded) "Close search" else "Search recents",
                                tint = if (isSearchActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // Sub-bar Area: Search Bar (when expanded) OR Clean Borderless Status Row (when collapsed)
            // Both states use an identical fixed container height (44.dp) to eliminate list bouncing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSearchExpanded) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val searchPlaceholder = when (selectedFilter) {
                            "SPAM" -> "Search spam calls..."
                            "MISSED" -> "Search missed calls..."
                            "INCOMING" -> "Search incoming calls..."
                            "OUTGOING" -> "Search outgoing calls..."
                            "WHATSAPP" -> "Search WhatsApp calls..."
                            "RULES" -> "Search rules..."
                            "NOTES" -> "Search notes..."
                            else -> "Search recents..."
                        }
                        CompactSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = searchPlaceholder,
                            modifier = Modifier.weight(1f),
                            testTag = "recents_search_input"
                        )
                        Surface(
                            onClick = {
                                searchQuery = ""
                                isSearchExpanded = false
                            },
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Fixed-height, clean borderless status row so the list never jumps when changing filters or opening search
                    val activeOption = filterOptions.firstOrNull { it.key == selectedFilter }
                    val isFiltered = (selectedFilter != "ALL")
                    val accentColor = activeOption?.activeColor ?: MaterialTheme.colorScheme.primary

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = activeOption?.icon ?: Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                tint = if (isFiltered) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isFiltered) {
                                    "Filtered by: ${activeOption?.label} • ${filteredGroupedCalls.size} ${if (filteredGroupedCalls.size == 1) "call" else "calls"}"
                                } else {
                                    "All Calls • ${filteredGroupedCalls.size} ${if (filteredGroupedCalls.size == 1) "call" else "calls"}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isFiltered) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isFiltered) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (isFiltered) {
                            Surface(
                                onClick = { selectedFilter = "ALL" },
                                shape = RoundedCornerShape(6.dp),
                                color = accentColor.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Show All",
                                        tint = accentColor,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Show All",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filteredGroupedCalls.isEmpty() && searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No calls match \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().testTag("call_log_list"),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(filteredGroupedCalls, key = { index, _ -> callLogItemKeys[index] }) { _, group ->
                val call = group.primaryCall
                val isVoicemail = remember(call.phoneNumber) {
                    ContactHelper.isVoicemailNumber(context, call.phoneNumber)
                }
                val matchedFav = remember(call.phoneNumber, fastFavoritesMap, favorites, isVoicemail) {
                    if (isVoicemail) return@remember null
                    val norm = ContactHelper.normalizeToLocalDigits(call.phoneNumber)
                    fastFavoritesMap[norm] ?: favorites.firstOrNull { fav ->
                        ContactHelper.isSamePhoneNumber(fav.phoneNumber, call.phoneNumber) ||
                        (call.phoneNumber.isNotBlank() && ContactHelper.normalizeToLocalDigits(fav.phoneNumber) == norm)
                    }
                }
                val isFav = remember(call.phoneNumber, fastFavoritesNormalizedSet, matchedFav, isVoicemail) {
                    if (isVoicemail) false else (matchedFav != null || fastFavoritesNormalizedSet.contains(ContactHelper.normalizeToLocalDigits(call.phoneNumber)))
                }
                val matchedDc = remember(call.phoneNumber, call.callerName, fastDeviceContactsMap, deviceContacts, matchedFav, isVoicemail) {
                    if (isVoicemail) return@remember null
                    val norm = ContactHelper.normalizeToLocalDigits(call.phoneNumber)
                    var dcMatch = if (norm.length >= 7) fastDeviceContactsMap[norm] else null
                    if (dcMatch == null && !call.callerName.isNullOrBlank()) {
                        dcMatch = deviceContacts.firstOrNull { it.name.equals(call.callerName, ignoreCase = true) }
                    }
                    if (dcMatch != null) {
                        if (!matchedFav?.nickname.isNullOrBlank()) {
                            dcMatch.copy(nickname = matchedFav?.nickname)
                        } else {
                            dcMatch
                        }
                    } else if (matchedFav != null) {
                        DeviceContact(
                            name = matchedFav.name.ifBlank { call.callerName ?: call.phoneNumber },
                            phoneNumber = call.phoneNumber,
                            nickname = matchedFav.nickname?.ifBlank { null },
                            label = matchedFav.label,
                            photoUri = matchedFav.photoUri ?: call.photoUri,
                            phoneNumbers = listOf(ContactPhoneNumber(call.phoneNumber, matchedFav.label))
                        )
                    } else {
                        null
                    }
                }
                val hasMultipleNumbers = remember(matchedDc, call.callerName, call.phoneNumber, fastRecentCallerDistinctNumbersCount) {
                    if (matchedDc != null) {
                        val hasMultipleInContact = matchedDc.phoneNumbers.size > 1
                        val numberNotInContact = matchedDc.phoneNumbers.none {
                            ContactHelper.isSamePhoneNumber(it.number, call.phoneNumber)
                        }
                        if (hasMultipleInContact || numberNotInContact) {
                            return@remember true
                        }
                    }
                    if (!call.callerName.isNullOrBlank()) {
                        val distinctCount = fastRecentCallerDistinctNumbersCount[call.callerName.lowercase()] ?: 0
                        if (distinctCount > 1) {
                            return@remember true
                        }
                    }
                    false
                }
                val numberLabel = remember(matchedDc, call.phoneNumber) {
                    ContactHelper.getDescriptiveNumberLabel(matchedDc, call.phoneNumber)
                }

                CallLogItem(
                    group = group,
                    isFavorite = isFav,
                    highlightNumber = if (activeHighlightedCallId == group.primaryCall.id) group.primaryCall.phoneNumber else null,
                    hasMultipleNumbers = hasMultipleNumbers,
                    numberLabel = numberLabel,
                    matchedDc = matchedDc,
                    activeSims = activeSims,
                    onCallBack = { onCallBack(group.primaryCall) },
                    onCreateRule = { onCreateRuleForNumber(group.primaryCall.phoneNumber) },
                    onOpenNoteDialog = { target ->
                        noteDialogCall = target
                    },
                    onToggleSpam = {
                        if (group.isSpam) {
                            onRemoveSpam(group.primaryCall.phoneNumber)
                        } else {
                            onMarkSpam(group.primaryCall.phoneNumber)
                        }
                    },
                    onToggleFavorite = {
                        onToggleFavorite(
                            matchedDc?.name?.ifBlank { null } ?: group.primaryCall.callerName ?: group.primaryCall.phoneNumber,
                            group.primaryCall.phoneNumber,
                            matchedDc?.label ?: "Mobile",
                            matchedDc?.photoUri ?: group.primaryCall.photoUri
                        )
                    },
                    onOpenDetails = {
                        val call = group.primaryCall
                        val isVm = ContactHelper.isVoicemailNumber(context, call.phoneNumber)
                        val contactName = if (isVm) "Voicemail" else (call.callerName?.ifBlank { null } ?: call.phoneNumber)
                        val callMatchedFav = if (isVm) null else favorites.firstOrNull { fav ->
                            ContactHelper.isSamePhoneNumber(fav.phoneNumber, call.phoneNumber) ||
                            (call.phoneNumber.isNotBlank() && ContactHelper.normalizeToLocalDigits(fav.phoneNumber) == ContactHelper.normalizeToLocalDigits(call.phoneNumber))
                        }
                        val resolvedDc = if (isVm) {
                            DeviceContact(
                                name = "Voicemail",
                                phoneNumber = call.phoneNumber,
                                label = "Voicemail",
                                photoUri = null,
                                phoneNumbers = listOf(ContactPhoneNumber(call.phoneNumber, "Voicemail"))
                            )
                        } else {
                            val baseDc = deviceContacts.firstOrNull { dc ->
                                ContactHelper.isSamePhoneNumber(dc.phoneNumber, call.phoneNumber) ||
                                dc.phoneNumbers.any { ContactHelper.isSamePhoneNumber(it.number, call.phoneNumber) }
                            }?.let { dc ->
                                val matchedItem = dc.phoneNumbers.firstOrNull { ContactHelper.isSamePhoneNumber(it.number, call.phoneNumber) }
                                if (matchedItem != null) {
                                    dc.copy(phoneNumber = matchedItem.number, label = matchedItem.label)
                                } else {
                                    dc
                                }
                            } ?: DeviceContact(
                                name = callMatchedFav?.name?.ifBlank { null } ?: contactName,
                                phoneNumber = call.phoneNumber,
                                label = callMatchedFav?.label ?: "Mobile",
                                photoUri = callMatchedFav?.photoUri ?: call.photoUri,
                                phoneNumbers = listOf(ContactPhoneNumber(call.phoneNumber, callMatchedFav?.label ?: "Mobile"))
                            )
                            if (!callMatchedFav?.nickname.isNullOrBlank()) {
                                baseDc.copy(nickname = callMatchedFav?.nickname)
                            } else {
                                baseDc
                            }
                        }
                        contactDetailsTarget = Pair(resolvedDc, callMatchedFav)
                    },
                    onDeleteCall = { onDeleteCall(group.primaryCall) },
                    onDeleteCallsForNumber = { onDeleteCallsForNumber(group.primaryCall.phoneNumber) }
                )
            }
        }
    }
}
}
}

