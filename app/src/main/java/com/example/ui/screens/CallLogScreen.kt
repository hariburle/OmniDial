package com.example.ui.screens

import com.example.util.ContactHelper

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.components.CompactSearchBar
import com.example.ui.components.ContactDetailsBottomSheet
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
    onCallBack: (String) -> Unit,
    onCreateRuleForNumber: (String) -> Unit,
    onMarkSpam: (String) -> Unit = {},
    onRemoveSpam: (String) -> Unit = {},
    onToggleFavorite: (name: String, number: String, label: String, photoUri: String?) -> Unit = { _, _, _, _ -> },
    onUpdateFavoriteNumber: (FavoriteContact, String, String) -> Unit = { _, _, _ -> },
    onAddNewContact: (name: String, number: String, label: String, saveToDevice: Boolean, addToFavorites: Boolean) -> Unit = { _, _, _, _, _ -> },
    onUpdateNoteAndReminder: (RecentCall, String?, Long?) -> Unit = { _, _, _ -> },
    onUpdateContact: (oldNum: String, name: String, number: String, label: String, nickname: String?) -> Unit = { _, _, _, _, _ -> },
    onDeleteCall: (RecentCall) -> Unit = {},
    onDeleteCallsForNumber: (String) -> Unit = {},
    rules: List<com.example.data.CallerRule> = emptyList(),
    deviceContacts: List<DeviceContact> = emptyList(),
    getPreferredCallingMode: (String) -> String = { "cellular" },
    onSaveLearnedCallMode: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var noteDialogCall by remember { mutableStateOf<RecentCall?>(null) }
    var noteText by remember { mutableStateOf("") }
    var reminderMinutes by remember { mutableStateOf<Long?>(null) }
    var contactDetailsTarget by remember { mutableStateOf<Pair<DeviceContact, FavoriteContact?>?>(null) }

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
                onCallBack(num)
            },
            onSelectInDialer = { num ->
                onCallBack(num)
            },
            onToggleFavorite = {
                onToggleFavorite(matchedContact.name, favContact?.phoneNumber ?: matchedContact.phoneNumber, favContact?.label ?: matchedContact.label, matchedContact.photoUri)
            },
            onSetAsDefaultNumber = { newNum, newLabel ->
                if (favContact != null) {
                    onUpdateFavoriteNumber(favContact, newNum, newLabel)
                } else {
                    onToggleFavorite(matchedContact.name, newNum, newLabel, matchedContact.photoUri)
                }
            },
            onClearDefaultNumber = {
                if (favContact != null) {
                    onToggleFavorite(favContact.name, favContact.phoneNumber, favContact.label, favContact.photoUri)
                }
            },
            onCreateRule = { num ->
                onCreateRuleForNumber(num)
            },
            onAddNewContact = { name, number, label, saveToDevice, addToFav ->
                onAddNewContact(name, number, label, saveToDevice, addToFav)
                contactDetailsTarget = null
            },
            getPreferredCallingMode = getPreferredCallingMode,
            onSaveLearnedCallMode = onSaveLearnedCallMode,
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
        AlertDialog(
            onDismissRequest = { noteDialogCall = null },
            title = {
                Text(
                    text = "Post-Call Note & Reminder",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = targetCall.callerName ?: targetCall.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("Add call summary, action items...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_call_note_input")
                    )

                    Text(
                        text = "Follow-up Reminder:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(null to "None", 15L to "15m", 60L to "1h", 1440L to "Tomorrow").forEach { (mins, label) ->
                            AssistChip(
                                onClick = { reminderMinutes = mins },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (reminderMinutes == mins) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reminderEpoch = reminderMinutes?.let { System.currentTimeMillis() + it * 60 * 1000 }
                        onUpdateNoteAndReminder(targetCall, noteText.ifBlank { null }, reminderEpoch)
                        noteDialogCall = null
                    },
                    modifier = Modifier.testTag("dialog_save_note_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { noteDialogCall = null }) {
                    Text("Cancel")
                }
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

    androidx.compose.runtime.LaunchedEffect(highlightNumber) {
        if (!highlightNumber.isNullOrBlank()) {
            searchQuery = ""
            selectedFilter = "ALL"
        }
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Precomputed lookups for performance optimization during scrolling
    val fastFavoritesNormalizedSet = remember(favorites) {
        favorites.map { ContactHelper.normalizeToLocalDigits(it.phoneNumber) }.filter { it.isNotBlank() }.toSet()
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

    androidx.compose.runtime.LaunchedEffect(highlightNumber, filteredGroupedCalls) {
        if (!highlightNumber.isNullOrBlank() && filteredGroupedCalls.isNotEmpty()) {
            val targetDigits = highlightNumber.filter { it.isDigit() }.takeLast(10)
            val index = filteredGroupedCalls.indexOfFirst {
                val callDigits = it.primaryCall.phoneNumber.filter { c -> c.isDigit() }.takeLast(10)
                (targetDigits.isNotBlank() && callDigits == targetDigits) || it.primaryCall.phoneNumber == highlightNumber
            }
            if (index >= 0) {
                listState.animateScrollToItem(index)
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
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            CompactSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search by name or number",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                testTag = "recents_search_input"
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filterOptions = listOf(
                    "ALL" to "All",
                    "MISSED" to "Missed",
                    "INCOMING" to "In",
                    "OUTGOING" to "Out",
                    "WHATSAPP" to "WhatsApp",
                    "SPAM" to "Spam",
                    "RULES" to "Rules",
                    "NOTES" to "Notes"
                )
                filterOptions.forEach { (key, label) ->
                    val isSelected = (selectedFilter == key)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = key },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        modifier = Modifier.height(28.dp)
                    )
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
                    itemsIndexed(filteredGroupedCalls, key = { index, group -> "${group.primaryCall.id}_${group.primaryCall.timestamp}_$index" }) { _, group ->
                val call = group.primaryCall
                val isFav = remember(call.phoneNumber, fastFavoritesNormalizedSet) {
                    val norm = ContactHelper.normalizeToLocalDigits(call.phoneNumber)
                    fastFavoritesNormalizedSet.contains(norm)
                }
                val matchedDc = remember(call.phoneNumber, call.callerName, fastDeviceContactsMap, deviceContacts) {
                    val norm = ContactHelper.normalizeToLocalDigits(call.phoneNumber)
                    var dcMatch = fastDeviceContactsMap[norm]
                    if (dcMatch == null && !call.callerName.isNullOrBlank()) {
                        dcMatch = deviceContacts.firstOrNull { it.name.equals(call.callerName, ignoreCase = true) }
                    }
                    dcMatch
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
                    highlightNumber = highlightNumber,
                    hasMultipleNumbers = hasMultipleNumbers,
                    numberLabel = numberLabel,
                    matchedDc = matchedDc,
                    onCallBack = { onCallBack(group.primaryCall.phoneNumber) },
                    onCreateRule = { onCreateRuleForNumber(group.primaryCall.phoneNumber) },
                    onOpenNoteDialog = { target ->
                        noteDialogCall = target
                        noteText = target.note ?: ""
                        reminderMinutes = target.reminderTime?.let {
                            val diff = (it - System.currentTimeMillis()) / (60 * 1000)
                            if (diff > 0) diff else null
                        }
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
                            group.primaryCall.callerName ?: group.primaryCall.phoneNumber,
                            group.primaryCall.phoneNumber,
                            "Mobile",
                            group.primaryCall.photoUri
                        )
                    },
                    onOpenDetails = {
                        val call = group.primaryCall
                        val contactName = call.callerName?.ifBlank { null } ?: call.phoneNumber
                        val resolvedDc = deviceContacts.firstOrNull { dc ->
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
                            name = contactName,
                            phoneNumber = call.phoneNumber,
                            label = "Mobile",
                            photoUri = call.photoUri,
                            phoneNumbers = listOf(ContactPhoneNumber(call.phoneNumber, "Mobile"))
                        )
                        val matchedFav = favorites.firstOrNull { fav ->
                            ContactHelper.isSamePhoneNumber(fav.phoneNumber, call.phoneNumber)
                        }
                        contactDetailsTarget = Pair(resolvedDc, matchedFav)
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

@Composable
private fun CallLogItem(
    group: GroupedCallLog,
    isFavorite: Boolean,
    highlightNumber: String? = null,
    hasMultipleNumbers: Boolean = false,
    numberLabel: String = "Mobile",
    onCallBack: () -> Unit,
    onCreateRule: () -> Unit,
    onOpenNoteDialog: (RecentCall) -> Unit,
    onToggleSpam: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenDetails: () -> Unit,
    onDeleteCall: () -> Unit = {},
    onDeleteCallsForNumber: () -> Unit = {},
    matchedDc: DeviceContact? = null
) {
    val call = group.primaryCall
    val photoToUse = matchedDc?.photoUri?.ifBlank { null } ?: call.photoUri?.ifBlank { null }
    val nameToUse = matchedDc?.name?.ifBlank { null } ?: call.callerName?.ifBlank { null }
    val isWhatsApp = call.callReason?.contains("WhatsApp", ignoreCase = true) == true
    val (typeIcon, typeColor, typeLabel) = when {
        isWhatsApp -> Triple(Icons.AutoMirrored.Filled.CallMade, Color(0xFF25D366), "WhatsApp Call")
        call.callType == 1 -> Triple(Icons.AutoMirrored.Filled.CallReceived, Color(0xFF16A34A), "Incoming")
        call.callType == 2 -> Triple(Icons.AutoMirrored.Filled.CallMade, Color(0xFF2563EB), "Outgoing")
        else -> Triple(Icons.AutoMirrored.Filled.CallMissed, Color(0xFFDC2626), "Missed")
    }

    val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(call.timestamp))

    val isHighlighted = remember(highlightNumber, call.phoneNumber) {
        if (highlightNumber.isNullOrBlank()) false
        else {
            val targetDigits = highlightNumber.filter { it.isDigit() }.takeLast(10)
            val callDigits = call.phoneNumber.filter { it.isDigit() }.takeLast(10)
            (targetDigits.isNotBlank() && callDigits == targetDigits) || call.phoneNumber == highlightNumber
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() }
            .testTag("call_item_${call.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            } else if (isWhatsApp) {
                Color(0xFF25D366).copy(alpha = 0.08f)
            } else if (group.isSpam) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        border = if (isHighlighted) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Avatar with contact photo or call type indicator or Spam warning
                Box(modifier = Modifier.size(36.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = if (group.isSpam) Color(0xFFDC2626).copy(alpha = 0.15f) else if (isWhatsApp) Color(0xFF25D366).copy(alpha = 0.18f) else typeColor.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (group.isSpam) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Report,
                                    contentDescription = "Spam Caller",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        } else if (!photoToUse.isNullOrBlank()) {
                            coil.compose.AsyncImage(
                                model = photoToUse,
                                contentDescription = nameToUse ?: call.phoneNumber,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (!nameToUse.isNullOrBlank()) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = nameToUse.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWhatsApp) Color(0xFF15803D) else typeColor,
                                    fontSize = 16.sp
                                )
                            }
                        } else if (isWhatsApp) {
                            Box(contentAlignment = Alignment.Center) {
                                WhatsAppIcon(
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = typeLabel,
                                    tint = typeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (isWhatsApp) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF25D366),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                WhatsAppIcon(
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    } else if (!photoToUse.isNullOrBlank() || !nameToUse.isNullOrBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = typeIcon,
                                    contentDescription = typeLabel,
                                    tint = typeColor,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Line 1: Name / Number + Count Badge + WhatsApp Badge + Missed Call Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = nameToUse ?: call.phoneNumber,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (group.isSpam) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (group.count > 1) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "(${group.count})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (isWhatsApp) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF25D366).copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    WhatsAppIcon(modifier = Modifier.size(11.dp))
                                    Text(
                                        text = "WhatsApp",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (isHighlighted) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDC2626)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallMissed,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Missed",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        if (group.isSpam) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = "Spam",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Line 1.5: Specific Phone Number and Label if contact has multiple numbers
                    if (hasMultipleNumbers && !call.callerName.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(vertical = 1.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            ) {
                                Text(
                                    text = numberLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                            Text(
                                text = call.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Line 2: Time + Duration + Rules
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        if (isWhatsApp) {
                            Text(
                                text = "• WhatsApp Call",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF15803D)
                            )
                        } else if (call.durationSeconds > 0) {
                            val mins = call.durationSeconds / 60
                            val secs = call.durationSeconds % 60
                            val durText = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                            Text(
                                text = "• $durText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!call.ruleMatched.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "🤖 ${call.ruleMatched}",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    val noteToShow = call.note
                    val reminderToShow = call.reminderTime

                    // Note Display in Recents
                    if (!noteToShow.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notes,
                                    contentDescription = "Note",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = noteToShow,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Scheduled Reminder Badge
                    if (reminderToShow != null && reminderToShow > System.currentTimeMillis()) {
                        val minsLeft = ((reminderToShow - System.currentTimeMillis()) / (60 * 1000)).coerceAtLeast(1)
                        val reminderText = if (minsLeft < 60) "Reminder in ${minsLeft}m" else "Reminder in ${minsLeft / 60}h ${minsLeft % 60}m"
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = "Follow-up Reminder",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = reminderText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            var showOverflowMenu by remember { mutableStateOf(false) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // One-tap Call Back Button
                FilledIconButton(
                    onClick = onCallBack,
                    modifier = Modifier.size(38.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call Back",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // One-tap Favorite Star
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isFavorite) "Remove Favorite" else "Add Favorite",
                        tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // More Options Menu (Note, Spam, Rule)
                Box {
                    IconButton(
                        onClick = { showOverflowMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Actions",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (!call.note.isNullOrBlank() || call.reminderTime != null) "Edit Note / Reminder" else "Add Note / Reminder") },
                            leadingIcon = {
                                Icon(Icons.Default.EditNote, contentDescription = null)
                            },
                            onClick = {
                                showOverflowMenu = false
                                onOpenNoteDialog(call)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (group.isSpam) "Unmark as Spam" else "Report as Spam") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (group.isSpam) Icons.Default.Security else Icons.Default.Block,
                                    contentDescription = null,
                                    tint = if (group.isSpam) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showOverflowMenu = false
                                onToggleSpam()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Create Automation Rule") },
                            leadingIcon = {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            onClick = {
                                showOverflowMenu = false
                                onCreateRule()
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Entry") },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            },
                            onClick = {
                                showOverflowMenu = false
                                onDeleteCall()
                            }
                        )
                        if (group.count > 1) {
                            DropdownMenuItem(
                                text = { Text("Clear All (${group.count}) for Number") },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onDeleteCallsForNumber()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
