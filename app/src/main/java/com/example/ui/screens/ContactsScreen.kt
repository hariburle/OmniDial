package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.data.FavoriteContact
import com.example.data.RecentCall
import com.example.ui.components.CompactSearchBar
import com.example.ui.components.ContactDetailsBottomSheet
import com.example.ui.components.ContactSaveDestination
import com.example.ui.components.MultiNumberCallDialog
import com.example.ui.components.WhatsAppIcon
import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.ui.models.ContactSortBy
import com.example.ui.models.ContactSortOrder
import com.example.ui.models.ContactSourceFilter
import com.example.ui.models.SmartContactSort
import com.example.ui.components.ContactRowItem

private fun getContactStats(
    c: DeviceContact,
    lastTsMap: Map<String, Long>,
    countMap: Map<String, Int>
): Pair<Long, Int> {
    var maxTs = 0L
    var totalCount = 0
    val allNumbers = if (c.phoneNumbers.isNotEmpty()) {
        c.phoneNumbers.map { it.number }
    } else {
        listOf(c.phoneNumber)
    }
    for (num in allNumbers) {
        val digits = num.filter { it.isDigit() }.takeLast(10)
        if (digits.isNotBlank()) {
            val ts = lastTsMap[digits] ?: 0L
            if (ts > maxTs) maxTs = ts
            totalCount += countMap[digits] ?: 0
        }
    }
    return Pair(maxTs, totalCount)
}

