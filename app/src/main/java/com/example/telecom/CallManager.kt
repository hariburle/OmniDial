@file:Suppress("DEPRECATION")

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
import android.telecom.TelecomManager
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val simSlot: Int = 1,
    val simDisplayName: String? = null,
    val isRoaming: Boolean = false,
    // Conference capabilities, refreshed from Call.Details (kept for diagnostics;
    // button visibility no longer depends on them — see ConferenceUiGating.kt)
    val canMergeConference: Boolean = false,
    val canSwapConference: Boolean = false,
    val canSeparateFromConference: Boolean = false,
    val isConference: Boolean = false,
    // True when this info was built from a real android.telecom.Call (cellular).
    // Merge/swap are only offered for native calls.
    val hasNativeCall: Boolean = false
)

/**
 * A single participant inside a merged conference call (a child [Call] of the conference).
 */
data class ConferenceParticipant(
    val id: String,
    val displayName: String,
    val phoneNumber: String,
    val photoUri: String? = null
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

/**
 * Central state machine and telephony coordinator for OmniDial.
 *
 * Responsibilities:
 * - Bridges Android's native Telecom [InCallService] ([TelecomCallService]) with Compose UI StateFlows.
 * - Reactive, non-blocking incoming call lifecycle evaluation (spam check, contact resolution, rules engine).
 * - Multi-device audio routing (Earpiece, Speakerphone, Wired Headset, Bluetooth SCO/A2DP).
 * - Touch-tone DTMF tone generation and interactive call automation pipelines.
 * - Call duration tracking and persistent call log synchronizations.
 */
object CallManager {
    private const val TAG = "CallManager"
    /** Logcat tag for conference diagnostics: filter with `adb logcat -s OmniConf:D`. */
    private const val CONF_TAG = "OmniConf"

    private fun logConf(msg: String) {
        Log.d(CONF_TAG, msg)
    }

    private fun callStateName(state: Int): String = when (state) {
        Call.STATE_NEW -> "NEW"
        Call.STATE_CONNECTING -> "CONNECTING"
        Call.STATE_DIALING -> "DIALING"
        Call.STATE_RINGING -> "RINGING"
        Call.STATE_ACTIVE -> "ACTIVE"
        Call.STATE_HOLDING -> "HOLDING"
        Call.STATE_DISCONNECTED -> "DISCONNECTED"
        Call.STATE_DISCONNECTING -> "DISCONNECTING"
        Call.STATE_SELECT_PHONE_ACCOUNT -> "SELECT_PHONE_ACCOUNT"
        else -> "UNKNOWN($state)"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var automationJob: Job? = null

    // Native Telecom Call instance if real call is active
    private var nativeCall: Call? = null
    private var telecomService: TelecomCallService? = null

    /**
     * Held so it can be unregistered. Without this the framework keeps a reference to the
     * callback — and through it to the service Context — for the lifetime of the process, and it
     * keeps delivering state changes after teardown.
     */
    private var activeCallCallback: Call.Callback? = null

    private fun unregisterActiveCallCallback() {
        val callback = activeCallCallback ?: return
        activeCallCallback = null
        try {
            nativeCall?.unregisterCallback(callback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister call callback: ${e.message}")
        }
    }

    // Call UI State
    private val _activeCall = MutableStateFlow<ActiveCallInfo?>(null)
    val activeCall: StateFlow<ActiveCallInfo?> = _activeCall.asStateFlow()

    /**
     * Calls other than the primary one (e.g. the first call while a second "add call" is dialed).
     * Tracked with lightweight callbacks so the primary-call machinery is untouched.
     */
    private val extraCalls = LinkedHashMap<Call, ActiveCallInfo>()
    private val extraCallCallbacks = mutableMapOf<Call, Call.Callback>()
    private val _extraCallInfos = MutableStateFlow<List<ActiveCallInfo>>(emptyList())
    val extraCallInfos: StateFlow<List<ActiveCallInfo>> = _extraCallInfos.asStateFlow()

    /**
     * Participants of the current merged conference (children of the conference [Call]).
     * Refreshed whenever the primary call's state/details change.
     */
    private val participantCalls = mutableMapOf<String, Call>()
    private val _conferenceParticipants = MutableStateFlow<List<ConferenceParticipant>>(emptyList())
    val conferenceParticipants: StateFlow<List<ConferenceParticipant>> = _conferenceParticipants.asStateFlow()

    /**
     * Name/number identities retained for the current conference's participants. The
     * framework re-adds merged participants as new, usually blank, child Call objects:
     * a tracked call that is reparented under the conference keeps its identity here,
     * and one that the framework tears down during the merge keeps it too. Manage rows
     * are labeled from these (matched by number when the child has one) instead of
     * showing "Unknown".
     */
    private val absorbedParticipantInfos = mutableListOf<ActiveCallInfo>()

    private fun retainParticipantIdentity(info: ActiveCallInfo) {
        absorbedParticipantInfos.removeAll {
            it.id == info.id || (it.phoneNumber.isNotBlank() && it.phoneNumber == info.phoneNumber)
        }
        absorbedParticipantInfos.add(info)
        while (absorbedParticipantInfos.size > 20) absorbedParticipantInfos.removeAt(0)
        logConf("retained participant identity: ${info.displayName} (${info.phoneNumber}), total retained: ${absorbedParticipantInfos.size}")
    }

    /**
     * Which retained identity labeled each live conference child. When the conference
     * collapses, the survivor is promoted using its labeled identity directly — no
     * number re-matching needed (the child's own details are often blank by then).
     */
    private val labeledChildIdentities = mutableMapOf<Call, ActiveCallInfo>()

    /** IDs of participants explicitly ended from Manage (to exclude them from survivor selection). */
    private val droppedParticipantIds = mutableSetOf<String>()

    /** Detached survivor Call object remembered while the conference shell is still active. */
    private var collapsedSurvivorCall: Call? = null

    /** Synthetic 1:1 call info for the survivor when the conference collapses to one person. */
    private var collapsedSurvivorIdentity: ActiveCallInfo? = null

    private val _callLoggedEvent = MutableSharedFlow<Long>(extraBufferCapacity = 5)
    val callLoggedEvent: SharedFlow<Long> = _callLoggedEvent.asSharedFlow()

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

    private val _isRingerSilenced = MutableStateFlow(false)
    val isRingerSilenced: StateFlow<Boolean> = _isRingerSilenced.asStateFlow()

    fun silenceRinger(context: Context) {
        val current = _activeCall.value
        if (current != null && current.state == Call.STATE_RINGING) {
            try {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                tm?.silenceRinger()
                _isRingerSilenced.value = true
                Log.d(TAG, "Incoming call ringer silenced by user action or motion")
            } catch (e: Exception) {
                Log.e(TAG, "Error silencing ringer", e)
            }
        }
    }

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

    /**
     * The full-featured callback for the primary (foreground) call: drives _activeCall, the
     * foreground service, notifications, proximity sensor, and conference participant tracking.
     * Extracted so a promoted extra call (after the primary is removed) can be re-attached
     * without re-running contact/spam enrichment.
     */
    private fun createPrimaryCallback(call: Call, context: Context): Call.Callback {
        return object : Call.Callback() {
            override fun onStateChanged(call: Call, state: Int) {
                Log.d(TAG, "Call state changed: $state")
                if (state != Call.STATE_RINGING) {
                    _isRingerSilenced.value = false
                }
                val current = _activeCall.value
                if (current != null) {
                    val connectTime = if (state == Call.STATE_ACTIVE && current.connectTimeMillis == 0L) {
                        com.example.util.HapticFeedbackHelper.performCallConnected(context)
                        System.currentTimeMillis()
                    } else current.connectTimeMillis

                    val flags = conferenceFlags(call)
                    val isCollapsed = collapsedSurvivorIdentity != null
                    _activeCall.value = current.copy(
                        state = state,
                        connectTimeMillis = connectTime,
                        displayName = if (isCollapsed) (collapsedSurvivorIdentity?.displayName ?: current.displayName) else current.displayName,
                        phoneNumber = if (isCollapsed) (collapsedSurvivorIdentity?.phoneNumber ?: current.phoneNumber) else current.phoneNumber,
                        photoUri = if (isCollapsed) (collapsedSurvivorIdentity?.photoUri ?: current.photoUri) else current.photoUri,
                        canMergeConference = if (isCollapsed) false else flags.canMerge,
                        canSwapConference = if (isCollapsed) false else flags.canSwap,
                        canSeparateFromConference = if (isCollapsed) false else flags.canSeparate,
                        isConference = if (isCollapsed) false else flags.isConference
                    )
                    CallForegroundService.start(context)
                    OngoingCallNotificationHelper.showCallNotification(context, _activeCall.value!!)
                    if (!isCollapsed) {
                        refreshConferenceParticipants()
                    }
                }

                if (state == Call.STATE_DISCONNECTED) {
                    releaseProximityWakeLock()
                    val survivor = collapsedSurvivorCall
                    if (survivor != null && survivor.state != Call.STATE_DISCONNECTED && survivor.state != Call.STATE_DISCONNECTING) {
                        logConf("conference shell disconnected, promoting survivor ${survivor.hashCode()}")
                        promoteCollapsedSurvivor(context)
                        return
                    }
                    handleCallEnded(context, current)
                } else {
                    updateProximitySensor(context)
                }
            }

            override fun onDetailsChanged(call: Call, details: Call.Details) {
                Log.d(TAG, "Call details changed")
                val current = _activeCall.value
                if (current != null) {
                    val updatedSim = SimHelper.resolveSimInfo(context, details.accountHandle)
                    val updatedSlot = updatedSim?.slotIndex?.plus(1) ?: SimHelper.resolveSimSlot(context, details.accountHandle)
                    val updatedName = updatedSim?.displayName
                    val updatedRoaming = updatedSim?.isRoaming == true
                    val flags = conferenceFlags(call)
                    val isCollapsed = collapsedSurvivorIdentity != null
                    if (updatedSlot != current.simSlot || updatedName != current.simDisplayName ||
                        updatedRoaming != current.isRoaming || flags.canMerge != current.canMergeConference ||
                        flags.canSwap != current.canSwapConference || flags.canSeparate != current.canSeparateFromConference ||
                        flags.isConference != current.isConference || isCollapsed
                    ) {
                        _activeCall.value = current.copy(
                            simSlot = updatedSlot,
                            simDisplayName = updatedName,
                            isRoaming = updatedRoaming,
                            displayName = if (isCollapsed) (collapsedSurvivorIdentity?.displayName ?: current.displayName) else current.displayName,
                            phoneNumber = if (isCollapsed) (collapsedSurvivorIdentity?.phoneNumber ?: current.phoneNumber) else current.phoneNumber,
                            photoUri = if (isCollapsed) (collapsedSurvivorIdentity?.photoUri ?: current.photoUri) else current.photoUri,
                            canMergeConference = if (isCollapsed) false else flags.canMerge,
                            canSwapConference = if (isCollapsed) false else flags.canSwap,
                            canSeparateFromConference = if (isCollapsed) false else flags.canSeparate,
                            isConference = if (isCollapsed) false else flags.isConference
                        )
                        if (!isCollapsed) {
                            refreshConferenceParticipants()
                        }
                    }
                }
            }

            override fun onChildrenChanged(call: Call, children: List<Call>) {
                logConf("primary ${call.hashCode()} children -> ${children.size}")
                if (children.isEmpty()) {
                    // The conference may have collapsed (e.g. one of two participants was
                    // disconnected and the framework detached the survivor). If a live
                    // detached child remains, it becomes the primary call.
                    if (maybeCollapseConference(call, context)) return
                }
                refreshConferenceParticipants()
            }

            override fun onParentChanged(call: Call, parent: Call?) {
                logConf("primary ${call.hashCode()} parent -> ${parent?.hashCode()}")
            }
        }
    }

    fun onCallAdded(call: Call, context: Context) {
        logConf("onCallAdded id=${call.hashCode()} state=${callStateName(call.state)} " +
            "isConference=${call.details?.hasProperty(Call.Details.PROPERTY_CONFERENCE)} " +
            "children=${call.children.size} parent=${call.parent?.hashCode()} " +
            "extras=${extraCalls.size}")
        // A call that arrives already parented is a conference participant, never a new
        // standalone call. Some stacks re-add merged participants as new child Call objects
        // right after the merge — letting such a call demote the real conference would flip
        // isConference off on the primary and hide Manage mid-conference. Track it lightly:
        // no primary-callback steal, no extraCalls entry (it is not mergeable), no Recents
        // write (participants are subsumed by the conference).
        val addedParent = try { call.parent } catch (_: Exception) { null }
        if (addedParent != null) {
            logConf("parented child ${call.hashCode()} arrived under ${addedParent.hashCode()}; primary untouched")
            val childCallback = object : Call.Callback() {
                override fun onStateChanged(c: Call, state: Int) {
                    logConf("child ${c.hashCode()} state -> ${callStateName(state)}")
                    if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
                        try { c.unregisterCallback(this) } catch (_: Exception) {}
                        extraCallCallbacks.remove(c)
                        labeledChildIdentities.remove(c)
                        if (collapsedSurvivorCall === c) {
                            logConf("collapsed survivor husk ${c.hashCode()} disconnected; live shell ${nativeCall?.hashCode()} continues")
                            collapsedSurvivorCall = null
                            return
                        }
                        refreshConferenceParticipants()
                    }
                }

                override fun onDetailsChanged(c: Call, details: Call.Details) {
                    // Details (name/number) can populate after the add — refresh labels.
                    if (collapsedSurvivorIdentity == null) {
                        refreshConferenceParticipants()
                    }
                }

                override fun onParentChanged(c: Call, parent: Call?) {
                    logConf("tracked child ${c.hashCode()} parent -> ${parent?.hashCode()}")
                    if (parent == null) {
                        val conf = nativeCall
                        if (conf != null) {
                            maybeCollapseConference(conf, context)
                        }
                        if (collapsedSurvivorIdentity == null) {
                            refreshConferenceParticipants()
                        }
                    }
                }
            }
            extraCallCallbacks[call]?.let {
                try { call.unregisterCallback(it) } catch (_: Exception) {}
            }
            extraCallCallbacks[call] = childCallback
            try { call.registerCallback(childCallback) } catch (_: Exception) {}
            refreshConferenceParticipants()
            return
        }
        // A fresh standalone call arrives. Only clear retained participant identities
        // if there are NO existing tracked calls (not mid-conference, not second call).
        val isFirstCall = (nativeCall == null && extraCalls.isEmpty())
        if (isFirstCall) {
            absorbedParticipantInfos.clear()
            labeledChildIdentities.clear()
            droppedParticipantIds.clear()
            collapsedSurvivorCall = null
            collapsedSurvivorIdentity = null
        }
        val prevCall = nativeCall
        val prevCallback = activeCallCallback
        if (prevCall != null && prevCall != call && prevCallback != null &&
            prevCall.state != Call.STATE_DISCONNECTED && prevCall.state != Call.STATE_DISCONNECTING
        ) {
            // A live call is already tracked (e.g. the user tapped "Add call" and dialed a second
            // person, or Telecom added a new conference parent). Retain its identity immediately
            // so blank child Call objects from Telecom can be labeled later.
            try { prevCall.unregisterCallback(prevCallback) } catch (_: Exception) {}
            activeCallCallback = null
            _activeCall.value?.let {
                retainParticipantIdentity(it)
                extraCalls[prevCall] = it
            }
            val lightCallback = object : Call.Callback() {
                override fun onStateChanged(c: Call, state: Int) {
                    val info = extraCalls[c] ?: return
                    if (state == Call.STATE_DISCONNECTED || state == Call.STATE_DISCONNECTING) {
                        // Retain its identity: on some stacks the originals are torn down
                        // (not reparented) when the merge completes, and the framework
                        // re-adds each participant as a new, often blank, child object.
                        // The retained name/number labels that participant's Manage row.
                        retainParticipantIdentity(info)
                        try { c.unregisterCallback(this) } catch (_: Exception) {}
                        extraCalls.remove(c)
                        extraCallCallbacks.remove(c)
                        _extraCallInfos.value = extraCalls.values.toList()
                        logEndedCall(context, info)
                    } else {
                        val flags = conferenceFlags(c)
                        extraCalls[c] = info.copy(
                            state = state,
                            canMergeConference = flags.canMerge,
                            canSwapConference = flags.canSwap,
                            canSeparateFromConference = flags.canSeparate,
                            isConference = flags.isConference
                        )
                        _extraCallInfos.value = extraCalls.values.toList()
                    }
                }

                override fun onDetailsChanged(c: Call, details: Call.Details) {
                    val info = extraCalls[c] ?: return
                    val flags = conferenceFlags(c)
                    extraCalls[c] = info.copy(
                        canMergeConference = flags.canMerge,
                        canSwapConference = flags.canSwap,
                        canSeparateFromConference = flags.canSeparate,
                        isConference = flags.isConference
                    )
                    _extraCallInfos.value = extraCalls.values.toList()
                }

                override fun onParentChanged(c: Call, parent: Call?) {
                    logConf("extra ${c.hashCode()} parent -> ${parent?.hashCode()}")
                    if (parent != null && extraCalls.containsKey(c)) {
                        // This call is now a child of a conference — stop tracking it as a
                        // separate "second call" so merge/swap hide and it shows under Manage.
                        // Retain its name/number: the framework may re-add it as a new, blank
                        // child Call object, and Manage rows need the real identity.
                        extraCalls[c]?.let { retainParticipantIdentity(it) }
                        try { c.unregisterCallback(this) } catch (_: Exception) {}
                        extraCalls.remove(c)
                        extraCallCallbacks.remove(c)
                        _extraCallInfos.value = extraCalls.values.toList()
                        refreshConferenceParticipants()
                    }
                }

                override fun onChildrenChanged(c: Call, children: List<Call>) {
                    logConf("extra ${c.hashCode()} children -> ${children.size}")
                }
            }
            prevCall.registerCallback(lightCallback)
            extraCallCallbacks[prevCall] = lightCallback
            _extraCallInfos.value = extraCalls.values.toList()
            Log.d(TAG, "Stashed previous live call for conference tracking: $prevCall")
        } else {
            // Must run before nativeCall is reassigned, so the previous call's callback is released.
            unregisterActiveCallCallback()
        }
        this.nativeCall = call
        this.appContext = context.applicationContext
        val number = extractPhoneNumber(call)
        val isVoicemail = ContactHelper.isVoicemailNumber(context, number)
        val isIncoming = call.state == Call.STATE_RINGING

        val callerDisplayName = call.details?.callerDisplayName
        val preliminaryName = when {
            isVoicemail -> "Voicemail"
            !callerDisplayName.isNullOrBlank() -> callerDisplayName
            isIncoming -> "Incoming Caller"
            number.isNotBlank() -> number
            else -> "Outgoing Call"
        }

        val resolvedSimInfo = SimHelper.resolveSimInfo(context, accountHandle = call.details?.accountHandle)
        val resolvedSimSlot = resolvedSimInfo?.slotIndex?.plus(1) ?: SimHelper.resolveSimSlot(
            context = context,
            accountHandle = call.details?.accountHandle
        )
        val resolvedSimName = resolvedSimInfo?.displayName
        val isRoaming = resolvedSimInfo?.isRoaming == true

        // Instant UI Presentation (<16ms): Post placeholder call state immediately before any disk/Room queries
        val confFlags = conferenceFlags(call)
        val initialCallInfo = ActiveCallInfo(
            id = call.hashCode().toString(),
            phoneNumber = number,
            displayName = preliminaryName,
            state = call.state,
            isIncoming = isIncoming,
            connectTimeMillis = if (call.state == Call.STATE_ACTIVE) System.currentTimeMillis() else 0L,
            isSimulated = false,
            photoUri = null,
            communityInfo = null,
            nickname = null,
            numberLabel = "Mobile",
            trustTier = if (isVoicemail) com.example.domain.usecase.TrustTier.VERIFIED_BUSINESS else com.example.domain.usecase.TrustTier.NEUTRAL_UNKNOWN,
            trustBadgeLabel = if (isVoicemail) "Voicemail" else null,
            simSlot = resolvedSimSlot,
            simDisplayName = resolvedSimName,
            isRoaming = isRoaming,
            canMergeConference = confFlags.canMerge,
            canSwapConference = confFlags.canSwap,
            canSeparateFromConference = confFlags.canSeparate,
            isConference = confFlags.isConference,
            hasNativeCall = true
        )
        _activeCall.value = initialCallInfo
        _isRingerSilenced.value = false

        val stateCallback = createPrimaryCallback(call, context)
        activeCallCallback = stateCallback
        call.registerCallback(stateCallback)

        CallForegroundService.start(context)
        OngoingCallNotificationHelper.showCallNotification(context, initialCallInfo)
        updateProximitySensor(context)

        // Asynchronous Metadata Enrichment, DND, Whitelist, & Spam Evaluation on Dispatchers.IO
        scope.launch(Dispatchers.IO) {
            try {
                val lookedUp = ContactHelper.lookupContactByNumber(context, number)
                val dao = AppDatabase.getInstance(context).appDao()
                val favContact = try {
                    dao.getAllFavoritesList().firstOrNull {
                        ContactHelper.isSamePhoneNumber(it.phoneNumber, number)
                    }
                } catch (_: Exception) { null }

                val communityInfo = if (lookedUp == null && favContact == null && !isVoicemail) {
                    com.example.util.CommunityCallerIdService.lookup(number)
                } else null

                val resolvedNickname = lookedUp?.nickname?.ifBlank { null } ?: favContact?.nickname?.ifBlank { null }
                val resolvedLabel = lookedUp?.label?.ifBlank { null } ?: favContact?.label?.ifBlank { null } ?: "Mobile"

                val enrichedName = when {
                    isVoicemail -> "Voicemail"
                    lookedUp != null -> lookedUp.name
                    favContact != null -> favContact.name
                    communityInfo != null -> communityInfo.name
                    !callerDisplayName.isNullOrBlank() -> callerDisplayName
                    isIncoming -> "Incoming Caller"
                    number.isNotBlank() -> number
                    else -> "Outgoing Call"
                }
                val photoUri = lookedUp?.photoUri ?: favContact?.photoUri

                val isWhitelisted = isWhitelistedOrRuleMatched(context, number)
                val prefs = context.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)
                val autoBlockCarrier = prefs.getBoolean("auto_block_carrier_spam", true)
                val blockSpamPreset = prefs.getBoolean("block_telemarketers_robocalls", true)
                val silenceUnknownPrivate = prefs.getBoolean("silence_unknown_private", false)

                val normalizedNumber = PhoneNumberNormalizer.toE164(number)
                val matchedSpamNumber = if (isIncoming && !isWhitelisted) {
                    try {
                        dao.getSpamByNormalizedNumber(normalizedNumber) ?: dao.getSpamByNumber(number, normalizedNumber)
                    } catch (_: Exception) { null }
                } else null

                val isDatabaseSpam = matchedSpamNumber != null
                val isCarrierSpamThreat = isIncoming && !isWhitelisted && autoBlockCarrier && isCarrierSpam(call, number, enrichedName)
                // When OmniDial holds the Caller ID & spam role, the screening service
                // already evaluated spam-list numbers (silence or reject per the user's
                // "Block spam automatically" setting), so don't auto-decline them here
                // a second time. Carrier-flagged calls stay on the legacy toggle.
                val screeningOwnsSpamList = isDatabaseSpam && RoleHelper.isCallScreeningRoleHeld(context)
                val shouldAutoDeclineSpam = (isCarrierSpamThreat || (isDatabaseSpam && blockSpamPreset && !screeningOwnsSpamList))

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
                    try {
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
                                callerName = enrichedName.ifBlank { "Spam Threat" },
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
                        _callLoggedEvent.tryEmit(System.currentTimeMillis())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error writing spam log", e)
                    }
                    SpamNotificationHelper.showBlockedSpamNotification(context, number, enrichedName)
                    if (nativeCall == call) {
                        _activeCall.value = null
                        // The framework's onCallRemoved (after the reject above) runs handleCallEnded
                        // and promotes any remaining extra call.
                    } else {
                        // The dropped call was already stashed as an extra call — just stop tracking it.
                        extraCallCallbacks.remove(call)?.let {
                            try { call.unregisterCallback(it) } catch (_: Exception) {}
                        }
                        extraCalls.remove(call)
                        _extraCallInfos.value = extraCalls.values.toList()
                    }
                    return@launch
                }

                val trustBadge = resolveTrustBadge(
                    isVoicemail = isVoicemail,
                    hasContact = lookedUp != null,
                    hasFav = favContact != null,
                    isSpam = isDatabaseSpam || isCarrierSpamThreat,
                    communityInfo = communityInfo
                )

                val enrichedConfFlags = conferenceFlags(call)
                val enrichedCallInfo = ActiveCallInfo(
                    id = call.hashCode().toString(),
                    phoneNumber = number,
                    displayName = enrichedName,
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
                    simSlot = resolvedSimSlot,
                    simDisplayName = resolvedSimName,
                    isRoaming = isRoaming,
                    canMergeConference = enrichedConfFlags.canMerge,
                    canSwapConference = enrichedConfFlags.canSwap,
                    canSeparateFromConference = enrichedConfFlags.canSeparate,
                    isConference = enrichedConfFlags.isConference,
                    hasNativeCall = true
                )
                if (nativeCall == call) {
                    _activeCall.value = enrichedCallInfo
                    refreshConferenceParticipants()
                } else {
                    // No longer the primary call (a second call was added while this enrichment
                    // was running) — update the stashed copy instead of clobbering the new call.
                    extraCalls[call]?.let { extraCalls[call] = enrichedCallInfo }
                    _extraCallInfos.value = extraCalls.values.toList()
                }

                if (!isCallUiForegrounded) {
                    OngoingCallNotificationHelper.showCallNotification(context, enrichedCallInfo)
                }

                // Check Do Not Disturb (DND) or Silence Unknown/Private status
                try {
                    val isKnownCaller = (lookedUp != null || favContact != null)
                    if (isIncoming && silenceUnknownPrivate && !isKnownCaller && !isWhitelisted) {
                        Log.d(TAG, "Incoming call from unknown caller $number silenced by Silence Unknown/Private preset")
                        silenceRinger(context)
                    } else {
                        val isFavoriteCaller = lookedUp?.isStarred == true
                        if (FlipToShhhManager.isDndActive(context)) {
                            val allowed = FlipToShhhManager.isCallerAllowedUnderCurrentDnd(context, isFavoriteCaller)
                            if (!allowed) {
                                Log.d(TAG, "Incoming call from $number silenced by Do Not Disturb")
                                silenceRinger(context)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error checking DND / silence policy for caller", e)
                }

                checkAndExecuteAutomation(context, number, isIncoming)
            } catch (e: Exception) {
                Log.e(TAG, "Error in background call metadata enrichment", e)
            }
        }
    }

    fun onCallRemoved(call: Call, context: Context) {
        if (nativeCall == call) {
            val survivor = collapsedSurvivorCall
            if (survivor != null && survivor.state != Call.STATE_DISCONNECTED && survivor.state != Call.STATE_DISCONNECTING) {
                logConf("conference shell removed, promoting survivor ${survivor.hashCode()}")
                promoteCollapsedSurvivor(context)
                return
            }
            _activeCall.value?.let { retainParticipantIdentity(it) }
            handleCallEnded(context, _activeCall.value)
            unregisterActiveCallCallback()
            nativeCall = null
            clearConferenceParticipants()
            // If another tracked call is still live (e.g. the second "add call" party hung up, or
            // the first call was never merged), promote it to primary instead of leaving the UI
            // on a dead post-call screen.
            promoteExtraCallIfAny(context)
        } else {
            if (collapsedSurvivorCall === call) {
                logConf("collapsed survivor husk ${call.hashCode()} removed; live shell continues")
                collapsedSurvivorCall = null
                extraCallCallbacks.remove(call)?.let {
                    try { call.unregisterCallback(it) } catch (_: Exception) {}
                }
                return
            }
            // A stashed extra call ended: log it and drop it. The primary-call UI is untouched.
            val info = extraCalls.remove(call)
            extraCallCallbacks.remove(call)?.let {
                try { call.unregisterCallback(it) } catch (_: Exception) {}
            }
            if (info != null) {
                retainParticipantIdentity(info)
                logEndedCall(context, info)
                _extraCallInfos.value = extraCalls.values.toList()
                Log.d(TAG, "Extra call removed and logged")
            }
        }
    }

    /**
     * Promote the most recent still-live extra call to primary. Returns true if one was promoted.
     */
    private fun promoteExtraCallIfAny(context: Context): Boolean {
        val remaining = extraCalls.entries.firstOrNull { (c, _) ->
            c.state != Call.STATE_DISCONNECTED && c.state != Call.STATE_DISCONNECTING
        } ?: return false
        val (promotedCall, promotedInfo) = remaining
        extraCalls.remove(promotedCall)
        extraCallCallbacks.remove(promotedCall)?.let {
            try { promotedCall.unregisterCallback(it) } catch (_: Exception) {}
        }
        _extraCallInfos.value = extraCalls.values.toList()
        nativeCall = promotedCall
        _activeCall.value = promotedInfo
        val callback = createPrimaryCallback(promotedCall, context)
        activeCallCallback = callback
        promotedCall.registerCallback(callback)
        CallForegroundService.start(context)
        OngoingCallNotificationHelper.showCallNotification(context, promotedInfo)
        refreshConferenceParticipants()
        Log.d(TAG, "Promoted remaining call to primary")
        return true
    }

    /**
     * Write one ended call to the recents database (deduped by session id).
     * Extracted from handleCallEnded so stashed extra calls can be logged without
     * touching the primary-call UI state.
     */
    private fun logEndedCall(context: Context, callInfo: ActiveCallInfo?) {
        // A merged conference parent is a container, not a real call — its number is blank and
        // each participant was already logged individually. Never write it to Recents.
        if (callInfo != null && callInfo.isConference && callInfo.phoneNumber.isBlank()) {
            Log.d(TAG, "Skipping Recents entry for blank conference-parent call")
            return
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
                        _callLoggedEvent.tryEmit(System.currentTimeMillis())

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
    }

    private fun handleCallEnded(context: Context, callInfo: ActiveCallInfo?) {
        OmniCallRedirectionService.dismissRedirectionNotification(context)
        _isRingerSilenced.value = false
        releaseProximityWakeLock()
        TelecomVoipHelper.endVoipCall()
        automationJob?.cancel()
        automationJob = null
        simulatedTimerJob?.cancel()
        simulatedTimerJob = null
        collapsedSurvivorCall = null
        collapsedSurvivorIdentity = null
        CallForegroundService.stop(context)
        OngoingCallNotificationHelper.cancelCallNotification(context)
        appContext?.let { ctx ->
            CallForegroundService.stop(ctx)
            OngoingCallNotificationHelper.cancelCallNotification(ctx)
        }

        logEndedCall(context, callInfo)

        // Post-call state: keep in STATE_DISCONNECTED so InCallScreen note-taking panel can display.
        // It will be dismissed by the user or by InCallScreen's auto-close timer if not interacted with.
        val current = _activeCall.value
        if (current != null) {
            _activeCall.value = current.copy(state = Call.STATE_DISCONNECTED)
        }
        _isMuted.value = false
        _isSpeakerOn.value = false

        if (!isCallUiForegrounded) {
            scope.launch {
                delay(2000)
                if (_activeCall.value?.state == Call.STATE_DISCONNECTED) {
                    dismissActiveCall()
                }
            }
        }
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

    private suspend fun isWhitelistedOrRuleMatched(context: Context, number: String): Boolean {
        if (number.isBlank()) return false
        try {
            val prefs = context.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)
            val notSpamSet = prefs.getStringSet("not_spam_whitelist", emptySet()) ?: emptySet()
            if (notSpamSet.any { ContactHelper.isSamePhoneNumber(it, number) }) {
                Log.d(TAG, "Number $number is in user Not-Spam Whitelist. Whitelisted from carrier spam filter.")
                return true
            }

            val dao = AppDatabase.getInstance(context).appDao()
            val rules = dao.getEnabledRules()
            val ruleMatched = rules.any { rule ->
                rule.isEnabled && matchesRulePattern(rule.phoneNumberPattern, number)
            }
            if (ruleMatched) {
                Log.d(TAG, "Number $number matched user automation rule. Whitelisted from carrier spam filter.")
                return true
            }

            val favs = dao.getAllFavoritesList()
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
        appContext?.let { OmniCallRedirectionService.dismissRedirectionNotification(it) }
        _activeCall.value = null
        _automationState.value = null
        _isMuted.value = false
        _isSpeakerOn.value = false
        automationJob?.cancel()
        automationJob = null
        simulatedTimerJob?.cancel()
        simulatedTimerJob = null
        clearExtraCalls()
        clearConferenceParticipants()
    }

    private fun clearExtraCalls() {
        for ((call, callback) in extraCallCallbacks) {
            try { call.unregisterCallback(callback) } catch (_: Exception) {}
        }
        extraCallCallbacks.clear()
        extraCalls.clear()
        _extraCallInfos.value = emptyList()
    }

    private fun clearConferenceParticipants() {
        participantCalls.clear()
        absorbedParticipantInfos.clear()
        labeledChildIdentities.clear()
        droppedParticipantIds.clear()
        collapsedSurvivorCall = null
        collapsedSurvivorIdentity = null
        _conferenceParticipants.value = emptyList()
    }

    /**
     * If the primary call is an empty but still-live conference and a detached former
     * child is still live, the conference collapsed (e.g. one of two participants was
     * disconnected and the framework detached the survivor).
     *
     * CRITICAL BUG 1 FIX:
     * On many carriers (e.g. Spectrum Mobile / IMS), the conference shell Call stays
     * ACTIVE with 0 children and carries the live two-way audio. The detached child
     * Call object is a husk that quickly disconnects/dies. Promoting the husk over the
     * live shell causes a false "call ended" while audio is still live.
     *
     * We keep the active conference shell as primary (nativeCall), remember the survivor,
     * update the UI to show the survivor's identity with isConference = false, and
     * dismiss the Manage dialog. The survivor Call object is only promoted if the shell
     * itself disconnects.
     */
    private fun maybeCollapseConference(conference: Call, context: Context): Boolean {
        if (nativeCall !== conference) return false
        if (!conferenceFlags(conference).isConference) return false
        if (conference.state == Call.STATE_DISCONNECTED ||
            conference.state == Call.STATE_DISCONNECTING
        ) return false
        if (conference.children.isNotEmpty()) return false
        if (collapsedSurvivorIdentity != null) return true

        val liveCandidates = extraCallCallbacks.keys.filter { c ->
            !extraCalls.containsKey(c) &&
                c.state != Call.STATE_DISCONNECTED &&
                c.state != Call.STATE_DISCONNECTING &&
                c.hashCode().toString() !in droppedParticipantIds
        }
        val survivor = liveCandidates.firstOrNull {
            (try { it.parent } catch (_: Exception) { null }) == null
        } ?: liveCandidates.firstOrNull()

        val number = survivor?.let { extractPhoneNumber(it) } ?: ""
        // Prefer labeled child identity, or matching by number/id, or non-dropped absorbed identity
        val retained = (survivor?.let { labeledChildIdentities[it] })
            ?: absorbedParticipantInfos.firstOrNull { info ->
                info.id !in droppedParticipantIds &&
                    number.isNotBlank() &&
                    ContactHelper.isSamePhoneNumber(info.phoneNumber, number)
            }
            ?: absorbedParticipantInfos.firstOrNull { info ->
                info.id !in droppedParticipantIds
            }
            ?: absorbedParticipantInfos.firstOrNull()

        val survivorInfo = retained?.copy(
            id = survivor?.hashCode()?.toString() ?: conference.hashCode().toString(),
            state = conference.state,
            canMergeConference = false,
            canSwapConference = false,
            canSeparateFromConference = false,
            isConference = false,
            hasNativeCall = true
        ) ?: ActiveCallInfo(
            id = conference.hashCode().toString(),
            phoneNumber = number,
            displayName = number.ifBlank { "Ongoing Call" },
            state = conference.state,
            isIncoming = false,
            connectTimeMillis = _activeCall.value?.connectTimeMillis ?: System.currentTimeMillis(),
            canMergeConference = false,
            canSwapConference = false,
            canSeparateFromConference = false,
            isConference = false,
            hasNativeCall = true
        )

        logConf("keeping active conference shell ${conference.hashCode()} as primary for survivor ${survivor?.hashCode()} (${survivorInfo.displayName})")
        collapsedSurvivorCall = survivor
        collapsedSurvivorIdentity = survivorInfo

        _activeCall.value = survivorInfo
        participantCalls.clear()
        _conferenceParticipants.value = emptyList()

        CallForegroundService.start(context)
        OngoingCallNotificationHelper.showCallNotification(context, survivorInfo)
        return true
    }

    private fun promoteCollapsedSurvivor(context: Context) {
        val survivor = collapsedSurvivorCall ?: return
        val info = collapsedSurvivorIdentity ?: _activeCall.value ?: return
        collapsedSurvivorCall = null
        collapsedSurvivorIdentity = null

        unregisterActiveCallCallback()
        extraCallCallbacks.remove(survivor)?.let {
            try { survivor.unregisterCallback(it) } catch (_: Exception) {}
        }

        nativeCall = survivor
        _activeCall.value = info.copy(
            id = survivor.hashCode().toString(),
            state = survivor.state,
            canMergeConference = false,
            canSwapConference = false,
            canSeparateFromConference = false,
            isConference = false
        )
        val callback = createPrimaryCallback(survivor, context)
        activeCallCallback = callback
        try { survivor.registerCallback(callback) } catch (_: Exception) {}
        participantCalls.clear()
        _conferenceParticipants.value = emptyList()
        CallForegroundService.start(context)
        OngoingCallNotificationHelper.showCallNotification(context, _activeCall.value!!)
        Log.d(TAG, "Promoted collapsed survivor ${survivor.hashCode()} to primary")
    }

    // -------------------------------------------------------------------------
    // Conference calling ("add person", merge, swap, manage participants)
    // -------------------------------------------------------------------------

    private data class ConferenceFlags(
        val canMerge: Boolean = false,
        val canSwap: Boolean = false,
        val canSeparate: Boolean = false,
        val isConference: Boolean = false
    )

    /**
     * Reads merge/swap/separate/conference flags from a [Call]'s details.
     * Any failure means "not supported" — buttons stay hidden rather than crash.
     */
    private fun conferenceFlags(call: Call): ConferenceFlags {
        return try {
            val details = call.details ?: return ConferenceFlags()
            ConferenceFlags(
                canMerge = details.can(Call.Details.CAPABILITY_MERGE_CONFERENCE),
                canSwap = details.can(Call.Details.CAPABILITY_SWAP_CONFERENCE),
                canSeparate = details.can(Call.Details.CAPABILITY_SEPARATE_FROM_CONFERENCE),
                isConference = details.hasProperty(Call.Details.PROPERTY_CONFERENCE)
            )
        } catch (_: Exception) {
            ConferenceFlags()
        }
    }

    /** Merge the held call(s) into the primary call. Returns false if the merge failed. */
    fun mergeConferenceCalls(): Boolean {
        return try {
            val primary = nativeCall
            if (primary == null) {
                Log.w(TAG, "merge requested with no primary call")
                return false
            }
            // Retain identities of primary and all extra calls before merge
            _activeCall.value?.let { retainParticipantIdentity(it) }
            for (info in extraCalls.values) {
                retainParticipantIdentity(info)
            }
            if (primary.details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true) {
                // Already a conference object (e.g. created by the carrier) — merge its
                // participants at the network level.
                primary.mergeConference()
                Log.d(TAG, "mergeConference() on existing conference requested")
            } else {
                // Two separate calls: ask Telecom to conference them together.
                // NOTE: Call.mergeConference() only works on an existing conference object;
                // on a plain call it is a silent no-op. Call.conference(other) is the API
                // that merges two separate calls into a conference.
                val other = extraCalls.keys.firstOrNull {
                    it.state != Call.STATE_DISCONNECTED && it.state != Call.STATE_DISCONNECTING
                }
                if (other == null) {
                    Log.w(TAG, "merge requested with no second call to merge")
                    return false
                }
                logConf("merge request: primary=${primary.hashCode()} state=${callStateName(primary.state)} " +
                    "other=${other.hashCode()} state=${callStateName(other.state)}")
                primary.conference(other)
                Log.d(TAG, "conference(primary, second call) requested")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "mergeConference failed", e)
            false
        }
    }

    /** Swap the active and held calls without merging. Returns false if unsupported/failed. */
    fun swapConferenceCalls(): Boolean {
        return try {
            val primary = nativeCall
            if (primary == null) {
                Log.w(TAG, "swap requested with no primary call")
                return false
            }
            if (primary.details?.hasProperty(Call.Details.PROPERTY_CONFERENCE) == true) {
                primary.swapConference()
                Log.d(TAG, "swapConference() on existing conference requested")
            } else {
                // Two separate calls: hold the active one and resume the held one.
                // NOTE: Call.swapConference() only works on an existing conference object;
                // on plain calls it is a silent no-op.
                val all = listOf(primary) + extraCalls.keys
                val active = all.firstOrNull { it.state == Call.STATE_ACTIVE }
                val held = all.firstOrNull { it != active && it.state == Call.STATE_HOLDING }
                if (active == null || held == null) {
                    Log.w(TAG, "swap requested without an active+held call pair")
                    return false
                }
                active.hold()
                held.unhold()
                Log.d(TAG, "swap via hold(active) + unhold(held) requested")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "swapConference failed", e)
            false
        }
    }

    /**
     * Refresh the participant list from the conference call's children.
     * Called whenever the primary call's state or details change.
     */
    fun refreshConferenceParticipants() {
        if (collapsedSurvivorIdentity != null) {
            logConf("skipping refreshConferenceParticipants: conference collapsed to survivor")
            return
        }
        try {
            val children = nativeCall?.children ?: emptyList()
            val source: List<Call> = when {
                children.isNotEmpty() -> {
                    logConf("participants from conference children: ${children.size}")
                    children
                }
                extraCalls.isNotEmpty() -> {
                    // No conference children reported (yet) — fall back to the live tracked
                    // calls so participant names still show after a merge.
                    val tracked = listOfNotNull(nativeCall) + extraCalls.keys
                    val live = tracked.filter {
                        it.state != Call.STATE_DISCONNECTED && it.state != Call.STATE_DISCONNECTING
                    }
                    logConf("participants from tracked-call fallback: ${live.size}")
                    live
                }
                else -> emptyList()
            }
            participantCalls.clear()
            val usedRetainedIds = mutableSetOf<String>()
            _conferenceParticipants.value = source.mapNotNull { child ->
                try {
                    val id = child.hashCode().toString()
                    val details = child.details
                    var number = details?.handle?.schemeSpecificPart ?: ""
                    val trackedName = extraCalls[child]?.displayName
                        .takeIf { !it.isNullOrBlank() && it != number }
                        ?: _activeCall.value?.takeIf { child == nativeCall }?.displayName
                            .takeIf { !it.isNullOrBlank() && it != number }
                    var name = details?.callerDisplayName?.takeIf { it.isNotBlank() } ?: trackedName
                    if (name.isNullOrBlank()) {
                        // The framework often re-adds merged participants as new child
                        // objects with a number but no name (or fully blank). Label the
                        // row from a retained pre-merge identity — matched by number when
                        // possible, positionally otherwise (retained identities only ever
                        // belong to this conference's participants) — instead of "Unknown".
                        val retained = absorbedParticipantInfos.firstOrNull {
                            it.id !in usedRetainedIds &&
                                number.isNotBlank() &&
                                com.example.util.ContactHelper.isSamePhoneNumber(it.phoneNumber, number)
                        } ?: absorbedParticipantInfos.firstOrNull { it.id !in usedRetainedIds }
                        retained?.let {
                            usedRetainedIds.add(it.id)
                            name = it.displayName.takeIf { d -> d.isNotBlank() }
                            if (number.isBlank()) number = it.phoneNumber
                            labeledChildIdentities[child] = it
                            logConf("labeled child $id as '${it.displayName}'")
                        }
                    }
                    var photoUri: String? = null
                    if (number.isNotBlank()) {
                        appContext?.let { ctx ->
                            com.example.util.ContactHelper.lookupContactByNumber(ctx, number)?.let { contact ->
                                if (name.isNullOrBlank()) {
                                    name = contact.name
                                }
                                photoUri = contact.photoUri
                            }
                        }
                    }
                    participantCalls[id] = child
                    ConferenceParticipant(
                        id = id,
                        displayName = name ?: number.ifBlank { "Unknown" },
                        phoneNumber = number,
                        photoUri = photoUri
                    )
                } catch (_: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "refreshConferenceParticipants failed: ${e.message}")
        }
    }

    /** Hang up one conference participant. Returns false on failure. */
    fun endConferenceParticipant(participantId: String): Boolean {
        val participant = participantCalls[participantId] ?: return false
        droppedParticipantIds.add(participantId)
        return try {
            participant.disconnect()
            Log.d(TAG, "Disconnect requested for conference participant $participantId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "endConferenceParticipant failed", e)
            false
        }
    }

    /** Split one participant out of the conference into a private held call. Returns false on failure. */
    fun splitConferenceParticipant(participantId: String): Boolean {
        val participant = participantCalls[participantId] ?: return false
        return try {
            participant.splitFromConference()
            Log.d(TAG, "splitFromConference requested for participant $participantId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "splitConferenceParticipant failed", e)
            false
        }
    }

    /** Toggle call hold/unhold state on the primary native call. Returns false on failure. */
    fun toggleHold(): Boolean {
        val call = nativeCall ?: return false
        return try {
            if (call.state == Call.STATE_HOLDING) {
                call.unhold()
                Log.d(TAG, "unhold requested for active call")
            } else if (call.state == Call.STATE_ACTIVE) {
                call.hold()
                Log.d(TAG, "hold requested for active call")
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "toggleHold failed: ${e.message}")
            false
        }
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

    private fun matchesRuleConditions(context: Context, rawNumber: String, rule: CallerRule): Boolean {
        if (!rule.isEnabled) return false
        if (!matchesRulePattern(rawNumber, rule.phoneNumberPattern)) return false

        // Ambient Geofence Guard: Wi-Fi SSID
        if (rule.requiredWifiSsid.isNotBlank()) {
            val currentSsid = getCurrentWifiSsid(context)
            if (currentSsid == null || !currentSsid.contains(rule.requiredWifiSsid.trim(), ignoreCase = true)) {
                Log.d(TAG, "Rule '${rule.name}' skipped: required Wi-Fi '${rule.requiredWifiSsid}' not matched (current: $currentSsid)")
                return false
            }
        }

        // Ambient Geofence Guard: Bluetooth Device
        if (rule.requiredBluetoothDevice.isNotBlank()) {
            val activeBt = _bluetoothDeviceName.value ?: ""
            val availableBt = _availableBluetoothDevices.value.map { it.name }
            val targetBt = rule.requiredBluetoothDevice.trim()
            val btMatched = activeBt.contains(targetBt, ignoreCase = true) ||
                    availableBt.any { it.contains(targetBt, ignoreCase = true) }
            if (!btMatched) {
                Log.d(TAG, "Rule '${rule.name}' skipped: required Bluetooth '$targetBt' not active")
                return false
            }
        }

        return true
    }

    private fun getCurrentWifiSsid(context: Context): String? {
        try {
            val appContext = context.applicationContext
            val ssid: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Modern, non-deprecated path (API 29+): read WifiInfo from the active
                // network's capabilities instead of the deprecated WifiManager.getConnectionInfo().
                val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE)
                        as? android.net.ConnectivityManager
                val activeNetwork = connectivityManager?.activeNetwork
                val transportInfo = activeNetwork?.let { connectivityManager?.getNetworkCapabilities(it)?.transportInfo }
                (transportInfo as? android.net.wifi.WifiInfo)?.ssid
            } else {
                @Suppress("DEPRECATION")
                val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE)
                        as? android.net.wifi.WifiManager
                wifiManager?.connectionInfo?.ssid
            }
            if (ssid != null && ssid != android.net.wifi.WifiManager.UNKNOWN_SSID && ssid != "0x") {
                return ssid.removeSurrounding("\"")
            }
            if (ssid == android.net.wifi.WifiManager.UNKNOWN_SSID) {
                Log.w(TAG, "Wi-Fi SSID unreadable (<unknown ssid>): Android 10+ requires location permission for SSID-based rules")
            }
        } catch (_: Exception) {}
        return null
    }

    fun checkAndExecuteAutomation(context: Context, rawNumber: String, isIncoming: Boolean) {
        if (!isIncoming) return

        automationJob?.cancel()
        automationJob = scope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).appDao()

            // 1. Evaluate user-configured automation rules FIRST (user rules take top priority)
            val rules = dao.getEnabledRules()
            val isSimulationTest = _activeCall.value?.callReason == "Rule Simulation Test"
            val matchedRule = if (isSimulationTest) {
                rules.firstOrNull { it.name.equals(_activeCall.value?.displayName, ignoreCase = true) }
                    ?: rules.firstOrNull { rule -> matchesRuleConditions(context, rawNumber, rule) }
            } else {
                rules.firstOrNull { rule -> matchesRuleConditions(context, rawNumber, rule) }
            }

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

            // Step 2b: Auto Speakerphone Activation
            if (rule.autoSpeakerphone) {
                _automationState.value = AutomationStep(
                    ruleName = rule.name,
                    stepDescription = "Routing audio to Speakerphone..."
                )
                setAudioRoute(CallAudioState.ROUTE_SPEAKER)
                actions.add("Speakerphone Activated")
                delay(200)
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

            if (rule.autoMuteMic) {
                _isMuted.value = true
                actions.add("Mic Muted")
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

            if (rule.autoMuteMic && !rule.autoHangup) {
                _isMuted.value = false
            }
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
            try {
                collapsedSurvivorCall?.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting collapsed survivor call", e)
            }
            for (extra in extraCalls.keys.toList()) {
                try {
                    extra.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Error disconnecting extra call", e)
                }
            }
            for (part in participantCalls.values.toList()) {
                try {
                    part.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Error disconnecting participant call", e)
                }
            }
            for (tracked in extraCallCallbacks.keys.toList()) {
                if (tracked !== nativeCall && tracked !== collapsedSurvivorCall && !extraCalls.containsKey(tracked) && !participantCalls.containsValue(tracked)) {
                    try {
                        tracked.disconnect()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error disconnecting tracked child leg", e)
                    }
                }
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

    @Suppress("DEPRECATION")
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

        val isVoicemail = ContactHelper.isVoicemailNumber(context, number)
        val simInfo = SimHelper.resolveSimInfo(context)
        val initialCallInfo = ActiveCallInfo(
            id = "sim_${System.currentTimeMillis()}",
            phoneNumber = number,
            displayName = if (isVoicemail) "Voicemail" else name.ifBlank { "Incoming Caller" },
            state = Call.STATE_RINGING,
            isIncoming = true,
            connectTimeMillis = 0L,
            isSimulated = true,
            photoUri = null,
            callReason = reason,
            communityInfo = null,
            nickname = null,
            numberLabel = "Mobile",
            trustTier = if (isVoicemail) com.example.domain.usecase.TrustTier.VERIFIED_BUSINESS else com.example.domain.usecase.TrustTier.NEUTRAL_UNKNOWN,
            trustBadgeLabel = if (isVoicemail) "Voicemail" else null,
            simSlot = simInfo?.slotIndex?.plus(1) ?: 1,
            simDisplayName = simInfo?.displayName,
            isRoaming = simInfo?.isRoaming == true
        )
        _activeCall.value = initialCallInfo
        TelecomVoipHelper.startVoipCall(context, number, initialCallInfo.displayName, isIncoming = true)
        CallForegroundService.start(context)
        OngoingCallNotificationHelper.showCallNotification(context, initialCallInfo)

        scope.launch(Dispatchers.IO) {
            try {
                val lookedUp = ContactHelper.lookupContactByNumber(context, number)
                val dao = AppDatabase.getInstance(context).appDao()
                val favContact = try {
                    dao.getAllFavoritesList().firstOrNull {
                        ContactHelper.isSamePhoneNumber(it.phoneNumber, number)
                    }
                } catch (_: Exception) { null }
                val communityInfo = if (lookedUp == null && favContact == null && !isVoicemail) com.example.util.CommunityCallerIdService.lookup(number) else null
                val resolvedName = lookedUp?.name ?: favContact?.name ?: communityInfo?.name ?: name
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
                val enrichedCall = initialCallInfo.copy(
                    displayName = resolvedName,
                    photoUri = photoUri,
                    callReason = reason ?: communityInfo?.defaultCallReason,
                    communityInfo = communityInfo,
                    nickname = resolvedNickname,
                    numberLabel = resolvedLabel,
                    trustTier = trustBadge.first,
                    trustBadgeLabel = trustBadge.second
                )
                _activeCall.value = enrichedCall
                if (!isCallUiForegrounded) {
                    OngoingCallNotificationHelper.showCallNotification(context, enrichedCall)
                }
                checkAndExecuteAutomation(context, number, true)
            } catch (e: Exception) {
                Log.e(TAG, "Error enriching simulated incoming call", e)
            }
        }
    }

    fun startSimulatedOutgoingCall(context: Context, number: String, reason: String? = null) {
        appContext = context.applicationContext
        automationJob?.cancel()
        updateBluetoothDevicesForSimulation(context)

        val isVoicemail = ContactHelper.isVoicemailNumber(context, number)
        val simInfo = SimHelper.resolveSimInfo(context)
        val initialCallInfo = ActiveCallInfo(
            id = "sim_out_${System.currentTimeMillis()}",
            phoneNumber = number,
            displayName = if (isVoicemail) "Voicemail" else number.ifBlank { "Outgoing Call" },
            state = Call.STATE_DIALING,
            isIncoming = false,
            connectTimeMillis = 0L,
            isSimulated = true,
            photoUri = null,
            callReason = reason,
            communityInfo = null,
            nickname = null,
            numberLabel = "Mobile",
            trustTier = if (isVoicemail) com.example.domain.usecase.TrustTier.VERIFIED_BUSINESS else com.example.domain.usecase.TrustTier.NEUTRAL_UNKNOWN,
            trustBadgeLabel = if (isVoicemail) "Voicemail" else null,
            simSlot = simInfo?.slotIndex?.plus(1) ?: 1,
            simDisplayName = simInfo?.displayName,
            isRoaming = simInfo?.isRoaming == true
        )
        _activeCall.value = initialCallInfo
        TelecomVoipHelper.startVoipCall(context, number, initialCallInfo.displayName, isIncoming = false)
        CallForegroundService.start(context)
        OngoingCallNotificationHelper.showCallNotification(context, initialCallInfo)

        scope.launch(Dispatchers.IO) {
            try {
                val lookedUp = ContactHelper.lookupContactByNumber(context, number)
                val dao = AppDatabase.getInstance(context).appDao()
                val favContact = try {
                    dao.getAllFavoritesList().firstOrNull {
                        ContactHelper.isSamePhoneNumber(it.phoneNumber, number)
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
                val enrichedCall = initialCallInfo.copy(
                    displayName = resolvedName,
                    photoUri = photoUri,
                    communityInfo = communityInfo,
                    nickname = resolvedNickname,
                    numberLabel = resolvedLabel,
                    trustTier = trustBadge.first,
                    trustBadgeLabel = trustBadge.second
                )
                _activeCall.value = enrichedCall
                if (!isCallUiForegrounded) {
                    OngoingCallNotificationHelper.showCallNotification(context, enrichedCall)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error enriching simulated outgoing call", e)
            }
        }

        scope.launch {
            delay(1500)
            val current = _activeCall.value
            if (current != null && current.state == Call.STATE_DIALING) {
                val updated = current.copy(
                    state = Call.STATE_ACTIVE,
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
