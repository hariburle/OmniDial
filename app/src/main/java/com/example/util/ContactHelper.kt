package com.example.util

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.compose.runtime.Immutable
import com.example.data.RecentCall

@Immutable
data class ContactPhoneNumber(
    val number: String,
    val label: String = "Mobile"
)

@Immutable
data class DeviceContact(
    val name: String,
    val phoneNumber: String,
    val label: String = "Mobile",
    val photoUri: String? = null,
    val contactId: Long? = null,
    val nickname: String? = null,
    val isStarred: Boolean = false,
    val isAppOnly: Boolean = false,
    val phoneNumbers: List<ContactPhoneNumber> = if (phoneNumber.isNotBlank()) listOf(ContactPhoneNumber(phoneNumber, label)) else emptyList(),
    val t9Name: String = T9Helper.stringToT9(name),
    val t9Words: List<String> = name.split(Regex("\\s+")).map { T9Helper.stringToT9(it) }.filter { it.isNotBlank() },
    val t9Nickname: String? = nickname?.trim()?.takeIf { it.isNotBlank() }?.let { T9Helper.stringToT9(it) },
    val t9NicknameWords: List<String> = nickname?.trim()?.takeIf { it.isNotBlank() }?.split(Regex("\\s+"))?.map { T9Helper.stringToT9(it) }?.filter { it.isNotBlank() } ?: emptyList()
)

object ContactHelper {

