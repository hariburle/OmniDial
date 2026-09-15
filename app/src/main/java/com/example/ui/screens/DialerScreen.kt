package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.util.DeviceContact
import com.example.util.T9Helper
import com.example.util.T9SearchResult
import com.example.ui.components.QuickRecentsSection
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
    getPreferredCallingMode: (String) -> String = { "cellular" },
    learnedCallModes: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
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

    // Combine all contacts for T9, prioritizing official device contacts for accurate names and user nicknames
    val allSearchContacts = remember(favorites, effectiveContacts) {
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
            val digits = normDigits(dc.phoneNumber)
            if (digits.isNotBlank()) digits else (dc.name.trim().lowercase() + "_" + (dc.contactId ?: 0L))
        }
    }

    // T9 search results (Asynchronous background search so keypad input is lightning fast)
    var t9Matches by remember { mutableStateOf<List<T9SearchResult>>(emptyList()) }

    LaunchedEffect(number, allSearchContacts) {
        if (number.isNotBlank()) {
            val fav = favorites.firstOrNull { it.phoneNumber == number }
            if (fav != null) {
                matchedContact = DeviceContact(fav.name, fav.phoneNumber, fav.label, fav.photoUri)
            } else {
                withContext(Dispatchers.IO) {
                    val lookedUp = ContactHelper.lookupContactByNumber(context, number)
                    withContext(Dispatchers.Main) {
                        matchedContact = lookedUp
                    }
                }
            }

            withContext(Dispatchers.Default) {
                val matches = T9Helper.search(allSearchContacts, number)
                withContext(Dispatchers.Main) {
                    t9Matches = matches
                }
            }
        } else {
            matchedContact = null
            t9Matches = emptyList()
        }
    }

    // Filter out the already-matched contact from T9 search matches to eliminate duplicate suggestions
    val filteredT9Matches = remember(t9Matches, matchedContact, number) {
        if (matchedContact != null) {
            val cleanNum = number.filter { it.isDigit() }
            t9Matches.filter { match ->
                val matchClean = match.phoneNumber.filter { it.isDigit() }
                val isSameNum = matchClean.isNotEmpty() && (matchClean == cleanNum || (cleanNum.length >= 7 && (matchClean.endsWith(cleanNum) || cleanNum.endsWith(matchClean))))
                val isSameName = match.name.equals(matchedContact?.name, ignoreCase = true)
                !isSameNum && !isSameName
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
                number = number,
                recentCalls = recentCalls,
                t9Matches = filteredT9Matches,
                matchedContact = matchedContact,
                onSelectContactNumber = { onSelectContactNumber(it) },
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
            var localNumber by remember(number) { mutableStateOf(number) }
            LaunchedEffect(number) {
                if (localNumber != number) {
                    localNumber = number
                }
            }

            var selectionState by remember(localNumber) {
                mutableStateOf(TextRange(localNumber.length))
            }

            // Resolve full international contact number from matched contact or device address book
            val effectiveNumber = remember(localNumber, matchedContact, allSearchContacts) {
                if (localNumber.isBlank()) ""
                else {
                    val clean = localNumber.filter { it.isDigit() }
                    val last10 = if (clean.length >= 10) clean.takeLast(10) else clean
                    val matchedDc = matchedContact ?: allSearchContacts.firstOrNull { dc ->
                        val dcClean = dc.phoneNumber.filter { it.isDigit() }
                        (dcClean.isNotEmpty() && (dcClean.endsWith(last10) || last10.endsWith(dcClean))) ||
                            dc.phoneNumbers.any { pn ->
                                val pnClean = pn.number.filter { it.isDigit() }
                                pnClean.isNotEmpty() && (pnClean.endsWith(last10) || last10.endsWith(pnClean))
                            }
                    }
                    val intlNum = matchedDc?.let { dc ->
                        if (dc.phoneNumber.trim().startsWith("+")) dc.phoneNumber
                        else dc.phoneNumbers.firstOrNull { it.number.trim().startsWith("+") }?.number
                            ?: dc.phoneNumber.ifBlank { null }
                    }
                    intlNum ?: localNumber
                }
            }

            val localOnDigitPress: (Char) -> Unit = { digit ->
                val current = localNumber
                val start = selectionState.start.coerceIn(0, current.length)
                val end = selectionState.end.coerceIn(0, current.length)
                val minSel = minOf(start, end)
                val maxSel = maxOf(start, end)
                val newText = current.substring(0, minSel) + digit + current.substring(maxSel)
                localNumber = newText
                selectionState = TextRange(minSel + 1)
                onSelectContactNumber(newText)
            }

            val localOnDeleteDigit: () -> Unit = {
                val current = localNumber
                val start = selectionState.start.coerceIn(0, current.length)
                val end = selectionState.end.coerceIn(0, current.length)
                if (start == end) {
                    if (start > 0) {
                        val newText = current.substring(0, start - 1) + current.substring(start)
                        localNumber = newText
                        selectionState = TextRange(start - 1)
                        onSelectContactNumber(newText)
                    }
                } else {
                    val minSel = minOf(start, end)
                    val maxSel = maxOf(start, end)
                    val newText = current.substring(0, minSel) + current.substring(maxSel)
                    localNumber = newText
                    selectionState = TextRange(minSel)
                    onSelectContactNumber(newText)
                }
            }

            val localOnClearDigits: () -> Unit = {
                localNumber = ""
                selectionState = TextRange.Zero
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
                                        onDigitPress(',')
                                        showOverflowMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add wait (;)") },
                                    onClick = {
                                        onDigitPress(';')
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
                                        ContactHelper.launchWhatsAppMessage(context, effectiveNumber.ifBlank { number })
                                        showOverflowMenu = false
                                    }
                                )
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
                            BasicTextField(
                                value = tFV,
                                onValueChange = { newValue ->
                                    selectionState = newValue.selection
                                    keyboardController?.hide()
                                },
                                readOnly = true,
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

            // Main Telephone Keypad with Speed Dial Long-Press
            Keypad(
                compact = false,
                speedDialMap = speedDialMap,
                speedDialDisplayMode = speedDialKeypadDisplay,
                onDigitPress = localOnDigitPress,
                onDigitLongPress = { digit ->
                    when (digit) {
                        '0' -> localOnDigitPress('+')
                        '*' -> localOnDigitPress(',')
                        '#' -> localOnDigitPress(';')
                        '1' -> {
                            val vmNumber = ContactHelper.getVoicemailNumber(context)
                            speedDialToast = "Voicemail ($vmNumber)"
                            onSelectContactNumber(vmNumber)
                            onPlaceCall(vmNumber, null)
                        }
                        in '2'..'9' -> {
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
            val isWaPreferred = callingMode == "whatsapp"
            val isGsmPreferred = callingMode == "cellular"
            val isDark = isSystemInDarkTheme()
            val isNumEmpty = localNumber.isBlank()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 360.dp)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. PRIMARY ACTION ROW (Aligned with 3 Keypad Columns: [SIM] [CALL] [BACKSPACE])
                val heroContainerColor = if (isWaPreferred && !isNumEmpty) {
                    Color(0xFF25D366) // WhatsApp brand green
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
                    // PART 1 (Left 1/3, under column 1): SIM Selector Pill
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            onClick = onToggleSim,
                            shape = RoundedCornerShape(20.dp),
                            color = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("sim_toggle_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SimCard,
                                    contentDescription = "Active SIM",
                                    tint = if (simSlot == 1) Color(0xFF2563EB) else Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                val currentSim = activeSims.firstOrNull { it.slotIndex + 1 == simSlot }
                                val simLabel = if (currentSim != null && currentSim.displayName.isNotBlank()) {
                                    currentSim.displayName.take(5)
                                } else {
                                    "SIM $simSlot"
                                }
                                Text(
                                    text = simLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // PART 2 (Center 1/3, under column 2): Call Button (Circular or Pill when WhatsApp)
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            onClick = {
                                if (isNumEmpty) {
                                    if (recentCalls.isNotEmpty()) {
                                        val firstUnique = recentCalls.distinctBy { it.phoneNumber.filter { c -> c.isDigit() || c == '+' } }.firstOrNull()
                                        if (firstUnique != null) {
                                            onSelectContactNumber(firstUnique.phoneNumber)
                                        }
                                    }
                                } else if (isWaPreferred) {
                                    onPlaceWhatsAppCall(effectiveNumber)
                                } else {
                                    onPlaceCall(effectiveNumber, selectedCallReason)
                                }
                            },
                            shape = CircleShape,
                            color = heroContainerColor,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("hero_call_button")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isWaPreferred && !isNumEmpty) {
                                    WhatsAppIcon(
                                        modifier = Modifier.size(28.dp),
                                        tint = Color.White
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
                    // Exactly identical behavior to the back button next to number field:
                    // Single click deletes one digit, long-press triggers haptic vibration and clears all digits.
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

                // 2. AUXILIARY / SECONDARY ACTIONS ROW (SMS, WhatsApp Chat, Secondary Voice Channel)
                // Kept consistently positioned to avoid keypad jumping; hide/show based on user preference
                if (showDialerQuickActions) {
                    val secBgColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                    val secBorder = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    val hasNumber = !isNumEmpty
                    val actionAlpha = if (hasNumber) 1f else 0.45f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Texting Button 1: SMS (Left)
                        Card(
                            onClick = {
                                if (hasNumber) {
                                    ContactHelper.launchSms(context, effectiveNumber)
                                } else if (recentCalls.isNotEmpty()) {
                                    val firstUnique = recentCalls.distinctBy { it.phoneNumber.filter { c -> c.isDigit() || c == '+' } }.firstOrNull()
                                    if (firstUnique != null) {
                                        ContactHelper.launchSms(context, firstUnique.phoneNumber)
                                    }
                                } else {
                                    speedDialToast = "Enter number to send SMS"
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (hasNumber) secBgColor else secBgColor.copy(alpha = 0.35f)),
                            border = secBorder,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("secondary_sms")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "SMS",
                                    tint = Color(0xFF0284C7).copy(alpha = actionAlpha),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "SMS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasNumber) 1f else 0.5f),
                                    maxLines = 1
                                )
                            }
                        }

                        // Texting Button 2: WhatsApp Chat (Middle)
                        Card(
                            onClick = {
                                if (hasNumber) {
                                    ContactHelper.launchWhatsAppMessage(context, effectiveNumber)
                                } else if (recentCalls.isNotEmpty()) {
                                    val firstUnique = recentCalls.distinctBy { it.phoneNumber.filter { c -> c.isDigit() || c == '+' } }.firstOrNull()
                                    if (firstUnique != null) {
                                        ContactHelper.launchWhatsAppMessage(context, firstUnique.phoneNumber)
                                    }
                                } else {
                                    speedDialToast = "Enter number to chat on WhatsApp"
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (hasNumber) secBgColor else secBgColor.copy(alpha = 0.35f)),
                            border = secBorder,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("secondary_whatsapp_chat")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                WhatsAppIcon(
                                    modifier = Modifier.size(15.dp),
                                    tint = Color(0xFF25D366).copy(alpha = actionAlpha)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "WA Chat",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasNumber) 1f else 0.5f),
                                    maxLines = 1
                                )
                            }
                        }

                        // Calling Button: Secondary Voice Channel (Right)
                        Card(
                            onClick = {
                                if (hasNumber) {
                                    if (isWaPreferred) {
                                        onPlaceCall(effectiveNumber, selectedCallReason)
                                    } else {
                                        onPlaceWhatsAppCall(effectiveNumber)
                                    }
                                } else if (recentCalls.isNotEmpty()) {
                                    val firstUnique = recentCalls.distinctBy { it.phoneNumber.filter { c -> c.isDigit() || c == '+' } }.firstOrNull()
                                    if (firstUnique != null) {
                                        onPlaceWhatsAppCall(firstUnique.phoneNumber)
                                    }
                                } else {
                                    speedDialToast = "Enter number to call"
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = if (hasNumber) secBgColor else secBgColor.copy(alpha = 0.35f)),
                            border = secBorder,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag(if (isWaPreferred) "secondary_phone_call" else "secondary_whatsapp_call")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isWaPreferred) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Cellular Call",
                                        tint = Color(0xFF059669).copy(alpha = actionAlpha),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Cellular",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasNumber) 1f else 0.5f),
                                        maxLines = 1
                                    )
                                } else {
                                    WhatsAppIcon(
                                        modifier = Modifier.size(15.dp),
                                        tint = Color(0xFF25D366).copy(alpha = actionAlpha)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "WhatsApp",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (hasNumber) 1f else 0.5f),
                                        maxLines = 1
                                    )
                                }
                            }
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
}