private fun formatRelativeTime(timestamp: Long): String {
    if (timestamp <= 0L) return "Never"
    val diff = System.currentTimeMillis() - timestamp
    if (diff < 0) return "Just now"
    val minutes = diff / 60_000L
    val hours = diff / 3600_000L
    val days = diff / 86400_000L
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 30 -> "${days}d ago"
        days < 365 -> "${days / 30}mo ago"
        else -> "${days / 365}y ago"
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    favorites: List<FavoriteContact>,
    recentCalls: List<RecentCall> = emptyList(),
    onCallNumber: (String) -> Unit,
    onSelectNumber: (String) -> Unit,
    onToggleFavorite: (name: String, number: String, label: String, photoUri: String?) -> Unit,
    onDeleteFavorite: (FavoriteContact) -> Unit = {},
    onAddFavorite: (name: String, number: String, label: String, photoUri: String?) -> Unit = { _, _, _, _ -> },
    onEditFavorite: (FavoriteContact, String?) -> Unit = { _, _ -> },
    onUpdateFavoriteNumber: (FavoriteContact, String, String) -> Unit = { _, _, _ -> },
    onCreateRule: (String) -> Unit,
    onAddNewContact: (name: String, number: String, label: String, destination: ContactSaveDestination, addToFavorites: Boolean) -> Unit = { _, _, _, _, _ -> },
    onAddNewContactMulti: ((name: String, numbers: List<ContactPhoneNumber>, destination: ContactSaveDestination, addToFavorites: Boolean) -> Unit)? = null,
    onUpdateContact: (oldNumber: String, name: String, number: String, label: String, nickname: String?) -> Unit = { _, _, _, _, _ -> },
    onSetDefaultContactNumber: ((contact: DeviceContact, number: String, label: String) -> Unit)? = null,
    defaultContactNumbers: List<com.example.data.ContactDefaultNumber> = emptyList(),
    onSyncContactToPhone: (DeviceContact) -> Unit = {},
    onSyncAllAppContactsToDevice: () -> Unit = {},
    onDeleteContact: (DeviceContact) -> Unit = {},
    deviceContacts: List<DeviceContact> = emptyList(),
    onRefreshContacts: () -> Unit = {},
    onPlaceWhatsAppCall: (String) -> Unit = {},
    getPreferredCallingMode: (String) -> String = { "cellular" },
    onSaveLearnedCallMode: (String, String) -> Unit = { _, _ -> },
    activeSims: List<com.example.telecom.SimInfo> = emptyList(),
    getPreferredSimSlot: (String) -> Int = { 0 },
    onSetPreferredSimSlot: ((String, Int) -> Unit)? = null,
    globalSimPreferenceMode: String = "system",
    whatsAppCallMode: String = "ask_learn",
    onCallNumberDirect: ((String, Int?) -> Unit)? = null,
    isContactsPermissionGranted: Boolean = true,
    onRequestContactsPermission: () -> Unit = {},
    dismissModalsTrigger: Long = 0L,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    fun getChosenDefaultNumber(c: DeviceContact): String? {
        if (c.contactId != null && c.contactId > 0) {
            val byId = defaultContactNumbers.firstOrNull { it.contactId == c.contactId }
            if (byId != null) return byId.defaultNumber
        }
        for (pn in c.phoneNumbers) {
            val norm = pn.number.replace(Regex("[^0-9+]"), "")
            if (norm.isNotBlank()) {
                val byNorm = defaultContactNumbers.firstOrNull { it.normalizedNumber.replace(Regex("[^0-9+]"), "") == norm }
                if (byNorm != null) return byNorm.defaultNumber
            }
        }
        val normPrimary = c.phoneNumber.replace(Regex("[^0-9+]"), "")
        if (normPrimary.isNotBlank()) {
            val byPrimary = defaultContactNumbers.firstOrNull { it.normalizedNumber.replace(Regex("[^0-9+]"), "") == normPrimary }
            if (byPrimary != null) return byPrimary.defaultNumber
        }
        return null
    }

    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(ContactSortBy.FIRST_NAME) }
    var sortOrder by remember { mutableStateOf(ContactSortOrder.ASCENDING) }
    var sourceFilter by remember { mutableStateOf(ContactSourceFilter.ALL) }
    var smartSortBy by remember { mutableStateOf(SmartContactSort.ALL) }
    var rediscoverShuffleSeed by remember { mutableLongStateOf(0L) }
    var showAddCustomDialog by remember { mutableStateOf(false) }
    var contactForDetailsSheet by remember { mutableStateOf<DeviceContact?>(null) }
    var contactForMultiCall by remember { mutableStateOf<DeviceContact?>(null) }
    var favoriteContactForMultiCall by remember { mutableStateOf<FavoriteContact?>(null) }

    BackHandler(
        enabled = contactForDetailsSheet != null ||
                  contactForMultiCall != null ||
                  favoriteContactForMultiCall != null ||
                  showAddCustomDialog ||
                  searchQuery.isNotBlank() || 
                  sourceFilter != ContactSourceFilter.ALL ||
                  smartSortBy != SmartContactSort.ALL
    ) {
        if (contactForDetailsSheet != null) {
            contactForDetailsSheet = null
        } else if (contactForMultiCall != null) {
            contactForMultiCall = null
        } else if (favoriteContactForMultiCall != null) {
            favoriteContactForMultiCall = null
        } else if (showAddCustomDialog) {
            showAddCustomDialog = false
        } else if (searchQuery.isNotBlank()) {
            searchQuery = ""
        } else if (sourceFilter != ContactSourceFilter.ALL) {
            sourceFilter = ContactSourceFilter.ALL
        } else if (smartSortBy != SmartContactSort.ALL) {
            smartSortBy = SmartContactSort.ALL
        }
    }

    LaunchedEffect(dismissModalsTrigger) {
        if (dismissModalsTrigger > 0L) {
            contactForDetailsSheet = null
            contactForMultiCall = null
            favoriteContactForMultiCall = null
            showAddCustomDialog = false
        }
    }

    // Directly use deviceContacts as the single source of truth, enriched with favorite nicknames and photos
    val effectiveContacts = remember(deviceContacts, favorites) {
        fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)

        val favNickMap = favorites.filter { !it.nickname.isNullOrBlank() }.associate { f ->
            f.phoneNumber.replace(Regex("[^0-9+]"), "") to f.nickname!!.trim()
        }
        val favNameNickMap = favorites.filter { !it.nickname.isNullOrBlank() }.associate { f ->
            f.name.trim().lowercase() to f.nickname!!.trim()
        }
        val favPhotoByNumMap = favorites.filter { !it.photoUri.isNullOrBlank() }.associate { f ->
            normDigits(f.phoneNumber) to f.photoUri!!
        }
        val favPhotoByNameMap = favorites.filter { !it.photoUri.isNullOrBlank() }.associate { f ->
            f.name.trim().lowercase() to f.photoUri!!
        }

        deviceContacts.map { c ->
            val normNum = c.phoneNumber.replace(Regex("[^0-9+]"), "")
            val normTen = normDigits(c.phoneNumber)
            val nickByNum = favNickMap[normNum] ?: c.phoneNumbers.firstNotNullOfOrNull { pn ->
                favNickMap[pn.number.replace(Regex("[^0-9+]"), "")]
            }
            val nickByName = favNameNickMap[c.name.trim().lowercase()]
            val fallbackNick = nickByNum ?: nickByName
            val effectiveNick = if (c.nickname.isNullOrBlank()) fallbackNick else c.nickname

            val photoByNum = favPhotoByNumMap[normTen] ?: c.phoneNumbers.firstNotNullOfOrNull { pn ->
                favPhotoByNumMap[normDigits(pn.number)]
            }
            val photoByName = favPhotoByNameMap[c.name.trim().lowercase()]
            val fallbackPhoto = photoByNum ?: photoByName
            val effectivePhoto = if (c.photoUri.isNullOrBlank()) fallbackPhoto else c.photoUri

            if (effectiveNick != c.nickname || effectivePhoto != c.photoUri) {
                c.copy(nickname = effectiveNick, photoUri = effectivePhoto)
            } else c
        }
    }

    // Live update or close bottom sheet if contact was modified or deleted externally
    LaunchedEffect(deviceContacts) {
        val current = contactForDetailsSheet
        if (current != null) {
            val updated = effectiveContacts.firstOrNull { c ->
                (current.contactId != null && c.contactId == current.contactId) ||
                (c.name.equals(current.name, ignoreCase = true) && c.phoneNumber == current.phoneNumber)
            }
            if (updated == null) {
                contactForDetailsSheet = null
            } else {
                contactForDetailsSheet = updated
            }
        }
    }

    // Filter by source and search query
    val filteredContacts = remember(effectiveContacts, searchQuery, sourceFilter) {
        var list = effectiveContacts

        // Source Filter: All, App Only, or Google / Device
        when (sourceFilter) {
            ContactSourceFilter.ALL -> {}
            ContactSourceFilter.APP_ONLY -> {
                list = list.filter { it.isAppOnly }
            }
            ContactSourceFilter.DEVICE -> {
                list = list.filter { !it.isAppOnly }
            }
        }

        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            val qLower = q.lowercase()
            list = list.filter {
                it.name.lowercase().contains(qLower) ||
                (it.nickname != null && it.nickname.lowercase().contains(qLower)) ||
                ContactHelper.matchesNumberQuery(it.phoneNumber, q) ||
                it.phoneNumbers.any { pn ->
                    ContactHelper.matchesNumberQuery(pn.number, q) ||
                    pn.label.lowercase().contains(qLower)
                }
            }
        }
        list
    }

    // Map phone numbers to recent call stats for smart sorting & discovery
    val contactCallStats = remember(recentCalls, effectiveContacts) {
        val lastTimestampMap = mutableMapOf<String, Long>()
        val callCountMap = mutableMapOf<String, Int>()

        recentCalls.forEach { call ->
            val digits = call.phoneNumber.filter { it.isDigit() }.takeLast(10)
            if (digits.isNotBlank()) {
                val existingTs = lastTimestampMap[digits] ?: 0L
                if (call.timestamp > existingTs) {
                    lastTimestampMap[digits] = call.timestamp
                }
                callCountMap[digits] = (callCountMap[digits] ?: 0) + 1
            }
        }
        Pair(lastTimestampMap, callCountMap)
    }

    // Precomputed lookups for performance optimization in Contacts list
    val fastFavoritesPhoneDigitsSet = remember(favorites) {
        favorites.map { it.phoneNumber.replace(Regex("[^0-9+]"), "") }.filter { it.isNotBlank() }.toSet()
    }
    val fastFavoritesNamesSet = remember(favorites) {
        favorites.map { it.name.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
    }

    // Dynamic contact surfacing based on active filter mode
    val sortedContacts = remember(filteredContacts, favorites, smartSortBy, sortBy, sortOrder, contactCallStats, rediscoverShuffleSeed, fastFavoritesPhoneDigitsSet, fastFavoritesNamesSet) {
        val (lastTsMap, countMap) = contactCallStats

        when (smartSortBy) {
            SmartContactSort.ALL -> {
                val comparator = if (sortBy == ContactSortBy.LAST_NAME) {
                    compareBy<DeviceContact, String>(String.CASE_INSENSITIVE_ORDER) { contact ->
                        val parts = contact.name.trim().split(Regex("\\s+"))
                        if (parts.size > 1) parts.last() else parts.first()
                    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                } else {
                    compareBy(String.CASE_INSENSITIVE_ORDER) { contact ->
                        contact.name.trim()
                    }
                }
                val ordered = filteredContacts.sortedWith(comparator)
                if (sortOrder == ContactSortOrder.ASCENDING) ordered else ordered.reversed()
            }
            SmartContactSort.FAVORITES -> {
                filteredContacts.filter { c ->
                    val contactNameLower = c.name.trim().lowercase()
                    val isNameFav = fastFavoritesNamesSet.contains(contactNameLower)
                    val isNickFav = !c.nickname.isNullOrBlank() && fastFavoritesNamesSet.contains(c.nickname.trim().lowercase())
                    val normContact = c.phoneNumber.replace(Regex("[^0-9+]"), "")
                    val isPhoneFav = normContact.isNotBlank() && fastFavoritesPhoneDigitsSet.contains(normContact)
                    val isAnyPnFav = c.phoneNumbers.any { pn ->
                        val normPn = pn.number.replace(Regex("[^0-9+]"), "")
                        normPn.isNotBlank() && fastFavoritesPhoneDigitsSet.contains(normPn)
                    }
                    isNameFav || isNickFav || isPhoneFav || isAnyPnFav
                }
            }
            SmartContactSort.NICKNAMES -> {
                val comparator = compareBy<DeviceContact, String>(String.CASE_INSENSITIVE_ORDER) {
                    it.nickname?.trim() ?: it.name.trim()
                }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                val nickList = filteredContacts.filter { !it.nickname.isNullOrBlank() }.sortedWith(comparator)
                if (sortOrder == ContactSortOrder.ASCENDING) nickList else nickList.reversed()
            }
            SmartContactSort.RECENT -> {
                filteredContacts
                    .map { c -> c to getContactStats(c, lastTsMap, countMap) }
                    .filter { it.second.first > 0L }
                    .sortedByDescending { it.second.first }
                    .map { it.first }
            }
            SmartContactSort.FREQUENT -> {
                filteredContacts
                    .map { c -> c to getContactStats(c, lastTsMap, countMap) }
                    .filter { it.second.second > 0 }
                    .sortedByDescending { it.second.second }
                    .map { it.first }
            }
            SmartContactSort.REDISCOVER -> {
                // Combines Long Time No Talk (inactive contacts with prior calls) and Dormant contacts (0 calls)
                val inactiveList = filteredContacts
                    .map { c -> c to getContactStats(c, lastTsMap, countMap) }
                    .filter { it.second.first > 0L }
                    .sortedBy { it.second.first }
                    .map { it.first }

                val dormantList = filteredContacts
                    .map { c -> c to getContactStats(c, lastTsMap, countMap) }
                    .filter { it.second.second == 0 }
                    .map { it.first }

                val combined = inactiveList + dormantList
                if (rediscoverShuffleSeed == 0L) {
                    combined.shuffled(java.util.Random(42L))
                } else {
                    combined.shuffled(java.util.Random(rediscoverShuffleSeed))
                }
            }
        }
    }

    // All contacts matching the search query across the entire directory (unfiltered by smartSortBy or sourceFilter)
    val allSearchQueryMatches = remember(effectiveContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val q = searchQuery.trim()
            val qLower = q.lowercase()
            effectiveContacts.filter {
                it.name.lowercase().contains(qLower) ||
                (it.nickname != null && it.nickname.lowercase().contains(qLower)) ||
                ContactHelper.matchesNumberQuery(it.phoneNumber, q) ||
                it.phoneNumbers.any { pn ->
                    ContactHelper.matchesNumberQuery(pn.number, q) ||
                    pn.label.lowercase().contains(qLower)
                }
            }
        }
    }

    // Contacts that matched the search query but were filtered out by the active smartSortBy or sourceFilter
    val otherFilteredOutMatches = remember(allSearchQueryMatches, sortedContacts, smartSortBy, sourceFilter, searchQuery) {
        if (searchQuery.isBlank() || (smartSortBy == SmartContactSort.ALL && sourceFilter == ContactSourceFilter.ALL)) {
            emptyList()
        } else {
            val sortedKeys = sortedContacts.map { it.contactId?.toString() ?: (it.name + "_" + it.phoneNumber) }.toSet()
            allSearchQueryMatches
                .filter { c ->
                    val key = c.contactId?.toString() ?: (c.name + "_" + c.phoneNumber)
                    !sortedKeys.contains(key)
                }
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name.trim() })
        }
    }

    // Group contacts alphabetically by initial (strictly for All / A-Z Directory mode)
    val groupedContacts = remember(sortedContacts, smartSortBy, sortBy, sortOrder) {
        if (smartSortBy != SmartContactSort.ALL) {
            emptyMap<Char, List<DeviceContact>>()
        } else {
            sortedContacts.groupBy { contact ->
                val nameToUse = if (sortBy == ContactSortBy.LAST_NAME) {
                    val parts = contact.name.trim().split(Regex("\\s+"))
                    if (parts.size > 1) parts.last() else parts.first()
                } else {
                    contact.name.trim()
                }
                val firstChar = nameToUse.trim().firstOrNull()?.uppercaseChar() ?: '#'
                if (firstChar in 'A'..'Z') firstChar else '#'
            }.let { groups ->
                if (sortOrder == ContactSortOrder.ASCENDING) {
                    groups.toSortedMap(compareBy { if (it == '#') "ZZZ" else it.toString() })
                } else {
                    groups.toSortedMap(compareByDescending { if (it == '#') "" else it.toString() })
                }
            }
        }
    }

    val alphabet = remember(sortOrder) {
        if (sortOrder == ContactSortOrder.ASCENDING) {
            ('A'..'Z').toList() + listOf('#')
        } else {
            listOf('#') + ('Z' downTo 'A').toList()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("contacts_screen")
    ) {
        // Smart Contact Discovery & Filter Row (Equally distributed, elegant wide rounded-rectangular buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmartContactSort.values().forEach { sortMode ->
                val isSelected = (smartSortBy == sortMode)
                val icon = when (sortMode) {
                    SmartContactSort.ALL -> Icons.Default.People
                    SmartContactSort.FAVORITES -> Icons.Default.Star
                    SmartContactSort.NICKNAMES -> Icons.Default.Face
                    SmartContactSort.RECENT -> Icons.Default.History
                    SmartContactSort.FREQUENT -> Icons.Default.LocalFireDepartment
                    SmartContactSort.REDISCOVER -> Icons.Default.Casino
                }
                val accentColor = when (sortMode) {
                    SmartContactSort.ALL -> MaterialTheme.colorScheme.primary
                    SmartContactSort.FAVORITES -> Color(0xFFF59E0B)
                    SmartContactSort.NICKNAMES -> Color(0xFFEC4899)
                    SmartContactSort.RECENT -> Color(0xFF10B981)
                    SmartContactSort.FREQUENT -> Color(0xFFEF4444)
                    SmartContactSort.REDISCOVER -> Color(0xFF8B5CF6)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                ) {
                    @OptIn(ExperimentalMaterial3Api::class)
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = {
                            PlainTooltip {
                                Text(sortMode.description)
                            }
                        },
                        state = rememberTooltipState(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Surface(
                            onClick = {
                                if (sortMode == SmartContactSort.REDISCOVER && isSelected) {
                                    rediscoverShuffleSeed = System.currentTimeMillis()
                                    Toast.makeText(context, "Reshuffled Rediscover list", Toast.LENGTH_SHORT).show()
                                } else {
                                    smartSortBy = sortMode
                                }
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
                                .fillMaxSize()
                                .testTag("contact_filter_${sortMode.name.lowercase()}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = sortMode.description,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search bar & Add button (Placed below filter row for search context clarity)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CompactSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search in ${smartSortBy.label}...",
                modifier = Modifier.weight(1f),
                testTag = "contacts_search_input"
            )

            FilledIconButton(
                onClick = { showAddCustomDialog = true },
                modifier = Modifier
                    .size(42.dp)
                    .testTag("add_contact_button"),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "New Contact",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        val appOnlyCount = effectiveContacts.count { it.isAppOnly }
        val deviceCount = effectiveContacts.count { !it.isAppOnly }

        // Source Filter Chips: All, App Only, Phone Contacts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = sourceFilter == ContactSourceFilter.ALL,
                onClick = { sourceFilter = ContactSourceFilter.ALL },
                label = { Text("All (${effectiveContacts.size})", fontSize = 12.sp) }
            )
            FilterChip(
                selected = sourceFilter == ContactSourceFilter.APP_ONLY,
                onClick = { sourceFilter = ContactSourceFilter.APP_ONLY },
                label = { Text("App Only ($appOnlyCount)", fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            )
            FilterChip(
                selected = sourceFilter == ContactSourceFilter.DEVICE,
                onClick = { sourceFilter = ContactSourceFilter.DEVICE },
                label = { Text("Phone Contacts ($deviceCount)", fontSize = 12.sp) }
            )
        }

        // Just-in-Time Contacts Permission Banner
        if (!isContactsPermissionGranted) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Contacts Access Disabled",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Grant permission to view, search, and manage phone contacts.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRequestContactsPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (sourceFilter == ContactSourceFilter.APP_ONLY && appOnlyCount > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sync All App Contacts",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Move $appOnlyCount contact${if (appOnlyCount > 1) "s" else ""} to phone's default contacts app",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Button(
                        onClick = {
                            onSyncAllAppContactsToDevice()
                            Toast.makeText(context, "Synced $appOnlyCount contact${if (appOnlyCount > 1) "s" else ""} to Phone Contacts!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Sync All",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Sync All", fontSize = 12.sp)
                    }
                }
            }
        }

        // Sort / Display Description Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = when {
                    searchQuery.isNotBlank() && otherFilteredOutMatches.isNotEmpty() -> {
                        val base = when (smartSortBy) {
                            SmartContactSort.ALL -> "All (${sortedContacts.size})"
                            SmartContactSort.FAVORITES -> "Favorites (${sortedContacts.size})"
                            SmartContactSort.NICKNAMES -> "Nicknames (${sortedContacts.size})"
                            SmartContactSort.RECENT -> "Recents (${sortedContacts.size})"
                            SmartContactSort.FREQUENT -> "Frequent (${sortedContacts.size})"
                            SmartContactSort.REDISCOVER -> "Rediscover (${sortedContacts.size})"
                        }
                        "$base • +${otherFilteredOutMatches.size} other"
                    }
                    smartSortBy == SmartContactSort.ALL -> "Showing All (${sortedContacts.size})"
                    smartSortBy == SmartContactSort.FAVORITES -> "Showing Favorites (${sortedContacts.size})"
                    smartSortBy == SmartContactSort.NICKNAMES -> "Showing Nicknames (${sortedContacts.size})"
                    smartSortBy == SmartContactSort.RECENT -> "Showing Recents (${sortedContacts.size})"
                    smartSortBy == SmartContactSort.FREQUENT -> "Showing Frequent (${sortedContacts.size})"
                    smartSortBy == SmartContactSort.REDISCOVER -> "Showing Rediscover (${sortedContacts.size})"
                    else -> "Showing (${sortedContacts.size})"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (smartSortBy == SmartContactSort.ALL) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                sortBy = if (sortBy == ContactSortBy.FIRST_NAME) ContactSortBy.LAST_NAME else ContactSortBy.FIRST_NAME
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (sortBy == ContactSortBy.FIRST_NAME) "First" else "Last",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                sortOrder = if (sortOrder == ContactSortOrder.ASCENDING) ContactSortOrder.DESCENDING else ContactSortOrder.ASCENDING
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (sortOrder == ContactSortOrder.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            } else if (smartSortBy == SmartContactSort.REDISCOVER) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            rediscoverShuffleSeed = System.currentTimeMillis()
                            Toast.makeText(context, "Reshuffled Rediscover list", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Reshuffle",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Shuffle",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // Contact List with Clean Group Headers and Vertical Right-Side A-Z Strip
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (sortedContacts.isEmpty() && otherFilteredOutMatches.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                !isContactsPermissionGranted && effectiveContacts.isEmpty() -> Icons.Default.Contacts
                                smartSortBy == SmartContactSort.FAVORITES -> Icons.Default.Star
                                smartSortBy == SmartContactSort.NICKNAMES -> Icons.Default.Face
                                smartSortBy == SmartContactSort.RECENT -> Icons.Default.History
                                smartSortBy == SmartContactSort.FREQUENT -> Icons.Default.LocalFireDepartment
                                smartSortBy == SmartContactSort.REDISCOVER -> Icons.Default.Casino
                                else -> Icons.Default.Person
                            },
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = if (!isContactsPermissionGranted && effectiveContacts.isEmpty()) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                        val emptyTitle = when {
                            !isContactsPermissionGranted && effectiveContacts.isEmpty() -> "Contacts Permission Required"
                            searchQuery.isNotBlank() -> "No contacts match '$searchQuery'"
                            smartSortBy == SmartContactSort.FAVORITES -> "No favorite contacts added yet"
                            smartSortBy == SmartContactSort.NICKNAMES -> "No contacts with nicknames found"
                            smartSortBy == SmartContactSort.RECENT -> "No recent call activity found"
                            smartSortBy == SmartContactSort.FREQUENT -> "No call frequency history found"
                            smartSortBy == SmartContactSort.REDISCOVER -> "No dormant or long-unspoken contacts found"
                            effectiveContacts.isEmpty() -> "No contacts found on device"
                            else -> "No contacts found"
                        }
                        Text(
                            text = emptyTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (!isContactsPermissionGranted && effectiveContacts.isEmpty()) {
                            Text(
                                text = "Allow OmniDial to access your contacts to view your address book and set channel preferences.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = onRequestContactsPermission,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("Grant Contacts Access", fontWeight = FontWeight.Bold)
                            }
                        } else if (smartSortBy != SmartContactSort.ALL) {
                            OutlinedButton(
                                onClick = { smartSortBy = SmartContactSort.ALL },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("View All Contacts", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                    val showAlphabetStrip = searchQuery.isBlank() && groupedContacts.isNotEmpty() && smartSortBy == SmartContactSort.ALL
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = if (showAlphabetStrip) 28.dp else 0.dp)
                            .testTag("contacts_list")
                    ) {
                    if (sortedContacts.isEmpty()) {
                        item(key = "no_filter_matches_banner") {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val activeFilterLabel = when (smartSortBy) {
                                        SmartContactSort.FAVORITES -> "Favorites"
                                        SmartContactSort.NICKNAMES -> "Nicknames"
                                        SmartContactSort.RECENT -> "Recents"
                                        SmartContactSort.FREQUENT -> "Frequent"
                                        SmartContactSort.REDISCOVER -> "Rediscover"
                                        else -> if (sourceFilter == ContactSourceFilter.APP_ONLY) "App Only" else "Phone Contacts"
                                    }
                                    Text(
                                        text = "No $activeFilterLabel matched \"$searchQuery\"",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Showing ${otherFilteredOutMatches.size} other matching contact${if (otherFilteredOutMatches.size > 1) "s" else ""} from your contacts list below:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }

                    if (smartSortBy == SmartContactSort.ALL) {
                        groupedContacts.forEach { (initial, contactsInGroup) ->
                            stickyHeader(key = "header_$initial") {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = initial.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            items(contactsInGroup, key = { it.contactId?.toString() ?: (it.name + "_" + it.phoneNumber) }) { contact ->
                                val isFav = remember(contact.contactId, contact.phoneNumber, contact.name, contact.nickname, contact.phoneNumbers, fastFavoritesPhoneDigitsSet, fastFavoritesNamesSet) {
                                    val contactNameLower = contact.name.trim().lowercase()
                                    if (fastFavoritesNamesSet.contains(contactNameLower)) {
                                        return@remember true
                                    }
                                    if (!contact.nickname.isNullOrBlank() && fastFavoritesNamesSet.contains(contact.nickname.trim().lowercase())) {
                                        return@remember true
                                    }
                                    val normContact = contact.phoneNumber.replace(Regex("[^0-9+]"), "")
                                    if (normContact.isNotBlank() && fastFavoritesPhoneDigitsSet.contains(normContact)) {
                                        return@remember true
                                    }
                                    contact.phoneNumbers.any { pn ->
                                        val normPn = pn.number.replace(Regex("[^0-9+]"), "")
                                        normPn.isNotBlank() && fastFavoritesPhoneDigitsSet.contains(normPn)
                                    }
                                }

                                ContactRowItem(
                                    contact = contact,
                                    searchQuery = searchQuery,
                                    isFavorite = isFav,
                                    onItemClick = { contactForDetailsSheet = contact },
                                    onRequestCall = {
                                        val matchedFav = favorites.firstOrNull { f ->
                                            val normFav = f.phoneNumber.replace(Regex("[^0-9+]"), "")
                                            contact.phoneNumbers.any { pn ->
                                                val normPn = pn.number.replace(Regex("[^0-9+]"), "")
                                                normPn.isNotBlank() && normPn == normFav
                                            } || (f.name.isNotBlank() && f.name.equals(contact.name.trim(), ignoreCase = true))
                                        }
                                        if (contact.phoneNumbers.size > 1) {
                                            contactForMultiCall = contact
                                            favoriteContactForMultiCall = matchedFav
                                        } else {
                                            onCallNumber(contact.phoneNumber)
                                        }
                                    },
                                    onCallDirect = onCallNumber,
                                    onSelectNumber = onSelectNumber,
                                    onSmsClick = { num ->
                                        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$num"))
                                        context.startActivity(smsIntent)
                                    },
                                    onCreateRule = { num -> onCreateRule(num) },
                                    onToggleFavorite = {
                                        val defNum = getChosenDefaultNumber(contact) ?: contact.phoneNumber
                                        val defLabel = contact.phoneNumbers.firstOrNull { it.number == defNum }?.label ?: contact.label
                                        onToggleFavorite(contact.name, defNum, defLabel, contact.photoUri)
                                    },
                                    onPlaceWhatsAppCall = onPlaceWhatsAppCall,
                                    onSyncToPhone = {
                                        onSyncContactToPhone(contact)
                                    },
                                    getPreferredCallingMode = getPreferredCallingMode
                                )
                            }
                        }
                    } else {
                        val (lastTsMap, countMap) = contactCallStats
                        items(sortedContacts, key = { it.contactId?.toString() ?: (it.name + "_" + it.phoneNumber) }) { contact ->
                            val isFav = remember(contact.contactId, contact.phoneNumber, contact.name, contact.nickname, contact.phoneNumbers, fastFavoritesPhoneDigitsSet, fastFavoritesNamesSet) {
                                val contactNameLower = contact.name.trim().lowercase()
                                if (fastFavoritesNamesSet.contains(contactNameLower)) {
                                    return@remember true
                                }
                                if (!contact.nickname.isNullOrBlank() && fastFavoritesNamesSet.contains(contact.nickname.trim().lowercase())) {
                                    return@remember true
                                }
                                val normContact = contact.phoneNumber.replace(Regex("[^0-9+]"), "")
                                if (normContact.isNotBlank() && fastFavoritesPhoneDigitsSet.contains(normContact)) {
                                    return@remember true
                                }
                                contact.phoneNumbers.any { pn ->
                                    val normPn = pn.number.replace(Regex("[^0-9+]"), "")
                                    normPn.isNotBlank() && fastFavoritesPhoneDigitsSet.contains(normPn)
                                }
                            }

                            val (lastTs, count) = getContactStats(contact, lastTsMap, countMap)
                            val badge = when (smartSortBy) {
                                SmartContactSort.FAVORITES -> null
                                SmartContactSort.NICKNAMES -> null
                                SmartContactSort.RECENT -> if (lastTs > 0L) formatRelativeTime(lastTs) else null
                                SmartContactSort.FREQUENT -> if (count > 0) "$count call${if (count > 1) "s" else ""}" else null
                                SmartContactSort.REDISCOVER -> if (lastTs > 0L) formatRelativeTime(lastTs) else "Dormant"
                                else -> null
                            }

                            ContactRowItem(
                                contact = contact,
                                searchQuery = searchQuery,
                                isFavorite = isFav,
                                discoveryBadge = badge,
                                onItemClick = { contactForDetailsSheet = contact },
                                onRequestCall = {
                                    val matchedFav = favorites.firstOrNull { f ->
                                        val normFav = f.phoneNumber.replace(Regex("[^0-9+]"), "")
                                        contact.phoneNumbers.any { pn ->
                                            val normPn = pn.number.replace(Regex("[^0-9+]"), "")
                                            normPn.isNotBlank() && normPn == normFav
                                        } || (f.name.isNotBlank() && f.name.equals(contact.name.trim(), ignoreCase = true))
                                    }
                                    if (contact.phoneNumbers.size > 1) {
                                        contactForMultiCall = contact
                                        favoriteContactForMultiCall = matchedFav
                                    } else {
                                        onCallNumber(contact.phoneNumber)
                                    }
                                },
                                onCallDirect = onCallNumber,
                                onSelectNumber = onSelectNumber,
                                onSmsClick = { num ->
                                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$num"))
                                    context.startActivity(smsIntent)
                                },
                                onCreateRule = { num -> onCreateRule(num) },
                                onToggleFavorite = {
                                    val defNum = getChosenDefaultNumber(contact) ?: contact.phoneNumber
                                    val defLabel = contact.phoneNumbers.firstOrNull { it.number == defNum }?.label ?: contact.label
                                    onToggleFavorite(contact.name, defNum, defLabel, contact.photoUri)
                                },
                                onPlaceWhatsAppCall = onPlaceWhatsAppCall,
                                onSyncToPhone = {
                                    onSyncContactToPhone(contact)
                                },
                                getPreferredCallingMode = getPreferredCallingMode
                            )
                        }
                    }

                    if (otherFilteredOutMatches.isNotEmpty()) {
                        item(key = "header_other_matches") {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (sortedContacts.isNotEmpty()) {
                                            "Other Matches Outside Filter (${otherFilteredOutMatches.size})"
                                        } else {
                                            "Contacts Outside Filter (${otherFilteredOutMatches.size})"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        items(otherFilteredOutMatches, key = { "other_" + (it.contactId?.toString() ?: (it.name + "_" + it.phoneNumber)) }) { contact ->
                            val isFav = remember(contact.contactId, contact.phoneNumber, contact.name, contact.nickname, contact.phoneNumbers, fastFavoritesPhoneDigitsSet, fastFavoritesNamesSet) {
                                val contactNameLower = contact.name.trim().lowercase()
                                if (fastFavoritesNamesSet.contains(contactNameLower)) return@remember true
                                if (!contact.nickname.isNullOrBlank() && fastFavoritesNamesSet.contains(contact.nickname.trim().lowercase())) return@remember true
                                val normContact = contact.phoneNumber.replace(Regex("[^0-9+]"), "")
                                if (normContact.isNotBlank() && fastFavoritesPhoneDigitsSet.contains(normContact)) return@remember true
                                contact.phoneNumbers.any { pn ->
                                    val normPn = pn.number.replace(Regex("[^0-9+]"), "")
                                    normPn.isNotBlank() && fastFavoritesPhoneDigitsSet.contains(normPn)
                                }
                            }

                            ContactRowItem(
                                contact = contact,
                                searchQuery = searchQuery,
                                isFavorite = isFav,
                                discoveryBadge = null,
                                onItemClick = { contactForDetailsSheet = contact },
                                onRequestCall = {
                                    val matchedFav = favorites.firstOrNull { f ->
                                        val normFav = f.phoneNumber.replace(Regex("[^0-9+]"), "")
                                        contact.phoneNumbers.any { pn ->
                                            val normPn = pn.number.replace(Regex("[^0-9+]"), "")
                                            normPn.isNotBlank() && normPn == normFav
                                        } || (f.name.isNotBlank() && f.name.equals(contact.name.trim(), ignoreCase = true))
                                    }
                                    if (contact.phoneNumbers.size > 1) {
                                        contactForMultiCall = contact
                                        favoriteContactForMultiCall = matchedFav
                                    } else {
                                        onCallNumber(contact.phoneNumber)
                                    }
                                },
                                onCallDirect = onCallNumber,
                                onSelectNumber = onSelectNumber,
                                onSmsClick = { num ->
                                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$num"))
                                    context.startActivity(smsIntent)
                                },
                                onCreateRule = { num -> onCreateRule(num) },
                                onToggleFavorite = {
                                    val defNum = getChosenDefaultNumber(contact) ?: contact.phoneNumber
                                    val defLabel = contact.phoneNumbers.firstOrNull { it.number == defNum }?.label ?: contact.label
                                    onToggleFavorite(contact.name, defNum, defLabel, contact.photoUri)
                                },
                                onPlaceWhatsAppCall = onPlaceWhatsAppCall,
                                onSyncToPhone = {
                                    onSyncContactToPhone(contact)
                                },
                                getPreferredCallingMode = getPreferredCallingMode
                            )
                        }
                    }
                }
            }

            // Vertical A-Z Strip on Right Side
            if (searchQuery.isBlank() && groupedContacts.isNotEmpty() && smartSortBy == SmartContactSort.ALL) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .width(26.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        alphabet.forEach { char ->
                            val hasItems = groupedContacts.containsKey(char)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = hasItems) {
                                        scope.launch {
                                            var index = 0
                                            for ((key, items) in groupedContacts) {
                                                if (key == char) {
                                                    listState.animateScrollToItem(index)
                                                    break
                                                }
                                                index += items.size + 1
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = if (hasItems) FontWeight.Bold else FontWeight.Normal,
                                    color = if (hasItems) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Multi-Number Quick Call & Default Selection Dialog
    if (contactForMultiCall != null) {
        val currentContact = contactForMultiCall!!
        val chosenDef = getChosenDefaultNumber(currentContact) ?: favoriteContactForMultiCall?.phoneNumber ?: currentContact.phoneNumber
        MultiNumberCallDialog(
            contactName = currentContact.name,
            phoneNumbers = currentContact.phoneNumbers,
            defaultNumber = chosenDef,
            titlePrefix = if (favoriteContactForMultiCall != null) "Favorite Contact Numbers" else "Select Number to Call",
            onSelectNumberToCall = { chosenNumber ->
                onCallNumber(chosenNumber)
                contactForMultiCall = null
                favoriteContactForMultiCall = null
            },
            onSetDefaultNumber = { newNum, newLabel ->
                if (onSetDefaultContactNumber != null) {
                    onSetDefaultContactNumber(currentContact, newNum, newLabel)
                }
            },
            onDismiss = {
                contactForMultiCall = null
                favoriteContactForMultiCall = null
            }
        )
    }

    // Contact Details Bottom Sheet
    if (contactForDetailsSheet != null) {
        val detailContact = contactForDetailsSheet!!
        val matchedFav = favorites.firstOrNull { fav ->
            val favDigits = fav.phoneNumber.filter { it.isDigit() }.takeLast(10)
            val phoneMatch = if (favDigits.length >= 7) {
                detailContact.phoneNumbers.any { it.number.filter { c -> c.isDigit() }.takeLast(10) == favDigits } ||
                detailContact.phoneNumber.filter { c -> c.isDigit() }.takeLast(10) == favDigits
            } else false
            phoneMatch ||
            fav.name.equals(detailContact.name.trim(), ignoreCase = true) ||
            (!detailContact.nickname.isNullOrBlank() && fav.name.equals(detailContact.nickname!!.trim(), ignoreCase = true))
        }
        val isFav = matchedFav != null

        ContactDetailsBottomSheet(
            contact = detailContact,
            favoriteContact = matchedFav,
            isFavorite = isFav,
            whatsAppCallMode = whatsAppCallMode,
            onCallNumberDirect = { num, slot ->
                if (onCallNumberDirect != null) {
                    onCallNumberDirect(num, slot)
                } else {
                    onCallNumber(num)
                }
                contactForDetailsSheet = null
            },
            onCallNumber = { num ->
                onCallNumber(num)
                contactForDetailsSheet = null
            },
            onSelectInDialer = { num ->
                onSelectNumber(num)
                contactForDetailsSheet = null
            },
            onToggleFavorite = {
                if (matchedFav != null) {
                    onDeleteFavorite(matchedFav)
                } else {
                    val defNum = detailContact.phoneNumber.ifBlank { detailContact.phoneNumbers.firstOrNull()?.number ?: "" }
                    val defLabel = detailContact.label.ifBlank { detailContact.phoneNumbers.firstOrNull()?.label ?: "Mobile" }
                    onAddFavorite(detailContact.name, defNum, defLabel, detailContact.photoUri)
                }
            },
            onSetAsDefaultNumber = { num, label ->
                if (onSetDefaultContactNumber != null) {
                    onSetDefaultContactNumber(detailContact, num, label)
                } else {
                    if (matchedFav != null) {
                        onUpdateFavoriteNumber(matchedFav, num, label)
                    }
                    onUpdateContact(detailContact.phoneNumber, detailContact.name, num, label, detailContact.nickname)
                }
                contactForDetailsSheet = detailContact.copy(phoneNumber = num, label = label)
            },
            onClearDefaultNumber = {
                val firstNum = detailContact.phoneNumbers.firstOrNull()?.number ?: detailContact.phoneNumber
                val firstLabel = detailContact.phoneNumbers.firstOrNull()?.label ?: detailContact.label
                if (onSetDefaultContactNumber != null) {
                    onSetDefaultContactNumber(detailContact, firstNum, firstLabel)
                } else {
                    onUpdateContact(detailContact.phoneNumber, detailContact.name, firstNum, firstLabel, detailContact.nickname)
                }
                contactForDetailsSheet = detailContact.copy(phoneNumber = firstNum, label = firstLabel)
            },
            onCreateRule = { num ->
                onCreateRule(num)
                contactForDetailsSheet = null
            },
            onAddNewContact = { name, number, label, saveToDevice, addToFav ->
                val dest = if (saveToDevice) ContactSaveDestination.PHONE_CONTACTS else ContactSaveDestination.APP_ONLY
                onAddNewContact(name, number, label, dest, addToFav)
                contactForDetailsSheet = null
            },
            onSyncToPhone = {
                onSyncContactToPhone(detailContact)
            },
            onDeleteContact = { contactToDelete ->
                onDeleteContact(contactToDelete)
                contactForDetailsSheet = null
            },
            getPreferredCallingMode = getPreferredCallingMode,
            onSaveLearnedCallMode = onSaveLearnedCallMode,
            activeSims = activeSims,
            getPreferredSimSlot = getPreferredSimSlot,
            onSetPreferredSimSlot = onSetPreferredSimSlot,
            globalSimPreferenceMode = globalSimPreferenceMode,
            onEditContact = { name, number, label, nickname ->
                onUpdateContact(detailContact.phoneNumber, name, number, label, nickname)
                contactForDetailsSheet = null
            },
            onDismiss = {
                contactForDetailsSheet = null
            }
        )
    }

    if (showAddCustomDialog) {
        com.example.ui.components.CreateContactDialog(
            initialName = if (searchQuery.any { it.isLetter() }) searchQuery else "",
            initialNumber = searchQuery.filter { it.isDigit() || it == '+' },
            onDismiss = { showAddCustomDialog = false },
            onSave = { name, number, label, destination, addToFav ->
                onAddNewContact(name, number, label, destination, addToFav)
                showAddCustomDialog = false
            },
            onSaveMulti = { name, numbers, destination, addToFav ->
                if (onAddNewContactMulti != null) {
                    onAddNewContactMulti(name, numbers, destination, addToFav)
                } else {
                    val first = numbers.firstOrNull() ?: ContactPhoneNumber("", "Mobile")
                    onAddNewContact(name, first.number, first.label, destination, addToFav)
                }
                showAddCustomDialog = false
            }
        )
    }
}