    fun launchContactEditor(context: Context, contact: DeviceContact) {
        val cId = contact.contactId
        val intent = if (cId != null && cId > 0) {
            Intent(Intent.ACTION_EDIT).apply {
                data = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, cId)
            }
        } else {
            Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                type = ContactsContract.Contacts.CONTENT_ITEM_TYPE
                putExtra(ContactsContract.Intents.Insert.NAME, contact.name)
                putExtra(ContactsContract.Intents.Insert.PHONE, contact.phoneNumber)
            }
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            android.widget.Toast.makeText(context, "Unable to open Phone Contacts editor", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteContactFromDevice(context: Context, contactId: Long?): Boolean {
        if (contactId == null || contactId <= 0) return false
        return try {
            val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
            val rows = context.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            android.util.Log.e("ContactHelper", "Failed to delete contact from device", e)
            false
        }
    }

    fun deleteContactFromDeviceByNumber(context: Context, phoneNumber: String): Boolean {
        val clean = phoneNumber.filter { it.isDigit() }.takeLast(10)
        if (clean.isBlank()) return false
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            var deletedAny = false
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                while (cursor.moveToNext()) {
                    val cId = if (idIdx >= 0) cursor.getLong(idIdx) else null
                    if (cId != null && cId > 0) {
                        val deleteUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, cId)
                        val count = context.contentResolver.delete(deleteUri, null, null)
                        if (count > 0) deletedAny = true
                    }
                }
            }
            deletedAny
        } catch (e: Exception) {
            android.util.Log.e("ContactHelper", "Failed to delete contact by number", e)
            false
        }
    }

    fun isDialOrTelIntent(intent: Intent?): Boolean {
        if (intent == null) return false
        val action = intent.action
        val data = intent.data
        val scheme = data?.scheme
        return action == Intent.ACTION_DIAL ||
                action == Intent.ACTION_CALL ||
                action == Intent.ACTION_CALL_BUTTON ||
                (action == Intent.ACTION_VIEW && scheme == "tel") ||
                scheme == "tel" ||
                intent.hasExtra(Intent.EXTRA_PHONE_NUMBER) ||
                intent.hasExtra("android.intent.extra.PHONE_NUMBER") ||
                intent.hasExtra("phone") ||
                intent.hasExtra("phoneNumber") ||
                intent.hasExtra("number") ||
                intent.getStringExtra("EXTRA_NAV_TAB") == "DIALER" ||
                intent.getStringExtra("EXTRA_NAV_TAB") == "KEYPAD" ||
                intent.getIntExtra("EXTRA_NAV_TAB_INDEX", -1) == 2
    }

    fun extractPhoneNumberFromIntent(intent: Intent?): String? {
        if (intent == null) return null
        val data = intent.data
        if (data != null && data.scheme == "tel") {
            val ssp = data.schemeSpecificPart
            if (!ssp.isNullOrBlank()) {
                val clean = ssp.substringBefore('?').trim()
                return Uri.decode(clean)
            }
        }
        val ssp = data?.schemeSpecificPart
        if (!ssp.isNullOrBlank() && (intent.action == Intent.ACTION_DIAL || intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_CALL)) {
            val clean = ssp.substringBefore('?').trim()
            return Uri.decode(clean)
        }
        val extraNum = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            ?: intent.getStringExtra("android.intent.extra.PHONE_NUMBER")
            ?: intent.getStringExtra("phoneNumber")
            ?: intent.getStringExtra("phone")
            ?: intent.getStringExtra("number")
        if (!extraNum.isNullOrBlank()) {
            return extraNum.trim()
        }
        return null
    }

    fun getPrimaryContactsAccount(context: Context): Pair<String?, String?> {
        try {
            val uri = ContactsContract.RawContacts.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.RawContacts.ACCOUNT_NAME,
                ContactsContract.RawContacts.ACCOUNT_TYPE
            )
            // 1. Prefer Google Account from RawContacts
            context.contentResolver.query(
                uri,
                projection,
                "${ContactsContract.RawContacts.ACCOUNT_TYPE} = ?",
                arrayOf("com.google"),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val nameCol = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
                    val typeCol = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                    val name = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val type = if (typeCol >= 0) cursor.getString(typeCol) else null
                    if (!name.isNullOrBlank() && !type.isNullOrBlank()) {
                        return Pair(name, type)
                    }
                }
            }

            // 2. Check for any other non-null account in RawContacts
            context.contentResolver.query(
                uri,
                projection,
                "${ContactsContract.RawContacts.ACCOUNT_NAME} IS NOT NULL AND ${ContactsContract.RawContacts.ACCOUNT_TYPE} IS NOT NULL",
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val nameCol = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
                    val typeCol = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
                    val name = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val type = if (typeCol >= 0) cursor.getString(typeCol) else null
                    if (!name.isNullOrBlank() && !type.isNullOrBlank()) {
                        return Pair(name, type)
                    }
                }
            }
        } catch (_: Exception) {}

        // 3. Fallback: Query AccountManager for Google accounts
        try {
            val am = android.accounts.AccountManager.get(context)
            val googleAccounts = am.getAccountsByType("com.google")
            if (googleAccounts.isNotEmpty()) {
                return Pair(googleAccounts[0].name, googleAccounts[0].type)
            }
            val allAccounts = am.accounts
            if (allAccounts.isNotEmpty()) {
                return Pair(allAccounts[0].name, allAccounts[0].type)
            }
        } catch (_: Exception) {}

        return Pair(null, null)
    }

    fun saveContactToDevice(context: Context, name: String, phoneNumbers: List<ContactPhoneNumber>): Boolean {
        return try {
            val cleanName = name.trim()
            val validNumbers = phoneNumbers.filter { it.number.trim().isNotBlank() }
            if (cleanName.isBlank() || validNumbers.isEmpty()) return false

            val (accountName, accountType) = getPrimaryContactsAccount(context)

            val ops = ArrayList<android.content.ContentProviderOperation>()
            val rawContactOp = android.content.ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            if (!accountName.isNullOrBlank() && !accountType.isNullOrBlank()) {
                rawContactOp.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                rawContactOp.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
            } else {
                rawContactOp.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                rawContactOp.withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            }
            ops.add(rawContactOp.build())

            ops.add(
                android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, cleanName)
                    .build()
            )

            for ((idx, pn) in validNumbers.withIndex()) {
                val cleanNum = pn.number.trim()
                val phoneType = when (pn.label.lowercase()) {
                    "home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
                    "work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                    "other" -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
                    else -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                }
                val phoneOp = android.content.ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, cleanNum)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
                if (idx == 0) {
                    phoneOp.withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, 1)
                    phoneOp.withValue(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY, 1)
                }
                ops.add(phoneOp.build())
            }

            val results = context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            val success = results.isNotEmpty()
            if (success) {
                try {
                    if (accountName != null && accountType != null) {
                        val account = android.accounts.Account(accountName, accountType)
                        android.content.ContentResolver.requestSync(
                            account,
                            ContactsContract.AUTHORITY,
                            android.os.Bundle().apply {
                                putBoolean(android.content.ContentResolver.SYNC_EXTRAS_MANUAL, true)
                                putBoolean(android.content.ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                            }
                        )
                    }
                } catch (_: Exception) {}
            }
            success
        } catch (e: Exception) {
            android.util.Log.e("ContactHelper", "Failed to save multi-number contact to device", e)
            false
        }
    }

    fun saveContactToDevice(context: Context, name: String, phoneNumber: String, label: String = "Mobile"): Boolean {
        return saveContactToDevice(context, name, listOf(ContactPhoneNumber(phoneNumber, label)))
    }

    fun addPhoneNumberToExistingContact(
        context: Context,
        contactId: Long?,
        existingNumber: String?,
        newNumber: String,
        label: String = "Mobile"
    ): Boolean {
        return try {
            val cleanNewNumber = newNumber.trim()
            if (cleanNewNumber.isBlank()) return false

            var rawContactId: Long? = null

            // 1. Try resolving raw contact ID via contactId
            if (contactId != null) {
                context.contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts._ID),
                    "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                    arrayOf(contactId.toString()),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        rawContactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts._ID))
                    }
                }
            }

            // 2. Fallback: Query by existing phone number
            if (rawContactId == null && !existingNumber.isNullOrBlank()) {
                val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(existingNumber))
                var foundContactId: Long? = null
                context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                        if (idIdx != -1) foundContactId = cursor.getLong(idIdx)
                    }
                }
                if (foundContactId != null) {
                    context.contentResolver.query(
                        ContactsContract.RawContacts.CONTENT_URI,
                        arrayOf(ContactsContract.RawContacts._ID),
                        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                        arrayOf(foundContactId.toString()),
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            rawContactId = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts._ID))
                        }
                    }
                }
            }

            if (rawContactId == null) return false

            val phoneType = when (label.lowercase()) {
                "home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
                "work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                "other" -> ContactsContract.CommonDataKinds.Phone.TYPE_OTHER
                else -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
            }

            val values = android.content.ContentValues().apply {
                put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                put(ContactsContract.CommonDataKinds.Phone.NUMBER, cleanNewNumber)
                put(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
            }
            val insertedUri = context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, values)
            val success = insertedUri != null

            if (success) {
                try {
                    val (accountName, accountType) = getPrimaryContactsAccount(context)
                    if (accountName != null && accountType != null) {
                        val account = android.accounts.Account(accountName, accountType)
                        android.content.ContentResolver.requestSync(
                            account,
                            ContactsContract.AUTHORITY,
                            android.os.Bundle().apply {
                                putBoolean(android.content.ContentResolver.SYNC_EXTRAS_MANUAL, true)
                                putBoolean(android.content.ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                            }
                        )
                    }
                } catch (_: Exception) {}
            }
            success
        } catch (e: Exception) {
            android.util.Log.e("ContactHelper", "Failed to add phone number to existing contact", e)
            false
        }
    }

    fun createContactPickerIntent(): Intent {
        return Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
    }

    /**
     * Resolves a phone number into an international E.164-compatible digit string for WhatsApp and messaging:
     * 1. If the number already has a leading '+', strips non-digits and uses it.
     * 2. Checks saved contact records in address book to see if the contact has an international number stored (+...).
     * 3. If the number is a standard domestic/national number (e.g. 10 digits without country code), automatically
     *    prepends the device's default country calling code (e.g. 91 for India, 1 for US, 44 for UK, etc.).
     */
    fun resolveFullInternationalNumber(context: Context, rawNumber: String): String {
        val trimmed = rawNumber.trim()
        if (trimmed.isEmpty()) return ""

        // 1. If starts with '+', it already has international country code
        if (trimmed.startsWith("+")) {
            return trimmed.replace(Regex("[^0-9]"), "")
        }

        // 2. If starts with "00", strip "00"
        if (trimmed.startsWith("00")) {
            return trimmed.substring(2).replace(Regex("[^0-9]"), "")
        }

        val cleanDigits = trimmed.replace(Regex("[^0-9]"), "")
        if (cleanDigits.isEmpty()) return ""
        val last10 = if (cleanDigits.length >= 10) cleanDigits.takeLast(10) else cleanDigits

        // 3. Search Device Contacts directly via CommonDataKinds.Phone.CONTENT_URI for any contact number ending in last 10 digits with a '+'
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
            )
            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            cursor?.use { c ->
                val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val normIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
                while (c.moveToNext()) {
                    val rawCandidates = mutableListOf<String>()
                    if (numIdx != -1) c.getString(numIdx)?.let { rawCandidates.add(it) }
                    if (normIdx != -1) c.getString(normIdx)?.let { rawCandidates.add(it) }

                    for (candidate in rawCandidates) {
                        val candTrimmed = candidate.trim()
                        if (candTrimmed.startsWith("+")) {
                            val candDigits = candTrimmed.replace(Regex("[^0-9]"), "")
                            if (candDigits.endsWith(last10) || (cleanDigits.length >= 7 && candDigits.endsWith(cleanDigits))) {
                                return candDigits
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        // 4. Try to find a matching contact in address book via lookupContactByNumber
        try {
            val contact = lookupContactByNumber(context, trimmed)
            if (contact != null) {
                val contactNumber = contact.phoneNumber.trim()
                if (contactNumber.startsWith("+")) {
                    return contactNumber.replace(Regex("[^0-9]"), "")
                }
                for (pn in contact.phoneNumbers) {
                    if (pn.number.trim().startsWith("+")) {
                        return pn.number.replace(Regex("[^0-9]"), "")
                    }
                }
            }
        } catch (_: Exception) {}

        // 5. If cleanDigits already starts with a known international calling code (e.g. 919663306802 length 12 -> 91 + 10 digits)
        for ((_, code) in countryCallingCodes) {
            if (code != "1" && cleanDigits.startsWith(code) && cleanDigits.length == (code.length + 10)) {
                return cleanDigits
            }
        }

        // 6. Get device default country calling code
        val deviceIso = getDeviceCountryIso(context)
        val callingCode = getCountryCallingCode(deviceIso)

        // Handle national trunk prefix '0' (e.g. UK 07xxx -> 447xxx, India 098xxx -> 9198xxx)
        var nationalDigits = cleanDigits
        if (nationalDigits.startsWith("0") && nationalDigits.length > 10) {
            nationalDigits = nationalDigits.substring(1)
        }

        // If nationalDigits is a 10-digit domestic number or shorter, prepend country calling code
        if (nationalDigits.length == 10) {
            return "$callingCode$nationalDigits"
        } else if (nationalDigits.length < 10) {
            return "$callingCode$nationalDigits"
        } else {
            // If already starts with calling code or is longer international digits
            if (!nationalDigits.startsWith(callingCode) && nationalDigits.length <= 11 && callingCode != "1") {
                return "$callingCode$nationalDigits"
            }
            return nationalDigits
        }
    }

    /**
     * Initiates a direct WhatsApp voice call without opening the chat screen.
     * Uses Android Contacts Provider VoIP Data item if available, or direct WhatsApp call intent.
     */
    fun launchWhatsAppCall(context: Context, rawNumber: String) {
        val digitsOnly = resolveFullInternationalNumber(context, rawNumber)

        if (digitsOnly.isEmpty()) {
            android.widget.Toast.makeText(context, "Invalid phone number for WhatsApp", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Query Android Contacts Provider for WhatsApp VoIP Call MIME type for this number
        try {
            val resolver = context.contentResolver
            val uri = ContactsContract.Data.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.Data._ID,
                ContactsContract.Data.DATA1,
                ContactsContract.Data.DATA3,
                ContactsContract.Data.MIMETYPE
            )
            val selection = "${ContactsContract.Data.MIMETYPE} IN (?, ?)"
            val selectionArgs = arrayOf(
                "vnd.android.cursor.item/vnd.com.whatsapp.voip.call",
                "vnd.android.cursor.item/vnd.com.whatsapp.w4b.voip.call"
            )

            var targetDataId: Long? = null
            var targetMimeType: String? = null

            resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(ContactsContract.Data._ID)
                val data1Col = cursor.getColumnIndex(ContactsContract.Data.DATA1)
                val data3Col = cursor.getColumnIndex(ContactsContract.Data.DATA3)
                val mimeCol = cursor.getColumnIndex(ContactsContract.Data.MIMETYPE)

                while (cursor.moveToNext()) {
                    val data1 = if (data1Col >= 0) cursor.getString(data1Col) ?: "" else ""
                    val data3 = if (data3Col >= 0) cursor.getString(data3Col) ?: "" else ""
                    val rowDigits1 = data1.replace(Regex("[^0-9]"), "")
                    val rowDigits3 = data3.replace(Regex("[^0-9]"), "")

                    if (rowDigits1.endsWith(digitsOnly) || digitsOnly.endsWith(rowDigits1) ||
                        rowDigits3.endsWith(digitsOnly) || digitsOnly.endsWith(rowDigits3) ||
                        (rowDigits1.length >= 7 && digitsOnly.contains(rowDigits1))) {
                        targetDataId = if (idCol >= 0) cursor.getLong(idCol) else null
                        targetMimeType = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                        break
                    }
                }
            }

            if (targetDataId != null && targetMimeType != null) {
                val directCallIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(
                        Uri.parse("content://com.android.contacts/data/$targetDataId"),
                        targetMimeType
                    )
                    setPackage(if (targetMimeType!!.contains("w4b")) "com.whatsapp.w4b" else "com.whatsapp")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(directCallIntent)
                return
            }
        } catch (_: Exception) {
            // Proceed to direct scheme fallback
        }

        // 2. Direct VoIP Call Intent via WhatsApp URL scheme
        try {
            val callIntent = Intent(Intent.ACTION_VIEW, Uri.parse("whatsapp://call?phone=$digitsOnly")).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(callIntent)
            return
        } catch (_: Exception) {
            // Fallback to chat link
        }

        // 3. Fallback: Open WhatsApp directly
        try {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$digitsOnly")).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
            try {
                // Try open browser wa.me if whatsapp app not installed
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digitsOnly")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "WhatsApp is not installed on this device", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchWhatsAppMessage(context: Context, rawNumber: String) {
        val digitsOnly = resolveFullInternationalNumber(context, rawNumber)
        if (digitsOnly.isEmpty()) {
            android.widget.Toast.makeText(context, "Invalid phone number for WhatsApp", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$digitsOnly")).apply {
                setPackage("com.whatsapp")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$digitsOnly")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "WhatsApp is not installed on this device", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchSms(context: Context, rawNumber: String) {
        val targetNumber = try {
            val contact = lookupContactByNumber(context, rawNumber)
            val fullNum = contact?.phoneNumber ?: rawNumber
            fullNum.ifBlank { rawNumber }
        } catch (_: Exception) {
            rawNumber
        }
        try {
            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$targetNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(smsIntent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Could not open Messaging app", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Standard country ISO to international calling code mapping
    private val countryCallingCodes = mapOf(
        "us" to "1", "ca" to "1", "in" to "91", "gb" to "44", "uk" to "44",
        "au" to "61", "de" to "49", "fr" to "33", "it" to "39", "es" to "34",
        "mx" to "52", "br" to "55", "ru" to "7", "jp" to "81", "cn" to "86",
        "kr" to "82", "sg" to "65", "ae" to "971", "sa" to "966", "pk" to "92",
        "bd" to "880", "np" to "977", "lk" to "94", "my" to "60", "id" to "62",
        "ph" to "63", "nz" to "64", "za" to "27", "nl" to "31", "se" to "46",
        "no" to "47", "dk" to "45", "ch" to "41", "at" to "43", "ie" to "353",
        "il" to "972", "eg" to "20", "ng" to "234", "ke" to "254", "th" to "66",
        "vn" to "84", "tr" to "90", "gr" to "30", "pt" to "351", "pl" to "48",
        "hk" to "852", "tw" to "886", "ar" to "54", "co" to "57", "cl" to "56"
    )

    /**
     * Determines current country ISO of device based on cellular network, SIM card, or locale.
     */
    fun getDeviceCountryIso(context: Context?): String {
        if (context != null) {
            try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
                val networkCountry = tm?.networkCountryIso?.trim()?.lowercase()
                if (!networkCountry.isNullOrBlank()) return networkCountry

                val simCountry = tm?.simCountryIso?.trim()?.lowercase()
                if (!simCountry.isNullOrBlank()) return simCountry
            } catch (_: Exception) {}
        }
        try {
            val localeCountry = java.util.Locale.getDefault().country.trim().lowercase()
            if (localeCountry.isNotBlank()) return localeCountry
        } catch (_: Exception) {}
        return "us"
    }

    /**
     * Retrieves calling code (e.g. "91" for India, "1" for USA) for given country ISO.
     */
    fun getCountryCallingCode(countryIso: String): String {
        return countryCallingCodes[countryIso.lowercase().trim()] ?: "1"
    }

    /**
     * Identifies the uppercase country ISO (e.g. "US", "IN", "GB") for a phone number based on its international prefix.
     */
    fun getCountryIsoForNumber(phoneNumber: String): String? {
        val code = extractCountryCallingCode(phoneNumber) ?: return null
        return countryCallingCodes.entries.firstOrNull { it.value == code }?.key?.uppercase()
    }

    /**
     * Returns a human-friendly label for a contact's phone number, adding a country tag to disambiguate.
     */
    fun getDescriptiveNumberLabel(contact: DeviceContact?, phoneNumber: String): String {
        val matchedPhone = contact?.phoneNumbers?.firstOrNull {
            isSamePhoneNumber(it.number, phoneNumber)
        }
        val rawLabel = matchedPhone?.label?.ifBlank { null } ?: contact?.label?.ifBlank { null } ?: "Mobile"

        val countryIso = getCountryIsoForNumber(phoneNumber)
        return if (countryIso != null && !rawLabel.contains(countryIso, ignoreCase = true)) {
            "$rawLabel • $countryIso"
        } else {
            rawLabel
        }
    }

    /**
     * Intelligently checks if a phone number is international relative to the device's physical location:
     * - If physically in India (calling code 91), a +91 number is NOT international.
     * - If physically in USA (calling code 1), a +91 number IS international, but a +1 number is NOT.
     * - Any number dialed without international prefix (+, 00, 011) is treated as a domestic/local call.
     */
    fun isInternationalNumber(context: Context?, phoneNumber: String): Boolean {
        val clean = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (clean.isBlank()) return false

        val hasPlus = clean.startsWith("+")
        val has00Exit = clean.startsWith("00")
        val has011Exit = clean.startsWith("011")

        // If no international prefix was dialed, this is a local/domestic call
        if (!hasPlus && !has00Exit && !has011Exit) {
            return false
        }

        val deviceCountryIso = getDeviceCountryIso(context)
        val deviceCallingCode = getCountryCallingCode(deviceCountryIso)

        val digitsAfterPrefix = when {
            hasPlus -> clean.removePrefix("+")
            has011Exit -> clean.removePrefix("011")
            has00Exit -> clean.removePrefix("00")
            else -> clean
        }

        // If the number's country code matches the current device location, it is domestic
        if (digitsAfterPrefix.startsWith(deviceCallingCode)) {
            return false
        }

        // Otherwise, it targets a foreign country calling code
        return true
    }

    fun isInternationalNumber(phoneNumber: String): Boolean {
        return isInternationalNumber(null, phoneNumber)
    }

    /**
     * Checks if a phone number matches international WhatsApp routing rules.
     */
    fun shouldSuggestWhatsApp(phoneNumber: String): Boolean {
        return isInternationalNumber(null, phoneNumber)
    }

    fun shouldSuggestWhatsApp(context: Context?, phoneNumber: String): Boolean {
        return isInternationalNumber(context, phoneNumber)
    }

    /**
     * Strips leading international calling code or trunk zero to extract core local digits.
     */
    fun normalizeToLocalDigits(phoneNumber: String): String {
        val clean = phoneNumber.filter { it.isDigit() }
        if (clean.isBlank()) return ""

        for ((_, code) in countryCallingCodes) {
            if (clean.startsWith(code) && clean.length > code.length + 5) {
                return clean.substring(code.length)
            }
        }
        if (clean.startsWith("0") && clean.length > 8) {
            return clean.substring(1)
        }
        return clean
    }

    /**
     * Extracts international calling code (e.g. "91" for India, "1" for USA) from a phone number if present.
     */
    fun extractCountryCallingCode(phoneNumber: String): String? {
        val clean = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (clean.isBlank()) return null
        val digits = when {
            clean.startsWith("+") -> clean.removePrefix("+")
            clean.startsWith("00") -> clean.removePrefix("00")
            clean.startsWith("011") -> clean.removePrefix("011")
            else -> return null
        }
        val sortedCodes = countryCallingCodes.values.distinct().sortedByDescending { it.length }
        for (code in sortedCodes) {
            if (digits.startsWith(code) && digits.length > code.length) {
                return code
            }
        }
        return null
    }

    /**
     * Safely determines if two phone numbers refer to the exact same telephone line.
     * Critically ensures that numbers from different countries (e.g. +1 US vs +91 India)
     * are NEVER considered the same number, even if they share the same suffix digits.
     */
    fun isSamePhoneNumber(num1: String?, num2: String?, context: Context? = null): Boolean {
        if (num1.isNullOrBlank() || num2.isNullOrBlank()) return false
        val s1 = num1.trim()
        val s2 = num2.trim()
        if (s1.equals(s2, ignoreCase = true)) return true

        val d1 = s1.filter { it.isDigit() }
        val d2 = s2.filter { it.isDigit() }

        // Short codes, star codes (e.g. *86, 911, 611), or short numbers (< 7 digits)
        // must match exactly and can never fuzzy match standard telephone numbers.
        if (d1.length < 7 || d2.length < 7) {
            val clean1 = s1.replace(Regex("[^0-9+*#]"), "")
            val clean2 = s2.replace(Regex("[^0-9+*#]"), "")
            return clean1.equals(clean2, ignoreCase = true)
        }

        // Libphonenumber Match Check
        if (PhoneNumberNormalizer.isSamePhoneNumber(s1, s2, context)) {
            return true
        }

        val clean1 = s1.replace(Regex("[^0-9+]"), "")
        val clean2 = s2.replace(Regex("[^0-9+]"), "")
        if (clean1.isEmpty() || clean2.isEmpty()) return false
        if (clean1 == clean2) return true

        val country1 = extractCountryCallingCode(clean1)
        val country2 = extractCountryCallingCode(clean2)

        // If BOTH numbers specify country codes and they differ (+1 vs +91), they are NEVER the same!
        if (country1 != null && country2 != null && country1 != country2) {
            return false
        }

        // Check local / national digits
        val local1 = normalizeToLocalDigits(clean1)
        val local2 = normalizeToLocalDigits(clean2)

        if (local1.isNotEmpty() && local1 == local2 && local1.length >= 7) {
            return true
        }

        // Suffix match only if country codes are not conflicting and suffix is at least 10 digits
        if (country1 == null && country2 == null) {
            val d1 = clean1.filter { it.isDigit() }
            val d2 = clean2.filter { it.isDigit() }
            if (d1.length >= 10 && d2.length >= 10 && d1.takeLast(10) == d2.takeLast(10)) {
                return true
            }
        }

        return false
    }

    /**
     * Matches a contact phone number against a user search query, allowing searches without
     * international dial codes (+91, +1, etc.) to match contacts saved with international dial codes,
     * while strictly isolating numbers from different countries (+1 vs +91).
     */
    fun matchesNumberQuery(contactNumber: String, searchQuery: String): Boolean {
        if (searchQuery.isBlank()) return true
        val queryTrimmed = searchQuery.trim()
        val queryDigits = queryTrimmed.filter { it.isDigit() }
        val contactDigits = contactNumber.filter { it.isDigit() }

        if (queryDigits.isEmpty()) {
            return contactNumber.contains(queryTrimmed, ignoreCase = true)
        }

        // Prevent cross-country matches (e.g. +1 US contact matching +91 India query or vice versa)
        val countryContact = extractCountryCallingCode(contactNumber)
        val countryQuery = extractCountryCallingCode(searchQuery)
        if (countryContact != null && countryQuery != null && countryContact != countryQuery) {
            return false
        }

        if (isSamePhoneNumber(contactNumber, searchQuery)) return true

        // Direct digit substring match (e.g. "98765" in "919876543210")
        if (contactDigits.contains(queryDigits)) return true

        // Local digits match (stripped of international code)
        val contactLocal = normalizeToLocalDigits(contactNumber)
        if (contactLocal.contains(queryDigits)) return true

        val queryLocal = normalizeToLocalDigits(searchQuery)
        if (queryLocal.isNotEmpty() && contactDigits.contains(queryLocal)) return true
        if (queryLocal.isNotEmpty() && contactLocal.contains(queryLocal)) return true

        // Last 10-digit suffix match (only if neither or same country)
        if (contactDigits.length >= 10 && queryDigits.length >= 10) {
            if (contactDigits.takeLast(10) == queryDigits.takeLast(10)) return true
        }

        return contactNumber.contains(queryTrimmed, ignoreCase = true)
    }

    /**
     * Gets the carrier voicemail number from TelephonyManager, or defaults to *86
     * (the standard voicemail access code for Spectrum Mobile, Verizon, and partner MVNOs).
     */
    fun getVoicemailNumber(context: Context): String {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            val num = tm?.voiceMailNumber
            if (!num.isNullOrBlank()) return num
        } catch (e: Exception) {
            // Permission or carrier exception
        }
        return "*86"
    }

    /**
     * Checks if the given dialed or incoming number corresponds to voicemail.
     */
    fun isVoicemailNumber(context: Context, number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        val clean = number.trim()
        val configuredVm = getVoicemailNumber(context).trim()
        if (clean == configuredVm || clean == "*86" || clean == "1") return true
        if (clean == "901" || clean == "121" || clean == "123" || clean == "171" || clean == "800") return true
        try {
            if (android.telephony.PhoneNumberUtils.isVoiceMailNumber(clean)) return true
        } catch (_: Exception) {}
        return false
    }

    /**
     * Fetches a map of Contact ID -> Nickname from ContactsContract.Data
     */
    fun fetchNicknameMap(context: Context): Map<Long, String> {
        val nicknameMap = mutableMapOf<Long, String>()
        try {
            val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Nickname.NAME
            )
            val selection = "${ContactsContract.Data.MIMETYPE} = ?"
            val selectionArgs = arrayOf(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
            val cursor = context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.Data.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME)
                while (it.moveToNext()) {
                    if (idIdx != -1 && nameIdx != -1) {
                        val cid = it.getLong(idIdx)
                        val nick = it.getString(nameIdx)
                        if (!nick.isNullOrBlank()) {
                            nicknameMap[cid] = nick.trim()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Read contacts permission might not be granted
        }
        return nicknameMap
    }

    fun extractContactFromUri(context: Context, uri: Uri): DeviceContact? {
        var cursor: Cursor? = null
        val nicknameMap = fetchNicknameMap(context)
        return try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.TYPE
            )
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val cidIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val thumbIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)

                val contactId = if (cidIndex != -1) cursor.getLong(cidIndex) else null
                val number = if (numberIndex != -1) cursor.getString(numberIndex) ?: "" else ""
                val fullName = if (nameIndex != -1) cursor.getString(nameIndex) ?: "" else ""
                val nickname = if (contactId != null) nicknameMap[contactId] else null
                val photo = if (photoIndex != -1) cursor.getString(photoIndex) else null
                val thumb = if (thumbIndex != -1) cursor.getString(thumbIndex) else null
                val type = if (typeIndex != -1) cursor.getInt(typeIndex) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE

                val label = when (type) {
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                    else -> "Mobile"
                }

                DeviceContact(
                    name = fullName.ifBlank { "Unknown" },
                    phoneNumber = number,
                    label = label,
                    photoUri = photo ?: thumb,
                    contactId = contactId,
                    nickname = nickname
                )
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            cursor?.close()
        }
    }

    fun lookupContactByNumber(context: Context, phoneNumber: String): DeviceContact? {
        if (phoneNumber.isBlank()) return null
        var cursor: Cursor? = null
        val nicknameMap = fetchNicknameMap(context)
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(
                ContactsContract.PhoneLookup._ID,
                ContactsContract.PhoneLookup.DISPLAY_NAME,
                ContactsContract.PhoneLookup.NUMBER,
                ContactsContract.PhoneLookup.PHOTO_URI,
                ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI,
                ContactsContract.PhoneLookup.TYPE,
                ContactsContract.PhoneLookup.LABEL
            )
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                val thumbIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)
                val typeIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.TYPE)
                val labelIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.LABEL)

                val contactId = if (idIdx != -1) cursor.getLong(idIdx) else null
                val fullName = if (nameIdx != -1) cursor.getString(nameIdx) ?: phoneNumber else phoneNumber
                val nickname = if (contactId != null) nicknameMap[contactId] else null
                val num = if (numIdx != -1) cursor.getString(numIdx) ?: phoneNumber else phoneNumber
                val photo = if (photoIdx != -1) cursor.getString(photoIdx) else null
                val thumb = if (thumbIdx != -1) cursor.getString(thumbIdx) else null
                val type = if (typeIdx != -1) cursor.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                val customLabel = if (labelIdx != -1) cursor.getString(labelIdx) else null

                val label = when (type) {
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
                    ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> "Work Mobile"
                    ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "Other"
                    ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> customLabel?.ifBlank { "Custom" } ?: "Custom"
                    else -> ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.resources, type, customLabel)?.toString()?.ifBlank { "Mobile" } ?: "Mobile"
                }

                // Query all phone numbers for this contact to identify the specific number & label that matched the incoming call
                val phoneNumbers = mutableListOf<ContactPhoneNumber>()
                var matchedSpecificNumber = phoneNumber
                var matchedSpecificLabel = label
                if (contactId != null) {
                    try {
                        val pUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                        val pProj = arrayOf(
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.TYPE,
                            ContactsContract.CommonDataKinds.Phone.LABEL
                        )
                        context.contentResolver.query(
                            pUri,
                            pProj,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId.toString()),
                            null
                        )?.use { pCursor ->
                            val pNumIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val pTypeIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                            val pLabelIdx = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                            while (pCursor.moveToNext()) {
                                val pn = if (pNumIdx != -1) pCursor.getString(pNumIdx) ?: "" else ""
                                val pt = if (pTypeIdx != -1) pCursor.getInt(pTypeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                                val pl = if (pLabelIdx != -1) pCursor.getString(pLabelIdx) else null
                                val itemLabel = when (pt) {
                                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
                                    ContactsContract.CommonDataKinds.Phone.TYPE_MAIN -> "Main"
                                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE -> "Work Mobile"
                                    ContactsContract.CommonDataKinds.Phone.TYPE_OTHER -> "Other"
                                    ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM -> pl?.ifBlank { "Custom" } ?: "Custom"
                                    else -> ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.resources, pt, pl)?.toString()?.ifBlank { "Mobile" } ?: "Mobile"
                                }
                                if (pn.isNotBlank()) {
                                    phoneNumbers.add(ContactPhoneNumber(pn, itemLabel))
                                    if (isSamePhoneNumber(pn, phoneNumber)) {
                                        matchedSpecificNumber = pn
                                        matchedSpecificLabel = itemLabel
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                if (phoneNumbers.isEmpty()) {
                    phoneNumbers.add(ContactPhoneNumber(matchedSpecificNumber, matchedSpecificLabel))
                }

                DeviceContact(
                    name = fullName,
                    phoneNumber = matchedSpecificNumber,
                    label = matchedSpecificLabel,
                    photoUri = photo ?: thumb,
                    contactId = contactId,
                    nickname = nickname,
                    phoneNumbers = phoneNumbers
                )
            } else null
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            cursor?.close()
        }
    }

    /**
     * Reads all starred/favorite contacts from device Contacts database
     * Using nickname if available, else full display name.
     */
    fun fetchStarredContacts(context: Context): List<DeviceContact> {
        val nicknameMap = fetchNicknameMap(context)
        val contactMap = linkedMapOf<String, DeviceContactAccumulator>()
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.STARRED,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.STARRED} = 1"
            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.let {
                val cidIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val thumbIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val priIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)
                val supIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY)

                while (it.moveToNext()) {
                    val contactId = if (cidIdx != -1) it.getLong(cidIdx) else null
                    val fullName = if (nameIdx != -1) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numIdx != -1) it.getString(numIdx) ?: "" else ""
                    val cleanNum = number.replace(Regex("[^0-9+]"), "")
                    if (cleanNum.isEmpty()) continue

                    val type = if (typeIdx != -1) it.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    val label = when (type) {
                        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                        else -> "Mobile"
                    }
                    val photo = if (photoIdx != -1) it.getString(photoIdx) else null
                    val thumb = if (thumbIdx != -1) it.getString(thumbIdx) else null
                    val isPrimary = (priIdx != -1 && it.getInt(priIdx) > 0) || (supIdx != -1 && it.getInt(supIdx) > 0)

                    val key = contactId?.toString() ?: fullName.trim().lowercase()
                    val accumulator = contactMap.getOrPut(key) {
                        val nickname = if (contactId != null) nicknameMap[contactId] else null
                        DeviceContactAccumulator(
                            name = fullName,
                            photoUri = photo ?: thumb,
                            contactId = contactId,
                            nickname = nickname,
                            isStarred = true
                        )
                    }
                    accumulator.addNumber(number, label, isPrimary)
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return contactMap.values.map { it.toDeviceContact() }
    }

    /**
     * Updates the STARRED flag in Android's Contacts Provider for a contact by phone number
     */
    fun setContactStarred(context: Context, phoneNumber: String, starred: Boolean): Boolean {
        var contactId: Long? = null
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    if (idIdx != -1) contactId = cursor.getLong(idIdx)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (contactId != null) {
            return try {
                val values = android.content.ContentValues().apply {
                    put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
                }
                val contactUri = android.content.ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId!!)
                val count = context.contentResolver.update(contactUri, values, null, null)
                count > 0
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

    /**
     * Updates or creates a Nickname record for a contact in ContactsContract.Data
     */
    fun updateContactNickname(context: Context, phoneNumber: String, newNickname: String, knownContactId: Long? = null): Boolean {
        var contactId: Long? = knownContactId
        if (contactId == null) {
            try {
                val uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(phoneNumber)
                )
                context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                        if (idIdx != -1) contactId = cursor.getLong(idIdx)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (contactId != null) {
            return try {
                val dataUri = ContactsContract.Data.CONTENT_URI
                val cursor = context.contentResolver.query(
                    dataUri,
                    arrayOf(ContactsContract.Data._ID),
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE),
                    null
                )
                val dataId = cursor?.use {
                    if (it.moveToFirst()) it.getLong(it.getColumnIndexOrThrow(ContactsContract.Data._ID)) else null
                }

                if (dataId != null) {
                    val values = android.content.ContentValues().apply {
                        put(ContactsContract.CommonDataKinds.Nickname.NAME, newNickname)
                    }
                    context.contentResolver.update(
                        android.content.ContentUris.withAppendedId(dataUri, dataId),
                        values,
                        null,
                        null
                    )
                    true
                } else if (newNickname.isNotBlank()) {
                    val rawCursor = context.contentResolver.query(
                        ContactsContract.RawContacts.CONTENT_URI,
                        arrayOf(ContactsContract.RawContacts._ID),
                        "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                        arrayOf(contactId.toString()),
                        null
                    )
                    val rawContactId = rawCursor?.use {
                        if (it.moveToFirst()) it.getLong(it.getColumnIndexOrThrow(ContactsContract.RawContacts._ID)) else null
                    }
                    if (rawContactId != null) {
                        val values = android.content.ContentValues().apply {
                            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                            put(ContactsContract.CommonDataKinds.Nickname.NAME, newNickname)
                        }
                        context.contentResolver.insert(dataUri, values)
                        true
                    } else false
                } else false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        return false
    }

    fun updateContactDetails(
        context: Context,
        oldPhoneNumber: String,
        newName: String,
        newPhoneNumber: String,
        newLabel: String,
        newNickname: String?
    ): Boolean {
        var contactId: Long? = null
        try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(oldPhoneNumber)
            )
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                    if (idIdx != -1) contactId = cursor.getLong(idIdx)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (contactId != null) {
            try {
                val values = android.content.ContentValues().apply {
                    put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newName)
                }
                context.contentResolver.update(
                    ContactsContract.Data.CONTENT_URI,
                    values,
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                )

                val phoneValues = android.content.ContentValues().apply {
                    put(ContactsContract.CommonDataKinds.Phone.NUMBER, newPhoneNumber)
                    put(ContactsContract.CommonDataKinds.Phone.LABEL, newLabel)
                }
                context.contentResolver.update(
                    ContactsContract.Data.CONTENT_URI,
                    phoneValues,
                    "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                    arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                )

                if (newNickname != null) {
                    updateContactNickname(context, newPhoneNumber, newNickname)
                }
                return true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    /**
     * Sets a phone number as the default primary number for a contact in Android's Contacts Provider.
     * Updates IS_PRIMARY and IS_SUPER_PRIMARY on the selected number row and clears them on other rows,
     * allowing immediate synchronization with Android contacts and Google Contacts.
     */
    fun setDefaultPhoneNumber(
        context: Context,
        contactId: Long?,
        targetPhoneNumber: String
    ): Boolean {
        var targetContactId = contactId
        if (targetContactId == null || targetContactId <= 0L) {
            try {
                val uri = Uri.withAppendedPath(
                    ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(targetPhoneNumber)
                )
                context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                        if (idIdx != -1) targetContactId = cursor.getLong(idIdx)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (targetContactId == null || targetContactId <= 0L) {
            return false
        }

        try {
            val projection = arrayOf(
                ContactsContract.Data._ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val cursor = context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                projection,
                "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(targetContactId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                null
            )

            val ops = ArrayList<android.content.ContentProviderOperation>()
            cursor?.use {
                val idIdx = it.getColumnIndex(ContactsContract.Data._ID)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val dataId = if (idIdx != -1) it.getLong(idIdx) else continue
                    val num = if (numIdx != -1) it.getString(numIdx) ?: "" else ""
                    val isMatch = isSamePhoneNumber(num, targetPhoneNumber, context)

                    ops.add(
                        android.content.ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                            .withSelection("${ContactsContract.Data._ID} = ?", arrayOf(dataId.toString()))
                            .withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, if (isMatch) 1 else 0)
                            .withValue(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY, if (isMatch) 1 else 0)
                            .build()
                    )
                }
            }

            if (ops.isNotEmpty()) {
                context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun fetchDeviceContacts(context: Context, nicknameMap: Map<Long, String>? = null): List<DeviceContact> {
        val effectiveNicknameMap = nicknameMap ?: fetchNicknameMap(context)
        val contactsMap = linkedMapOf<String, DeviceContactAccumulator>()
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY
            )
            cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.let {
                val cidIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val typeIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
                val photoIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                val thumbIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                val priIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY)
                val supIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY)

                while (it.moveToNext()) {
                    val contactId = if (cidIdx != -1) it.getLong(cidIdx) else null
                    val fullName = if (nameIdx != -1) it.getString(nameIdx) ?: "Unknown" else "Unknown"
                    val number = if (numIdx != -1) it.getString(numIdx) ?: "" else ""
                    val cleanNum = number.replace(Regex("[^0-9+]"), "")
                    if (cleanNum.isEmpty()) continue

                    val type = if (typeIdx != -1) it.getInt(typeIdx) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    val label = when (type) {
                        ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                        ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                        else -> "Mobile"
                    }
                    val photo = if (photoIdx != -1) it.getString(photoIdx) else null
                    val thumb = if (thumbIdx != -1) it.getString(thumbIdx) else null
                    val isPrimary = (priIdx != -1 && it.getInt(priIdx) > 0) || (supIdx != -1 && it.getInt(supIdx) > 0)

                    val key = contactId?.toString() ?: fullName.trim().lowercase()
                    val accumulator = contactsMap.getOrPut(key) {
                        val nickname = if (contactId != null) effectiveNicknameMap[contactId] else null
                        DeviceContactAccumulator(
                            name = fullName,
                            photoUri = photo ?: thumb,
                            contactId = contactId,
                            nickname = nickname
                        )
                    }
                    accumulator.addNumber(number, label, isPrimary)
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted; return empty list
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return contactsMap.values.map { it.toDeviceContact() }
    }

    /**
     * Fetches call history from CallLog.Calls for a specific list of phone numbers or contact name.
     */
    fun fetchDeviceCallHistoryForContact(context: Context, numbers: List<String>, name: String? = null): List<com.example.data.RecentCall> {
        val result = mutableListOf<com.example.data.RecentCall>()
        try {
            val normalizedNumbers = numbers.map { it.filter { c -> c.isDigit() }.takeLast(10) }.filter { it.isNotBlank() }.toSet()
            if (normalizedNumbers.isEmpty() && name.isNullOrBlank()) return emptyList()

            val projection = arrayOf(
                android.provider.CallLog.Calls.NUMBER,
                android.provider.CallLog.Calls.CACHED_NAME,
                android.provider.CallLog.Calls.TYPE,
                android.provider.CallLog.Calls.DATE,
                android.provider.CallLog.Calls.DURATION,
                android.provider.CallLog.Calls.PHONE_ACCOUNT_ID
            )
            val selectionClauses = mutableListOf<String>()
            val selectionArgs = mutableListOf<String>()

            if (numbers.isNotEmpty()) {
                val cleanNumbers = numbers.filter { it.isNotBlank() }.distinct()
                if (cleanNumbers.isNotEmpty()) {
                    val placeholders = cleanNumbers.map { "?" }.joinToString(",")
                    selectionClauses.add("${android.provider.CallLog.Calls.NUMBER} IN ($placeholders)")
                    selectionArgs.addAll(cleanNumbers)

                    val last10List = cleanNumbers.map { it.filter { c -> c.isDigit() }.takeLast(10) }.filter { it.length >= 7 }.distinct()
                    for (last10 in last10List) {
                        selectionClauses.add("${android.provider.CallLog.Calls.NUMBER} LIKE ?")
                        selectionArgs.add("%$last10")
                    }
                }
            }

            if (!name.isNullOrBlank()) {
                selectionClauses.add("${android.provider.CallLog.Calls.CACHED_NAME} = ?")
                selectionArgs.add(name)
            }

            val selection = if (selectionClauses.isNotEmpty()) selectionClauses.joinToString(" OR ") else null
            val selectionArgsArray = if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null

            val cursor = context.contentResolver.query(
                android.provider.CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgsArray,
                "${android.provider.CallLog.Calls.DATE} DESC LIMIT 50"
            )
            cursor?.use {
                val numIdx = it.getColumnIndex(android.provider.CallLog.Calls.NUMBER)
                val nameIdx = it.getColumnIndex(android.provider.CallLog.Calls.CACHED_NAME)
                val typeIdx = it.getColumnIndex(android.provider.CallLog.Calls.TYPE)
                val dateIdx = it.getColumnIndex(android.provider.CallLog.Calls.DATE)
                val durIdx = it.getColumnIndex(android.provider.CallLog.Calls.DURATION)
                val accountIdIdx = it.getColumnIndex(android.provider.CallLog.Calls.PHONE_ACCOUNT_ID)

                var matchedCount = 0
                while (it.moveToNext() && matchedCount < 50) {
                    val rawNumber = if (numIdx != -1) it.getString(numIdx) ?: "" else ""
                    val cachedName = if (nameIdx != -1) it.getString(nameIdx) ?: "" else ""
                    val norm = rawNumber.filter { c -> c.isDigit() }.takeLast(10)

                    val matchesNumber = norm.isNotBlank() && normalizedNumbers.contains(norm)
                    val matchesName = !name.isNullOrBlank() && cachedName.equals(name, ignoreCase = true)

                    if (matchesNumber || matchesName) {
                        val type = if (typeIdx != -1) it.getInt(typeIdx) else 1
                        val date = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()
                        val duration = if (durIdx != -1) it.getLong(durIdx) else 0L
                        val accountId = if (accountIdIdx != -1) it.getString(accountIdIdx) else null
                        val simSlot = com.example.telecom.SimHelper.resolveSimSlot(context, accountId = accountId)

                        result.add(
                            com.example.data.RecentCall(
                                phoneNumber = rawNumber,
                                callerName = if (cachedName.isNotBlank()) cachedName else (name ?: rawNumber),
                                callType = type,
                                timestamp = date,
                                durationSeconds = duration,
                                simSlot = simSlot
                            )
                        )
                        matchedCount++
                    }
                }
            }
        } catch (_: Exception) {
            // Permission not granted or query failed
        }
        return result
    }

    /**
     * Fetches all device call history from system CallLog.Calls.
     */
    fun fetchDeviceCallHistory(context: Context, limit: Int = 100): List<RecentCall> {
        val result = mutableListOf<RecentCall>()
        try {
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.PHONE_ACCOUNT_ID
            )
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )
            cursor?.use {
                val idIdx = it.getColumnIndex(CallLog.Calls._ID)
                val numIdx = it.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIdx = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val typeIdx = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIdx = it.getColumnIndex(CallLog.Calls.DATE)
                val durIdx = it.getColumnIndex(CallLog.Calls.DURATION)
                val accountIdIdx = it.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

                var count = 0
                while (it.moveToNext() && count < limit) {
                    val rawId = if (idIdx != -1) it.getLong(idIdx) else 0L
                    val rawNumber = if (numIdx != -1) it.getString(numIdx) ?: "" else ""
                    val cachedName = if (nameIdx != -1) it.getString(nameIdx) ?: "" else ""
                    val type = if (typeIdx != -1) it.getInt(typeIdx) else 1
                    val date = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()
                    val duration = if (durIdx != -1) it.getLong(durIdx) else 0L
                    val accountId = if (accountIdIdx != -1) it.getString(accountIdIdx) else null
                    val simSlot = com.example.telecom.SimHelper.resolveSimSlot(context, accountId = accountId)

                    if (rawNumber.isNotBlank()) {
                        val sysId = if (rawId > 0L) -rawId else -Math.abs("${rawNumber}_${date}_$count".hashCode().toLong()).coerceAtLeast(1L)
                        result.add(
                            RecentCall(
                                id = sysId,
                                phoneNumber = rawNumber,
                                callerName = cachedName.ifBlank { null },
                                callType = type,
                                timestamp = date,
                                durationSeconds = duration,
                                simSlot = simSlot
                            )
                        )
                        count++
                    }
                }
            }
        } catch (_: Exception) {
            // Permission not granted or query failed
        }
        return result
    }

    /**
     * Deletes a specific call log entry from Android system CallLog.Calls
     */
    fun deleteDeviceCallLogEntry(context: Context, phoneNumber: String?, timestamp: Long?, sysCallId: Long? = null): Boolean {
        try {
            val resolver = context.contentResolver
            var deleted = 0
            if (sysCallId != null && sysCallId > 0L) {
                try {
                    val uri = ContentUris.withAppendedId(CallLog.Calls.CONTENT_URI, sysCallId)
                    deleted += resolver.delete(uri, null, null)
                } catch (_: Exception) {}
            }
            if (deleted == 0 && timestamp != null && timestamp > 0L) {
                try {
                    val timeWhere = "${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.DATE} <= ?"
                    val timeArgs = arrayOf((timestamp - 10000L).toString(), (timestamp + 10000L).toString())
                    deleted += resolver.delete(CallLog.Calls.CONTENT_URI, timeWhere, timeArgs)
                } catch (_: Exception) {}
            }
            if (deleted == 0 && !phoneNumber.isNullOrBlank()) {
                val clean = phoneNumber.trim()
                val digits = clean.filter { it.isDigit() }
                val last10 = digits.takeLast(10)
                try {
                    val where = "${CallLog.Calls.NUMBER} = ? OR ${CallLog.Calls.NUMBER} = ? OR ${CallLog.Calls.NUMBER} LIKE ?"
                    if (timestamp != null && timestamp > 0L) {
                        val whereTime = "($where) AND ${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.DATE} <= ?"
                        val argsTime = arrayOf(clean, digits, "%$last10", (timestamp - 30000L).toString(), (timestamp + 30000L).toString())
                        deleted += resolver.delete(CallLog.Calls.CONTENT_URI, whereTime, argsTime)
                    } else {
                        val args = arrayOf(clean, digits, "%$last10")
                        deleted += resolver.delete(CallLog.Calls.CONTENT_URI, where, args)
                    }
                } catch (_: Exception) {}
            }
            return deleted > 0
        } catch (e: Exception) {
            android.util.Log.e("ContactHelper", "Failed to delete call log entry from device", e)
            return false
        }
    }

    /**
     * Deletes all call log entries for a given phone number from Android system CallLog.Calls
     */
    fun deleteDeviceCallLogsForNumber(context: Context, phoneNumber: String): Int {
        try {
            val clean = phoneNumber.trim()
            val digits = clean.filter { it.isDigit() }
            val last10 = digits.takeLast(10)
            val resolver = context.contentResolver
            val where = "${CallLog.Calls.NUMBER} = ? OR ${CallLog.Calls.NUMBER} = ? OR ${CallLog.Calls.NUMBER} LIKE ?"
            val args = arrayOf(clean, digits, "%$last10")
            return resolver.delete(CallLog.Calls.CONTENT_URI, where, args)
        } catch (e: Exception) {
            android.util.Log.e("ContactHelper", "Failed to delete call logs for number from device", e)
            return 0
        }
    }
}

private class DeviceContactAccumulator(
    val name: String,
    val photoUri: String?,
    val contactId: Long?,
    val nickname: String?,
    val isStarred: Boolean = false
) {
    private val numbers = mutableListOf<ContactPhoneNumber>()
    private val seen = mutableSetOf<String>()
    private var defaultNumberItem: ContactPhoneNumber? = null

    fun addNumber(number: String, label: String, isPrimary: Boolean = false) {
        val clean = number.replace(Regex("[^0-9+]"), "")
        if (clean.isNotEmpty() && seen.add(clean)) {
            val item = ContactPhoneNumber(number, label)
            numbers.add(item)
            if (isPrimary || defaultNumberItem == null) {
                defaultNumberItem = item
            }
        }
    }

    fun toDeviceContact(): DeviceContact {
        val primary = defaultNumberItem ?: numbers.firstOrNull()
        val orderedList = if (primary != null) {
            listOf(primary) + numbers.filter { it != primary }
        } else {
            numbers.toList()
        }
        return DeviceContact(
            name = name,
            phoneNumber = primary?.number ?: "",
            label = primary?.label ?: "Mobile",
            photoUri = photoUri,
            contactId = contactId,
            nickname = nickname,
            phoneNumbers = orderedList,
            isStarred = isStarred
        )
    }
}
