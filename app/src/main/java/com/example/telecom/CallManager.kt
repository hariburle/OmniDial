package com.example.telecom

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.VideoProfile
import android.telephony.SmsManager
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.AutomationLog
import com.example.data.CallerRule
import com.example.data.RecentCall
import com.example.util.ContactHelper
import com.example.util.PhoneNumberNormalizer
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Immutable
data class ActiveCallInfo(
    val id: String,
    val phoneNumber: String,
    val displayName: String,
    val state: Int, // Call.STATE_*
    val isIncoming: Boolean,
    val connectTimeMillis: Long = 0L,
    val isSimulated: Boolean = false,
    val photoUri: String? = null,
    val callReason: String? = null,
    val communityInfo: com.example.util.CommunityCallerInfo? = null,
    val nickname: String? = null,
    val numberLabel: String? = null,
    val trustTier: com.example.domain.usecase.TrustTier = com.example.domain.usecase.TrustTier.NEUTRAL_UNKNOWN,
    val trustBadgeLabel: String? = null,
    val simSlot: Int = 1
)

@Immutable
data class AutomationStep(
    val ruleName: String,
    val stepDescription: String,
    val isRunning: Boolean = true,
    val completed: Boolean = false,
    val error: String? = null
)

@Immutable
data class BluetoothDeviceItem(
    val name: String,
    val address: String,
    val isCar: Boolean = false,
    val isHeadphone: Boolean = false
)

object CallManager {
    private const val TAG = "CallManager"

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var automationJob: Job? = null

    // Native Telecom Call instance if real call is active
    private var nativeCall: Call? = null
    private var telecomService: TelecomCallService? = null

    // Call UI State
    private val _activeCall = MutableStateFlow<ActiveCallInfo?>(null)
    val activeCall: StateFlow<ActiveCallInfo?> = _activeCall.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _currentAudioRoute = MutableStateFlow(CallAudioState.ROUTE_EARPIECE)
    val currentAudioRoute: StateFlow<Int> = _currentAudioRoute.asStateFlow()

    private val _supportedAudioRoutes = MutableStateFlow(CallAudioState.ROUTE_EARPIECE or CallAudioState.ROUTE_SPEAKER)
    val supportedAudioRoutes: StateFlow<Int> = _supportedAudioRoutes.asStateFlow()

    private val _bluetoothDeviceName = MutableStateFlow<String?>(null)
    val bluetoothDeviceName: StateFlow<String?> = _bluetoothDeviceName.asStateFlow()

    private val _availableBluetoothDevices = MutableStateFlow<List<BluetoothDeviceItem>>(emptyList())
    val availableBluetoothDevices: StateFlow<List<BluetoothDeviceItem>> = _availableBluetoothDevices.asStateFlow()

    private val _activeBluetoothDeviceAddress = MutableStateFlow<String?>(null)
    val activeBluetoothDeviceAddress: StateFlow<String?> = _activeBluetoothDeviceAddress.asStateFlow()

    private val _automationState = MutableStateFlow<AutomationStep?>(null)
    val automationState: StateFlow<AutomationStep?> = _automationState.asStateFlow()

    private val _lastDtmfKey = MutableStateFlow<Char?>(null)
    val lastDtmfKey: StateFlow<Char?> = _lastDtmfKey.asStateFlow()

    @Volatile
    var lastInsertedCallId: Long? = null
        private set

    @Volatile
    var isCallUiForegrounded: Boolean = false

    private val loggedCallSessionIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    private var simulatedTimerJob: Job? = null
    private var appContext: Context? = null
    private var proximityWakeLock: PowerManager.WakeLock? = null

    fun init(context: Context) {
        this.appContext = context.applicationContext
        OngoingCallNotificationHelper.createNotificationChannel(context)
        initProximityWakeLock(context)
    }

