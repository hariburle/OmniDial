package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import java.util.UUID

enum class ContactSaveDestination {
    PHONE_CONTACTS,
    APP_ONLY
}

data class PhoneFieldDraft(
    val id: String = UUID.randomUUID().toString(),
    var number: String = "",
    var label: String = "Mobile"
)

@Composable
fun CreateContactDialog(
    initialNumber: String = "",
    initialName: String = "",
    dialogTitle: String = "New Contact",
    initialAddToFavorites: Boolean = false,
    existingContacts: List<DeviceContact> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (name: String, number: String, label: String, destination: ContactSaveDestination, addToFavorites: Boolean) -> Unit,
    onSaveMulti: ((name: String, numbers: List<ContactPhoneNumber>, destination: ContactSaveDestination, addToFavorites: Boolean) -> Unit)? = null,
    onAddToExistingContact: ((contact: DeviceContact, newNumber: String, label: String, addToFavorites: Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Dialog modes: 0 = Create New, 1 = Add to Existing
    var selectedTab by remember { mutableStateOf(0) }

    var name by remember { mutableStateOf(initialName) }
    val phoneFields = remember {
        mutableStateListOf(
            PhoneFieldDraft(
                number = initialNumber.filter { it.isDigit() || it in "+*#()- .," },
                label = "Mobile"
            )
        )
    }

    var saveDestination by remember { mutableStateOf(ContactSaveDestination.PHONE_CONTACTS) }
    var addToFavorites by remember { mutableStateOf(initialAddToFavorites) }

    // State for "Add to Existing" mode
    var contactSearchQuery by remember { mutableStateOf("") }
    var selectedExistingContact by remember { mutableStateOf<DeviceContact?>(null) }
    var existingNumberToAdd by remember {
        mutableStateOf(initialNumber.filter { it.isDigit() || it in "+*#()- .," })
    }
    var existingNumberLabel by remember { mutableStateOf("Work") }

    // Lazy load existing contacts if not passed
    var allContacts by remember { mutableStateOf(existingContacts) }
    LaunchedEffect(Unit) {
        if (allContacts.isEmpty()) {
            allContacts = ContactHelper.fetchDeviceContacts(context)
        }
    }

    val presetLabels = listOf("Mobile", "Work", "Home", "Main", "Other")

    // Check if initial number matches an existing contact
    LaunchedEffect(initialNumber) {
        if (initialNumber.isNotBlank() && name.isBlank()) {
            val contact = ContactHelper.lookupContactByNumber(context, initialNumber)
            if (contact != null) {
                name = contact.name
                if (phoneFields.isNotEmpty() && (phoneFields[0].label.isBlank() || phoneFields[0].label == "Mobile")) {
                    phoneFields[0] = phoneFields[0].copy(label = contact.label)
                }
            }
        }
    }

    // Smart duplicate detection
    val matchingExistingContact = remember(name, phoneFields.firstOrNull()?.number, allContacts) {
        val cleanName = name.trim().lowercase()
        val firstNumDigits = phoneFields.firstOrNull()?.number?.filter { it.isDigit() }?.takeLast(10) ?: ""
        if (cleanName.length >= 3 || firstNumDigits.length >= 7) {
            allContacts.firstOrNull { c ->
                (cleanName.length >= 3 && c.name.trim().lowercase() == cleanName) ||
                (firstNumDigits.length >= 7 && c.phoneNumbers.any { pn -> pn.number.filter { it.isDigit() }.takeLast(10) == firstNumDigits })
            }
        } else null
    }

    val isNameValid = name.trim().isNotBlank()
    val isAnyNumberValid = phoneFields.any { draft ->
        val clean = draft.number.trim()
        clean.any { it.isDigit() || it in "+*#" } && clean.length >= 2
    }

    val isExistingAddValid = selectedExistingContact != null &&
            existingNumberToAdd.any { it.isDigit() || it in "+*#" } &&
            existingNumberToAdd.trim().length >= 2

    val dismissKeyboardAndClearFocus = {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    val initials = if (selectedTab == 0) {
        name.trim().take(1).uppercase().ifBlank {
            if (phoneFields.firstOrNull()?.number?.isNotBlank() == true) "#" else "?"
        }
    } else {
        selectedExistingContact?.name?.take(1)?.uppercase() ?: "?"
    }

    AlertDialog(
        onDismissRequest = {
            dismissKeyboardAndClearFocus()
            onDismiss()
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { dismissKeyboardAndClearFocus() })
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(46.dp),
                        shadowElevation = 1.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initials,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (selectedTab == 0) dialogTitle else "Add Number to Contact",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (selectedTab == 0) "Create new contact or multiple numbers" else "Append number to an existing person",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Mode Tabs: Create New vs Add to Existing
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            dismissKeyboardAndClearFocus()
                            selectedTab = 0
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("New Contact", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            dismissKeyboardAndClearFocus()
                            selectedTab = 1
                            if (matchingExistingContact != null && selectedExistingContact == null) {
                                selectedExistingContact = matchingExistingContact
                            }
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Add to Existing", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (selectedTab == 0) {
                    // ==========================================
                    // TAB 0: CREATE NEW CONTACT (MULTI-NUMBER)
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Smart Existing Contact Match Banner
                        if (matchingExistingContact != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        dismissKeyboardAndClearFocus()
                                        selectedExistingContact = matchingExistingContact
                                        if (phoneFields.isNotEmpty() && phoneFields[0].number.isNotBlank()) {
                                            existingNumberToAdd = phoneFields[0].number
                                        }
                                        selectedTab = 1
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Contact '${matchingExistingContact.name}' already exists",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = "Tap to add this number to them instead",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Name Field
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("e.g. Sarah Connor") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("create_contact_name_input")
                        )

                        // Phone Numbers Header
                        Text(
                            text = "Phone Numbers",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Multiple Phone Number Entries
                        phoneFields.forEachIndexed { index, draft ->
                            val isThisNumberValid = draft.number.any { it.isDigit() || it in "+*#" } && draft.number.trim().length >= 2
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (index == 0) "Primary Number" else "Secondary Number #${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (phoneFields.size > 1) {
                                            IconButton(
                                                onClick = {
                                                    dismissKeyboardAndClearFocus()
                                                    phoneFields.removeAt(index)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove number",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = draft.number,
                                        onValueChange = { input ->
                                            val filtered = input.filter { it.isDigit() || it in "+*#()- .," }
                                            phoneFields[index] = draft.copy(number = filtered)
                                        },
                                        label = { Text("Phone Number") },
                                        placeholder = { Text("+1 (555) 000-0000") },
                                        isError = draft.number.isNotBlank() && !isThisNumberValid,
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = null,
                                                tint = if (draft.number.isNotBlank() && !isThisNumberValid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Phone,
                                            imeAction = if (index == phoneFields.lastIndex) ImeAction.Done else ImeAction.Next
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = { dismissKeyboardAndClearFocus() }
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("create_contact_number_input_$index")
                                    )

                                    // Label selector chips for this phone number
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        presetLabels.forEach { preset ->
                                            FilterChip(
                                                selected = draft.label.equals(preset, ignoreCase = true),
                                                onClick = {
                                                    dismissKeyboardAndClearFocus()
                                                    phoneFields[index] = draft.copy(label = preset)
                                                },
                                                label = { Text(preset, fontSize = 11.sp, maxLines = 1) },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Add Another Phone Number Button
                        OutlinedButton(
                            onClick = {
                                dismissKeyboardAndClearFocus()
                                phoneFields.add(PhoneFieldDraft(label = "Work"))
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Add Another Phone Number", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Save Location Header
                        Text(
                            text = "Save Location",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Option 1: Phone Contacts (Google / Device synced)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (saveDestination == ContactSaveDestination.PHONE_CONTACTS) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    dismissKeyboardAndClearFocus()
                                    saveDestination = ContactSaveDestination.PHONE_CONTACTS
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = saveDestination == ContactSaveDestination.PHONE_CONTACTS,
                                    onClick = {
                                        dismissKeyboardAndClearFocus()
                                        saveDestination = ContactSaveDestination.PHONE_CONTACTS
                                    }
                                )
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Device / Phone Contacts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Saved to your device address book and default sync account",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Option 2: App Only (Local)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (saveDestination == ContactSaveDestination.APP_ONLY) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    dismissKeyboardAndClearFocus()
                                    saveDestination = ContactSaveDestination.APP_ONLY
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = saveDestination == ContactSaveDestination.APP_ONLY,
                                    onClick = {
                                        dismissKeyboardAndClearFocus()
                                        saveDestination = ContactSaveDestination.APP_ONLY
                                    }
                                )
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp)
                                        .size(22.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "App Only (Local)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Private offline entry inside dialer, syncable later",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Optional: Add to Favorites checkbox
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (addToFavorites) Color(0xFFFEF3C7) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    dismissKeyboardAndClearFocus()
                                    addToFavorites = !addToFavorites
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Checkbox(
                                    checked = addToFavorites,
                                    onCheckedChange = {
                                        dismissKeyboardAndClearFocus()
                                        addToFavorites = it
                                    }
                                )
                                Icon(
                                    imageVector = if (addToFavorites) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    tint = if (addToFavorites) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Add to Favorites for instant speed dialing",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (addToFavorites) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (addToFavorites) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else {
                    // ==========================================
                    // TAB 1: ADD NUMBER TO EXISTING CONTACT
                    // ==========================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Select Existing Contact",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Search Contact input
                        OutlinedTextField(
                            value = contactSearchQuery,
                            onValueChange = { contactSearchQuery = it },
                            placeholder = { Text("Search contact name...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                if (contactSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { contactSearchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { dismissKeyboardAndClearFocus() }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Filtered contacts list
                        val filteredContacts = remember(contactSearchQuery, allContacts) {
                            if (contactSearchQuery.isBlank()) allContacts.take(5)
                            else allContacts.filter {
                                it.name.contains(contactSearchQuery, ignoreCase = true) ||
                                it.phoneNumbers.any { pn -> pn.number.contains(contactSearchQuery) }
                            }.take(8)
                        }

                        if (selectedExistingContact == null) {
                            Text(
                                text = "Choose a contact to add a number to:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (filteredContacts.isEmpty()) {
                                    Text(
                                        text = "No contacts found matching '${contactSearchQuery}'",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                } else {
                                    filteredContacts.forEach { contact ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    dismissKeyboardAndClearFocus()
                                                    selectedExistingContact = contact
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = contact.name.take(1).uppercase(),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        )
                                                    }
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = contact.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    val numsSummary = contact.phoneNumbers.joinToString(", ") { "${it.label}: ${it.number}" }
                                                    Text(
                                                        text = numsSummary.ifBlank { contact.phoneNumber },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Selected Contact Card
                            val sel = selectedExistingContact!!
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = sel.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sel.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Current: " + (sel.phoneNumbers.ifEmpty { listOf(ContactPhoneNumber(sel.phoneNumber, sel.label)) }).joinToString(", ") { "${it.label}: ${it.number}" },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = {
                                        dismissKeyboardAndClearFocus()
                                        selectedExistingContact = null
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Change contact", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            // New Phone Number Input for Selected Contact
                            OutlinedTextField(
                                value = existingNumberToAdd,
                                onValueChange = { input ->
                                    existingNumberToAdd = input.filter { it.isDigit() || it in "+*#()- .," }
                                },
                                label = { Text("New Phone Number to Add") },
                                placeholder = { Text("+1 (555) 000-0000") },
                                isError = existingNumberToAdd.isNotBlank() && !existingNumberToAdd.any { it.isDigit() || it in "+*#" },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Phone,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { dismissKeyboardAndClearFocus() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Label Selection
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Phone Label",
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
                                            selected = existingNumberLabel.equals(preset, ignoreCase = true),
                                            onClick = {
                                                dismissKeyboardAndClearFocus()
                                                existingNumberLabel = preset
                                            },
                                            label = { Text(preset, fontSize = 12.sp, maxLines = 1) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }

                            // Add to Favorites for new number
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (addToFavorites) Color(0xFFFEF3C7) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        dismissKeyboardAndClearFocus()
                                        addToFavorites = !addToFavorites
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Checkbox(
                                        checked = addToFavorites,
                                        onCheckedChange = {
                                            dismissKeyboardAndClearFocus()
                                            addToFavorites = it
                                        }
                                    )
                                    Icon(
                                        imageVector = if (addToFavorites) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = null,
                                        tint = if (addToFavorites) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Add this new number to Favorites",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (addToFavorites) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (addToFavorites) Color(0xFF92400E) else MaterialTheme.colorScheme.onSurface
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
                    dismissKeyboardAndClearFocus()
                    if (selectedTab == 0 && isNameValid && isAnyNumberValid) {
                        val validNumbers = phoneFields
                            .map { ContactPhoneNumber(it.number.trim(), it.label.trim().ifBlank { "Mobile" }) }
                            .filter { it.number.isNotBlank() }

                        if (onSaveMulti != null) {
                            onSaveMulti(name.trim(), validNumbers, saveDestination, addToFavorites)
                        } else {
                            val first = validNumbers.firstOrNull() ?: ContactPhoneNumber("", "Mobile")
                            onSave(name.trim(), first.number, first.label, saveDestination, addToFavorites)
                        }
                        onDismiss()
                    } else if (selectedTab == 1 && isExistingAddValid) {
                        val target = selectedExistingContact!!
                        if (onAddToExistingContact != null) {
                            onAddToExistingContact(target, existingNumberToAdd.trim(), existingNumberLabel.trim().ifBlank { "Mobile" }, addToFavorites)
                        } else {
                            ContactHelper.addPhoneNumberToExistingContact(
                                context = context,
                                contactId = target.contactId,
                                existingNumber = target.phoneNumber,
                                newNumber = existingNumberToAdd.trim(),
                                label = existingNumberLabel.trim().ifBlank { "Mobile" }
                            )
                        }
                        onDismiss()
                    }
                },
                enabled = if (selectedTab == 0) (isNameValid && isAnyNumberValid) else isExistingAddValid,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("create_contact_save_btn")
            ) {
                Text(
                    text = if (selectedTab == 0) "Save Contact" else "Add Number",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    dismissKeyboardAndClearFocus()
                    onDismiss()
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
