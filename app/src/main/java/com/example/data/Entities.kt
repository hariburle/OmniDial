package com.example.data

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.util.PhoneNumberNormalizer

@Immutable
@Entity(
    tableName = "caller_rules",
    indices = [Index(value = ["phoneNumberPattern"])]
)
data class CallerRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumberPattern: String,
    val isEnabled: Boolean = true,
    val autoAnswer: Boolean = true,
    val answerDelaySec: Int = 1,
    val dtmfSequence: String = "",
    val dtmfDelayMs: Long = 800L,
    val sendSms: Boolean = false,
    val smsMessage: String = "",
    val autoHangup: Boolean = false,
    val hangupDelaySec: Int = 2,
    val autoSpeakerphone: Boolean = false,
    val autoMuteMic: Boolean = false,
    val requiredWifiSsid: String = "",
    val requiredBluetoothDevice: String = ""
)

@Immutable
@Entity(tableName = "automation_logs")
data class AutomationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val phoneNumber: String,
    val ruleName: String,
    val actionsSummary: String,
    val status: String // "SUCCESS", "EXECUTING", "FAILED"
)

@Immutable
@Entity(
    tableName = "recent_calls",
    indices = [
        Index(value = ["normalized_number"]),
        Index(value = ["phoneNumber"]),
        Index(value = ["timestamp"])
    ]
)
data class RecentCall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phoneNumber: String,
    val callerName: String? = null,
    val photoUri: String? = null,
    val callType: Int, // 1: Incoming, 2: Outgoing, 3: Missed
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val ruleMatched: String? = null,
    val simSlot: Int = 1,
    val isSpam: Boolean = false,
    val note: String? = null,
    val reminderTime: Long? = null,
    val callReason: String? = null,
    val communityTag: String? = null,
    @ColumnInfo(name = "normalized_number")
    val normalizedNumber: String = PhoneNumberNormalizer.toE164(phoneNumber)
)

@Immutable
@Entity(
    tableName = "favorite_contacts",
    indices = [Index(value = ["normalized_number"])]
)
data class FavoriteContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nickname: String? = null,
    val phoneNumber: String,
    val label: String = "Mobile",
    val avatarColor: Long = 0xFF2563EB,
    val photoUri: String? = null,
    val speedDialSlot: Int? = null, // 1 to 9
    val sortOrder: Int = 0,
    @ColumnInfo(name = "normalized_number")
    val normalizedNumber: String = PhoneNumberNormalizer.toE164(phoneNumber)
)

@Immutable
@Entity(
    tableName = "offline_spam_numbers",
    indices = [Index(value = ["normalized_number"])]
)
data class SpamNumber(
    @PrimaryKey val phoneNumber: String,
    val label: String = "Suspected Spam",
    val reportCount: Int = 1,
    val isBlocked: Boolean = true,
    @ColumnInfo(name = "normalized_number")
    val normalizedNumber: String = PhoneNumberNormalizer.toE164(phoneNumber)
)

@Immutable
@Entity(tableName = "ignored_contacts")
data class IgnoredContact(
    @PrimaryKey val phoneNumber: String,
    val name: String = "",
    val category: String = "",
    val tag: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "local_contacts")
data class LocalContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val label: String = "Mobile",
    val nickname: String? = null,
    val photoUri: String? = null
)

/**
 * Stores contact-specific cellular SIM preference.
 * preferredSimSlot:
 *   0: System Default (default choice, follows system/active slot)
 *  -1: Ask & Learn (prompts user upon calling and learns choice)
 *  -2: International (routes overseas/international calls to roaming or intl SIM)
 *   1: SIM 1 (Slot 0, fixed or learned)
 *   2: SIM 2 (Slot 1, fixed or learned)
 */
@Immutable
@Entity(
    tableName = "contact_sim_preferences",
    indices = [Index(value = ["normalized_number"])]
)
data class ContactSimPreference(
    @PrimaryKey
    @ColumnInfo(name = "normalized_number")
    val normalizedNumber: String,
    @ColumnInfo(name = "preferred_sim_slot")
    val preferredSimSlot: Int
)