    private fun initProximityWakeLock(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager?.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK) == true) {
                proximityWakeLock = powerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "OmniDial:ProximityWakeLock"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to initialize proximity wake lock: ${e.message}")
        }
    }

    fun updateProximitySensor(context: Context) {
        val call = _activeCall.value
        val isCallOngoing = call != null &&
                call.state != Call.STATE_DISCONNECTED &&
                call.state != Call.STATE_DISCONNECTING
        val isHandsetRoute = _currentAudioRoute.value == CallAudioState.ROUTE_EARPIECE ||
                _currentAudioRoute.value == CallAudioState.ROUTE_WIRED_HEADSET

        if (isCallOngoing && isHandsetRoute) {
            acquireProximityWakeLock(context)
        } else {
            releaseProximityWakeLock()
        }
    }

    private fun acquireProximityWakeLock(context: Context) {
        try {
            if (proximityWakeLock == null) {
                initProximityWakeLock(context)
            }
            if (proximityWakeLock?.isHeld == false) {
                proximityWakeLock?.acquire()
                Log.d(TAG, "Acquired proximity screen off wake lock")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire proximity wake lock: ${e.message}")
        }
    }

    private fun releaseProximityWakeLock() {
        try {
            if (proximityWakeLock?.isHeld == true) {
                proximityWakeLock?.release()
                Log.d(TAG, "Released proximity screen off wake lock")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to release proximity wake lock: ${e.message}")
        }
    }

    fun setTelecomService(service: TelecomCallService?) {
        this.telecomService = service
        if (service != null) {
            this.appContext = service.applicationContext
            initProximityWakeLock(service.applicationContext)
        }
    }

    fun onCallAdded(call: Call, context: Context) {
        this.nativeCall = call
        this.appContext = context.applicationContext
        val number = extractPhoneNumber(call)
        val isVoicemail = ContactHelper.isVoicemailNumber(context, number)
        val lookedUp = ContactHelper.lookupContactByNumber(context, number)
        val favContact = try {
            runBlocking {
                val dao = AppDatabase.getInstance(context).appDao()
                dao.getAllFavoritesList().firstOrNull {
                    ContactHelper.isSamePhoneNumber(it.phoneNumber, number)
                }
            }
        } catch (_: Exception) { null }

        val communityInfo = if (lookedUp == null && favContact == null && !isVoicemail) com.example.util.CommunityCallerIdService.lookup(number) else null
        val isIncoming = call.state == Call.STATE_RINGING

        val resolvedNickname = lookedUp?.nickname?.ifBlank { null } ?: favContact?.nickname?.ifBlank { null }
        val resolvedLabel = lookedUp?.label?.ifBlank { null } ?: favContact?.label?.ifBlank { null } ?: "Mobile"

        val name = when {
            isVoicemail -> "Voicemail"
            lookedUp != null -> lookedUp.name
            favContact != null -> favContact.name
            communityInfo != null -> communityInfo.name
            !call.details?.callerDisplayName.isNullOrBlank() -> call.details!!.callerDisplayName
            isIncoming -> "Incoming Caller"
            number.isNotBlank() -> number
            else -> "Outgoing Call"
        }
        val photoUri = lookedUp?.photoUri ?: favContact?.photoUri

        val isWhitelisted = isWhitelistedOrRuleMatched(context, number)
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val autoBlockCarrier = prefs.getBoolean("auto_block_carrier_spam", true)
        val blockSpamPreset = prefs.getBoolean("block_telemarketers_robocalls", true)
        val silenceUnknownPrivate = prefs.getBoolean("silence_unknown_private", false)

        val normalizedNumber = PhoneNumberNormalizer.toE164(number)
        val matchedSpamNumber = if (isIncoming && !isWhitelisted) {
            try {
                runBlocking {
                    val dao = AppDatabase.getInstance(context).appDao()
                    dao.getSpamByNormalizedNumber(normalizedNumber) ?: dao.getSpamByNumber(number, normalizedNumber)
                }
            } catch (_: Exception) { null }
        } else null

        val isDatabaseSpam = matchedSpamNumber != null
        val isCarrierSpamThreat = isIncoming && !isWhitelisted && autoBlockCarrier && isCarrierSpam(call, number, name)
        val shouldAutoDeclineSpam = (isCarrierSpamThreat || (isDatabaseSpam && blockSpamPreset))

        val resolvedSimSlot = SimHelper.resolveSimSlot(
            context = context,
            accountHandle = call.details?.accountHandle
        )

        if (shouldAutoDeclineSpam) {
            val spamReason = when {
                matchedSpamNumber != null -> matchedSpamNumber.label.ifBlank { "Known Spam Number" }
                isCarrierSpamThreat -> "Carrier Spam Filter"
                else -> "Spam Block Preset"
            }
            Log.w(TAG, "Spam blocked ($spamReason) for incoming call from $number. Auto-rejecting before ringing.")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    call.reject(Call.REJECT_REASON_DECLINED)
                } else {
                    @Suppress("DEPRECATION")
                    call.reject(false, null)
                }
            } catch (_: Exception) {
                call.disconnect()
            }
            scope.launch(Dispatchers.IO) {
                try {
                    val dao = AppDatabase.getInstance(context).appDao()
                    dao.insertAutomationLog(
                        AutomationLog(
                            phoneNumber = number,
                            ruleName = spamReason,
                            actionsSummary = "Auto-rejected incoming spam call ($spamReason) before ringing device",
                            status = "BLOCKED"
                        )
                    )
                    dao.insertRecentCall(
                        RecentCall(
                            phoneNumber = number,
                            callerName = name.ifBlank { "Spam Threat" },
                            callType = 3,
                            timestamp = System.currentTimeMillis(),
                            durationSeconds = 0,
                            ruleMatched = spamReason,
                            isSpam = true,
                            callReason = "$spamReason auto-dropped",
                            note = null,
                            simSlot = resolvedSimSlot
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error writing spam log", e)
                }
            }
            SpamNotificationHelper.showBlockedSpamNotification(context, number, name)
            return
        }

        val trustBadge = resolveTrustBadge(
            isVoicemail = isVoicemail,
            hasContact = lookedUp != null,
            hasFav = favContact != null,
            isSpam = isDatabaseSpam || isCarrierSpamThreat,
            communityInfo = communityInfo
        )

        val callInfo = ActiveCallInfo(
            id = call.hashCode().toString(),
            phoneNumber = number,
            displayName = name,
            state = call.state,
            isIncoming = isIncoming,
            connectTimeMillis = if (call.state == Call.STATE_ACTIVE) System.currentTimeMillis() else 0L,
            isSimulated = false,
            photoUri = photoUri,
            communityInfo = communityInfo,
            nickname = resolvedNickname,
            numberLabel = resolvedLabel,
            trustTier = trustBadge.first,
            trustBadgeLabel = trustBadge.second,
            simSlot = resolvedSimSlot
        )
        _activeCall.value = callInfo

        call.registerCallback(object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                Log.d(TAG, "Call state changed: $state")
                val current = _activeCall.value
                if (current != null) {
                    val connectTime = if (state == Call.STATE_ACTIVE && current.connectTimeMillis == 0L) {
                        com.example.util.HapticFeedbackHelper.performCallConnected(context)
                        System.currentTimeMillis()
                    } else current.connectTimeMillis

                    _activeCall.value = current.copy(state = state, connectTimeMillis = connectTime)
                    CallForegroundService.start(context)
                    OngoingCallNotificationHelper.showCallNotification(context, _activeCall.value!!)
                }

                if (state == Call.STATE_DISCONNECTED) {
                    releaseProximityWakeLock()
                    handleCallEnded(context, current)
                } else {
                    updateProximitySensor(context)
                }
            }
        })

        CallForegroundService.start(context)
        OngoingCallNotificationHelper.showCallNotification(context, callInfo)
        updateProximitySensor(context)

        // Check Do Not Disturb (DND) or Silence Unknown/Private status
        try {
            val isKnownCaller = (lookedUp != null || favContact != null)
            if (isIncoming && silenceUnknownPrivate && !isKnownCaller && !isWhitelisted) {
                Log.d(TAG, "Incoming call from unknown caller $number silenced by Silence Unknown/Private preset")
                FlipToShhhManager.silenceIncomingCallIfRinging(context)
            } else {
                val isFavoriteCaller = lookedUp?.isStarred == true
                if (FlipToShhhManager.isDndActive(context)) {
                    val allowed = FlipToShhhManager.isCallerAllowedUnderCurrentDnd(context, isFavoriteCaller)
                    if (!allowed) {
                        Log.d(TAG, "Incoming call from $number silenced by Do Not Disturb")
                        FlipToShhhManager.silenceIncomingCallIfRinging(context)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking DND / silence policy for caller", e)
        }

        // Check if selective automation rule matches
        checkAndExecuteAutomation(context, number, isIncoming)
    }

    fun onCallRemoved(call: Call, context: Context) {
        if (nativeCall == call) {
            handleCallEnded(context, _activeCall.value)
            nativeCall = null
        }
    }

    private fun handleCallEnded(context: Context, callInfo: ActiveCallInfo?) {
        releaseProximityWakeLock()
        TelecomVoipHelper.endVoipCall()
        automationJob?.cancel()
        automationJob = null
        simulatedTimerJob?.cancel()
        simulatedTimerJob = null
        CallForegroundService.stop(context)
        OngoingCallNotificationHelper.cancelCallNotification(context)
        appContext?.let { ctx ->
            CallForegroundService.stop(ctx)
            OngoingCallNotificationHelper.cancelCallNotification(ctx)
        }

        if (callInfo != null) {
            val sessionId = callInfo.id
            val isAlreadyLogged = synchronized(loggedCallSessionIds) {
                if (loggedCallSessionIds.contains(sessionId)) {
                    true
                } else {
                    loggedCallSessionIds.add(sessionId)
                    false
                }
            }

            if (!isAlreadyLogged) {
                val duration = if (callInfo.connectTimeMillis > 0) {
                    (System.currentTimeMillis() - callInfo.connectTimeMillis) / 1000
                } else 0L

                val callType = if (callInfo.isIncoming) {
                    if (duration > 0) 1 else 3 // 1 = Incoming, 3 = Missed
                } else 2 // Outgoing

                scope.launch(Dispatchers.IO) {
                    try {
                        val dao = AppDatabase.getInstance(context).appDao()
                        val latest = dao.getLatestRecentCallForNumber(callInfo.phoneNumber)
                        if (latest != null && Math.abs(System.currentTimeMillis() - latest.timestamp) < 4000L) {
                            Log.d(TAG, "Duplicate recent call write suppressed for ${callInfo.phoneNumber} within 4s window")
                            lastInsertedCallId = latest.id
                            return@launch
                        }

                        val insertedId = dao.insertRecentCall(
                            RecentCall(
                                phoneNumber = callInfo.phoneNumber,
                                callerName = callInfo.displayName,
                                photoUri = callInfo.photoUri,
                                callType = callType,
                                timestamp = System.currentTimeMillis(),
                                durationSeconds = duration,
                                ruleMatched = _automationState.value?.ruleName,
                                callReason = callInfo.callReason,
                                communityTag = callInfo.communityInfo?.category,
                                simSlot = callInfo.simSlot
                            )
                        )
                        lastInsertedCallId = insertedId

                        if (callType == 3) {
                            OngoingCallNotificationHelper.showMissedCallNotification(
                                context,
                                callInfo.phoneNumber,
                                callInfo.displayName
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to log recent call", e)
                    }
                }
            } else {
                Log.d(TAG, "Call session $sessionId already recorded. Skipping duplicate insert.")
            }
        }

        // Post-call state: keep in STATE_DISCONNECTED so InCallScreen note-taking panel can display.
        // It will be dismissed by the user or by InCallScreen's auto-close timer if not interacted with.
        val current = _activeCall.value
        if (current != null) {
            _activeCall.value = current.copy(state = Call.STATE_DISCONNECTED)
        }
        _isMuted.value = false
        _isSpeakerOn.value = false
    }

    /**
     * Carrier-Level Metadata and STIR/SHAKEN Spam Detection
     */
    fun isCarrierSpam(call: Call, number: String, name: String): Boolean {
        val details = call.details
        if (details != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (details.callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED) {
                    Log.w(TAG, "Carrier STIR/SHAKEN verification failed for $number")
                    return true
                }
            }
            val extras = details.extras
            if (extras != null) {
                val isSpamExtra = extras.getBoolean("android.telecom.extra.IS_SPAM", false) ||
                    extras.getBoolean("carrier_spam_flag", false) ||
                    extras.getBoolean("com.android.telecom.extra.IS_SPAM", false)
                if (isSpamExtra) return true

                val spamLabel = extras.getString("com.android.telecom.extra.SPAM_LABEL")
                    ?: extras.getString("android.telecom.extra.SPAM_LABEL")
                    ?: ""
                if (spamLabel.contains("spam", ignoreCase = true) ||
                    spamLabel.contains("scam", ignoreCase = true) ||
                    spamLabel.contains("fraud", ignoreCase = true)) {
                    return true
                }
            }
            val callerDisplayName = details.callerDisplayName ?: ""
            if (callerDisplayName.contains("Spam", ignoreCase = true) ||
                callerDisplayName.contains("Scam", ignoreCase = true) ||
                callerDisplayName.contains("Robocall", ignoreCase = true) ||
                callerDisplayName.contains("Fraud", ignoreCase = true)) {
                return true
            }
        }
        if (name.contains("Spam", ignoreCase = true) ||
            name.contains("Scam Likely", ignoreCase = true) ||
            name.contains("Robocall", ignoreCase = true) ||
            name.contains("Fraud", ignoreCase = true)) {
            return true
        }
        return false
    }

    private fun isWhitelistedOrRuleMatched(context: Context, number: String): Boolean {
        if (number.isBlank()) return false
        try {
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val notSpamSet = prefs.getStringSet("not_spam_whitelist", emptySet()) ?: emptySet()
            if (notSpamSet.any { ContactHelper.isSamePhoneNumber(it, number) }) {
                Log.d(TAG, "Number $number is in user Not-Spam Whitelist. Whitelisted from carrier spam filter.")
                return true
            }

            val dao = AppDatabase.getInstance(context).appDao()
            val rules = runBlocking { dao.getEnabledRules() }
            val ruleMatched = rules.any { rule ->
                rule.isEnabled && matchesRulePattern(rule.phoneNumberPattern, number)
            }
            if (ruleMatched) {
                Log.d(TAG, "Number $number matched user automation rule. Whitelisted from carrier spam filter.")
                return true
            }

            val favs = runBlocking { dao.getAllFavoritesList() }
            val isFav = favs.any { f ->
                ContactHelper.isSamePhoneNumber(f.phoneNumber, number)
            }
            if (isFav) {
                Log.d(TAG, "Number $number is in Starred Favorites. Whitelisted from carrier spam filter.")
                return true
            }

            val contact = ContactHelper.lookupContactByNumber(context, number)
            if (contact != null) {
                Log.d(TAG, "Number $number found in saved contacts. Whitelisted from carrier spam filter.")
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking whitelist for $number", e)
        }
        return false
    }

    fun dismissActiveCall() {
        _activeCall.value = null
        _automationState.value = null
        _isMuted.value = false
        _isSpeakerOn.value = false
        automationJob?.cancel()
        automationJob = null
        simulatedTimerJob?.cancel()
        simulatedTimerJob = null
    }

    private fun extractPhoneNumber(call: Call): String {
        val handle: Uri? = call.details?.handle
        if (handle != null) {
            val scheme = handle.schemeSpecificPart
            if (!scheme.isNullOrBlank()) return scheme
        }
        return "Unknown"
    }

    /**
     * Inspects active incoming call against configured automation rules.
     * If rule matches: executes automated workflow.
     * If NO rule matches: leaves manual control to user.
     */
    private fun matchesRulePattern(rawNumber: String, pattern: String): Boolean {
        val cleanPattern = pattern.trim()
        if (cleanPattern == "*") return true
        fun normDigits(s: String) = s.filter { it.isDigit() }.takeLast(10)
        val normTarget = normDigits(rawNumber)
        val normPat = normDigits(cleanPattern)
        if (normPat.isNotBlank() && normTarget.isNotBlank()) {
            return normTarget == normPat || normTarget.endsWith(normPat) || normPat.endsWith(normTarget)
        }
        return rawNumber.contains(cleanPattern, ignoreCase = true)
    }

    fun checkAndExecuteAutomation(context: Context, rawNumber: String, isIncoming: Boolean) {
        if (!isIncoming) return

        automationJob?.cancel()
        automationJob = scope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).appDao()

            // 1. Evaluate user-configured automation rules FIRST (user rules take top priority)
            val rules = dao.getEnabledRules()
            val matchedRule = rules.firstOrNull { rule -> matchesRulePattern(rawNumber, rule.phoneNumberPattern) }

            if (matchedRule != null) {
                Log.d(TAG, "Matched automation rule '${matchedRule.name}' for caller $rawNumber")
                executeAutomationWorkflow(context, matchedRule, rawNumber)
                return@launch
            }

            // 2. Only if no automation rule matched, check offline spam blocklist
            val spamEntry = dao.getSpamByNumber(rawNumber)
            if (spamEntry != null && spamEntry.isBlocked) {
                Log.d(TAG, "Spam number detected ($rawNumber). Auto-blocking call.")
                _automationState.value = AutomationStep(
                    ruleName = "Spam Shield",
                    stepDescription = "Blocked spam call from ${spamEntry.label}"
                )
                delay(500)
                declineCall()
                return@launch
            }

            Log.d(TAG, "No automation rule matched for caller $rawNumber. Showing standard in-call UI.")
            _automationState.value = null
        }
    }

    private suspend fun executeAutomationWorkflow(context: Context, rule: CallerRule, phoneNumber: String) {
        val actions = mutableListOf<String>()

        _automationState.value = AutomationStep(
            ruleName = rule.name,
            stepDescription = "Rule matched! Preparing auto-answer in ${rule.answerDelaySec}s..."
        )

        // Step 1: Pre-answer delay
        if (rule.answerDelaySec > 0) {
            delay(rule.answerDelaySec * 1000L)
        }

        // Step 2: Auto Answer
        if (rule.autoAnswer) {
            _automationState.value = AutomationStep(
                ruleName = rule.name,
                stepDescription = "Answering call via Call.answer(0)..."
            )
            actions.add("Auto-Answered")
            answerCall()

            // Wait for call to connect
            var waitCount = 0
            while (_activeCall.value?.state != Call.STATE_ACTIVE && waitCount < 15) {
                delay(300)
                waitCount++
            }
        }

        // Step 3: DTMF Sequence Transmission
        if (rule.dtmfSequence.isNotBlank()) {
            if (rule.dtmfDelayMs > 0) {
                _automationState.value = AutomationStep(
                    ruleName = rule.name,
                    stepDescription = "Waiting ${rule.dtmfDelayMs}ms before DTMF transmission..."
                )
                delay(rule.dtmfDelayMs)
            }

            _automationState.value = AutomationStep(
                ruleName = rule.name,
                stepDescription = "Transmitting in-band DTMF sequence: '${rule.dtmfSequence}'..."
            )

            for (char in rule.dtmfSequence) {
                if (char.isDigit() || char == '*' || char == '#') {
                    _lastDtmfKey.value = char
                    playDtmf(char)
                    delay(220) // Tone duration
                    stopDtmf()
                    delay(260) // Pause between digits
                }
            }
            _lastDtmfKey.value = null
            actions.add("Sent DTMF '${rule.dtmfSequence}'")
        }

        // Step 4: Send Auto-SMS Reply
        if (rule.sendSms && rule.smsMessage.isNotBlank()) {
            _automationState.value = AutomationStep(
                ruleName = rule.name,
                stepDescription = "Sending auto-reply SMS via SmsManager..."
            )
            sendSmsBackground(context, phoneNumber, rule.smsMessage)
            actions.add("Sent SMS auto-reply")
            delay(500)
        }

        // Step 5: Auto Hangup
        if (rule.autoHangup) {
            _automationState.value = AutomationStep(
                ruleName = rule.name,
                stepDescription = "Auto-hanging up in ${rule.hangupDelaySec}s..."
            )
            delay(rule.hangupDelaySec * 1000L)
            actions.add("Auto-Disconnected")
            disconnectCall()
        }

        _automationState.value = AutomationStep(
            ruleName = rule.name,
            stepDescription = "Automation completed successfully.",
            isRunning = false,
            completed = true
        )

        // Log to database
        try {
            val dao = AppDatabase.getInstance(context).appDao()
            dao.insertAutomationLog(
                AutomationLog(
                    phoneNumber = phoneNumber,
                    ruleName = rule.name,
                    actionsSummary = actions.joinToString(" -> "),
                    status = "SUCCESS"
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write automation log", e)
        }
    }

    // Call Actions
    fun answerCall() {
        val current = _activeCall.value ?: return
        if (current.isSimulated) {
            val updated = current.copy(
                state = Call.STATE_ACTIVE,
                connectTimeMillis = System.currentTimeMillis()
            )
            _activeCall.value = updated
            appContext?.let { ctx ->
                CallForegroundService.start(ctx)
                OngoingCallNotificationHelper.showCallNotification(ctx, updated)
            }
            telecomService?.let {
                CallForegroundService.start(it)
                OngoingCallNotificationHelper.showCallNotification(it, updated)
            }
        } else {
            try {
                nativeCall?.answer(VideoProfile.STATE_AUDIO_ONLY)
            } catch (e: Exception) {
                Log.e(TAG, "Error answering native call", e)
            }
        }
    }

    fun declineCall() {
        val current = _activeCall.value ?: return
        if (current.isSimulated) {
            appContext?.let { handleCallEnded(it, current) } ?: run {
                _activeCall.value = current.copy(state = Call.STATE_DISCONNECTED)
            }
        } else {
            try {
                nativeCall?.reject(false, null)
            } catch (e: Exception) {
                Log.e(TAG, "Error rejecting native call", e)
            }
        }
    }

    fun declineWithSms(context: Context, message: String) {
        val current = _activeCall.value
        val number = current?.phoneNumber ?: ""
        if (number.isNotBlank() && message.isNotBlank()) {
            sendSmsBackground(context, number, message)
        }
        declineCall()
    }

    fun disconnectCall() {
        TelecomVoipHelper.endVoipCall()
        val current = _activeCall.value ?: return
        if (current.isSimulated) {
            appContext?.let { handleCallEnded(it, current) } ?: run {
                _activeCall.value = current.copy(state = Call.STATE_DISCONNECTED)
            }
        } else {
            try {
                nativeCall?.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting native call", e)
            }
        }
    }

    fun playDtmf(digit: Char) {
        _lastDtmfKey.value = digit
        if (nativeCall != null) {
            try {
                nativeCall?.playDtmfTone(digit)
            } catch (e: Exception) {
                Log.e(TAG, "Error playing DTMF tone", e)
            }
        }
    }

    fun stopDtmf() {
        _lastDtmfKey.value = null
        if (nativeCall != null) {
            try {
                nativeCall?.stopDtmfTone()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping DTMF tone", e)
            }
        }
    }

    fun onCallAudioStateChanged(audioState: CallAudioState) {
        _isMuted.value = audioState.isMuted
        _currentAudioRoute.value = audioState.route
        _supportedAudioRoutes.value = audioState.supportedRouteMask
        _isSpeakerOn.value = (audioState.route == CallAudioState.ROUTE_SPEAKER)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val supportedBt = audioState.supportedBluetoothDevices.toList()
            val btList = supportedBt.mapIndexed { index, device ->
                resolveBluetoothDevice(device, index, supportedBt.size)
            }
            _availableBluetoothDevices.value = btList
            _activeBluetoothDeviceAddress.value = audioState.activeBluetoothDevice?.address
        }

        val btDevice = audioState.activeBluetoothDevice
        _bluetoothDeviceName.value = btDevice?.let { device ->
            resolveBluetoothDevice(device, 0, 1).name
        } ?: if ((audioState.supportedRouteMask and CallAudioState.ROUTE_BLUETOOTH) != 0) "Bluetooth Device" else null
        appContext?.let { updateProximitySensor(it) }
    }

    private fun resolveBluetoothDevice(device: BluetoothDevice, index: Int, totalDevices: Int): BluetoothDeviceItem {
        var resolvedName: String? = null

        // 1. Try device.alias (API 30+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                resolvedName = device.alias?.takeIf { it.isNotBlank() }
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException reading device.alias", e)
            } catch (e: Exception) {
                Log.w(TAG, "Exception reading device.alias", e)
            }
        }

        // 2. Try device.name
        if (resolvedName == null) {
            try {
                resolvedName = device.name?.takeIf { it.isNotBlank() }
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException reading device.name", e)
            } catch (e: Exception) {
                Log.w(TAG, "Exception reading device.name", e)
            }
        }

        // 3. Try checking bonded devices from BluetoothManager / BluetoothAdapter
        if (resolvedName == null || resolvedName.equals("Bluetooth Device", ignoreCase = true)) {
            try {
                val btManager = appContext?.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                val bonded = btManager?.adapter?.bondedDevices
                val match = bonded?.firstOrNull { it.address == device.address }
                if (match != null) {
                    val bondedName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try { match.alias } catch (e: SecurityException) { null }
                            ?: try { match.name } catch (e: SecurityException) { null }
                    } else {
                        try { match.name } catch (e: SecurityException) { null }
                    }
                    if (!bondedName.isNullOrBlank()) {
                        resolvedName = bondedName
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException reading bonded devices", e)
            } catch (e: Exception) {
                Log.w(TAG, "Exception accessing BluetoothAdapter", e)
            }
        }

        // 4. Check BluetoothClass for device type
        val btClass = try {
            device.bluetoothClass
        } catch (e: Exception) {
            null
        }
        val deviceClass = btClass?.deviceClass ?: 0
        val isCarClass = deviceClass == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
                         deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE
        val isHeadphoneClass = deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                               deviceClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
                               deviceClass == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER

        val nameToCheck = resolvedName ?: ""
        val isCar = isCarClass || isCarBluetooth(nameToCheck)
        val isHeadphone = isHeadphoneClass || isHeadphoneBluetooth(nameToCheck)

        // 5. Final friendly name formatting
        val finalName = when {
            !resolvedName.isNullOrBlank() && !resolvedName.equals("Bluetooth Device", ignoreCase = true) -> resolvedName
            isCar -> if (totalDevices > 1) "Car Bluetooth (${index + 1})" else "Car Bluetooth"
            isHeadphone -> if (totalDevices > 1) "Bluetooth Headset (${index + 1})" else "Bluetooth Headset"
            totalDevices > 1 -> "Bluetooth Device ${index + 1}"
            else -> "Bluetooth Device"
        }

        return BluetoothDeviceItem(
            name = finalName,
            address = device.address,
            isCar = isCar,
            isHeadphone = isHeadphone
        )
    }

    fun selectBluetoothDevice(address: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val audioState = telecomService?.callAudioState
            val targetDevice = audioState?.supportedBluetoothDevices?.firstOrNull { it.address == address }
            if (targetDevice != null) {
                telecomService?.requestBluetoothAudio(targetDevice)
                _activeBluetoothDeviceAddress.value = address
                _currentAudioRoute.value = CallAudioState.ROUTE_BLUETOOTH
                return
            }
        }
        val match = _availableBluetoothDevices.value.firstOrNull { it.address == address }
        if (match != null) {
            _activeBluetoothDeviceAddress.value = address
            _bluetoothDeviceName.value = match.name
            _currentAudioRoute.value = CallAudioState.ROUTE_BLUETOOTH
        } else {
            setAudioRoute(CallAudioState.ROUTE_BLUETOOTH)
        }
    }

    private fun isCarBluetooth(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("car") || lower.contains("vehicle") || lower.contains("auto") ||
               lower.contains("handsfree") || lower.contains("uconnect") || lower.contains("sync") ||
               lower.contains("bmw") || lower.contains("audi") || lower.contains("tesla") ||
               lower.contains("mercedes") || lower.contains("toyota") || lower.contains("honda") ||
               lower.contains("ford") || lower.contains("hyundai") || lower.contains("kia") ||
               lower.contains("chevrolet") || lower.contains("nissan") || lower.contains("subaru") ||
               lower.contains("mazda") || lower.contains("volvo") || lower.contains("porsche")
    }

    private fun isHeadphoneBluetooth(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("buds") || lower.contains("airpod") || lower.contains("headphone") ||
               lower.contains("headset") || lower.contains("earphone") || lower.contains("wh-") ||
               lower.contains("wf-") || lower.contains("bose") || lower.contains("sony") ||
               lower.contains("beats") || lower.contains("jabra") || lower.contains("sennheiser") ||
               lower.contains("pixel buds") || lower.contains("galaxy buds")
    }

    fun setAudioRoute(route: Int) {
        _currentAudioRoute.value = route
        _isSpeakerOn.value = (route == CallAudioState.ROUTE_SPEAKER)
        if (route == CallAudioState.ROUTE_BLUETOOTH && _activeBluetoothDeviceAddress.value == null) {
            val first = _availableBluetoothDevices.value.firstOrNull()
            if (first != null) {
                _activeBluetoothDeviceAddress.value = first.address
                _bluetoothDeviceName.value = first.name
            }
        }
        telecomService?.setAudioRoute(route)
        appContext?.let { ctx ->
            updateProximitySensor(ctx)
            _activeCall.value?.let { OngoingCallNotificationHelper.showCallNotification(ctx, it) }
        }
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        telecomService?.setMuted(newMuted)
        appContext?.let { ctx ->
            _activeCall.value?.let { OngoingCallNotificationHelper.showCallNotification(ctx, it) }
        }
    }

    fun toggleSpeaker() {
        val newRoute = if (_isSpeakerOn.value) {
            if ((_supportedAudioRoutes.value and CallAudioState.ROUTE_BLUETOOTH) != 0) {
                CallAudioState.ROUTE_BLUETOOTH
            } else {
                CallAudioState.ROUTE_EARPIECE
            }
        } else {
            CallAudioState.ROUTE_SPEAKER
        }
        setAudioRoute(newRoute)
    }

    private fun sendSmsBackground(context: Context, destination: String, text: String) {
        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(destination, null, text, null, null)
            Log.d(TAG, "Sent automated SMS to $destination: $text")
        } catch (e: Exception) {
            Log.w(TAG, "Unable to send SMS in background (needs SIM/permission): ${e.message}")
        }
    }

    /**
     * Simulator for testing in emulator environment where no GSM carrier is present.
     */
    fun startSimulatedIncomingCall(context: Context, number: String, name: String, reason: String? = null) {
        appContext = context.applicationContext
        automationJob?.cancel()
        updateBluetoothDevicesForSimulation(context)
        val lookedUp = ContactHelper.lookupContactByNumber(context, number)
        val favContact = try {
            runBlocking {
                val dao = AppDatabase.getInstance(context).appDao()
                dao.getAllFavoritesList().firstOrNull {
                    ContactHelper.isSamePhoneNumber(it.phoneNumber, number)
                }
            }
        } catch (_: Exception) { null }
        val communityInfo = if (lookedUp == null && favContact == null) com.example.util.CommunityCallerIdService.lookup(number) else null
        val resolvedName = lookedUp?.name ?: favContact?.name ?: communityInfo?.name ?: name
        val resolvedNickname = lookedUp?.nickname?.ifBlank { null } ?: favContact?.nickname?.ifBlank { null }
        val resolvedLabel = lookedUp?.label?.ifBlank { null } ?: favContact?.label?.ifBlank { null } ?: "Mobile"
        val photoUri = lookedUp?.photoUri ?: favContact?.photoUri
        val trustBadge = resolveTrustBadge(
            isVoicemail = ContactHelper.isVoicemailNumber(context, number),
            hasContact = lookedUp != null,
            hasFav = favContact != null,
            isSpam = communityInfo?.verificationType?.contains("Spam", ignoreCase = true) == true || (communityInfo?.spamScore ?: 0) >= 50,
            communityInfo = communityInfo
        )
        val callInfo = ActiveCallInfo(
            id = "sim_${System.currentTimeMillis()}",
            phoneNumber = number,
            displayName = resolvedName,
            state = Call.STATE_RINGING,
            isIncoming = true,
            connectTimeMillis = 0L,
            isSimulated = true,
            photoUri = photoUri,
            callReason = reason ?: communityInfo?.defaultCallReason,
            communityInfo = communityInfo,
            nickname = resolvedNickname,
            numberLabel = resolvedLabel,
            trustTier = trustBadge.first,
            trustBadgeLabel = trustBadge.second
        )
        _activeCall.value = callInfo
        TelecomVoipHelper.startVoipCall(context, number, resolvedName, isIncoming = true)
        CallForegroundService.start(context)
        OngoingCallNotificationHelper.showCallNotification(context, callInfo)
        checkAndExecuteAutomation(context, number, true)
    }

    fun startSimulatedOutgoingCall(context: Context, number: String, reason: String? = null) {
        appContext = context.applicationContext
        automationJob?.cancel()
        updateBluetoothDevicesForSimulation(context)
        val isVoicemail = ContactHelper.isVoicemailNumber(context, number)
        val lookedUp = ContactHelper.lookupContactByNumber(context, number)
        val favContact = try {
            runBlocking {
                val dao = AppDatabase.getInstance(context).appDao()
                dao.getAllFavoritesList().firstOrNull {
                    ContactHelper.isSamePhoneNumber(it.phoneNumber, number)
                }
            }
        } catch (_: Exception) { null }
        val communityInfo = if (lookedUp == null && favContact == null && !isVoicemail) com.example.util.CommunityCallerIdService.lookup(number) else null
        val resolvedName = when {
            isVoicemail -> "Voicemail"
            lookedUp != null -> lookedUp.name
            favContact != null -> favContact.name
            communityInfo != null -> communityInfo.name
            number.isNotBlank() -> number
            else -> "Outgoing Call"
        }
        val resolvedNickname = lookedUp?.nickname?.ifBlank { null } ?: favContact?.nickname?.ifBlank { null }
        val resolvedLabel = lookedUp?.label?.ifBlank { null } ?: favContact?.label?.ifBlank { null } ?: "Mobile"
        val photoUri = lookedUp?.photoUri ?: favContact?.photoUri
        val trustBadge = resolveTrustBadge(
            isVoicemail = isVoicemail,
            hasContact = lookedUp != null,
            hasFav = favContact != null,
            isSpam = communityInfo?.verificationType?.contains("Spam", ignoreCase = true) == true || (communityInfo?.spamScore ?: 0) >= 50,
            communityInfo = communityInfo
        )
        val callInfo = ActiveCallInfo(
            id = "sim_out_${System.currentTimeMillis()}",
            phoneNumber = number,
            displayName = resolvedName,
            state = Call.STATE_DIALING,
            isIncoming = false,
            connectTimeMillis = 0L,
            isSimulated = true,
            photoUri = photoUri,
            callReason = reason,
            communityInfo = communityInfo,
            nickname = resolvedNickname,
            numberLabel = resolvedLabel,
            trustTier = trustBadge.first,
            trustBadgeLabel = trustBadge.second
        )
        _activeCall.value = callInfo
        TelecomVoipHelper.startVoipCall(context, number, resolvedName, isIncoming = false)
        CallForegroundService.start(context)
        OngoingCallNotificationHelper.showCallNotification(context, callInfo)

        scope.launch {
            delay(1500)
            val current = _activeCall.value
            if (current != null && current.state == Call.STATE_DIALING) {
                val updated = current.copy(
                    state = Call.STATE_ACTIVE,
                    displayName = resolvedName,
                    connectTimeMillis = System.currentTimeMillis()
                )
                _activeCall.value = updated
                CallForegroundService.start(context)
                OngoingCallNotificationHelper.showCallNotification(context, updated)
            }
        }
    }

    fun updateBluetoothDevicesForSimulation(context: Context) {
        appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // If permission is not granted, populate mock devices for simulated/testing experience!
                _availableBluetoothDevices.value = listOf(
                    BluetoothDeviceItem(
                        name = "Tesla Model 3",
                        address = "00:11:22:33:44:55",
                        isCar = true,
                        isHeadphone = false
                    ),
                    BluetoothDeviceItem(
                        name = "Sony WH-1000XM4",
                        address = "66:77:88:99:AA:BB",
                        isCar = false,
                        isHeadphone = true
                    )
                )
                _supportedAudioRoutes.value = _supportedAudioRoutes.value or CallAudioState.ROUTE_BLUETOOTH
                _bluetoothDeviceName.value = "Sony WH-1000XM4"
                _activeBluetoothDeviceAddress.value = "66:77:88:99:AA:BB"
                return
            }
        }

        try {
            val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = btManager?.adapter
            if (adapter != null && adapter.isEnabled) {
                val bonded = adapter.bondedDevices
                if (!bonded.isNullOrEmpty()) {
                    val btList = bonded.mapIndexed { index, device ->
                        resolveBluetoothDevice(device, index, bonded.size)
                    }
                    _availableBluetoothDevices.value = btList
                    _supportedAudioRoutes.value = _supportedAudioRoutes.value or CallAudioState.ROUTE_BLUETOOTH
                    if (_activeBluetoothDeviceAddress.value == null) {
                        val first = btList.first()
                        _activeBluetoothDeviceAddress.value = first.address
                        _bluetoothDeviceName.value = first.name
                    }
                } else {
                    // No bonded devices, populate mock devices for rich visual feedback and testing!
                    _availableBluetoothDevices.value = listOf(
                        BluetoothDeviceItem(
                            name = "Tesla Model 3",
                            address = "00:11:22:33:44:55",
                            isCar = true,
                            isHeadphone = false
                        ),
                        BluetoothDeviceItem(
                            name = "Sony WH-1000XM4",
                            address = "66:77:88:99:AA:BB",
                            isCar = false,
                            isHeadphone = true
                        )
                    )
                    _supportedAudioRoutes.value = _supportedAudioRoutes.value or CallAudioState.ROUTE_BLUETOOTH
                    _bluetoothDeviceName.value = "Sony WH-1000XM4"
                    _activeBluetoothDeviceAddress.value = "66:77:88:99:AA:BB"
                }
            } else {
                // Adapter not enabled or null, populate mock devices for simulation
                _availableBluetoothDevices.value = listOf(
                    BluetoothDeviceItem(
                        name = "Tesla Model 3",
                        address = "00:11:22:33:44:55",
                        isCar = true,
                        isHeadphone = false
                    ),
                    BluetoothDeviceItem(
                        name = "Sony WH-1000XM4",
                        address = "66:77:88:99:AA:BB",
                        isCar = false,
                        isHeadphone = true
                    )
                )
                _supportedAudioRoutes.value = _supportedAudioRoutes.value or CallAudioState.ROUTE_BLUETOOTH
                _bluetoothDeviceName.value = "Sony WH-1000XM4"
                _activeBluetoothDeviceAddress.value = "66:77:88:99:AA:BB"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update bluetooth devices from system", e)
        }
    }

    fun resolveTrustBadge(
        isVoicemail: Boolean,
        hasContact: Boolean,
        hasFav: Boolean,
        isSpam: Boolean,
        communityInfo: com.example.util.CommunityCallerInfo?
    ): Pair<com.example.domain.usecase.TrustTier, String?> {
        return when {
            isSpam -> Pair(com.example.domain.usecase.TrustTier.HIGH_RISK_SPAM, "High Spam Risk")
            isVoicemail -> Pair(com.example.domain.usecase.TrustTier.VERIFIED_BUSINESS, "System Voicemail")
            hasContact -> Pair(com.example.domain.usecase.TrustTier.VERIFIED_BUSINESS, "Saved Contact")
            hasFav -> Pair(com.example.domain.usecase.TrustTier.VERIFIED_BUSINESS, "Favorite")
            communityInfo != null && (communityInfo.category.contains("Delivery", ignoreCase = true) ||
                communityInfo.category.contains("Logistics", ignoreCase = true) ||
                communityInfo.verificationType.contains("Delivery", ignoreCase = true) ||
                communityInfo.name.contains("Delivery", ignoreCase = true) ||
                communityInfo.defaultCallReason?.contains("Buzzer", ignoreCase = true) == true) ->
                Pair(com.example.domain.usecase.TrustTier.PRIORITY_LOGISTICS, "Priority Delivery")
            communityInfo != null && (communityInfo.isVerified ||
                communityInfo.verificationType.contains("Verified", ignoreCase = true) ||
                communityInfo.verificationType.contains("Financial", ignoreCase = true)) ->
                Pair(com.example.domain.usecase.TrustTier.VERIFIED_BUSINESS, communityInfo.verificationType)
            communityInfo != null -> Pair(com.example.domain.usecase.TrustTier.NEUTRAL_UNKNOWN, "Community Identified")
            else -> Pair(com.example.domain.usecase.TrustTier.NEUTRAL_UNKNOWN, null)
        }
    }

    private fun normalizePhoneNumber(raw: String): String {
        return raw.filter { it.isDigit() }
    }
}
