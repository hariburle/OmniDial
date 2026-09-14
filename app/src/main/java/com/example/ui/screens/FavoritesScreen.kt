package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import com.example.ui.components.CompactSearchBar
import com.example.ui.components.WhatsAppIcon
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.FavoriteContact
import com.example.data.IgnoredContact
import com.example.data.RecentCall
import com.example.ui.components.AddFavoriteDialog
import com.example.ui.components.ContactSaveDestination
import com.example.ui.components.CreateContactDialog
import com.example.ui.components.ContactDetailsBottomSheet
import com.example.ui.components.ContactPickerDialog
import com.example.ui.components.EditFavoriteDialog
import com.example.ui.components.MultiNumberCallDialog
import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import java.util.Collections

import com.example.ui.models.PopularContactItem
import com.example.ui.models.FavCardDesign
import com.example.ui.components.FavoriteGridCard
import com.example.ui.components.PopularGridCard
import com.example.ui.components.SpeedDialAssignDialog
import com.example.ui.components.EditIgnoredContactDialog

@Composable
fun FavoritesScreen(
    favorites: List<FavoriteContact>,
    recentCalls: List<RecentCall> = emptyList(),
    ignoredContacts: List<IgnoredContact> = emptyList(),
    onSelectNumber: (String) -> Unit,
    onCallNumber: (String) -> Unit,
    onCallWhatsApp: (String) -> Unit = {},
    onCreateRule: (String) -> Unit,
    onDeleteFavorite: (FavoriteContact) -> Unit,
    onAddFavorite: (name: String, number: String, label: String, photoUri: String?, nickname: String?) -> Unit = { _, _, _, _, _ -> },
    onAddNewContact: (name: String, number: String, label: String, destination: ContactSaveDestination, addToFavorites: Boolean) -> Unit = { _, _, _, _, _ -> },
    onAssignSpeedDial: (FavoriteContact, Int) -> Unit,
    onMoveFavorite: (fromIndex: Int, toIndex: Int) -> Unit = { _, _ -> },
    onReorderFavorites: (List<FavoriteContact>) -> Unit = {},
    onEditFavorite: (FavoriteContact, String?) -> Unit = { _, _ -> },
    onUpdateFavoriteNumber: (FavoriteContact, String, String) -> Unit = { _, _, _ -> },
    onIgnoreContact: (phoneNumber: String, name: String, category: String, tag: String) -> Unit = { _, _, _, _ -> },
    onUnignoreContact: (phoneNumber: String) -> Unit = {},
    onUpdateIgnoredContactTag: (phoneNumber: String, newTag: String, newName: String) -> Unit = { _, _, _ -> },
    getPreferredCallingMode: (String) -> String = { "cellular" },
    onSaveLearnedCallMode: (String, String) -> Unit = { _, _ -> },
    learnedCallModes: Map<String, String> = emptyMap(),
    confirmFavoritesCall: Boolean = true,
    favoriteCardStyle: String = "bento",
    onSetFavoriteCardStyle: (String) -> Unit = {},
    isFlipToShhhEnabled: Boolean = true,
    isShhhActive: Boolean = false,
    onToggleFlipToShhh: () -> Unit = {},
    onUpdateContact: (oldNum: String, name: String, number: String, label: String, nickname: String?) -> Unit = { _, _, _, _, _ -> },
    onDeleteContact: (DeviceContact) -> Unit = {},
    deviceContacts: List<DeviceContact> = emptyList(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var speedDialTargetContact by remember { mutableStateOf<FavoriteContact?>(null) }
    var editTargetContact by remember { mutableStateOf<FavoriteContact?>(null) }
    var editTargetIgnored by remember { mutableStateOf<IgnoredContact?>(null) }
    var isConfigureMode by remember { mutableStateOf(false) }
    val cardDesign = FavCardDesign.fromKey(favoriteCardStyle)
    var searchQuery by remember { mutableStateOf("") }
    var localFavorites by remember { mutableStateOf(favorites) }
    var draggingContactId by remember { mutableStateOf<Long?>(null) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }
    var dragTotalOffset by remember { mutableStateOf(Offset.Zero) }
    var dragItemSize by remember { mutableStateOf(IntSize.Zero) }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    BackHandler(enabled = searchQuery.isNotBlank() || isConfigureMode) {
        if (searchQuery.isNotBlank()) {
            searchQuery = ""
        }
        if (isConfigureMode) {
            isConfigureMode = false
        }
    }

    LaunchedEffect(favorites) {
        if (draggingContactId == null) {
            localFavorites = favorites
        }
    }
    var isSearchActive by remember { mutableStateOf(false) }
    var pendingCallConfirmation by remember { mutableStateOf<Triple<String, String, Boolean>?>(null) }
    var nicknameDialogTarget by remember { mutableStateOf<Pair<DeviceContact, String?>?>(null) }
    var nicknameDialogText by remember { mutableStateOf("") }
    val effectiveDeviceContacts = deviceContacts
    var multiNumberContactToCall by remember { mutableStateOf<DeviceContact?>(null) }
    var favoriteContactToCall by remember { mutableStateOf<FavoriteContact?>(null) }
    var contactDetailsTarget by remember { mutableStateOf<Pair<DeviceContact, FavoriteContact?>?>(null) }

    LaunchedEffect(effectiveDeviceContacts) {
        val currentTarget = contactDetailsTarget
        if (currentTarget != null) {
            val (currentContact, currentFav) = currentTarget
            val updated = effectiveDeviceContacts.firstOrNull { c ->
                (currentContact.contactId != null && c.contactId == currentContact.contactId) ||
                (c.name.equals(currentContact.name, ignoreCase = true) && c.phoneNumber == currentContact.phoneNumber)
            }
            if (updated == null) {
                contactDetailsTarget = null
            } else {
                contactDetailsTarget = Pair(updated, currentFav)
            }
        }
    }

    val ignoredNorms = remember(ignoredContacts) {
        ignoredContacts.map { it.phoneNumber.filter { c -> c.isDigit() }.takeLast(10) }.toSet()
    }
    val ignoredNames = remember(ignoredContacts) {
        ignoredContacts.map { it.name.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    }

    val popularContacts = remember(effectiveDeviceContacts, favorites, recentCalls, ignoredContacts) {
        val favNumbers = favorites.map { it.phoneNumber.filter { c -> c.isDigit() }.takeLast(10) }.filter { it.isNotBlank() }.toSet()
        val favNames = favorites.map { it.name.trim().lowercase() }.toSet()

        val excludedNames = setOf("voicemail", "spam", "gate", "intercom", "unknown")

        val callCounts = mutableMapOf<String, Int>()
        recentCalls.forEach { call ->
            val norm = call.phoneNumber.filter { it.isDigit() }.takeLast(10)
            if (norm.isNotBlank()) {
                callCounts[norm] = (callCounts[norm] ?: 0) + 1
            }
        }

        val list = mutableListOf<PopularContactItem>()
        val seenNorms = mutableSetOf<String>()

        fun isIgnored(norm: String, name: String): Boolean {
            val nameLower = name.trim().lowercase()
            return ignoredNorms.contains(norm) || ignoredNames.any { nameLower.contains(it) } || ignoredContacts.any { ic ->
                val icNorm = ic.phoneNumber.filter { c -> c.isDigit() }.takeLast(10)
                icNorm == norm || (ic.tag.isNotBlank() && nameLower.contains(ic.tag.trim().lowercase()))
            }
        }

        // 1. Device contacts that have call counts
        effectiveDeviceContacts.forEach { dc ->
            val norm = dc.phoneNumber.filter { it.isDigit() }.takeLast(10)
            val nameLower = dc.name.trim().lowercase()
            val isExcluded = excludedNames.any { nameLower.contains(it) }
            if (norm.isNotBlank() && !favNumbers.contains(norm) && !favNames.contains(nameLower) && !isExcluded && !isIgnored(norm, dc.name)) {
                val count = callCounts[norm] ?: 0
                if (count > 0 && seenNorms.add(norm)) {
                    list.add(
                        PopularContactItem(
                            name = dc.name,
                            phoneNumber = dc.phoneNumber,
                            label = dc.label,
                            photoUri = dc.photoUri,
                            callCount = count,
                            deviceContact = dc
                        )
                    )
                }
            }
        }

        // 2. Recent calls not in favorites or already added
        recentCalls.forEach { rc ->
            val norm = rc.phoneNumber.filter { it.isDigit() }.takeLast(10)
            val callerNameStr = rc.callerName ?: ""
            val nameLower = callerNameStr.trim().lowercase()
            val isExcluded = excludedNames.any { nameLower.contains(it) }
            if (norm.isNotBlank() && !favNumbers.contains(norm) && !favNames.contains(nameLower) && !isExcluded && !isIgnored(norm, callerNameStr) && seenNorms.add(norm)) {
                val count = callCounts[norm] ?: 1
                list.add(
                    PopularContactItem(
                        name = callerNameStr.ifBlank { rc.phoneNumber },
                        phoneNumber = rc.phoneNumber,
                        label = "Frequent",
                        photoUri = rc.photoUri,
                        callCount = count,
                        deviceContact = null
                    )
                )
            }
        }

        // 3. (Removed random fallback to contacts never called)

        list.sortedByDescending { it.callCount }.take(4)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("favorites_screen")
    ) {
        // Search bar & action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isConfigureMode) {
                Surface(
                    shape = RoundedCornerShape(21.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Reorder Favorites",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Drag grip to rearrange • Arrows for 1-step moves",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            } else {
                CompactSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search by name or number",
                    modifier = Modifier.weight(1f),
                    testTag = "favorites_search_input"
                )
            }

            if (favorites.isNotEmpty()) {
                FilledIconButton(
                    onClick = { isConfigureMode = !isConfigureMode },
                    modifier = Modifier.size(42.dp).testTag("fav_screen_configure_button"),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isConfigureMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isConfigureMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isConfigureMode) Icons.Default.Check else Icons.Default.Tune,
                        contentDescription = if (isConfigureMode) "Done" else "Configure",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            // Precomputed lookups for performance optimization in Favorites list search
            val fastFavoritesLast10DigitsMap = remember(favorites) {
                val map = mutableMapOf<String, FavoriteContact>()
                favorites.forEach { fav ->
                    val digits = fav.phoneNumber.filter { c -> c.isDigit() }.takeLast(10)
                    if (digits.length >= 7) {
                        map[digits] = fav
                    }
                }
                map
            }
            val fastFavoritesNameMap = remember(favorites) {
                favorites.associateBy { it.name.trim().lowercase() }
            }

            // Live Search Results across Contacts & Favorites showing all phone numbers
            val queryClean = searchQuery.trim().lowercase()
            val filteredContacts = remember(searchQuery, deviceContacts, favorites) {
                deviceContacts.filter { dc ->
                    dc.name.lowercase().contains(queryClean) ||
                    (dc.nickname != null && dc.nickname.lowercase().contains(queryClean)) ||
                    ContactHelper.matchesNumberQuery(dc.phoneNumber, searchQuery) ||
                    dc.phoneNumbers.any { ContactHelper.matchesNumberQuery(it.number, searchQuery) }
                }
            }

            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No contacts found matching \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredContacts, key = { "${it.contactId}_${it.phoneNumber}_${it.name}" }) { contact ->
                        val favContactForThis = remember(contact.contactId, contact.phoneNumber, contact.name, contact.phoneNumbers, fastFavoritesLast10DigitsMap, fastFavoritesNameMap) {
                            var match: FavoriteContact? = null
                            for (pn in contact.phoneNumbers) {
                                val pnDigits = pn.number.filter { c -> c.isDigit() }.takeLast(10)
                                if (pnDigits.length >= 7) {
                                    match = fastFavoritesLast10DigitsMap[pnDigits]
                                    if (match != null) break
                                }
                            }
                            if (match == null) {
                                val primaryDigits = contact.phoneNumber.filter { c -> c.isDigit() }.takeLast(10)
                                if (primaryDigits.length >= 7) {
                                    match = fastFavoritesLast10DigitsMap[primaryDigits]
                                }
                            }
                            if (match == null) {
                                match = fastFavoritesNameMap[contact.name.trim().lowercase()]
                            }
                            match
                        }
                        val isFav = favContactForThis != null
                        val effectiveNickname = favContactForThis?.nickname?.ifBlank { null } ?: contact.nickname?.ifBlank { null }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = contact.name.take(1).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = contact.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (isFav) {
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "Favorite",
                                                        tint = Color(0xFFF59E0B),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            if (!effectiveNickname.isNullOrBlank()) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.clickable {
                                                        nicknameDialogTarget = Pair(contact, null)
                                                        nicknameDialogText = effectiveNickname
                                                    }
                                                ) {
                                                    Text(
                                                        text = "Nickname: \"$effectiveNickname\"",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Nickname",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            } else {
                                                Text(
                                                    text = "+ Add Nickname",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.clickable {
                                                        nicknameDialogTarget = Pair(contact, null)
                                                        nicknameDialogText = ""
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(6.dp))

                                // List each phone number with an Add / Set Default button
                                val numbersToShow = if (contact.phoneNumbers.isNotEmpty()) {
                                    contact.phoneNumbers
                                } else {
                                    listOf(com.example.util.ContactPhoneNumber(contact.phoneNumber, contact.label))
                                }

                                numbersToShow.forEach { pn ->
                                    val pnDigits = pn.number.filter { it.isDigit() }.takeLast(10)
                                    val favDigits = favContactForThis?.phoneNumber?.filter { it.isDigit() }?.takeLast(10) ?: ""
                                    val isThisNumberDefault = isFav && (pnDigits.length >= 7 && pnDigits == favDigits)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .padding(vertical = 4.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pn.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = pn.number,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        if (isThisNumberDefault) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFF59E0B),
                                                contentColor = Color.White
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                                    Text("Default", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        } else if (isFav) {
                                            OutlinedButton(
                                                onClick = {
                                                    onUpdateFavoriteNumber(favContactForThis!!, pn.number, pn.label)
                                                },
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.StarBorder, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Set Default", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            }
                                        } else {
                                            Button(
                                                onClick = {
                                                    if (effectiveNickname.isNullOrBlank()) {
                                                        nicknameDialogTarget = Pair(contact, pn.number)
                                                        nicknameDialogText = ""
                                                    } else {
                                                        onAddFavorite(contact.name, pn.number, pn.label, contact.photoUri, effectiveNickname)
                                                    }
                                                },
                                                modifier = Modifier.height(32.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFF59E0B))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Add ★", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEF3C7),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Text(
                        text = "No Favorites Added Yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Star contacts in your Contact Book or tap below to add instant VIP speed dial shortcuts.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = { showContactPicker = true },
                        modifier = Modifier.testTag("empty_add_favorite_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick From Contacts")
                    }
                }
            }
        } else {
            // Dynamically calculate grid columns and card sizing
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("favorites_grid_container")
            ) {
                val availableWidth = maxWidth
                val columnsCount = if (availableWidth >= 600.dp) 3 else 2
                val isCompact = false

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(columnsCount),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize().testTag("favorites_grid")
                ) {
                    if (favorites.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "No Favorites Starred Yet",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Star frequent callers below or tap + to add VIPs",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { showContactPicker = true }) {
                                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add")
                                    }
                                }
                            }
                        }
                    } else {
                        // Direct Grid Items without header line

                        itemsIndexed(localFavorites, key = { _, it -> it.id }) { index, contact ->
                            val preferredMode = remember(contact.phoneNumber, learnedCallModes) {
                                getPreferredCallingMode(contact.phoneNumber)
                            }
                            val isBeingDragged = (draggingContactId == contact.id)

                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .alpha(if (isBeingDragged) 0.15f else 1.0f)
                            ) {
                                FavoriteGridCard(
                                    contact = contact,
                                    cardDesign = cardDesign,
                                    isCompact = isCompact,
                                    isConfigureMode = isConfigureMode,
                                    isDraggingActive = draggingContactId != null,
                                    preferredCallingMode = preferredMode,
                                    canMoveUpRow = index >= columnsCount,
                                    canMoveDownRow = index + columnsCount < localFavorites.size,
                                    canMoveLeftCol = index % columnsCount > 0,
                                    canMoveRightCol = (index % columnsCount < columnsCount - 1) && (index + 1 < localFavorites.size),
                                    onMoveUpRow = {
                                        val next = localFavorites.toMutableList()
                                        Collections.swap(next, index, index - columnsCount)
                                        localFavorites = next
                                        onReorderFavorites(next)
                                    },
                                    onMoveDownRow = {
                                        val next = localFavorites.toMutableList()
                                        Collections.swap(next, index, index + columnsCount)
                                        localFavorites = next
                                        onReorderFavorites(next)
                                    },
                                    onMoveLeftCol = {
                                        val next = localFavorites.toMutableList()
                                        Collections.swap(next, index, index - 1)
                                        localFavorites = next
                                        onReorderFavorites(next)
                                    },
                                    onMoveRightCol = {
                                        val next = localFavorites.toMutableList()
                                        Collections.swap(next, index, index + 1)
                                        localFavorites = next
                                        onReorderFavorites(next)
                                    },
                                    onDragStart = {
                                        val itemInfo = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == contact.id }
                                        if (itemInfo != null) {
                                            draggingContactId = contact.id
                                            dragStartOffset = Offset(itemInfo.offset.x.toFloat(), itemInfo.offset.y.toFloat())
                                            dragTotalOffset = Offset.Zero
                                            dragItemSize = itemInfo.size
                                            try {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    onDragEnd = {
                                        if (draggingContactId != null) {
                                            draggingContactId = null
                                            dragTotalOffset = Offset.Zero
                                            dragItemSize = IntSize.Zero
                                            onReorderFavorites(localFavorites)
                                        }
                                    },
                                    onDragDelta = { delta ->
                                        dragTotalOffset += delta
                                        val currentX = dragStartOffset.x + dragTotalOffset.x
                                        val currentY = dragStartOffset.y + dragTotalOffset.y
                                        val currentCenter = Offset(
                                            currentX + dragItemSize.width / 2f,
                                            currentY + dragItemSize.height / 2f
                                        )

                                        // Auto-scroll when near top or bottom
                                        val viewportHeight = gridState.layoutInfo.viewportSize.height
                                        if (viewportHeight > 0) {
                                            val scrollZone = 100f
                                            if (currentY < scrollZone && gridState.canScrollBackward) {
                                                val speed = -((scrollZone - currentY) / 4f).coerceIn(4f, 25f)
                                                coroutineScope.launch { gridState.scrollBy(speed) }
                                                dragStartOffset -= Offset(0f, speed)
                                            } else if (currentY + dragItemSize.height > viewportHeight - scrollZone && gridState.canScrollForward) {
                                                val diff = (currentY + dragItemSize.height) - (viewportHeight - scrollZone)
                                                val speed = (diff / 4f).coerceIn(4f, 25f)
                                                coroutineScope.launch { gridState.scrollBy(speed) }
                                                dragStartOffset -= Offset(0f, speed)
                                            }
                                        }

                                        // Find if hovered over another favorite item
                                        val targetItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                                            val key = info.key
                                            if (key !is Long || key == draggingContactId) return@firstOrNull false
                                            val left = info.offset.x.toFloat()
                                            val top = info.offset.y.toFloat()
                                            val right = left + info.size.width
                                            val bottom = top + info.size.height
                                            currentCenter.x in left..right && currentCenter.y in top..bottom
                                        }

                                        if (targetItem != null) {
                                            val fromIdx = localFavorites.indexOfFirst { it.id == draggingContactId }
                                            val toIdx = localFavorites.indexOfFirst { it.id == targetItem.key }
                                            if (fromIdx != -1 && toIdx != -1 && fromIdx != toIdx) {
                                                val next = localFavorites.toMutableList()
                                                val item = next.removeAt(fromIdx)
                                                next.add(toIdx, item)
                                                localFavorites = next
                                                try {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                } catch (_: Exception) {}
                                            }
                                        }
                                    },
                                    onCall = {
                                        if (confirmFavoritesCall) {
                                            pendingCallConfirmation = Triple(contact.name, contact.phoneNumber, false)
                                        } else {
                                            onCallNumber(contact.phoneNumber)
                                        }
                                    },
                                    onCallWhatsApp = {
                                        if (confirmFavoritesCall) {
                                            pendingCallConfirmation = Triple(contact.name, contact.phoneNumber, true)
                                        } else {
                                            onCallWhatsApp(contact.phoneNumber)
                                        }
                                    },
                                    onLongClick = {
                                        val normNum = contact.phoneNumber.replace(Regex("[^0-9+]"), "")
                                        val matched = deviceContacts.firstOrNull { dc ->
                                            dc.phoneNumbers.any { it.number.replace(Regex("[^0-9+]"), "") == normNum } ||
                                            dc.phoneNumber.replace(Regex("[^0-9+]"), "") == normNum ||
                                            dc.name.equals(contact.name, ignoreCase = true)
                                        } ?: DeviceContact(
                                            name = contact.name,
                                            phoneNumber = contact.phoneNumber,
                                            label = contact.label,
                                            photoUri = contact.photoUri,
                                            phoneNumbers = listOf(ContactPhoneNumber(contact.phoneNumber, contact.label))
                                        )
                                        contactDetailsTarget = Pair(matched, contact)
                                    },
                                    onSelect = { onSelectNumber(contact.phoneNumber) },
                                    onCreateRule = { onCreateRule(contact.phoneNumber) },
                                    onEdit = { editTargetContact = contact },
                                    onDelete = {
                                        localFavorites = localFavorites.filter { it.id != contact.id }
                                        onDeleteFavorite(contact)
                                    },
                                    onSpeedDialClick = { speedDialTargetContact = contact }
                                )
                            }
                        }
                    }

                    // ---------------------------------------------------------
                    // POPULAR (FREQUENTLY CONTACTED) SECTION
                    // ---------------------------------------------------------
                    if (popularContacts.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = if (favorites.isEmpty()) 6.dp else 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFFEA580C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Popular",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "• Frequently Contacted",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = "${popularContacts.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        items(popularContacts, key = { "pop_${it.phoneNumber}_${it.name}" }) { popItem ->
                            PopularGridCard(
                                item = popItem,
                                isConfigureMode = isConfigureMode,
                                onCall = { onCallNumber(popItem.phoneNumber) },
                                onAddFavorite = {
                                    onAddFavorite(popItem.name, popItem.phoneNumber, popItem.label, popItem.photoUri, null)
                                },
                                onIgnore = {
                                    onIgnoreContact(popItem.phoneNumber, popItem.name, popItem.label, popItem.name)
                                },
                                onClick = {
                                    val dc = popItem.deviceContact ?: DeviceContact(
                                        name = popItem.name,
                                        phoneNumber = popItem.phoneNumber,
                                        label = popItem.label,
                                        photoUri = popItem.photoUri,
                                        phoneNumbers = listOf(ContactPhoneNumber(popItem.phoneNumber, popItem.label))
                                    )
                                    val matchedFav = favorites.firstOrNull { f ->
                                        val fNum = f.phoneNumber.filter { it.isDigit() }.takeLast(10)
                                        val dcNum = dc.phoneNumber.filter { it.isDigit() }.takeLast(10)
                                        (fNum.isNotBlank() && fNum == dcNum) || f.name.equals(dc.name, ignoreCase = true)
                                    }
                                    contactDetailsTarget = Pair(dc, matchedFav)
                                }
                            )
                        }

                        if (isConfigureMode && ignoredContacts.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Ignored Popular Callers (${ignoredContacts.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "View ignored callers, restore them, or fix any wrong taggings.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    for (ignored in ignoredContacts) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = ignored.name.ifBlank { ignored.tag.ifBlank { "Ignored Caller" } },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${ignored.phoneNumber} • Tag: ${ignored.tag.ifBlank { "None" }}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(
                                                        onClick = { editTargetIgnored = ignored },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Fix Tag / Edit",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { onUnignoreContact(ignored.phoneNumber) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Restore",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
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
                }

                // Floating Dragged Card Overlay (Renders ON TOP of all grid items)
                if (draggingContactId != null && dragItemSize.width > 0) {
                    val dragContact = localFavorites.firstOrNull { it.id == draggingContactId }
                    if (dragContact != null) {
                        val density = LocalDensity.current
                        Box(
                            modifier = Modifier
                                .zIndex(9999f)
                                .graphicsLayer {
                                    translationX = dragStartOffset.x + dragTotalOffset.x
                                    translationY = dragStartOffset.y + dragTotalOffset.y
                                    scaleX = 1.05f
                                    scaleY = 1.05f
                                    shadowElevation = 24f
                                }
                                .width(with(density) { dragItemSize.width.toDp() })
                                .height(with(density) { dragItemSize.height.toDp() })
                        ) {
                            FavoriteGridCard(
                                contact = dragContact,
                                cardDesign = cardDesign,
                                isCompact = isCompact,
                                isConfigureMode = isConfigureMode,
                                isFloatingOverlay = true,
                                isDraggingActive = true,
                                preferredCallingMode = remember(dragContact.phoneNumber, learnedCallModes) {
                                    getPreferredCallingMode(dragContact.phoneNumber)
                                },
                                onCall = {},
                                onSelect = {},
                                onCreateRule = {},
                                onDelete = {},
                                onSpeedDialClick = {}
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Favorite dialog
    if (editTargetContact != null) {
        EditFavoriteDialog(
            contact = editTargetContact!!,
            onDismiss = { editTargetContact = null },
            onSave = { updatedContact, newNickname ->
                onEditFavorite(updatedContact, newNickname)
                editTargetContact = null
            }
        )
    }

    // Edit Ignored Contact Dialog
    if (editTargetIgnored != null) {
        EditIgnoredContactDialog(
            ignored = editTargetIgnored!!,
            onDismiss = { editTargetIgnored = null },
            onSave = { phone, newTag, newName ->
                onUpdateIgnoredContactTag(phone, newTag, newName)
                editTargetIgnored = null
            }
        )
    }

    // Add Favorite / New Contact dialog
    if (showAddDialog) {
        CreateContactDialog(
            dialogTitle = "Add to Favorites",
            initialAddToFavorites = true,
            onDismiss = { showAddDialog = false },
            onSave = { name, number, label, destination, addToFavs ->
                onAddNewContact(name, number, label, destination, addToFavs)
                showAddDialog = false
            }
        )
    }

    // Multi-Number Call Confirmation Dialog
    if (multiNumberContactToCall != null) {
        val contact = multiNumberContactToCall!!
        MultiNumberCallDialog(
            contactName = contact.name,
            phoneNumbers = contact.phoneNumbers,
            defaultNumber = favoriteContactToCall?.phoneNumber ?: contact.phoneNumber,
            titlePrefix = "Favorite Contact Numbers",
            onSelectNumberToCall = { chosenNumber ->
                onCallNumber(chosenNumber)
            },
            onSetAsFavoriteNumber = { newNum, newLabel ->
                favoriteContactToCall?.let { fav ->
                    onUpdateFavoriteNumber(fav, newNum, newLabel)
                }
            },
            onSearchOtherContacts = {
                isSearchActive = true
            },
            onDismiss = {
                multiNumberContactToCall = null
                favoriteContactToCall = null
            }
        )
    }

    // Android Phone Dialer Contact Details Card Bottom Sheet
    if (contactDetailsTarget != null) {
        val (matchedContact, _) = contactDetailsTarget!!
        val cleanMatchedDigits = matchedContact.phoneNumber.filter { it.isDigit() }.takeLast(10)
        val favContact = favorites.firstOrNull { fav ->
            val favDigits = fav.phoneNumber.filter { it.isDigit() }.takeLast(10)
            (cleanMatchedDigits.length >= 7 && favDigits == cleanMatchedDigits) ||
            fav.name.equals(matchedContact.name.trim(), ignoreCase = true)
        }
        val isFav = favContact != null

        ContactDetailsBottomSheet(
            contact = matchedContact,
            favoriteContact = favContact,
            isFavorite = isFav,
            onCallNumber = { num ->
                onCallNumber(num)
            },
            onSelectInDialer = { num ->
                onSelectNumber(num)
            },
            onToggleFavorite = {
                if (favContact != null) {
                    onDeleteFavorite(favContact)
                } else {
                    onAddFavorite(matchedContact.name, matchedContact.phoneNumber, matchedContact.label, matchedContact.photoUri, matchedContact.nickname)
                }
            },
            onSetAsDefaultNumber = { newNum, newLabel ->
                if (favContact != null) {
                    onUpdateFavoriteNumber(favContact, newNum, newLabel)
                    contactDetailsTarget = Pair(matchedContact, favContact.copy(phoneNumber = newNum, label = newLabel))
                } else {
                    onAddFavorite(matchedContact.name, newNum, newLabel, matchedContact.photoUri, matchedContact.nickname)
                }
            },
            onClearDefaultNumber = {
                if (favContact != null) {
                    onDeleteFavorite(favContact)
                    contactDetailsTarget = Pair(matchedContact, null)
                }
            },
            onCreateRule = { num ->
                onCreateRule(num)
            },
            getPreferredCallingMode = getPreferredCallingMode,
            onSaveLearnedCallMode = onSaveLearnedCallMode,
            onDeleteContact = { contactToDelete ->
                onDeleteContact(contactToDelete)
                contactDetailsTarget = null
            },
            onEditContact = { name, number, label, nickname ->
                onUpdateContact(matchedContact.phoneNumber, name, number, label, nickname)
                contactDetailsTarget = null
            },
            onDismiss = {
                contactDetailsTarget = null
            }
        )
    }

    // Contact picker dialog (Primary search flow)
    if (showContactPicker) {
        ContactPickerDialog(
            favorites = favorites,
            deviceContacts = deviceContacts,
            title = "Search Contacts to Favorite",
            onContactSelected = { name, number, photoUri ->
                onAddFavorite(name, number, "Mobile", photoUri, null)
                showContactPicker = false
            },
            onDismiss = { showContactPicker = false },
            onManualAddClick = {
                showAddDialog = true
            }
        )
    }

    // Speed Dial Selection & Explanation Dialog
    if (speedDialTargetContact != null) {
        SpeedDialAssignDialog(
            contact = speedDialTargetContact!!,
            allFavorites = favorites,
            onDismiss = { speedDialTargetContact = null },
            onAssign = { slot ->
                onAssignSpeedDial(speedDialTargetContact!!, slot)
                speedDialTargetContact = null
            }
        )
    }

    if (pendingCallConfirmation != null) {
        val (name, number, requestedWhatsApp) = pendingCallConfirmation!!
        val isWhatsApp = requestedWhatsApp || (getPreferredCallingMode(number) == "whatsapp")
        AlertDialog(
            onDismissRequest = { pendingCallConfirmation = null },
            title = { Text(if (isWhatsApp) "Call $name on WhatsApp?" else "Call $name?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Number: $number")
                    Text(
                        text = if (isWhatsApp) "Via WhatsApp Calling" else "Via Cellular Phone Call",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isWhatsApp) Color(0xFF25D366) else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val numToCall = number
                        val callWa = isWhatsApp
                        pendingCallConfirmation = null
                        if (callWa) {
                            onCallWhatsApp(numToCall)
                        } else {
                            onCallNumber(numToCall)
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = if (isWhatsApp) Color(0xFF25D366) else Color(0xFF16A34A)
                    )
                ) {
                    Text(if (isWhatsApp) "WhatsApp Call" else "Call")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCallConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (nicknameDialogTarget != null) {
        val (targetContact, numberToFav) = nicknameDialogTarget!!
        val isAdding = numberToFav != null
        AlertDialog(
            onDismissRequest = { nicknameDialogTarget = null },
            title = { Text(if (isAdding) "Favorite Nickname (Optional)" else "Set Nickname") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isAdding)
                            "Give ${targetContact.name} a nickname, or skip to save without one."
                        else
                            "Enter a nickname for ${targetContact.name}:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = nicknameDialogText,
                        onValueChange = { nicknameDialogText = it },
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
                        val trimmed = nicknameDialogText.trim()
                        val effectiveNick = trimmed.ifBlank { null }
                        if (isAdding) {
                            val matchedPn = targetContact.phoneNumbers.firstOrNull { it.number == numberToFav }
                            val label = matchedPn?.label ?: targetContact.label
                            onAddFavorite(targetContact.name, numberToFav!!, label, targetContact.photoUri, effectiveNick)
                            if (effectiveNick != null) {
                                onUpdateContact(numberToFav, targetContact.name, numberToFav, label, effectiveNick)
                                ContactHelper.updateContactNickname(context, numberToFav, effectiveNick, targetContact.contactId)
                            }
                        } else {
                            val fav = favorites.firstOrNull { f ->
                                f.name.equals(targetContact.name, ignoreCase = true) ||
                                targetContact.phoneNumbers.any { it.number == f.phoneNumber }
                            }
                            if (fav != null) {
                                onEditFavorite(fav, effectiveNick)
                            }
                            onUpdateContact(targetContact.phoneNumber, targetContact.name, targetContact.phoneNumber, targetContact.label, effectiveNick)
                            ContactHelper.updateContactNickname(context, targetContact.phoneNumber, effectiveNick ?: "", targetContact.contactId)
                        }
                        nicknameDialogTarget = null
                    }
                ) {
                    Text(if (isAdding && nicknameDialogText.isBlank()) "Save" else "Save Nickname")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (isAdding) {
                        val matchedPn = targetContact.phoneNumbers.firstOrNull { it.number == numberToFav }
                        val label = matchedPn?.label ?: targetContact.label
                        onAddFavorite(targetContact.name, numberToFav!!, label, targetContact.photoUri, null)
                    }
                    nicknameDialogTarget = null
                }) {
                    Text(if (isAdding) "Skip" else "Cancel")
                }
            }
        )
    }
}
