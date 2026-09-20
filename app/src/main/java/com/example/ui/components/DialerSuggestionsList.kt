package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.RecentCall
import com.example.util.CommunityCallerIdService
import com.example.util.DeviceContact
import com.example.util.T9SearchResult

/**
 * Suggestions section displayed in the top area of the dialer panel.
 * Shows recent calls when the dialed number is empty, and T9 search matches when typing.
 * Crucially justified to the bottom so a single match is immediately above the number field.
 */
@Composable
fun DialerSuggestionsList(
    number: String,
    recentCalls: List<RecentCall>,
    t9Matches: List<T9SearchResult>,
    matchedContact: DeviceContact?,
    onSelectContactNumber: (String) -> Unit,
    contacts: List<DeviceContact> = emptyList(),
    modifier: Modifier = Modifier
) {
    val contactsByDigits = remember(contacts) {
        val map = mutableMapOf<String, DeviceContact>()
        contacts.forEach { dc ->
            val digits = dc.phoneNumber.filter { it.isDigit() }.takeLast(10)
            if (digits.isNotBlank()) map[digits] = dc
            dc.phoneNumbers.forEach { pn ->
                val pnDigits = pn.number.filter { it.isDigit() }.takeLast(10)
                if (pnDigits.isNotBlank()) map[pnDigits] = dc
            }
        }
        map
    }
    val contactsByName = remember(contacts) {
        val map = mutableMapOf<String, DeviceContact>()
        contacts.forEach { dc ->
            if (dc.name.isNotBlank()) map[dc.name.trim().lowercase()] = dc
            if (!dc.nickname.isNullOrBlank()) map[dc.nickname.trim().lowercase()] = dc
        }
        map
    }

    if (number.isEmpty()) {
        val uniqueRecents = remember(recentCalls) {
            recentCalls
                .distinctBy { it.phoneNumber.filter { c -> c.isDigit() || c == '+' } }
                .take(6)
        }

        if (uniqueRecents.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Text(
                    text = "Keypad Ready",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.Bottom,
                reverseLayout = true
            ) {
                items(uniqueRecents, key = { it.id }) { call ->
                    val callDigits = call.phoneNumber.filter { it.isDigit() }.takeLast(10)
                    val dc = (if (callDigits.isNotBlank()) contactsByDigits[callDigits] else null)
                        ?: call.callerName?.takeIf { it.isNotBlank() }?.let { contactsByName[it.trim().lowercase()] }
                    DialerRecentSuggestionCard(
                        call = call,
                        matchedContact = dc,
                        onClick = { onSelectContactNumber(call.phoneNumber) }
                    )
                }
            }
        }
    } else {
        val searchSuggestions = remember(t9Matches, matchedContact, number) {
            val list = mutableListOf<T9SearchResult>()
            if (matchedContact != null) {
                list.add(
                    T9SearchResult(
                        name = matchedContact.name,
                        phoneNumber = matchedContact.phoneNumber,
                        label = matchedContact.label,
                        photoUri = matchedContact.photoUri,
                        nickname = matchedContact.nickname,
                        matchedByName = true,
                        matchSnippet = "Matched Contact"
                    )
                )
            }
            t9Matches.forEach { match ->
                val matchClean = match.phoneNumber.filter { it.isDigit() }
                val isDuplicate = list.any { existing ->
                    val exClean = existing.phoneNumber.filter { it.isDigit() }
                    exClean == matchClean || existing.name.equals(match.name, ignoreCase = true)
                }
                if (!isDuplicate) {
                    list.add(match)
                }
            }
            list
        }

        if (searchSuggestions.isEmpty()) {
            val comm = remember(number) { CommunityCallerIdService.lookup(number) }
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp, start = 16.dp, end = 16.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (comm != null) {
                    val isSpam = comm.spamScore > 50
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSpam) Color(0xFFFEE2E2) else Color(0xFFE0F2FE),
                        border = BorderStroke(1.dp, if (isSpam) Color(0xFFFCA5A5) else Color(0xFFBAE6FD))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpam) Icons.Default.Warning else Icons.Default.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSpam) Color(0xFFDC2626) else Color(0xFF0284C7)
                            )
                            Text(
                                text = "${comm.name} • ${comm.category}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSpam) Color(0xFF991B1B) else Color(0xFF0369A1)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No matching contacts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.Bottom,
                reverseLayout = true
            ) {
                items(searchSuggestions, key = { it.phoneNumber + "_" + it.name }) { match ->
                    DialerMatchSuggestionCard(
                        match = match,
                        query = number,
                        onClick = { onSelectContactNumber(match.phoneNumber) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DialerRecentSuggestionCard(
    call: RecentCall,
    matchedContact: DeviceContact? = null,
    onClick: () -> Unit
) {
    val callTypeIcon = when (call.callType) {
        1 -> Icons.AutoMirrored.Filled.CallReceived
        2 -> Icons.AutoMirrored.Filled.CallMade
        else -> Icons.AutoMirrored.Filled.CallMissed
    }
    val iconColor = when (call.callType) {
        1 -> Color(0xFF16A34A)
        2 -> MaterialTheme.colorScheme.primary
        else -> Color(0xFFDC2626)
    }

    val formalName = matchedContact?.name?.ifBlank { null } ?: call.callerName?.takeIf { it.isNotBlank() && it != call.phoneNumber }
    val nickname = matchedContact?.nickname?.ifBlank { null }
    val primaryText = nickname ?: formalName ?: call.phoneNumber
    val secondaryText = if (nickname != null && formalName != null && !nickname.equals(formalName, ignoreCase = true)) {
        "$formalName • ${call.phoneNumber}"
    } else if (formalName != null) {
        call.phoneNumber
    } else null

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp)
            .testTag("dialer_recent_suggestion_${call.phoneNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = primaryText.filter { it.isLetter() }.take(1).uppercase().ifEmpty { "#" },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = primaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = callTypeIcon,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = iconColor
                    )
                    Text(
                        text = secondaryText ?: "Recent Call",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Fill Number",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DialerMatchSuggestionCard(
    match: T9SearchResult,
    query: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.5.dp)
            .testTag("dialer_match_suggestion_${match.phoneNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                if (!match.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = match.photoUri,
                        contentDescription = match.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        val primaryInitial = (match.nickname?.ifBlank { null } ?: match.name).filter { it.isLetter() }.take(1).uppercase().ifEmpty { "#" }
                        Text(
                            text = primaryInitial,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                val hasNick = !match.nickname.isNullOrBlank() && !match.nickname.equals(match.name, ignoreCase = true)
                val displayName = if (hasNick) match.nickname!! else match.name
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = match.phoneNumber,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (match.label.isNotBlank()) {
                        Text(
                            text = "• ${match.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (match.matchSnippet.isNotBlank() && match.matchSnippet.startsWith("Nickname")) {
                        Text(
                            text = "• ${match.matchSnippet}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Fill Number",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
