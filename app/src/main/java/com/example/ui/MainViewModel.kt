package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.telecom.Call
import android.telecom.TelecomManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.AutomationLog
import com.example.data.CallerRule
import com.example.data.FavoriteContact
import com.example.data.IgnoredContact
import com.example.data.LocalContact
import com.example.data.RecentCall
import com.example.telecom.ActiveCallInfo
import com.example.telecom.AutomationStep
import com.example.telecom.CallManager
import com.example.telecom.RoleHelper
import com.example.telecom.SimHelper
import com.example.telecom.SimInfo
import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Collections
import kotlin.math.abs

data class CloudContactConfirmation(
    val title: String,
    val message: String,
    val contactName: String,
    val confirmButtonText: String = "Update Google Contacts",
    val secondaryButtonText: String? = null,
    val dismissButtonText: String = "Cancel",
    val onConfirmCloudAction: () -> Unit,
    val onSecondaryAction: (() -> Unit)? = null,
    val onDismissOrCancel: () -> Unit = {}
)

data class CallMethodChoicePrompt(
    val number: String,
    val contactName: String?,
    val reason: String? = null,
    val isLearnMode: Boolean = false
)

class MainViewModel(
    private val repository: AppRepository,
    private val appContext: Context
) : ViewModel() {

    // Preferences for Theme and WhatsApp calls
    private val prefs = appContext.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode).apply()
    }

    private val _whatsAppCallMode = MutableStateFlow(prefs.getString("whatsapp_call_mode", "ask_learn") ?: "ask_learn")
    val whatsAppCallMode: StateFlow<String> = _whatsAppCallMode.asStateFlow()

    fun setWhatsAppCallMode(mode: String) {
        _whatsAppCallMode.value = mode
        prefs.edit().putString("whatsapp_call_mode", mode).apply()
        appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putString("whatsapp_call_mode", mode).apply()
    }

    // Learned Calling Choices for Contacts (Map of normalized number -> "cellular" | "whatsapp")
    private val _learnedCallModes = MutableStateFlow<Map<String, String>>(loadLearnedCallModes())
    val learnedCallModes: StateFlow<Map<String, String>> = _learnedCallModes.asStateFlow()

    private val _localBackups = MutableStateFlow<List<java.io.File>>(emptyList())
    val localBackups: StateFlow<List<java.io.File>> = _localBackups.asStateFlow()

    fun refreshLocalBackups() {
        _localBackups.value = com.example.util.BackupManager.listLocalBackups(appContext)
    }

    private fun loadLearnedCallModes(): Map<String, String> {
        val rawSet = (prefs.getStringSet("whatsapp_learned_choices", emptySet()) ?: emptySet()) +
                     (prefs.getStringSet("learned_call_modes", emptySet()) ?: emptySet())
        val map = mutableMapOf<String, String>()
        rawSet.forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                map[parts[0]] = parts[1]
            }
        }
        return map
    }

    fun saveLearnedCallMode(phoneNumber: String, mode: String) {
        val digits = phoneNumber.filter { it.isDigit() }.takeLast(10)
        val clean = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (digits.isBlank() && clean.isBlank()) return
        val current = _learnedCallModes.value.toMutableMap()
        if (digits.isNotBlank()) current[digits] = mode
        if (clean.isNotBlank()) current[clean] = mode
        _learnedCallModes.value = current

        val set = current.map { "${it.key}:${it.value}" }.toSet()
        prefs.edit()
            .putStringSet("whatsapp_learned_choices", HashSet(set))
            .putStringSet("learned_call_modes", HashSet(set))
            .apply()
        // Also mirror to app_prefs for cross-process / service consistency
        appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
            .putStringSet("whatsapp_learned_choices", HashSet(set))
            .putStringSet("learned_call_modes", HashSet(set))
            .putString("whatsapp_call_mode", _whatsAppCallMode.value)
            .apply()
    }

    fun resetWhatsAppChoices() {
        _learnedCallModes.value = emptyMap()
        prefs.edit()
            .remove("whatsapp_learned_choices")
            .remove("learned_call_modes")
            .apply()
        appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit()
            .remove("whatsapp_learned_choices")
            .remove("learned_call_modes")
            .apply()
    }

    // Call method selection dialog state
    private val _pendingCallMethodChoice = MutableStateFlow<CallMethodChoicePrompt?>(null)
    val pendingCallMethodChoice: StateFlow<CallMethodChoicePrompt?> = _pendingCallMethodChoice.asStateFlow()

    fun dismissCallMethodChoice() {
        _pendingCallMethodChoice.value = null
    }

    fun chooseCallMethod(context: Context, method: String, remember: Boolean) {
        val prompt = _pendingCallMethodChoice.value ?: return
        _pendingCallMethodChoice.value = null

        if (remember || _whatsAppCallMode.value == "ask_learn") {
            saveLearnedCallMode(prompt.number, method)
        }

        if (method == "whatsapp") {
            placeWhatsAppCall(context, prompt.number)
        } else {
            placeCall(context, prompt.number, prompt.reason)
        }
    }

    // Explicit Not-Spam Whitelist (numbers explicitly unmarked as spam)
    private val _notSpamWhitelist = MutableStateFlow<Set<String>>(
        prefs.getStringSet("not_spam_whitelist", emptySet()) ?: emptySet()
    )
    val notSpamWhitelist: StateFlow<Set<String>> = _notSpamWhitelist.asStateFlow()

    // Accidental touch protection: Ask confirmation before calling favorites
    private val _confirmFavoritesCall = MutableStateFlow(prefs.getBoolean("confirm_fav_calls", true))
    val confirmFavoritesCall: StateFlow<Boolean> = _confirmFavoritesCall.asStateFlow()

    fun setConfirmFavoritesCall(enabled: Boolean) {
        _confirmFavoritesCall.value = enabled
        prefs.edit().putBoolean("confirm_fav_calls", enabled).apply()
    }

    // Speed Dial Confirmation: Ask confirmation before speed-dialing
    private val _confirmSpeedDialCall = MutableStateFlow(prefs.getBoolean("confirm_speed_dial_call", true))
    val confirmSpeedDialCall: StateFlow<Boolean> = _confirmSpeedDialCall.asStateFlow()

    fun setConfirmSpeedDialCall(enabled: Boolean) {
        _confirmSpeedDialCall.value = enabled
        prefs.edit().putBoolean("confirm_speed_dial_call", enabled).apply()
    }

    // Speed Dial Assignment: Ask to assign for unassigned numbers
    private val _askToAssignUnassignedSpeedDial = MutableStateFlow(prefs.getBoolean("ask_assign_unassigned_speed_dial", true))
    val askToAssignUnassignedSpeedDial: StateFlow<Boolean> = _askToAssignUnassignedSpeedDial.asStateFlow()

    fun setAskToAssignUnassignedSpeedDial(enabled: Boolean) {
        _askToAssignUnassignedSpeedDial.value = enabled
        prefs.edit().putBoolean("ask_assign_unassigned_speed_dial", enabled).apply()
    }

    // Speed Dial Keypad Display: "speed_dial_above" (fav and T9 stacked on right), "t9_only", "speed_dial_only"
    private val _speedDialKeypadDisplay = MutableStateFlow(
        prefs.getString("speed_dial_keypad_display", "speed_dial_above") ?: "speed_dial_above"
    )
    val speedDialKeypadDisplay: StateFlow<String> = _speedDialKeypadDisplay.asStateFlow()

    fun setSpeedDialKeypadDisplay(mode: String) {
        _speedDialKeypadDisplay.value = mode
        prefs.edit().putString("speed_dial_keypad_display", mode).apply()
    }

    // Show Dialer Quick Action Buttons (SMS, WhatsApp Chat, Secondary Call)
    private val _showDialerQuickActions = MutableStateFlow(prefs.getBoolean("show_dialer_quick_actions", true))
    val showDialerQuickActions: StateFlow<Boolean> = _showDialerQuickActions.asStateFlow()

    fun setShowDialerQuickActions(enabled: Boolean) {
        _showDialerQuickActions.value = enabled
        prefs.edit().putBoolean("show_dialer_quick_actions", enabled).apply()
    }

    // Default start tab: 0 (Favorites)
    private val _defaultStartTab = MutableStateFlow(prefs.getInt("default_start_tab", 0))
    val defaultStartTab: StateFlow<Int> = _defaultStartTab.asStateFlow()

    fun setDefaultStartTab(tabIndex: Int) {
        _defaultStartTab.value = tabIndex
        prefs.edit().putInt("default_start_tab", tabIndex).apply()
    }

    // Incoming Call Answering Style ("swipe_slider", "swipe_up", "button_tap")
    private val _callAnswerStyle = MutableStateFlow(prefs.getString("call_answer_style", "swipe_slider") ?: "swipe_slider")
    val callAnswerStyle: StateFlow<String> = _callAnswerStyle.asStateFlow()

    fun setCallAnswerStyle(style: String) {
        _callAnswerStyle.value = style
        prefs.edit().putString("call_answer_style", style).apply()
    }

    // Favorite Contact Card Style ("bento", "quick_action", "material_you")
    private val _favoriteCardStyle = MutableStateFlow(prefs.getString("favorite_card_style", "bento") ?: "bento")
    val favoriteCardStyle: StateFlow<String> = _favoriteCardStyle.asStateFlow()

    fun setFavoriteCardStyle(style: String) {
        _favoriteCardStyle.value = style
        prefs.edit().putString("favorite_card_style", style).apply()
    }

    // Swipe to Switch Main Panels setting (Default: true)
    private val _swipeToSwitchPanels = MutableStateFlow(prefs.getBoolean("swipe_to_switch_panels", true))
    val swipeToSwitchPanels: StateFlow<Boolean> = _swipeToSwitchPanels.asStateFlow()

    fun setSwipeToSwitchPanels(enabled: Boolean) {
        _swipeToSwitchPanels.value = enabled
        prefs.edit().putBoolean("swipe_to_switch_panels", enabled).apply()
    }

    // Navigation Bar Style ("full", "compact", "indicator")
    private val _navBarStyle = MutableStateFlow(prefs.getString("nav_bar_style", "full") ?: "full")
    val navBarStyle: StateFlow<String> = _navBarStyle.asStateFlow()

    fun setNavBarStyle(style: String) {
        _navBarStyle.value = style
        prefs.edit().putString("nav_bar_style", style).apply()
    }

    fun isNumberWhitelistedNotSpam(phoneNumber: String): Boolean {
        val clean = phoneNumber.filter { it.isDigit() }.takeLast(10)
        return _notSpamWhitelist.value.any { wl ->
            wl == phoneNumber || (clean.length >= 7 && wl.filter { it.isDigit() }.takeLast(10) == clean)
        }
    }

    fun isSpamNumber(phoneNumber: String): Boolean {
        if (isNumberWhitelistedNotSpam(phoneNumber)) return false
        val clean = phoneNumber.filter { it.isDigit() }.takeLast(10)
        return spamNumbers.value.any { sp ->
            val spClean = sp.phoneNumber.filter { it.isDigit() }.takeLast(10)
            sp.phoneNumber == phoneNumber || (clean.length >= 7 && spClean == clean)
        }
    }

    // Dialer Input
    private val _dialerNumber = MutableStateFlow("")
    val dialerNumber: StateFlow<String> = _dialerNumber.asStateFlow()

    // Default Dialer Status
    private val _isDefaultDialer = MutableStateFlow(RoleHelper.isDefaultDialer(appContext))
    val isDefaultDialer: StateFlow<Boolean> = _isDefaultDialer.asStateFlow()

    // Call Redirection Role Status (for Bluetooth/Car call interception)
    private val _isCallRedirectionRoleHeld = MutableStateFlow(RoleHelper.isCallRedirectionRoleHeld(appContext))
    val isCallRedirectionRoleHeld: StateFlow<Boolean> = _isCallRedirectionRoleHeld.asStateFlow()

    // Confirmation dialog before any changes to Google Account Contacts
    private val _pendingCloudConfirmation = MutableStateFlow<CloudContactConfirmation?>(null)
    val pendingCloudConfirmation: StateFlow<CloudContactConfirmation?> = _pendingCloudConfirmation.asStateFlow()

    fun clearCloudConfirmation() {
        _pendingCloudConfirmation.value = null
    }

    // Device Contacts Flow & Observer for live synchronization with system contacts app
    private val _deviceContacts = MutableStateFlow<List<DeviceContact>>(emptyList())
    val deviceContacts: StateFlow<List<DeviceContact>> = _deviceContacts.asStateFlow()

    private var contactsObserver: android.database.ContentObserver? = null

    // Active Call forwarded from CallManager
    val activeCall: StateFlow<ActiveCallInfo?> = CallManager.activeCall
    val isMuted: StateFlow<Boolean> = CallManager.isMuted
    val isSpeakerOn: StateFlow<Boolean> = CallManager.isSpeakerOn
    val automationState: StateFlow<AutomationStep?> = CallManager.automationState
    val lastDtmfKey: StateFlow<Char?> = CallManager.lastDtmfKey

    // In-Call keypad toggle state
    private val _showInCallKeypad = MutableStateFlow(false)
    val showInCallKeypad: StateFlow<Boolean> = _showInCallKeypad.asStateFlow()

    // Deep linking & Notification Navigation State
    private val _pendingNavTab = MutableStateFlow<Int?>(null)
    val pendingNavTab: StateFlow<Int?> = _pendingNavTab.asStateFlow()

    private val _pendingHighlightNumber = MutableStateFlow<String?>(null)
    val pendingHighlightNumber: StateFlow<String?> = _pendingHighlightNumber.asStateFlow()

    fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type
        val navTab = intent.getStringExtra("EXTRA_NAV_TAB")
        val navTabIndex = intent.getIntExtra("EXTRA_NAV_TAB_INDEX", -1)
        val highlightNum = intent.getStringExtra("EXTRA_HIGHLIGHT_NUMBER")

        val isDialIntent = ContactHelper.isDialOrTelIntent(intent)
        val extractedNumber = ContactHelper.extractPhoneNumberFromIntent(intent)

        if (navTab == "RECENTS" || 
            navTabIndex == 1 ||
            action == "android.telecom.action.SHOW_MISSED_CALLS_NOTIFICATION" ||
            type == "vnd.android.cursor.dir/calls"
        ) {
            _pendingNavTab.value = 1
        } else if (isDialIntent || !extractedNumber.isNullOrBlank() || navTab == "DIALER" || navTab == "KEYPAD" || navTabIndex == 2) {
            _pendingNavTab.value = 2
        } else if (navTabIndex in 0..4) {
            _pendingNavTab.value = navTabIndex
        }

        if (!extractedNumber.isNullOrBlank()) {
            _dialerNumber.value = extractedNumber
        }

        if (!highlightNum.isNullOrBlank()) {
            _pendingHighlightNumber.value = highlightNum
        }
    }

    fun navigateToKeypad(withNumber: String? = null) {
        if (!withNumber.isNullOrBlank()) {
            _dialerNumber.value = withNumber
        }
        _pendingNavTab.value = 2
    }

    fun clearPendingNavTab() {
        _pendingNavTab.value = null
    }

    fun clearPendingHighlight() {
        _pendingHighlightNumber.value = null
    }

    // Call minimization state (so user can browse the app during active call)
    private val _isCallScreenMinimized = MutableStateFlow(false)
    val isCallScreenMinimized: StateFlow<Boolean> = _isCallScreenMinimized.asStateFlow()

    fun minimizeCall() {
        _isCallScreenMinimized.value = true
    }

    fun maximizeCall() {
        _isCallScreenMinimized.value = false
    }

    // Flip to Shhh (DND status)
    val isFlipToShhhEnabled: StateFlow<Boolean> = com.example.telecom.FlipToShhhManager.isFlipToShhhEnabled
    val isShhhActive: StateFlow<Boolean> = com.example.telecom.FlipToShhhManager.isShhhActive

    fun toggleFlipToShhh() {
        com.example.telecom.FlipToShhhManager.setEnabled(appContext, !isFlipToShhhEnabled.value)
    }

    // Dual-SIM State (Slot Index + 1, e.g., 1 or 2)
    private val _selectedSimSlot = MutableStateFlow(1)
    val selectedSimSlot: StateFlow<Int> = _selectedSimSlot.asStateFlow()

    // Real SIM cards read from device's SubscriptionManager
    private val _activeSims = MutableStateFlow<List<SimInfo>>(emptyList())
    val activeSims: StateFlow<List<SimInfo>> = _activeSims.asStateFlow()

    // Database Flows
    val rules: StateFlow<List<CallerRule>> = repository.allRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _combinedRecentCalls = MutableStateFlow<List<RecentCall>>(emptyList())
    val recentCalls: StateFlow<List<RecentCall>> = _combinedRecentCalls.asStateFlow()

    private var callLogObserver: ContentObserver? = null

    val favorites: StateFlow<List<FavoriteContact>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pre-computed, background-normalized search contacts for instant dialer response without UI frame drops
    val searchContacts: StateFlow<List<DeviceContact>> = combine(_deviceContacts, favorites) { effectiveContacts, favs ->
        fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)
        val favByDigits = favs.associateBy { normDigits(it.phoneNumber) }
        val favByName = favs.associateBy { it.name.trim().lowercase() }
        val list = ArrayList<DeviceContact>(effectiveContacts.size + favs.size)

        for (dc in effectiveContacts) {
            val dcDigits = normDigits(dc.phoneNumber)
            val matchingFav = favByDigits[dcDigits] ?: favByName[dc.name.trim().lowercase()]
            val effectiveNickname = matchingFav?.nickname?.ifBlank { null } ?: dc.nickname?.ifBlank { null }
            if (effectiveNickname != null && effectiveNickname != dc.nickname) {
                list.add(dc.copy(nickname = effectiveNickname))
            } else {
                list.add(dc)
            }
        }
        val knownDigits = HashSet<String>(effectiveContacts.size * 2)
        for (dc in effectiveContacts) {
            val mainDigits = normDigits(dc.phoneNumber)
            if (mainDigits.isNotBlank()) knownDigits.add(mainDigits)
            for (pn in dc.phoneNumbers) {
                val pDigits = normDigits(pn.number)
                if (pDigits.isNotBlank()) knownDigits.add(pDigits)
            }
        }

        for (fav in favs) {
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
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val spamNumbers: StateFlow<List<com.example.data.SpamNumber>> = repository.spamNumbers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ignoredContacts: StateFlow<List<IgnoredContact>> = repository.ignoredContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val automationLogs: StateFlow<List<AutomationLog>> = repository.recentLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshContacts()
        refreshRecentCalls()
        refreshSimCards()
        refreshLocalBackups()
        registerContactsObserver()
        registerCallLogObserver()
        viewModelScope.launch {
            repository.localContacts.collect {
                refreshContacts()
            }
        }
        viewModelScope.launch {
            repository.recentCalls.collect {
                refreshRecentCalls()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            removeSpam("+1 469-731-3343")
            removeSpam("4697313343")
        }
    }

    fun refreshRecentCalls() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)

                val roomCalls = repository.getAllRecentCallsList()
                val systemCalls = ContactHelper.fetchDeviceCallHistory(appContext, limit = 100)

                val merged = mutableListOf<RecentCall>()
                val handledRoomIds = mutableSetOf<Long>()

                systemCalls.forEach { sysCall ->
                    val matchingRoomCall = roomCalls.firstOrNull { roomCall ->
                        val numMatches = ContactHelper.isSamePhoneNumber(sysCall.phoneNumber, roomCall.phoneNumber)
                        if (!numMatches) return@firstOrNull false

                        // Check time window: Account for system CallLog storing call start time while in-app logger might record disconnect time
                        val maxDurMs = Math.max(roomCall.durationSeconds, sysCall.durationSeconds) * 1000L
                        val rawDiff = Math.abs(roomCall.timestamp - sysCall.timestamp)
                        val startTimeDiff = Math.abs((roomCall.timestamp - roomCall.durationSeconds * 1000L) - sysCall.timestamp)
                        val endTimeDiff = Math.abs(roomCall.timestamp - (sysCall.timestamp + sysCall.durationSeconds * 1000L))

                        rawDiff <= (maxDurMs + 25000L) || startTimeDiff < 25000L || endTimeDiff < 25000L
                    }

                    if (matchingRoomCall != null) {
                        handledRoomIds.add(matchingRoomCall.id)
                        merged.add(
                            matchingRoomCall.copy(
                                durationSeconds = if (matchingRoomCall.durationSeconds > 0) matchingRoomCall.durationSeconds else sysCall.durationSeconds,
                                callerName = matchingRoomCall.callerName ?: sysCall.callerName,
                                photoUri = matchingRoomCall.photoUri ?: sysCall.photoUri,
                                timestamp = Math.max(matchingRoomCall.timestamp, sysCall.timestamp)
                            )
                        )
                    } else {
                        merged.add(sysCall)
                    }
                }

                roomCalls.forEach { roomCall ->
                    if (!handledRoomIds.contains(roomCall.id)) {
                        merged.add(roomCall)
                    }
                }

                // Deduplication pass across merged calls (eliminates any remaining close duplicate records)
                merged.sortByDescending { it.timestamp }
                val deduplicated = mutableListOf<RecentCall>()
                for (call in merged) {
                    val existingIdx = deduplicated.indexOfFirst { prev ->
                        val numMatch = ContactHelper.isSamePhoneNumber(call.phoneNumber, prev.phoneNumber)
                        val typeMatch = prev.callType == call.callType || (prev.callType in listOf(1, 3) && call.callType in listOf(1, 3))
                        val timeGap = Math.abs(prev.timestamp - call.timestamp)
                        val maxDur = Math.max(prev.durationSeconds, call.durationSeconds) * 1000L
                        numMatch && typeMatch && (timeGap <= (maxDur + 25000L))
                    }

                    if (existingIdx != -1) {
                        // Merge richer information into existing entry
                        val prev = deduplicated[existingIdx]
                        val enriched = prev.copy(
                            callerName = prev.callerName ?: call.callerName,
                            photoUri = prev.photoUri ?: call.photoUri,
                            durationSeconds = Math.max(prev.durationSeconds, call.durationSeconds),
                            note = prev.note ?: call.note,
                            reminderTime = prev.reminderTime ?: call.reminderTime,
                            ruleMatched = prev.ruleMatched ?: call.ruleMatched,
                            callReason = prev.callReason ?: call.callReason,
                            communityTag = prev.communityTag ?: call.communityTag,
                            isSpam = prev.isSpam || call.isSpam
                        )
                        deduplicated[existingIdx] = enriched
                    } else {
                        deduplicated.add(call)
                    }
                }

                // Fresh install seed: if Room database was empty, seed Room with system call history
                if (roomCalls.isEmpty() && systemCalls.isNotEmpty()) {
                    systemCalls.take(50).forEach { sysCall ->
                        try {
                            repository.insertRecentCall(sysCall.copy(id = 0L))
                        } catch (_: Exception) {}
                    }
                }

                _combinedRecentCalls.value = deduplicated
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)
                fun normName(n: String): String = n.trim().lowercase()

                val deviceList = ContactHelper.fetchDeviceContacts(appContext)
                val localList = repository.getAllLocalContactsList()
                val currentFavs = repository.getAllFavoritesList()

                val favDigits = currentFavs.map { normDigits(it.phoneNumber) }.filter { it.isNotEmpty() }.toSet()

                // Collect all phone numbers & names in device contacts
                val deviceNumbers = deviceList.flatMap { dc ->
                    dc.phoneNumbers.map { normDigits(it.number) } + listOf(normDigits(dc.phoneNumber))
                }.filter { it.isNotBlank() }.toSet()
                val deviceNames = deviceList.map { normName(it.name) }.toSet()

                // Clean up any local contacts that were synced to phone contacts or already exist on device
                localList.forEach { lc ->
                    val lcDigits = normDigits(lc.phoneNumber)
                    val lcName = normName(lc.name)
                    if ((lcDigits.isNotBlank() && deviceNumbers.contains(lcDigits)) || (lcName.isNotBlank() && deviceNames.contains(lcName))) {
                        repository.deleteLocalContact(lc)
                    }
                }

                // Retrieve active local contacts after pruning synced duplicates
                val activeLocalList = repository.getAllLocalContactsList()

                val localAsDeviceContacts = activeLocalList.map { lc ->
                    val isFav = favDigits.contains(normDigits(lc.phoneNumber))
                    DeviceContact(
                        name = lc.name,
                        phoneNumber = lc.phoneNumber,
                        label = lc.label,
                        photoUri = lc.photoUri,
                        nickname = lc.nickname,
                        isStarred = isFav,
                        isAppOnly = true
                    )
                }

                // Combine: device contacts take precedence, followed by app-only contacts, sorted strictly A-Z
                val combined = (deviceList + localAsDeviceContacts)
                    .distinctBy { dc ->
                        val digits = normDigits(dc.phoneNumber)
                        if (digits.isNotBlank()) digits else (normName(dc.name) + "_" + (dc.contactId ?: 0L))
                    }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

                _deviceContacts.value = combined
                syncWithDeviceContacts()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun registerContactsObserver() {
        try {
            contactsObserver = object : android.database.ContentObserver(android.os.Handler(android.os.Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    refreshContacts()
                }
            }
            appContext.contentResolver.registerContentObserver(
                android.provider.ContactsContract.AUTHORITY_URI,
                true,
                contactsObserver!!
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun registerCallLogObserver() {
        try {
            callLogObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    refreshRecentCalls()
                }
            }
            appContext.contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                callLogObserver!!
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        contactsObserver?.let {
            try {
                appContext.contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
        }
        callLogObserver?.let {
            try {
                appContext.contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
        }
    }

    /**
     * Synchronizes favorites with Android device Contacts database as the single source of truth.
     * Uses logical deduplication (matching phone digits or name) to identify existing contacts/favorites.
     * Keeps Room database clean and unique without creating duplicate entries on launch or re-install.
     */
    fun syncWithDeviceContacts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)
                fun normName(name: String): String = name.trim().lowercase()

                val currentDbFavorites = repository.getAllFavoritesList()

                // Deduplicate any existing duplicate favorites in Room (one card per contact person)
                val seenNames = mutableMapOf<String, com.example.data.FavoriteContact>()
                for (fav in currentDbFavorites) {
                    val key = normName(fav.name)
                    val existing = seenNames[key]
                    if (existing == null) {
                        seenNames[key] = fav
                    } else {
                        // Duplicate found for the same contact person: retain the one with speed dial slot, or first one
                        if (fav.speedDialSlot != null && existing.speedDialSlot == null) {
                            repository.deleteFavorite(existing)
                            seenNames[key] = fav
                        } else {
                            repository.deleteFavorite(fav)
                        }
                    }
                }

                val activeDbFavorites = repository.getAllFavoritesList()
                val activeByName = activeDbFavorites.associateBy { normName(it.name) }.toMutableMap()

                // Starred device contacts: returns at most one entry per contact holding their default number
                val starredOnDevice = ContactHelper.fetchStarredContacts(appContext)
                val colors = listOf(0xFF2563EBL, 0xFF16A34AL, 0xFFDC2626L, 0xFFD97706L, 0xFF7C3AEDL, 0xFF0891B2L)
                var maxOrder = activeDbFavorites.maxOfOrNull { it.sortOrder } ?: -1

                // Read saved sort orders from SharedPreferences to persist custom order across reinstalls
                val savedSortOrdersRaw = prefs.getStringSet("favorite_sort_orders", emptySet()) ?: emptySet()
                val savedSortMap = mutableMapOf<String, Int>()
                savedSortOrdersRaw.forEach { entry ->
                    val parts = entry.split(":")
                    if (parts.size >= 2) {
                        val key = parts[0]
                        val order = parts[1].toIntOrNull()
                        if (key.isNotBlank() && order != null) {
                            savedSortMap[key] = order
                            if (parts.size >= 3 && parts[2].isNotBlank()) {
                                savedSortMap[parts[2]] = order
                            }
                        }
                    }
                }

                // Read persistent speed dial assignments from SharedPreferences
                val savedSpeedDialsRaw = prefs.getStringSet("speed_dial_assignments", emptySet()) ?: emptySet()
                val savedSpeedDialByDigits = mutableMapOf<String, Int>()
                val savedSpeedDialByName = mutableMapOf<String, Int>()
                savedSpeedDialsRaw.forEach { entry ->
                    val parts = entry.split(":")
                    if (parts.size >= 2) {
                        val slot = parts[0].toIntOrNull()
                        val numDigits = parts[1]
                        val nameStr = if (parts.size >= 3) parts[2].lowercase().trim() else ""
                        if (slot != null && slot in 1..9) {
                            if (numDigits.isNotBlank()) {
                                savedSpeedDialByDigits[numDigits] = slot
                            }
                            if (nameStr.isNotBlank()) {
                                savedSpeedDialByName[nameStr] = slot
                            }
                        }
                    }
                }

                // Merge starred device contacts without duplicating the contact
                for (deviceContact in starredOnDevice) {
                    val nameKey = normName(deviceContact.name)
                    val devDigits = normDigits(deviceContact.phoneNumber)
                    val savedOrder = savedSortMap[devDigits] ?: savedSortMap[nameKey]
                    val savedSlot = savedSpeedDialByDigits[devDigits] ?: savedSpeedDialByName[nameKey]

                    val existing = activeByName[nameKey] ?: activeByName.values.firstOrNull {
                        val d1 = normDigits(it.phoneNumber)
                        d1.length >= 7 && d1 == devDigits
                    }

                    if (existing != null) {
                        // Keep user's chosen favorite phoneNumber intact; update display name, nickname, photo, or restore saved sort order & speed dial
                        val newSortOrder = savedOrder ?: existing.sortOrder
                        val newSpeedDialSlot = existing.speedDialSlot ?: savedSlot
                        val newNickname = deviceContact.nickname ?: existing.nickname
                        if (existing.name != deviceContact.name ||
                            existing.nickname != newNickname ||
                            existing.photoUri != deviceContact.photoUri ||
                            existing.sortOrder != newSortOrder ||
                            existing.speedDialSlot != newSpeedDialSlot) {
                            val updated = existing.copy(
                                name = deviceContact.name,
                                nickname = newNickname,
                                photoUri = deviceContact.photoUri ?: existing.photoUri,
                                sortOrder = newSortOrder,
                                speedDialSlot = newSpeedDialSlot
                            )
                            repository.updateFavorite(updated)
                            activeByName[nameKey] = updated
                        }
                    } else {
                        // Genuinely new contact
                        maxOrder++
                        val targetOrder = savedOrder ?: maxOrder
                        val color = colors[kotlin.math.abs(deviceContact.name.hashCode()) % colors.size]
                        val newFav = FavoriteContact(
                            name = deviceContact.name,
                            nickname = deviceContact.nickname,
                            phoneNumber = deviceContact.phoneNumber,
                            label = deviceContact.label,
                            avatarColor = color,
                            photoUri = deviceContact.photoUri,
                            sortOrder = targetOrder,
                            speedDialSlot = savedSlot
                        )
                        val insertedId = repository.insertFavorite(newFav)
                        activeByName[nameKey] = newFav.copy(id = insertedId)
                    }
                }

                // Resiliently restore any saved speed dial slot on remaining active favorites that lacked it
                val postMergeFavorites = repository.getAllFavoritesList()
                val assignedSlots = postMergeFavorites.mapNotNull { it.speedDialSlot }.toMutableSet()
                val allDevContacts = _deviceContacts.value
                for (fav in postMergeFavorites) {
                    val favDigits = normDigits(fav.phoneNumber)
                    val favName = normName(fav.name)

                    // Also sync nickname, name, or photo from any device contact (even if not starred on device)
                    val devMatch = allDevContacts.firstOrNull { dc ->
                        (favDigits.length >= 7 && normDigits(dc.phoneNumber) == favDigits) ||
                        dc.phoneNumbers.any { favDigits.length >= 7 && normDigits(it.number) == favDigits } ||
                        normName(dc.name) == favName
                    }
                    val devNickname = devMatch?.nickname?.trim()?.ifBlank { null }
                    val effectiveNick = devNickname ?: fav.nickname
                    val effectiveName = devMatch?.name ?: fav.name
                    val effectivePhoto = devMatch?.photoUri ?: fav.photoUri

                    var favToSave = fav
                    if (fav.nickname != effectiveNick || fav.name != effectiveName || fav.photoUri != effectivePhoto) {
                        favToSave = favToSave.copy(
                            nickname = effectiveNick,
                            name = effectiveName,
                            photoUri = effectivePhoto
                        )
                        repository.updateFavorite(favToSave)
                    }

                    if (favToSave.speedDialSlot == null) {
                        val candidateSlot = savedSpeedDialByDigits[favDigits] ?: savedSpeedDialByName[favName]
                        if (candidateSlot != null && !assignedSlots.contains(candidateSlot)) {
                            repository.updateFavorite(favToSave.copy(speedDialSlot = candidateSlot))
                            assignedSlots.add(candidateSlot)
                        }
                    }
                }

                // Also persist current state back to SharedPreferences as baseline backup
                persistFavoriteSortOrders(repository.getAllFavoritesList())
                persistSpeedDialAssignments(repository.getAllFavoritesList())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshSimCards() {
        viewModelScope.launch(Dispatchers.IO) {
            val detected = SimHelper.getActiveSimCards(appContext)
            _activeSims.value = detected
            if (detected.isNotEmpty()) {
                val currentSlotValid = detected.any { it.slotIndex + 1 == _selectedSimSlot.value }
                if (!currentSlotValid) {
                    val defaultSim = detected.firstOrNull { it.isDefault } ?: detected.first()
                    _selectedSimSlot.value = defaultSim.slotIndex + 1
                }
            }
        }
    }

    fun selectSimSlot(slot: Int) {
        _selectedSimSlot.value = slot
    }

    fun toggleSimSlot() {
        val detected = _activeSims.value
        if (detected.size > 1) {
            val currentIdx = detected.indexOfFirst { it.slotIndex + 1 == _selectedSimSlot.value }
            val nextIdx = (if (currentIdx >= 0) currentIdx + 1 else 0) % detected.size
            _selectedSimSlot.value = detected[nextIdx].slotIndex + 1
        } else {
            _selectedSimSlot.value = if (_selectedSimSlot.value == 1) 2 else 1
        }
    }

    fun refreshDefaultDialerStatus() {
        _isDefaultDialer.value = RoleHelper.isDefaultDialer(appContext)
    }

    fun refreshCallRedirectionStatus() {
        _isCallRedirectionRoleHeld.value = RoleHelper.isCallRedirectionRoleHeld(appContext)
    }

    fun appendDigit(digit: Char) {
        _dialerNumber.value += digit
    }

    fun deleteLastDigit() {
        if (_dialerNumber.value.isNotEmpty()) {
            _dialerNumber.value = _dialerNumber.value.dropLast(1)
        }
    }

    fun clearDigits() {
        _dialerNumber.value = ""
    }

    fun setDialerNumber(number: String) {
        _dialerNumber.value = number
    }

    fun toggleInCallKeypad() {
        _showInCallKeypad.value = !_showInCallKeypad.value
    }

    private val _selectedCallReason = MutableStateFlow<String?>(null)
    val selectedCallReason: StateFlow<String?> = _selectedCallReason.asStateFlow()

    fun selectCallReason(reason: String?) {
        _selectedCallReason.value = reason
    }

    @SuppressLint("MissingPermission")
    fun placeCall(context: Context, number: String, reason: String? = null) {
        val cleanNumber = number.ifBlank { _dialerNumber.value }
        if (cleanNumber.isBlank()) return
        val effectiveReason = reason ?: _selectedCallReason.value
        maximizeCall()

        // If in ask_learn mode and user explicitly triggered cellular call, learn the choice directly
        if (_whatsAppCallMode.value == "ask_learn") {
            saveLearnedCallMode(cleanNumber, "cellular")
        }

        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val uri = Uri.fromParts("tel", cleanNumber, null)

            val extras = Bundle().apply {
                putBoolean(TelecomManager.EXTRA_START_CALL_WITH_SPEAKERPHONE, false)
                if (!effectiveReason.isNullOrBlank()) {
                    putString("CALL_REASON", effectiveReason)
                }
            }

            // Ensure Android routes to cellular SIM carrier, using the selected SIM slot
            if (telecomManager != null) {
                try {
                    val simAccount = SimHelper.getPhoneAccountForSimSlot(context, _selectedSimSlot.value - 1)
                    val defaultAccount = simAccount
                        ?: telecomManager.getDefaultOutgoingPhoneAccount(uri.scheme)
                        ?: telecomManager.callCapablePhoneAccounts.firstOrNull { handle ->
                            handle.componentName.packageName.contains("telephony", ignoreCase = true) ||
                            handle.componentName.packageName.contains("phone", ignoreCase = true)
                        } ?: telecomManager.callCapablePhoneAccounts.firstOrNull()

                    if (defaultAccount != null) {
                        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, defaultAccount)
                    }
                } catch (_: SecurityException) {
                    // Ignore if permission not yet granted
                }
            }

            // If running on actual device/dialer role
            if (telecomManager != null && RoleHelper.isDefaultDialer(context)) {
                telecomManager.placeCall(uri, extras)
            } else {
                // Fallback to ACTION_CALL or start simulated outgoing call for preview
                val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtras(extras)
                }
                if (context.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    context.startActivity(callIntent)
                } else {
                    // Start simulated call so user can test the in-call interface & DTMF keypad
                    CallManager.startSimulatedOutgoingCall(context, cleanNumber, effectiveReason)
                }
            }
        } catch (e: Exception) {
            // Simulator fallback if hardware telephony is unavailable
            CallManager.startSimulatedOutgoingCall(context, cleanNumber, effectiveReason)
        }
    }

    fun placeWhatsAppCall(context: Context, number: String) {
        val cleanNumber = number.ifBlank { _dialerNumber.value }
        if (cleanNumber.isBlank()) return

        // If in ask_learn mode and user explicitly triggered WhatsApp call, learn the choice directly
        if (_whatsAppCallMode.value == "ask_learn") {
            saveLearnedCallMode(cleanNumber, "whatsapp")
        }

        ContactHelper.launchWhatsAppCall(context, cleanNumber)

        // Log outgoing WhatsApp call so frequency learning & preferred calling mode work
        viewModelScope.launch(Dispatchers.IO) {
            val contactName = _deviceContacts.value.firstOrNull { dc ->
                dc.phoneNumber.contains(cleanNumber) || dc.phoneNumbers.any { it.number.contains(cleanNumber) }
            }?.name ?: favorites.value.firstOrNull { it.phoneNumber.contains(cleanNumber) }?.name

            repository.insertRecentCall(
                RecentCall(
                    phoneNumber = cleanNumber,
                    callerName = contactName ?: cleanNumber,
                    callType = android.provider.CallLog.Calls.OUTGOING_TYPE,
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = 0,
                    callReason = "WhatsApp Call"
                )
            )
            refreshRecentCalls()
        }
    }

    /**
     * Determines whether cellular or WhatsApp calling is preferred for this contact/number,
     * checking explicitly learned choices, international rules, or recent call history.
     */
    fun getPreferredCallingMode(phoneNumber: String): String {
        // If mode is set to "never", override all preferred channels to cellular without wiping saved preferences!
        if (_whatsAppCallMode.value == "never") {
            return "cellular"
        }

        // If mode is set to "ask_always", return "ask_always" so both dialers are always shown
        if (_whatsAppCallMode.value == "ask_always") {
            return "ask_always"
        }

        val clean = phoneNumber.replace(Regex("[^0-9+]"), "")
        val digits = clean.filter { it.isDigit() }.takeLast(10)

        // 1. Check explicitly learned choice
        val learned = _learnedCallModes.value[clean]
            ?: (if (digits.isNotBlank()) _learnedCallModes.value[digits] else null)
            ?: _learnedCallModes.value.entries.firstOrNull { ContactHelper.isSamePhoneNumber(it.key, clean) }?.value
        if (learned != null) return learned

        // 2. If user configured all international to WhatsApp, check international based on phone location
        if (_whatsAppCallMode.value == "all_international" && ContactHelper.isInternationalNumber(appContext, clean)) {
            return "whatsapp"
        }

        // 3. Count past WhatsApp calls vs regular cellular calls in recent calls
        val calls = recentCalls.value.filter { call ->
            ContactHelper.isSamePhoneNumber(call.phoneNumber, clean)
        }
        val waCount = calls.count { it.callReason?.contains("WhatsApp", ignoreCase = true) == true }
        val gsmCount = calls.count { it.callReason?.contains("WhatsApp", ignoreCase = true) != true }

        return if (waCount > gsmCount && waCount > 0) {
            "whatsapp"
        } else if (_whatsAppCallMode.value == "ask_learn") {
            "ask"
        } else {
            "cellular"
        }
    }

    fun lookupContactByNumber(phoneNumber: String): DeviceContact? {
        val fav = favorites.value.firstOrNull {
            ContactHelper.isSamePhoneNumber(it.phoneNumber, phoneNumber)
        }
        if (fav != null) return DeviceContact(fav.name, fav.phoneNumber, fav.label, fav.photoUri)

        val matchedDc = deviceContacts.value.firstOrNull { dc ->
            ContactHelper.isSamePhoneNumber(dc.phoneNumber, phoneNumber) ||
            dc.phoneNumbers.any { ContactHelper.isSamePhoneNumber(it.number, phoneNumber) }
        }
        if (matchedDc != null) {
            val matchingItem = matchedDc.phoneNumbers.firstOrNull {
                ContactHelper.isSamePhoneNumber(it.number, phoneNumber)
            }
            return if (matchingItem != null) {
                matchedDc.copy(
                    phoneNumber = matchingItem.number,
                    label = matchingItem.label
                )
            } else {
                matchedDc
            }
        }
        return null
    }

    /**
     * Places a call honoring the configured WhatsApp calling mode (All International,
     * Ask Always prompt, Ask & Learn memory, or cellular).
     */
    fun initiateCall(context: Context, number: String, reason: String? = null) {
        val cleanNumber = number.ifBlank { _dialerNumber.value }
        if (cleanNumber.isBlank()) return

        val isInternational = ContactHelper.isInternationalNumber(context, cleanNumber)
        val mode = _whatsAppCallMode.value

        // 1. All International Mode
        if (mode == "all_international" && isInternational) {
            placeWhatsAppCall(context, cleanNumber)
            return
        }

        // 2. Ask Always Mode
        if (mode == "ask_always") {
            val contact = lookupContactByNumber(cleanNumber)
            _pendingCallMethodChoice.value = CallMethodChoicePrompt(
                number = cleanNumber,
                contactName = contact?.name,
                reason = reason,
                isLearnMode = false
            )
            return
        }

        // 3. Ask & Learn Mode
        if (mode == "ask_learn") {
            val clean = cleanNumber.replace(Regex("[^0-9+]"), "")
            val digits = cleanNumber.filter { it.isDigit() }.takeLast(10)
            val learnedChoice = _learnedCallModes.value[clean]
                ?: if (digits.isNotBlank()) _learnedCallModes.value[digits] else null
            if (learnedChoice != null) {
                if (learnedChoice == "whatsapp") {
                    placeWhatsAppCall(context, cleanNumber)
                } else {
                    placeCall(context, cleanNumber, reason)
                }
                return
            }

            // Not yet learned: show prompt so user can choose and learn
            val contact = lookupContactByNumber(cleanNumber)
            _pendingCallMethodChoice.value = CallMethodChoicePrompt(
                number = cleanNumber,
                contactName = contact?.name,
                reason = reason,
                isLearnMode = true
            )
            return
        }

        // 4. Default / Never
        placeCall(context, cleanNumber, reason)
    }

    fun simulateIncomingCall(context: Context, number: String, name: String = "Incoming Caller", reason: String? = null) {
        CallManager.startSimulatedIncomingCall(context, number, name, reason)
    }

    fun answerCall() {
        CallManager.answerCall()
    }

    fun declineCall() {
        CallManager.declineCall()
    }

    fun declineWithSms(message: String) {
        CallManager.declineWithSms(appContext, message)
    }

    fun updateRecentCallNoteAndReminder(recentCall: RecentCall, note: String?, reminderTime: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanNote = note?.trim()?.takeIf { it.isNotBlank() }
            val callId: Long
            if (recentCall.id <= 0L) {
                // If the call was loaded from system CallLog, insert a new row in Room to persist the note & reminder
                val newCall = recentCall.copy(
                    id = 0L,
                    note = cleanNote,
                    reminderTime = reminderTime
                )
                callId = repository.insertRecentCall(newCall)
            } else {
                val updated = recentCall.copy(
                    note = cleanNote,
                    reminderTime = reminderTime
                )
                repository.updateRecentCall(updated)
                callId = updated.id
            }

            if (reminderTime != null && reminderTime > System.currentTimeMillis()) {
                com.example.telecom.ReminderScheduler.scheduleReminder(
                    context = appContext,
                    callId = callId,
                    phoneNumber = recentCall.phoneNumber,
                    callerName = recentCall.callerName,
                    note = cleanNote,
                    reminderEpoch = reminderTime
                )
            } else {
                com.example.telecom.ReminderScheduler.cancelReminder(
                    context = appContext,
                    callId = callId,
                    phoneNumber = recentCall.phoneNumber
                )
            }
            refreshRecentCalls()
        }
    }

    fun deleteRecentCall(call: RecentCall) {
        viewModelScope.launch(Dispatchers.IO) {
            if (call.id > 0L) {
                repository.deleteRecentCallById(call.id)
            } else {
                repository.deleteRecentCallsForNumber(call.phoneNumber)
            }
            refreshRecentCalls()
        }
    }

    fun deleteRecentCallsForNumber(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRecentCallsForNumber(phoneNumber)
            refreshRecentCalls()
        }
    }

    fun disconnectCall() {
        CallManager.disconnectCall()
    }

    fun dismissCall() {
        CallManager.dismissActiveCall()
    }

    fun playDtmf(digit: Char) {
        CallManager.playDtmf(digit)
    }

    fun stopDtmf() {
        CallManager.stopDtmf()
    }

    fun toggleMute() {
        CallManager.toggleMute()
    }

    fun toggleSpeaker() {
        CallManager.toggleSpeaker()
    }

    val currentAudioRoute: StateFlow<Int> = CallManager.currentAudioRoute
    val supportedAudioRoutes: StateFlow<Int> = CallManager.supportedAudioRoutes
    val bluetoothDeviceName: StateFlow<String?> = CallManager.bluetoothDeviceName
    val availableBluetoothDevices: StateFlow<List<com.example.telecom.BluetoothDeviceItem>> = CallManager.availableBluetoothDevices
    val activeBluetoothDeviceAddress: StateFlow<String?> = CallManager.activeBluetoothDeviceAddress

    fun setAudioRoute(route: Int) {
        CallManager.setAudioRoute(route)
    }

    fun selectBluetoothDevice(address: String) {
        CallManager.selectBluetoothDevice(address)
    }

    fun savePostCallNote(phoneNumber: String, note: String?, reminderTime: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanNote = note?.trim()?.takeIf { it.isNotBlank() }
            val lastId = CallManager.lastInsertedCallId
            val existing = if (lastId != null) {
                repository.getLatestRecentCallForNumber(phoneNumber)?.takeIf { it.id == lastId }
                    ?: repository.getLatestRecentCallForNumber(phoneNumber)
            } else {
                repository.getLatestRecentCallForNumber(phoneNumber)
            }

            val savedCall = if (existing != null) {
                val up = existing.copy(
                    note = cleanNote,
                    reminderTime = reminderTime
                )
                repository.updateRecentCall(up)
                up
            } else {
                val newCall = RecentCall(
                    phoneNumber = phoneNumber,
                    callerName = phoneNumber,
                    callType = 2,
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = 0,
                    note = cleanNote,
                    reminderTime = reminderTime
                )
                val newId = repository.insertRecentCall(newCall)
                newCall.copy(id = newId)
            }

            if (reminderTime != null && reminderTime > System.currentTimeMillis()) {
                com.example.telecom.ReminderScheduler.scheduleReminder(
                    context = appContext,
                    callId = savedCall.id,
                    phoneNumber = savedCall.phoneNumber,
                    callerName = savedCall.callerName,
                    note = savedCall.note,
                    reminderEpoch = reminderTime
                )
            } else {
                com.example.telecom.ReminderScheduler.cancelReminder(
                    context = appContext,
                    callId = savedCall.id,
                    phoneNumber = savedCall.phoneNumber
                )
            }
        }
    }

    fun createNewContact(
        name: String,
        phoneNumbers: List<ContactPhoneNumber>,
        saveToDevice: Boolean,
        addToFavorites: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val validNumbers = phoneNumbers.filter { it.number.isNotBlank() }
            if (validNumbers.isEmpty() || name.isBlank()) return@launch

            if (saveToDevice) {
                ContactHelper.saveContactToDevice(appContext, name, validNumbers)
            } else {
                validNumbers.forEach { pn ->
                    repository.insertLocalContact(
                        LocalContact(
                            name = name,
                            phoneNumber = pn.number,
                            label = pn.label
                        )
                    )
                }
            }
            if (addToFavorites) {
                val primaryNum = validNumbers.first()
                val fav = FavoriteContact(
                    name = name,
                    phoneNumber = primaryNum.number,
                    label = primaryNum.label,
                    sortOrder = 999
                )
                repository.insertFavorite(fav)
            }
            refreshContacts()
        }
    }

    fun createNewContact(name: String, phoneNumber: String, label: String, saveToDevice: Boolean, addToFavorites: Boolean) {
        createNewContact(name, listOf(ContactPhoneNumber(phoneNumber, label)), saveToDevice, addToFavorites)
    }

    fun addNumberToExistingContact(
        contactId: Long?,
        existingNumber: String?,
        contactName: String,
        newNumber: String,
        label: String,
        isAppOnly: Boolean,
        addToFavorites: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanNum = newNumber.trim()
            if (cleanNum.isBlank()) return@launch

            if (isAppOnly) {
                repository.insertLocalContact(
                    LocalContact(
                        name = contactName,
                        phoneNumber = cleanNum,
                        label = label
                    )
                )
            } else {
                ContactHelper.addPhoneNumberToExistingContact(
                    context = appContext,
                    contactId = contactId,
                    existingNumber = existingNumber,
                    newNumber = cleanNum,
                    label = label
                )
            }

            if (addToFavorites) {
                val fav = FavoriteContact(
                    name = contactName,
                    phoneNumber = cleanNum,
                    label = label,
                    sortOrder = 999
                )
                repository.insertFavorite(fav)
            }
            refreshContacts()
        }
    }

    fun syncAppContactToPhone(contact: DeviceContact) {
        viewModelScope.launch(Dispatchers.IO) {
            ContactHelper.saveContactToDevice(appContext, contact.name, contact.phoneNumber, contact.label)
            if (contact.isAppOnly) {
                repository.deleteLocalContactByNumber(contact.phoneNumber)
            }
            refreshContacts()
        }
    }

    private suspend fun getContactInfoForConfirmation(phoneNumber: String, name: String): Pair<Boolean, Boolean> {
        fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)
        val cleanDigits = normDigits(phoneNumber)

        val matched = _deviceContacts.value.firstOrNull { dc ->
            (cleanDigits.length >= 7 && normDigits(dc.phoneNumber) == cleanDigits) ||
            dc.name.equals(name.trim(), ignoreCase = true)
        }

        if (matched != null) {
            return Pair(matched.isAppOnly, matched.isStarred)
        }

        val localList = repository.getAllLocalContactsList()
        val isLocal = localList.any { (cleanDigits.length >= 7 && normDigits(it.phoneNumber) == cleanDigits) || it.name.equals(name.trim(), ignoreCase = true) }
        if (isLocal) {
            return Pair(true, false)
        }

        val systemContact = ContactHelper.lookupContactByNumber(appContext, phoneNumber)
        return if (systemContact != null) {
            Pair(false, systemContact.isStarred)
        } else {
            Pair(true, false)
        }
    }

    fun syncAllAppContactsToDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            val localList = repository.getAllLocalContactsList()
            for (lc in localList) {
                ContactHelper.saveContactToDevice(appContext, lc.name, lc.phoneNumber, lc.label)
                repository.deleteLocalContact(lc)
            }
            refreshContacts()
        }
    }

    fun saveRule(rule: CallerRule) {
        viewModelScope.launch {
            if (rule.id == 0L) {
                repository.insertRule(rule)
            } else {
                repository.updateRule(rule)
            }
        }
    }

    fun toggleRuleEnabled(rule: CallerRule) {
        viewModelScope.launch {
            repository.updateRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun deleteRule(rule: CallerRule) {
        viewModelScope.launch {
            repository.deleteRule(rule)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAutomationLogs()
        }
    }

    fun addFavorite(name: String, phoneNumber: String, label: String = "Mobile", photoUri: String? = null, nickname: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)
            val cleanDigits = normDigits(phoneNumber)
            val currentList = repository.getAllFavoritesList()
            val matchedContact = lookupContactByNumber(phoneNumber)
                ?: deviceContacts.value.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
            val nicknameToUse = nickname?.trim()?.ifBlank { null } ?: matchedContact?.nickname?.ifBlank { null }

            val existing = currentList.firstOrNull {
                (cleanDigits.length >= 7 && normDigits(it.phoneNumber) == cleanDigits) ||
                it.name.equals(name.trim(), ignoreCase = true)
            }
            if (existing != null) {
                // Update existing rather than creating duplicate
                repository.updateFavorite(
                    existing.copy(
                        name = matchedContact?.name ?: name.trim(),
                        nickname = nicknameToUse ?: existing.nickname,
                        phoneNumber = phoneNumber.trim(),
                        label = label,
                        photoUri = photoUri ?: matchedContact?.photoUri ?: existing.photoUri
                    )
                )
            } else {
                val colors = listOf(0xFF2563EBL, 0xFF16A34AL, 0xFFDC2626L, 0xFFD97706L, 0xFF7C3AEDL, 0xFF0891B2L)
                val color = colors[abs((matchedContact?.name ?: name).hashCode()) % colors.size]
                val maxOrder = currentList.maxOfOrNull { it.sortOrder } ?: -1
                repository.insertFavorite(
                    FavoriteContact(
                        name = matchedContact?.name ?: name.trim(),
                        nickname = nicknameToUse,
                        phoneNumber = phoneNumber.trim(),
                        label = label,
                        avatarColor = color,
                        photoUri = photoUri ?: matchedContact?.photoUri,
                        sortOrder = maxOrder + 1
                    )
                )
            }
            persistFavoriteSortOrders(repository.getAllFavoritesList())

            if (!nickname.isNullOrBlank()) {
                ContactHelper.updateContactNickname(appContext, phoneNumber, nickname.trim(), matchedContact?.contactId)
            }

            val (isAppOnly, _) = getContactInfoForConfirmation(phoneNumber, name)
            if (!isAppOnly) {
                _pendingCloudConfirmation.value = CloudContactConfirmation(
                    title = "Star in Phone Contacts?",
                    message = "Added '$name' to favorites in this app.\n\nWould you like to also star this contact in your Phone Contacts?",
                    contactName = name,
                    confirmButtonText = "Star in Phone Contacts",
                    secondaryButtonText = null,
                    dismissButtonText = "Keep in App Only",
                    onConfirmCloudAction = {
                        viewModelScope.launch(Dispatchers.IO) {
                            ContactHelper.setContactStarred(appContext, phoneNumber, true)
                        }
                    },
                    onSecondaryAction = null,
                    onDismissOrCancel = {
                        // Kept in app only
                    }
                )
            }
        }
    }

    fun updateFavorite(contact: com.example.data.FavoriteContact, newNickname: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = contact.copy(
                name = contact.name.trim(),
                nickname = newNickname?.trim()?.takeIf { it.isNotBlank() } ?: contact.nickname
            )
            repository.updateFavorite(updated)

            if (!newNickname.isNullOrBlank()) {
                val (isAppOnly, _) = getContactInfoForConfirmation(contact.phoneNumber, contact.name)
                if (!isAppOnly) {
                    _pendingCloudConfirmation.value = CloudContactConfirmation(
                        title = "Update Phone Contacts Nickname?",
                        message = "You set a custom nickname '$newNickname' for ${contact.name}.\n\nDo you want to update this nickname in your Phone Contacts as well?",
                        contactName = contact.name,
                        confirmButtonText = "Update Phone Contacts",
                        secondaryButtonText = null,
                        dismissButtonText = "Save in App Only",
                        onConfirmCloudAction = {
                            viewModelScope.launch(Dispatchers.IO) {
                                ContactHelper.updateContactNickname(appContext, contact.phoneNumber, newNickname.trim())
                            }
                        },
                        onSecondaryAction = null,
                        onDismissOrCancel = {
                            // Saved in app only
                        }
                    )
                }
            }
        }
    }

    fun updateFavoritePhoneNumber(contact: com.example.data.FavoriteContact, newPhoneNumber: String, newLabel: String = "Mobile") {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = contact.copy(
                phoneNumber = newPhoneNumber.trim(),
                label = newLabel
            )
            repository.updateFavorite(updated)
        }
    }

    fun reorderFavorites(newOrderedList: List<com.example.data.FavoriteContact>) {
        viewModelScope.launch(Dispatchers.IO) {
            fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)
            val updated = newOrderedList.mapIndexed { index, item ->
                item.copy(sortOrder = index)
            }
            repository.updateFavorites(updated)

            // Save custom sort orders into SharedPreferences for persistence across reinstalls
            val sortOrderSet = updated.map { "${normDigits(it.phoneNumber)}:${it.sortOrder}:${it.name.trim().lowercase()}" }.toSet()
            prefs.edit().putStringSet("favorite_sort_orders", sortOrderSet).apply()
        }
    }

    fun moveFavorite(fromIndex: Int, toIndex: Int) {
        val current = favorites.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            Collections.swap(current, fromIndex, toIndex)
            reorderFavorites(current)
        }
    }

    fun toggleFavorite(name: String, phoneNumber: String, label: String = "Mobile", photoUri: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanDigits = phoneNumber.filter { it.isDigit() }.takeLast(10)
            val existing = favorites.value.firstOrNull { fav ->
                val favDigits = fav.phoneNumber.filter { it.isDigit() }.takeLast(10)
                (cleanDigits.length >= 7 && favDigits == cleanDigits) ||
                fav.name.equals(name.trim(), ignoreCase = true)
            }
            if (existing != null) {
                deleteFavorite(existing)
            } else {
                addFavorite(name, phoneNumber, label, photoUri)
            }
        }
    }

    fun deleteFavorite(contact: FavoriteContact) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFavorite(contact)
            persistSpeedDialAssignments(repository.getAllFavoritesList())
            persistFavoriteSortOrders(repository.getAllFavoritesList())
            try {
                ContactHelper.setContactStarred(appContext, contact.phoneNumber, false)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun markAsSpam(phoneNumber: String, label: String = "Reported Spam") {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanDigits = phoneNumber.filter { it.isDigit() }.takeLast(10)

            // Remove from whitelist if previously whitelisted
            val currentWl = _notSpamWhitelist.value.toMutableSet()
            val removedWl = currentWl.removeAll {
                it == phoneNumber || (cleanDigits.length >= 7 && it.filter { c -> c.isDigit() }.takeLast(10) == cleanDigits)
            }
            if (removedWl) {
                _notSpamWhitelist.value = currentWl
                prefs.edit().putStringSet("not_spam_whitelist", currentWl).apply()
            }

            repository.insertSpamNumber(
                com.example.data.SpamNumber(
                    phoneNumber = phoneNumber,
                    label = label,
                    reportCount = 1,
                    isBlocked = true
                )
            )
            repository.updateRecentCallSpamStatus(phoneNumber, true)
            // Also update any recent calls matching normalized digits
            val allCalls = repository.getAllRecentCallsList()
            for (call in allCalls) {
                val callDigits = call.phoneNumber.filter { it.isDigit() }.takeLast(10)
                if (call.phoneNumber == phoneNumber || (callDigits.length >= 7 && callDigits == cleanDigits)) {
                    if (!call.isSpam) {
                        repository.updateRecentCall(call.copy(isSpam = true))
                    }
                }
            }
        }
    }

    fun removeSpam(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanDigits = phoneNumber.filter { it.isDigit() }.takeLast(10)

            // Permanently add to not-spam whitelist
            val currentWl = _notSpamWhitelist.value.toMutableSet()
            currentWl.add(phoneNumber)
            if (cleanDigits.isNotBlank()) currentWl.add(cleanDigits)
            _notSpamWhitelist.value = currentWl
            prefs.edit().putStringSet("not_spam_whitelist", currentWl).apply()

            // Delete exact number match
            repository.deleteSpamByNumber(phoneNumber)
            // Also delete any entries matching normalized digits
            val allSpam = repository.getAllSpamNumbersList()
            for (sp in allSpam) {
                val spDigits = sp.phoneNumber.filter { it.isDigit() }.takeLast(10)
                if (sp.phoneNumber == phoneNumber || (spDigits.length >= 7 && spDigits == cleanDigits)) {
                    repository.deleteSpamNumber(sp)
                }
            }
            // Update recent_calls table to mark isSpam = false
            repository.updateRecentCallSpamStatus(phoneNumber, false)
            val allCalls = repository.getAllRecentCallsList()
            for (call in allCalls) {
                val callDigits = call.phoneNumber.filter { it.isDigit() }.takeLast(10)
                if (call.phoneNumber == phoneNumber || (callDigits.length >= 7 && callDigits == cleanDigits)) {
                    if (call.isSpam) {
                        repository.updateRecentCall(call.copy(isSpam = false))
                    }
                }
            }
        }
    }

    fun ignorePopularContact(phoneNumber: String, name: String, category: String, tag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertIgnoredContact(
                IgnoredContact(
                    phoneNumber = phoneNumber,
                    name = name,
                    category = category,
                    tag = tag,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun unignorePopularContact(phoneNumber: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteIgnoredContactByNumber(phoneNumber)
        }
    }

    fun updateIgnoredContactTag(phoneNumber: String, newTag: String, newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getIgnoredContactByNumber(phoneNumber)
            if (existing != null) {
                repository.insertIgnoredContact(
                    existing.copy(tag = newTag, name = newName)
                )
            } else {
                repository.insertIgnoredContact(
                    IgnoredContact(phoneNumber = phoneNumber, name = newName, tag = newTag)
                )
            }
        }
    }

    fun assignSpeedDial(contact: com.example.data.FavoriteContact, slot: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // If contact already has this slot, toggle it off (unassign)
            if (contact.speedDialSlot == slot) {
                repository.updateFavorite(contact.copy(speedDialSlot = null))
                persistSpeedDialAssignments(repository.getAllFavoritesList())
                return@launch
            }
            // Clear this slot from any other favorite contact that has it
            val currentFavorites = repository.getAllFavoritesList()
            currentFavorites.filter { it.speedDialSlot == slot && it.id != contact.id }.forEach { other ->
                repository.updateFavorite(other.copy(speedDialSlot = null))
            }
            // Assign slot to target contact
            repository.updateFavorite(contact.copy(speedDialSlot = slot))
            persistSpeedDialAssignments(repository.getAllFavoritesList())
        }
    }

    fun assignSpeedDialSlot(slot: Int, name: String, phoneNumber: String, photoUri: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentFavorites = repository.getAllFavoritesList()
            // Clear this slot from any other contact
            currentFavorites.filter { it.speedDialSlot == slot }.forEach { other ->
                repository.updateFavorite(other.copy(speedDialSlot = null))
            }

            val cleanDigits = phoneNumber.filter { it.isDigit() }.takeLast(10)
            val existing = currentFavorites.firstOrNull {
                it.phoneNumber == phoneNumber || (cleanDigits.length >= 7 && it.phoneNumber.filter { c -> c.isDigit() }.takeLast(10) == cleanDigits) || it.name.equals(name, ignoreCase = true)
            }

            if (existing != null) {
                repository.updateFavorite(existing.copy(speedDialSlot = slot, photoUri = photoUri ?: existing.photoUri))
            } else {
                val colors = listOf(0xFF2563EBL, 0xFF16A34AL, 0xFFDC2626L, 0xFFD97706L, 0xFF7C3AEDL, 0xFF0891B2L)
                val color = colors[kotlin.math.abs(name.hashCode()) % colors.size]
                val maxOrder = currentFavorites.maxOfOrNull { it.sortOrder } ?: -1
                repository.insertFavorite(
                    com.example.data.FavoriteContact(
                        name = name.trim(),
                        phoneNumber = phoneNumber.trim(),
                        label = "Mobile",
                        avatarColor = color,
                        photoUri = photoUri,
                        speedDialSlot = slot,
                        sortOrder = maxOrder + 1
                    )
                )
            }
            persistSpeedDialAssignments(repository.getAllFavoritesList())
            persistFavoriteSortOrders(repository.getAllFavoritesList())
        }
    }

    fun clearSpeedDialSlot(slot: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentFavorites = repository.getAllFavoritesList()
            currentFavorites.filter { it.speedDialSlot == slot }.forEach { fav ->
                repository.updateFavorite(fav.copy(speedDialSlot = null))
            }
            persistSpeedDialAssignments(repository.getAllFavoritesList())
        }
    }

    private fun persistSpeedDialAssignments(list: List<com.example.data.FavoriteContact>) {
        try {
            fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)
            val set = list.filter { it.speedDialSlot != null && it.speedDialSlot in 1..9 }
                .map { "${it.speedDialSlot}:${normDigits(it.phoneNumber)}:${it.name.trim().lowercase()}" }
                .toSet()
            prefs.edit().putStringSet("speed_dial_assignments", set).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun persistFavoriteSortOrders(list: List<com.example.data.FavoriteContact>) {
        try {
            fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)
            val sortOrderSet = list.map { "${normDigits(it.phoneNumber)}:${it.sortOrder}:${it.name.trim().lowercase()}" }.toSet()
            prefs.edit().putStringSet("favorite_sort_orders", sortOrderSet).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteContact(contact: DeviceContact) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                fun normDigits(num: String): String = num.filter { it.isDigit() }.takeLast(10)
                val contactDigits = normDigits(contact.phoneNumber)
                val allContactDigits = contact.phoneNumbers.map { normDigits(it.number) }.filter { it.isNotBlank() }.toSet()

                // 1. Delete from Android System Contacts Provider if it's a device contact
                if (contact.contactId != null && contact.contactId > 0 && !contact.isAppOnly) {
                    ContactHelper.deleteContactFromDevice(appContext, contact.contactId)
                } else if (!contact.isAppOnly && contact.phoneNumber.isNotBlank()) {
                    ContactHelper.deleteContactFromDeviceByNumber(appContext, contact.phoneNumber)
                }

                // 2. Delete from Room local_contacts (matching phone digits or matching clean name)
                val localList = repository.getAllLocalContactsList()
                localList.forEach { lc ->
                    val lcDigits = normDigits(lc.phoneNumber)
                    val isNumberMatch = (contactDigits.isNotBlank() && lcDigits == contactDigits) || (lcDigits.isNotBlank() && allContactDigits.contains(lcDigits))
                    val isNameMatch = lc.name.trim().equals(contact.name.trim(), ignoreCase = true)
                    if (isNumberMatch || isNameMatch) {
                        repository.deleteLocalContact(lc)
                    }
                }
                if (contact.phoneNumber.isNotBlank()) {
                    repository.deleteLocalContactByNumber(contact.phoneNumber)
                }
                if (contact.name.isNotBlank()) {
                    repository.deleteLocalContactByName(contact.name.trim())
                }

                // 3. Delete from Room FavoriteContact if present
                val favList = repository.getAllFavoritesList()
                favList.forEach { fav ->
                    val favDigits = normDigits(fav.phoneNumber)
                    val isNumberMatch = (contactDigits.isNotBlank() && favDigits == contactDigits) || (favDigits.isNotBlank() && allContactDigits.contains(favDigits))
                    val isNameMatch = fav.name.trim().equals(contact.name.trim(), ignoreCase = true)
                    if (isNumberMatch || isNameMatch) {
                        repository.deleteFavorite(fav)
                    }
                }

                refreshContacts()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateContact(oldNumber: String, newName: String, newNumber: String, newLabel: String, newNickname: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            ContactHelper.updateContactDetails(appContext, oldNumber, newName, newNumber, newLabel, newNickname)
            val localList = repository.getAllLocalContactsList()
            val existingLocal = localList.firstOrNull { it.phoneNumber == oldNumber }
            if (existingLocal != null) {
                repository.updateLocalContact(
                    existingLocal.copy(
                        name = newName,
                        phoneNumber = newNumber,
                        label = newLabel,
                        nickname = newNickname
                    )
                )
            }
            val fav = favorites.value.firstOrNull { it.phoneNumber == oldNumber }
            if (fav != null) {
                repository.updateFavorite(fav.copy(name = newName, phoneNumber = newNumber, label = newLabel))
            }
            refreshContacts()
        }
    }

    fun exportBackup(uri: android.net.Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = com.example.util.BackupManager.writeBackupToUri(appContext, uri)
            onComplete(success)
        }
    }

    fun importBackup(uri: android.net.Uri, onComplete: (com.example.util.BackupRestoreResult) -> Unit) {
        viewModelScope.launch {
            val result = com.example.util.BackupManager.restoreBackupFromUri(appContext, uri)
            if (result.success) {
                // Refresh local UI states from restored preferences
                _themeMode.value = prefs.getString("theme_mode", "system") ?: "system"
                _whatsAppCallMode.value = prefs.getString("whatsapp_call_mode", "ask_learn") ?: "ask_learn"
                _callAnswerStyle.value = prefs.getString("call_answer_style", "swipe_slider") ?: "swipe_slider"
                _favoriteCardStyle.value = prefs.getString("favorite_card_style", "bento") ?: "bento"
                _confirmFavoritesCall.value = prefs.getBoolean("confirm_fav_calls", true)
                _confirmSpeedDialCall.value = prefs.getBoolean("confirm_speed_dial_call", true)
                _askToAssignUnassignedSpeedDial.value = prefs.getBoolean("ask_assign_unassigned_speed_dial", true)
                _speedDialKeypadDisplay.value = prefs.getString("speed_dial_keypad_display", "speed_dial_above") ?: "speed_dial_above"
                _showDialerQuickActions.value = prefs.getBoolean("show_dialer_quick_actions", true)
                _defaultStartTab.value = prefs.getInt("default_start_tab", 0)
                _swipeToSwitchPanels.value = prefs.getBoolean("swipe_to_switch_panels", true)
                _notSpamWhitelist.value = prefs.getStringSet("not_spam_whitelist", emptySet()) ?: emptySet()
                _learnedCallModes.value = loadLearnedCallModes()
                refreshContacts()
                refreshRecentCalls()
                refreshLocalBackups()
            }
            onComplete(result)
        }
    }

    fun createLocalBackup(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = com.example.util.BackupManager.saveLocalBackup(appContext)
            if (success) {
                refreshLocalBackups()
            }
            onComplete(success)
        }
    }

    fun restoreLocalBackup(file: java.io.File, onComplete: (com.example.util.BackupRestoreResult) -> Unit) {
        viewModelScope.launch {
            val result = com.example.util.BackupManager.restoreBackupFromFile(appContext, file)
            if (result.success) {
                // Refresh local UI states from restored preferences
                _themeMode.value = prefs.getString("theme_mode", "system") ?: "system"
                _whatsAppCallMode.value = prefs.getString("whatsapp_call_mode", "ask_learn") ?: "ask_learn"
                _callAnswerStyle.value = prefs.getString("call_answer_style", "swipe_slider") ?: "swipe_slider"
                _favoriteCardStyle.value = prefs.getString("favorite_card_style", "bento") ?: "bento"
                _confirmFavoritesCall.value = prefs.getBoolean("confirm_fav_calls", true)
                _confirmSpeedDialCall.value = prefs.getBoolean("confirm_speed_dial_call", true)
                _askToAssignUnassignedSpeedDial.value = prefs.getBoolean("ask_assign_unassigned_speed_dial", true)
                _speedDialKeypadDisplay.value = prefs.getString("speed_dial_keypad_display", "speed_dial_above") ?: "speed_dial_above"
                _showDialerQuickActions.value = prefs.getBoolean("show_dialer_quick_actions", true)
                _defaultStartTab.value = prefs.getInt("default_start_tab", 0)
                _swipeToSwitchPanels.value = prefs.getBoolean("swipe_to_switch_panels", true)
                _notSpamWhitelist.value = prefs.getStringSet("not_spam_whitelist", emptySet()) ?: emptySet()
                _learnedCallModes.value = loadLearnedCallModes()
                refreshContacts()
                refreshRecentCalls()
                refreshLocalBackups()
            }
            onComplete(result)
        }
    }

    fun deleteLocalBackup(file: java.io.File) {
        viewModelScope.launch {
            com.example.util.BackupManager.deleteLocalBackup(file)
            refreshLocalBackups()
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getInstance(context)
                    val repo = AppRepository(db.appDao())
                    return MainViewModel(repo, context.applicationContext) as T
                }
            }
    }
}
