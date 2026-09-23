package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import android.provider.ContactsContract
import android.content.ContentUris
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.example.data.ChannelPreferenceRepository
import com.example.domain.model.CallingChannel
import com.example.telecom.ChannelDiscoveryManager
import com.example.util.PhoneNumberNormalizer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppDatabase
import com.example.data.FavoriteContact
import com.example.data.RecentCall
import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android Phone Dialer-style Contact Card Bottom Sheet.
 * Displays full contact details, quick action bar (Call, Message, WhatsApp, Star Favorite),
 * and an interactive list of all phone numbers.
 * Long-pressing any phone number opens options to set or clear as default number, copy, WhatsApp, or automate.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactDetailsBottomSheet(
    contact: DeviceContact,
    favoriteContact: FavoriteContact?,
    isFavorite: Boolean,
    onCallNumber: (String) -> Unit,
    onSelectInDialer: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    onSetAsDefaultNumber: (number: String, label: String) -> Unit,
    onClearDefaultNumber: () -> Unit,
    onCreateRule: (String) -> Unit,
    onSyncToPhone: (() -> Unit)? = null,
    onEditContact: (name: String, phoneNumber: String, label: String, nickname: String?) -> Unit = { _, _, _, _ -> },
    onAddNewContact: ((name: String, number: String, label: String, saveToDevice: Boolean, addToFavorites: Boolean) -> Unit)? = null,
    onDeleteContact: ((DeviceContact) -> Unit)? = null,
    onDeleteCallLog: (() -> Unit)? = null,
    getPreferredCallingMode: (String) -> String = { "cellular" },
    onSaveLearnedCallMode: (String, String) -> Unit = { _, _ -> },
    activeSims: List<com.example.telecom.SimInfo> = emptyList(),
    getPreferredSimSlot: (String) -> Int = { 0 },
    onSetPreferredSimSlot: ((String, Int) -> Unit)? = null,
    globalSimPreferenceMode: String = "system",
    whatsAppCallMode: String = "ask_learn",
    onCallNumberDirect: ((String, Int?) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val channelPrefRepo = remember { ChannelPreferenceRepository.getInstance(context) }
    val discoveryManager = remember { ChannelDiscoveryManager.getInstance(context) }
    val availableChannels by discoveryManager.availableChannels.collectAsState()
    val numberChannelPrefs by channelPrefRepo.allPreferences.collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf(false) }
    var showCreateContactDialog by remember { mutableStateOf(false) }
    var showAddNumberDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    val isVoicemail = remember(contact.phoneNumber, contact.name) {
        ContactHelper.isVoicemailNumber(context, contact.phoneNumber) || contact.name.equals("Voicemail", ignoreCase = true)
    }
    val isUnknownNumber = remember(contact, isVoicemail) {
        if (isVoicemail) return@remember false
        val trimmed = contact.name.trim()
        trimmed.startsWith("+") ||
        trimmed.all { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' } ||
        trimmed == contact.phoneNumber.trim()
    }
    val effectiveNickname = contact.nickname?.ifBlank { null } ?: favoriteContact?.nickname?.ifBlank { null } ?: ""
    var editName by remember(contact) { mutableStateOf(contact.name) }
    var editNumber by remember(contact) { mutableStateOf(contact.phoneNumber) }
    var editLabel by remember(contact) { mutableStateOf(contact.label) }
    var editNickname by remember(contact, favoriteContact) { mutableStateOf(effectiveNickname) }

    val stableContactKey = remember(contact.contactId, contact.name) {
        contact.contactId?.toString() ?: contact.name
    }

    val initialDefaultNumber = remember(stableContactKey) {
        val favNum = favoriteContact?.phoneNumber
        if (contact.phoneNumber.isNotBlank()) {
            contact.phoneNumber
        } else if (favNum != null && ContactHelper.isSamePhoneNumber(favNum, contact.phoneNumber)) {
            favNum
        } else {
            favNum ?: contact.phoneNumbers.firstOrNull()?.number ?: ""
        }
    }

    // Current default/primary number for this contact - reactive to user changes
    var currentDefaultNumber by remember(stableContactKey) {
        mutableStateOf(initialDefaultNumber)
    }
    var numberForActionMenu by remember { mutableStateOf<ContactPhoneNumber?>(null) }
    val preferredModes = remember { mutableStateMapOf<String, String>() }
    val preferredSims = remember { mutableStateMapOf<String, Int>() }
    var showNicknameEditDialog by remember { mutableStateOf(false) }
    var directNicknameText by remember(contact.nickname, favoriteContact?.nickname) { mutableStateOf(effectiveNickname) }
    var currentDisplayNickname by remember(contact.nickname, favoriteContact?.nickname) { mutableStateOf(effectiveNickname) }

    var contactCallHistory by remember { mutableStateOf<List<RecentCall>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(true) }
    var pendingChannelChoiceNumber by remember { mutableStateOf<ContactPhoneNumber?>(null) }

    LaunchedEffect(contact) {
        isLoadingHistory = true
        withContext(Dispatchers.IO) {
            val phoneNumbers = contact.phoneNumbers.map { it.number } + listOfNotNull(contact.phoneNumber.ifBlank { null })
            val db = AppDatabase.getInstance(context)
            val localHistory = db.appDao().getCallHistoryForContactList(phoneNumbers, contact.name)
            val deviceHistory = ContactHelper.fetchDeviceCallHistoryForContact(context, phoneNumbers, contact.name)

            // Merge local and device call logs, eliminating close duplicates
            val merged = (localHistory + deviceHistory)
                .distinctBy { "${it.phoneNumber}_${it.timestamp / 10000}_${it.callType}" }
                .sortedByDescending { it.timestamp }
                .take(40)

            contactCallHistory = merged
            isLoadingHistory = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(3.dp)
            ) {
                Spacer(modifier = Modifier.size(width = 38.dp, height = 4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Avatar, Name, and Close Button
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopEnd
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { showDeleteConfirmationDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = if (isUnknownNumber && onDeleteCallLog != null) "Remove from Recents" else "Delete Contact",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                onToggleFavorite()
                                Toast.makeText(
                                    context,
                                    if (isFavorite) "Removed from Favorites" else "★ Added to Favorites",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("contact_details_star_toggle")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                                tint = if (isFavorite) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Contact Avatar with interactive + badge for unknown numbers
                    Box(modifier = Modifier.size(76.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = if (isUnknownNumber) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(enabled = isUnknownNumber) { showCreateContactDialog = true },
                            shadowElevation = 2.dp
                        ) {
                            if (!contact.photoUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = contact.photoUri,
                                    contentDescription = contact.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (isVoicemail) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Voicemail,
                                        contentDescription = "Voicemail",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            } else if (isUnknownNumber) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Add to Contacts",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        if (isUnknownNumber) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(24.dp)
                                    .align(Alignment.BottomEnd)
                                    .clickable { showCreateContactDialog = true }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "+",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val hasNickname = currentDisplayNickname.isNotBlank()
                    val primaryHeadline = contact.name
                    val subtitleText = if (hasNickname) "($currentDisplayNickname)" else "+ Add Nickname"

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = primaryHeadline,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isFavorite) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Favorite Contact",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                directNicknameText = currentDisplayNickname
                                showNicknameEditDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = subtitleText,
                            style = if (hasNickname) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (hasNickname) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Nickname",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    if (isUnknownNumber) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { showCreateContactDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.78f)
                                .height(40.dp)
                                .testTag("btn_add_to_contacts")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add to Contacts",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val isPhoneContact = contact.contactId != null && contact.contactId > 0 && !contact.isAppOnly
                    OutlinedButton(
                        onClick = {
                            if (isPhoneContact) {
                                ContactHelper.launchContactEditor(context, contact)
                                onDismiss()
                            } else {
                                showEditDialog = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.72f)
                            .height(36.dp)
                            .testTag("btn_edit_in_system_contacts"),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPhoneContact) "Edit in Phone Contacts" else "Edit Local Contact",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Spacer(modifier = Modifier.height(8.dp))

            if (contact.isAppOnly && onSyncToPhone != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App-Only Contact",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Saved locally. Sync to Phone Contacts to make it available system-wide.",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSyncToPhone()
                                Toast.makeText(context, "Synced ${contact.name} to Phone Contacts!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Sync", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Phone Numbers Header & Info Hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PHONE NUMBERS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tap to call • Long press options",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // List of Phone Numbers with Dialer Card Look
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val displayedNumbers = remember(stableContactKey) {
                        val all = if (contact.phoneNumbers.isNotEmpty()) contact.phoneNumbers else listOf(ContactPhoneNumber(contact.phoneNumber, contact.label))
                        val defNum = initialDefaultNumber.ifBlank { contact.phoneNumber }
                        if (defNum.isNotBlank()) {
                            val defaultPn = all.firstOrNull { pn ->
                                pn.number == defNum || ContactHelper.isSamePhoneNumber(pn.number, defNum)
                            }
                            if (defaultPn != null) {
                                listOf(defaultPn) + all.filter { it != defaultPn }
                            } else {
                                all
                            }
                        } else {
                            all
                        }
                    }

                    displayedNumbers.forEachIndexed { index, pn ->
                        val normPn = pn.number.filter { it.isDigit() }.takeLast(10)
                        val favDigits = favoriteContact?.phoneNumber?.filter { it.isDigit() }?.takeLast(10) ?: ""
                        val isThisNumberFavorite = isFavorite && (
                            (favDigits.length >= 7 && normPn == favDigits) ||
                            (displayedNumbers.size == 1)
                        )
                        val isDefaultNumber = if (currentDefaultNumber.isNotBlank()) {
                            pn.number == currentDefaultNumber || ContactHelper.isSamePhoneNumber(pn.number, currentDefaultNumber)
                        } else {
                            index == 0
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(
                                    when {
                                        displayedNumbers.size == 1 -> RoundedCornerShape(16.dp)
                                        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                        index == displayedNumbers.lastIndex -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                                        else -> RoundedCornerShape(0.dp)
                                    }
                                )
                                .combinedClickable(
                                    onClick = {
                                        onDismiss()
                                        onCallNumber(pn.number)
                                    },
                                    onLongClick = {
                                        numberForActionMenu = pn
                                    }
                                )
                                .background(
                                    if (isDefaultNumber) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    else if (isThisNumberFavorite) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            val containerBg = MaterialTheme.colorScheme.surface
                            val containerBorder = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                            // Per-Number Channel Preference
                            val normalizedPn = remember(pn.number) {
                                PhoneNumberNormalizer.toE164(pn.number)
                            }
                            val savedPref = numberChannelPrefs.firstOrNull { it.normalizedNumber == normalizedPn }
                            val effectiveChannelId = savedPref?.preferredChannelId ?: run {
                                val legacyMode = preferredModes[pn.number] ?: getPreferredCallingMode(pn.number)
                                val legacySlot = preferredSims[pn.number] ?: getPreferredSimSlot(pn.number)
                                when {
                                    legacyMode == "whatsapp" -> "whatsapp"
                                    legacyMode == "whatsapp_business" -> "whatsapp_business"
                                    legacyMode == "google_voice" -> "google_voice"
                                    legacySlot == 1 -> "sim_1"
                                    legacySlot == 2 -> "sim_2"
                                    legacyMode == "cellular" -> "sim_1"
                                    else -> "ask"
                                }
                            }
                            val resolvedPreferredChannel = availableChannels.firstOrNull { it.id.equals(effectiveChannelId, ignoreCase = true) }
                            val isWhatsAppChannel = (resolvedPreferredChannel is CallingChannel.WhatsApp)
                            val isGoogleVoiceChannel = (resolvedPreferredChannel is CallingChannel.GoogleVoice)
                            val isCellularChannel = (resolvedPreferredChannel is CallingChannel.CellularSim) || (effectiveChannelId == "system" && availableChannels.any { it is CallingChannel.CellularSim })

                            // Row 1: Number + Label (Left) and Action Buttons (Right)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = pn.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        if (isDefaultNumber) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "DEFAULT",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        if (isThisNumberFavorite) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFF59E0B),
                                                contentColor = Color.White
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = "FAVORITE",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = pn.number,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 1. Star / Default Number toggle button
                                    IconButton(
                                        onClick = {
                                            if (isDefaultNumber) {
                                                currentDefaultNumber = ""
                                                onClearDefaultNumber()
                                                Toast.makeText(context, "Default number cleared", Toast.LENGTH_SHORT).show()
                                            } else {
                                                currentDefaultNumber = pn.number
                                                onSetAsDefaultNumber(pn.number, pn.label)
                                                Toast.makeText(context, "★ Set as default: ${pn.number}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("star_toggle_${pn.number}")
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isDefaultNumber) Color(0xFFF59E0B).copy(alpha = 0.15f) else containerBg,
                                            border = if (isDefaultNumber) BorderStroke(1.5.dp, Color(0xFFF59E0B)) else containerBorder,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isDefaultNumber) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                                    contentDescription = if (isDefaultNumber) "Default number (Tap to clear)" else "Set as default number",
                                                    tint = if (isDefaultNumber) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    // 2. Consolidated Message Button (SMS, WhatsApp chat, or Google Voice based on channel)
                                    val isWaBiz = effectiveChannelId == "whatsapp_business"
                                    val waBrandColor = if (isWaBiz) Color(0xFF128C7E) else Color(0xFF25D366)
                                    val gvBrandColor = Color(0xFF0F9D58)
                                    IconButton(
                                        onClick = {
                                            if (isWhatsAppChannel) {
                                                ContactHelper.launchWhatsAppMessage(context, pn.number, isBusiness = isWaBiz)
                                            } else if (isGoogleVoiceChannel) {
                                                ContactHelper.launchGoogleVoiceMessage(context, pn.number)
                                            } else {
                                                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${pn.number}"))
                                                context.startActivity(smsIntent)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("btn_message_${pn.number}")
                                    ) {
                                        val msgBg = when {
                                            isWhatsAppChannel -> waBrandColor.copy(alpha = 0.15f)
                                            isGoogleVoiceChannel -> gvBrandColor.copy(alpha = 0.15f)
                                            else -> containerBg
                                        }
                                        val msgBorder = when {
                                            isWhatsAppChannel -> BorderStroke(1.5.dp, waBrandColor)
                                            isGoogleVoiceChannel -> BorderStroke(1.5.dp, gvBrandColor)
                                            else -> containerBorder
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = msgBg,
                                            border = msgBorder,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (isWhatsAppChannel) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                                        contentDescription = if (isWaBiz) "WhatsApp Business Message" else "WhatsApp Message",
                                                        tint = waBrandColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else if (isGoogleVoiceChannel) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Chat,
                                                        contentDescription = "Google Voice Message",
                                                        tint = gvBrandColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Message,
                                                        contentDescription = "SMS Message",
                                                        tint = if (isCellularChannel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // 3. Consolidated Call Button (Direct dial if channel known; confirmation sheet if unknown)
                                    IconButton(
                                        onClick = {
                                            when {
                                                resolvedPreferredChannel is CallingChannel.WhatsApp -> {
                                                    onDismiss()
                                                    ContactHelper.launchWhatsAppCall(context, pn.number, isBusiness = resolvedPreferredChannel.isBusiness)
                                                }
                                                isGoogleVoiceChannel -> {
                                                    onDismiss()
                                                    val gvHandle = (availableChannels.firstOrNull { it is CallingChannel.GoogleVoice } as? CallingChannel.GoogleVoice)?.phoneAccountHandle
                                                    ContactHelper.launchGoogleVoiceCall(context, pn.number, accountHandle = gvHandle, onCellularFallback = { onCallNumber(pn.number) })
                                                }
                                                isCellularChannel -> {
                                                    val simSlot = (resolvedPreferredChannel as? CallingChannel.CellularSim)?.let { it.slotIndex + 1 }
                                                        ?: when (effectiveChannelId) {
                                                            "sim_1" -> 1
                                                            "sim_2" -> 2
                                                            else -> null
                                                        }
                                                    if (simSlot != null) {
                                                        onSetPreferredSimSlot?.invoke(pn.number, simSlot)
                                                    }
                                                    onDismiss()
                                                    if (onCallNumberDirect != null) {
                                                        onCallNumberDirect(pn.number, simSlot)
                                                    } else {
                                                        onCallNumber(pn.number)
                                                    }
                                                }
                                                else -> {
                                                    // Preference unknown or channel disabled -> Ask user in confirmation sheet before placing call
                                                    pendingChannelChoiceNumber = pn
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("btn_call_${pn.number}")
                                    ) {
                                        val callBg = when {
                                            isWhatsAppChannel -> Color(0xFF25D366).copy(alpha = 0.18f)
                                            isGoogleVoiceChannel -> Color(0xFF0F9D58).copy(alpha = 0.18f)
                                            isCellularChannel -> Color(0xFF16A34A).copy(alpha = 0.18f)
                                            else -> containerBg
                                        }
                                        val callBorder = when {
                                            isWhatsAppChannel -> BorderStroke(1.5.dp, Color(0xFF25D366))
                                            isGoogleVoiceChannel -> BorderStroke(1.5.dp, Color(0xFF0F9D58))
                                            isCellularChannel -> BorderStroke(1.5.dp, Color(0xFF16A34A))
                                            else -> containerBorder
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = callBg,
                                            border = callBorder,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (isWhatsAppChannel) {
                                                    WhatsAppIcon(modifier = Modifier.size(19.dp))
                                                } else if (isGoogleVoiceChannel) {
                                                    Icon(
                                                        imageVector = Icons.Default.Phone,
                                                        contentDescription = "Call via Google Voice",
                                                        tint = Color(0xFF0F9D58),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Call,
                                                        contentDescription = "Call",
                                                        tint = if (isCellularChannel) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Channel:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    // 1. Cellular SIM Channels
                                    val simChannels = availableChannels.filterIsInstance<CallingChannel.CellularSim>()
                                    if (simChannels.isNotEmpty()) {
                                        simChannels.forEach { simChan ->
                                            val isSelected = effectiveChannelId == simChan.id ||
                                                (effectiveChannelId == "system" && simChan.slotIndex == 0)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = {
                                                    coroutineScope.launch {
                                                        channelPrefRepo.setPreferenceForNumber(pn.number, simChan.id, pn.label)
                                                    }
                                                    preferredSims[pn.number] = simChan.slotIndex + 1
                                                    onSetPreferredSimSlot?.invoke(pn.number, simChan.slotIndex + 1)
                                                    preferredModes[pn.number] = "cellular"
                                                    onSaveLearnedCallMode(pn.number, "cellular")
                                                    Toast.makeText(context, "★ Preferred for ${pn.label.ifBlank { "number" }}: ${simChan.shortLabel}", Toast.LENGTH_SHORT).show()
                                                },
                                                label = {
                                                    Text(
                                                        text = simChan.shortLabel,
                                                        fontSize = 10.5.sp,
                                                        maxLines = 1
                                                    )
                                                },
                                                leadingIcon = if (isSelected) {
                                                    { Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                                } else null,
                                                modifier = Modifier.height(26.dp)
                                            )
                                        }
                                    } else {
                                        val isPhoneSelected = effectiveChannelId == "system" || effectiveChannelId == "sim_1"
                                        val phoneLabel = remember(context) {
                                            com.example.data.ChannelConfigRepository.getInstance(context).getCustomNameSync("sim_1") ?: "Phone"
                                        }
                                        FilterChip(
                                            selected = isPhoneSelected,
                                            onClick = {
                                                coroutineScope.launch {
                                                    channelPrefRepo.setPreferenceForNumber(pn.number, "sim_1", pn.label)
                                                }
                                                preferredModes[pn.number] = "cellular"
                                                onSaveLearnedCallMode(pn.number, "cellular")
                                                Toast.makeText(context, "★ Preferred channel: $phoneLabel", Toast.LENGTH_SHORT).show()
                                            },
                                            label = { Text(phoneLabel, fontSize = 10.5.sp) },
                                            leadingIcon = if (isPhoneSelected) {
                                                { Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                            } else null,
                                            modifier = Modifier.height(26.dp)
                                        )
                                    }

                                    // 2. WhatsApp Channel (Personal)
                                    val waChannel = availableChannels.filterIsInstance<CallingChannel.WhatsApp>()
                                        .firstOrNull { !it.isBusiness }
                                    if (waChannel != null) {
                                        val isWaSelected = effectiveChannelId == "whatsapp"
                                        FilterChip(
                                            selected = isWaSelected,
                                            onClick = {
                                                coroutineScope.launch {
                                                    channelPrefRepo.setPreferenceForNumber(pn.number, "whatsapp", pn.label)
                                                }
                                                preferredModes[pn.number] = "whatsapp"
                                                onSaveLearnedCallMode(pn.number, "whatsapp")
                                                Toast.makeText(context, "★ Preferred channel: ${waChannel.shortLabel}", Toast.LENGTH_SHORT).show()
                                            },
                                            label = { Text(waChannel.shortLabel, fontSize = 10.5.sp) },
                                            leadingIcon = if (isWaSelected) {
                                                { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                            } else null,
                                            modifier = Modifier.height(26.dp)
                                        )
                                    }

                                    // 3. WhatsApp Business Channel (if installed)
                                    val waBizChannel = availableChannels.filterIsInstance<CallingChannel.WhatsApp>()
                                        .firstOrNull { it.isBusiness }
                                    if (waBizChannel != null) {
                                        val isWaBizSelected = effectiveChannelId == "whatsapp_business"
                                        FilterChip(
                                            selected = isWaBizSelected,
                                            onClick = {
                                                coroutineScope.launch {
                                                    channelPrefRepo.setPreferenceForNumber(pn.number, "whatsapp_business", pn.label)
                                                }
                                                preferredModes[pn.number] = "whatsapp_business"
                                                onSaveLearnedCallMode(pn.number, "whatsapp_business")
                                                Toast.makeText(context, "★ Preferred channel: ${waBizChannel.shortLabel}", Toast.LENGTH_SHORT).show()
                                            },
                                            label = { Text(waBizChannel.shortLabel, fontSize = 10.5.sp) },
                                            leadingIcon = if (isWaBizSelected) {
                                                { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                            } else null,
                                            modifier = Modifier.height(26.dp)
                                        )
                                    }

                                    // 4. Google Voice Channel (if installed)
                                    val gvChannel = availableChannels.filterIsInstance<CallingChannel.GoogleVoice>()
                                        .firstOrNull()
                                    if (gvChannel != null) {
                                        val isGvSelected = effectiveChannelId == "google_voice"
                                        FilterChip(
                                            selected = isGvSelected,
                                            onClick = {
                                                coroutineScope.launch {
                                                    channelPrefRepo.setPreferenceForNumber(pn.number, "google_voice", pn.label)
                                                }
                                                preferredModes[pn.number] = "google_voice"
                                                onSaveLearnedCallMode(pn.number, "google_voice")
                                                Toast.makeText(context, "★ Preferred channel: ${gvChannel.shortLabel}", Toast.LENGTH_SHORT).show()
                                            },
                                            label = { Text(gvChannel.shortLabel, fontSize = 10.5.sp) },
                                            leadingIcon = if (isGvSelected) {
                                                { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                            } else null,
                                            modifier = Modifier.height(26.dp)
                                        )
                                    }

                                    // 5. Reset / Clear Preference (✕)
                                    val isAskSelected = effectiveChannelId == "ask" || effectiveChannelId == "ask_always" || effectiveChannelId.isBlank()
                                    FilterChip(
                                        selected = isAskSelected,
                                        onClick = {
                                            coroutineScope.launch {
                                                channelPrefRepo.setPreferenceForNumber(pn.number, "ask", pn.label)
                                            }
                                            preferredModes[pn.number] = "ask"
                                            onSaveLearnedCallMode(pn.number, "ask")
                                            Toast.makeText(context, "Preference cleared", Toast.LENGTH_SHORT).show()
                                        },
                                        label = { Text("✕", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        border = null,
                                        modifier = Modifier.height(26.dp)
                                    )
                                }
                            }
                        }

                        if (index < displayedNumbers.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }

                    // Add another phone number button for existing contact
                    if (!isUnknownNumber) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddNumberDialog = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Add another phone number",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            var isCallHistoryExpanded by remember { mutableStateOf(false) }

            // -------------------------------------------------------------
            // CALL HISTORY SECTION (Native Android Dialer Style)
            // -------------------------------------------------------------
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isCallHistoryExpanded = !isCallHistoryExpanded }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "CALL HISTORY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isCallHistoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isCallHistoryExpanded) "Collapse History" else "Expand History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isCallHistoryExpanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                if (isLoadingHistory) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (contactCallHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "No call history found",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Calls with ${contact.name} will be logged here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        contactCallHistory.forEachIndexed { idx, call ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onCallNumber(call.phoneNumber)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Call Type Icon
                                    val (icon, tint, typeLabel) = when (call.callType) {
                                        1 -> Triple(
                                            Icons.AutoMirrored.Filled.CallReceived,
                                            Color(0xFF16A34A),
                                            "Incoming"
                                        )
                                        2 -> Triple(
                                            Icons.AutoMirrored.Filled.CallMade,
                                            Color(0xFF2563EB),
                                            "Outgoing"
                                        )
                                        3 -> Triple(
                                            Icons.AutoMirrored.Filled.CallMissed,
                                            Color(0xFFDC2626),
                                            "Missed"
                                        )
                                        else -> Triple(
                                            Icons.Default.Call,
                                            Color(0xFF4B5563),
                                            "Call"
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = tint.copy(alpha = 0.12f),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = typeLabel,
                                                tint = tint,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = typeLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (call.callType == 3) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (call.durationSeconds > 0) {
                                                val mins = call.durationSeconds / 60
                                                val secs = call.durationSeconds % 60
                                                val durText = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                                                Text(
                                                    text = "• $durText",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        val numberLabel = if (contact.phoneNumbers.size > 1) {
                                            ContactHelper.getDescriptiveNumberLabel(contact, call.phoneNumber)
                                        } else null

                                        Text(
                                            text = "${dateFormat.format(Date(call.timestamp))} • ${if (numberLabel != null) "[$numberLabel] " else ""}${call.phoneNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // SIM Slot or WhatsApp Badge for Contact Call History (SIM shown only on multi-SIM devices)
                                        val isWaCall = call.callReason?.contains("WhatsApp", ignoreCase = true) == true
                                        val currentSims = if (activeSims.isNotEmpty()) activeSims else remember(context) {
                                            com.example.telecom.SimHelper.getActiveSimCards(context)
                                        }
                                        val showBadge = isWaCall || currentSims.size > 1
                                        if (showBadge) {
                                            val slot = if (call.simSlot > 0) call.simSlot else 1
                                            val matchedSim = currentSims.firstOrNull { it.slotIndex + 1 == slot }
                                            val simLabel = if (matchedSim != null && matchedSim.displayName.isNotBlank()) {
                                                matchedSim.displayName.take(12)
                                            } else {
                                                "SIM $slot"
                                            }
                                            val waLabel = availableChannels.firstOrNull { it is CallingChannel.WhatsApp }?.displayName ?: "WhatsApp"
                                            val simColor = if (slot == 2) Color(0xFF16A34A) else Color(0xFF2563EB)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isWaCall) Color(0xFF25D366).copy(alpha = 0.2f) else simColor.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = if (isWaCall) waLabel else simLabel,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isWaCall) Color(0xFF166534) else simColor,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        val dropAttribution = CallDropAttribution.forCall(call.isSpam, call.note, call.ruleMatched, call.callReason)
                                        if (dropAttribution != DropAttribution.NONE) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Security,
                                                        contentDescription = "Spam Blocked",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = dropAttribution.badgeText,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        } else if (!call.note.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Notes,
                                                    contentDescription = "Note",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = call.note,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        onDismiss()
                                        onCallNumber(call.phoneNumber)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (idx < contactCallHistory.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
        }
    }

    // Long-press Action Dialog for a selected phone number (Android Dialer Style)
    if (numberForActionMenu != null) {
        val selectedPn = numberForActionMenu!!
        val activeDef = currentDefaultNumber.ifBlank { contact.phoneNumber }
        val isCurrentDefault = (activeDef.isNotBlank() && (selectedPn.number == activeDef || ContactHelper.isSamePhoneNumber(selectedPn.number, activeDef))) || (contact.phoneNumbers.size <= 1)

        AlertDialog(
            onDismissRequest = { numberForActionMenu = null },
            title = {
                Column {
                    Text(
                        text = selectedPn.number,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${contact.name} • ${selectedPn.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Call Number
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                numberForActionMenu = null
                                onDismiss()
                                onCallNumber(selectedPn.number)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Call, contentDescription = null, tint = Color(0xFF16A34A))
                            Text(text = "Call ${selectedPn.number}", fontWeight = FontWeight.Medium)
                        }
                    }

                    // Set or Clear Default Number
                    if (!isCurrentDefault) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentDefaultNumber = selectedPn.number
                                    onSetAsDefaultNumber(selectedPn.number, selectedPn.label)
                                    Toast.makeText(context, "★ Set as default: ${selectedPn.number}", Toast.LENGTH_SHORT).show()
                                    numberForActionMenu = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B))
                                Column {
                                    Text(text = "Set as default number", fontWeight = FontWeight.Bold)
                                    Text(text = "Use this number as primary default for this contact", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    } else {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentDefaultNumber = ""
                                    onClearDefaultNumber()
                                    Toast.makeText(context, "Default number cleared", Toast.LENGTH_SHORT).show()
                                    numberForActionMenu = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(imageVector = Icons.Outlined.StarBorder, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(text = "Clear default number", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    // Open in Dialer
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                numberForActionMenu = null
                                onDismiss()
                                onSelectInDialer(selectedPn.number)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Dialpad, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(text = "Load in Dialer keypad", fontWeight = FontWeight.Medium)
                        }
                    }

                    // Copy Number
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                clipboardManager.setText(AnnotatedString(selectedPn.number))
                                Toast.makeText(context, "Copied ${selectedPn.number}", Toast.LENGTH_SHORT).show()
                                numberForActionMenu = null
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "Copy number", fontWeight = FontWeight.Medium)
                        }
                    }

                    // WhatsApp Call
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                numberForActionMenu = null
                                ContactHelper.launchWhatsAppCall(context, selectedPn.number, isBusiness = false)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            WhatsAppIcon(modifier = Modifier.size(26.dp))
                            Text(text = "Call on WhatsApp", fontWeight = FontWeight.Medium)
                        }
                    }

                    // WhatsApp Business Call (if available)
                    val hasWaBizChannel = availableChannels.any { it is CallingChannel.WhatsApp && it.isBusiness }
                    if (hasWaBizChannel) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    numberForActionMenu = null
                                    ContactHelper.launchWhatsAppCall(context, selectedPn.number, isBusiness = true)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                WhatsAppIcon(modifier = Modifier.size(26.dp))
                                Text(text = "Call on WhatsApp Business", fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Create Automation Rule
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                numberForActionMenu = null
                                onDismiss()
                                onCreateRule(selectedPn.number)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text(text = "Create automation rule", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { numberForActionMenu = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDialog) {
        EditContactDialog(
            initialName = editName,
            initialNumber = editNumber,
            initialLabel = editLabel,
            initialNickname = editNickname,
            onDismiss = { showEditDialog = false },
            onSave = { name, number, label, nickname ->
                onEditContact(name, number, label, nickname)
                showEditDialog = false
            }
        )
    }

    if (showAddNumberDialog) {
        var newNumberInput by remember { mutableStateOf("") }
        var newNumberLabel by remember { mutableStateOf("Work") }
        val isNewNumberValid = newNumberInput.any { it.isDigit() || it in "+*#" } && newNumberInput.trim().length >= 2
        val presetLabels = listOf("Mobile", "Work", "Home", "Main", "Other")

        AlertDialog(
            onDismissRequest = { showAddNumberDialog = false },
            title = {
                Text(
                    text = "Add Phone Number",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add an additional phone number for ${contact.name}:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newNumberInput,
                        onValueChange = { input ->
                            newNumberInput = input.filter { it.isDigit() || it in "+*#()- .," }
                        },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+1 (555) 000-0000") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Label",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetLabels.forEach { preset ->
                                FilterChip(
                                    selected = newNumberLabel.equals(preset, ignoreCase = true),
                                    onClick = { newNumberLabel = preset },
                                    label = { Text(preset, fontSize = 11.sp, maxLines = 1) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newNumberInput.trim()
                        if (clean.isNotBlank()) {
                            ContactHelper.addPhoneNumberToExistingContact(
                                context = context,
                                contactId = contact.contactId,
                                existingNumber = contact.phoneNumber,
                                newNumber = clean,
                                label = newNumberLabel
                            )
                            Toast.makeText(context, "Added $newNumberLabel number to ${contact.name}", Toast.LENGTH_SHORT).show()
                            showAddNumberDialog = false
                        }
                    },
                    enabled = isNewNumberValid,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Number")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddNumberDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirmationDialog) {
        val isRemovingFromRecents = isUnknownNumber && onDeleteCallLog != null
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (isRemovingFromRecents) "Remove from Recents?" else "Delete Contact?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = if (isRemovingFromRecents) {
                        "Are you sure you want to remove call history for ${contact.name} from Recents?"
                    } else {
                        "Are you sure you want to delete ${contact.name}? ${if (!contact.isAppOnly) "This will remove the contact from your Phone Contacts and this app." else "This will remove the local contact."}"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmationDialog = false
                        if (isRemovingFromRecents) {
                            onDeleteCallLog?.invoke()
                            Toast.makeText(context, "Removed from Recents", Toast.LENGTH_SHORT).show()
                        } else {
                            onDeleteContact?.invoke(contact)
                            Toast.makeText(context, "Deleted ${contact.name}", Toast.LENGTH_SHORT).show()
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isRemovingFromRecents) "Remove" else "Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmationDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateContactDialog) {
        CreateContactDialog(
            initialNumber = contact.phoneNumber,
            initialName = "",
            dialogTitle = "Add to Contacts",
            onDismiss = { showCreateContactDialog = false },
            onSave = { name, number, label, destination, addToFavorites ->
                val saveToDevice = (destination == ContactSaveDestination.PHONE_CONTACTS)
                if (onAddNewContact != null) {
                    onAddNewContact(name, number, label, saveToDevice, addToFavorites)
                } else {
                    if (saveToDevice) {
                        ContactHelper.saveContactToDevice(context, name, number, label)
                    }
                    onEditContact(name, number, label, null)
                    if (addToFavorites) {
                        onToggleFavorite()
                    }
                }
                showCreateContactDialog = false
            }
        )
    }

    if (showNicknameEditDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameEditDialog = false },
            title = { Text("Set Nickname", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter a nickname for ${contact.name}:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = directNicknameText,
                        onValueChange = { directNicknameText = it },
                        label = { Text("Nickname") },
                        placeholder = { Text("e.g. Mom, Boss, Honey") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = directNicknameText.trim()
                        currentDisplayNickname = trimmed
                        editNickname = trimmed
                        onEditContact(contact.name, contact.phoneNumber, contact.label, trimmed.ifBlank { null })
                        ContactHelper.updateContactNickname(context, contact.phoneNumber, trimmed, contact.contactId)
                        showNicknameEditDialog = false
                        Toast.makeText(context, "Nickname updated and synced", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNicknameEditDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (pendingChannelChoiceNumber != null) {
        val pnTarget = pendingChannelChoiceNumber!!
        MultiChannelChoiceDialog(
            phoneNumber = pnTarget.number,
            contactName = contact.name,
            channels = availableChannels,
            initialRememberChoice = (whatsAppCallMode == "ask_learn"),
            showRememberChoice = (whatsAppCallMode != "ask_always"),
            onSelectChannel = { channel, rememberChoice ->
                if (rememberChoice) {
                    coroutineScope.launch {
                        channelPrefRepo.setPreferenceForNumber(pnTarget.number, channel.id, pnTarget.label)
                    }
                    when (channel) {
                        is CallingChannel.CellularSim -> {
                            preferredSims[pnTarget.number] = channel.slotIndex + 1
                            onSetPreferredSimSlot?.invoke(pnTarget.number, channel.slotIndex + 1)
                            preferredModes[pnTarget.number] = "cellular"
                            onSaveLearnedCallMode(pnTarget.number, "cellular")
                        }
                        is CallingChannel.WhatsApp -> {
                            preferredModes[pnTarget.number] = channel.id
                            onSaveLearnedCallMode(pnTarget.number, if (channel.isBusiness) "whatsapp_business" else "whatsapp")
                        }
                        is CallingChannel.GoogleVoice -> {
                            preferredModes[pnTarget.number] = channel.id
                            onSaveLearnedCallMode(pnTarget.number, "google_voice")
                        }
                        else -> {}
                    }
                }
                onDismiss()
                when (channel) {
                    is CallingChannel.WhatsApp -> ContactHelper.launchWhatsAppCall(context, pnTarget.number, isBusiness = channel.isBusiness)
                    is CallingChannel.GoogleVoice -> ContactHelper.launchGoogleVoiceCall(context, pnTarget.number, accountHandle = channel.phoneAccountHandle, onCellularFallback = { onCallNumber(pnTarget.number) })
                    is CallingChannel.CellularSim -> {
                        val slot = channel.slotIndex + 1
                        onSetPreferredSimSlot?.invoke(pnTarget.number, slot)
                        if (onCallNumberDirect != null) {
                            onCallNumberDirect(pnTarget.number, slot)
                        } else {
                            onCallNumber(pnTarget.number)
                        }
                    }
                    else -> {
                        if (onCallNumberDirect != null) {
                            onCallNumberDirect(pnTarget.number, null)
                        } else {
                            onCallNumber(pnTarget.number)
                        }
                    }
                }
                pendingChannelChoiceNumber = null
            },
            onDismiss = { pendingChannelChoiceNumber = null }
        )
    }
}


