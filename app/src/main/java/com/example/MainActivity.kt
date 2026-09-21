package com.example

import android.Manifest
import android.app.NotificationManager
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.provider.Settings
import android.telecom.Call
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import com.example.telecom.ActiveCallInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.BorderStroke
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import android.app.PendingIntent
import android.app.RemoteAction
import androidx.compose.runtime.collectAsState
import com.example.data.ChannelConfigRepository
import com.example.telecom.CallNotificationReceiver
import com.example.telecom.ChannelDiscoveryManager
import com.example.ui.components.ChannelSetupDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import com.example.telecom.CallManager
import com.example.telecom.RoleHelper
import com.example.ui.MainViewModel
import com.example.ui.components.CloudContactSyncDialog
import com.example.ui.components.ContactSaveDestination
import com.example.ui.components.DefaultAppPromptDialog
import com.example.ui.components.FullScreenPermissionPromptDialog
import com.example.ui.components.OverlayPermissionPromptDialog
import com.example.ui.components.SimChoiceDialog
import com.example.ui.components.WhatsAppChoiceDialog
import com.example.ui.components.WhatsAppIcon
import com.example.ui.screens.CallLogScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DialerScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.InCallScreen
import com.example.ui.screens.RulesScreen
import com.example.ui.theme.MyApplicationTheme

/**
 * Main application window and root coordinator for OmniDial.
 *
 * Architecture & Features:
 * - Single-Activity architecture with 5 primary tabs (Favorites, Recents, Keypad, Contacts, Rules/Settings).
 * - Manages Telecom Default Dialer role requests, system overlay permissions, and lockscreen wake flags.
 * - Dynamically overlays [InCallScreen] when an active call is present, with Picture-in-Picture (PiP) support.
 * - Handles incoming notification deep links (e.g. Missed Call high-priority scrolling).
 */
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.provideFactory(this)
    }


    private var isInPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        CallManager.init(applicationContext)
        com.example.telecom.FlipToShhhManager.initialize(this)

        val isDialIntent = com.example.util.ContactHelper.isDialOrTelIntent(intent)
        val extractedNumber = com.example.util.ContactHelper.extractPhoneNumberFromIntent(intent)

        val tabExtra = if (intent.getIntExtra("EXTRA_INITIAL_TAB", -1) != -1) {
            intent.getIntExtra("EXTRA_INITIAL_TAB", 0)
        } else if (intent.getStringExtra("EXTRA_NAV_TAB") == "RECENTS" || 
            intent.getIntExtra("EXTRA_NAV_TAB_INDEX", -1) == 1 ||
            intent.action == "android.telecom.action.SHOW_MISSED_CALLS_NOTIFICATION" ||
            intent.type == "vnd.android.cursor.dir/calls"
        ) {
            1
        } else if (isDialIntent || !extractedNumber.isNullOrBlank() || intent.getStringExtra("EXTRA_NAV_TAB") == "DIALER" || intent.getStringExtra("EXTRA_NAV_TAB") == "KEYPAD") {
            2
        } else {
            -1
        }
        handleDialIntent(intent)
        viewModel.handleIncomingIntent(intent)
        if (intent.getBooleanExtra("EXTRA_IN_CALL", false)) {
            viewModel.maximizeCall()
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            MyApplicationTheme(darkTheme = darkTheme) {
                if (isInPipMode) {
                    PipCallContent(viewModel = viewModel)
                } else {
                    MainAppContent(
                        viewModel = viewModel,
                        initialTab = tabExtra,
                        onOpenDialNumber = { number ->
                            viewModel.setDialerNumber(number)
                            viewModel.navigateToKeypad(number)
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDialIntent(intent)
        viewModel.handleIncomingIntent(intent)
        if (intent.getBooleanExtra("EXTRA_IN_CALL", false)) {
            viewModel.maximizeCall()
        }
        viewModel.refreshRecentCalls()
    }

    override fun onResume() {
        super.onResume()
        CallManager.isCallUiForegrounded = true
        val currentCall = CallManager.activeCall.value
        if (currentCall != null && currentCall.state != android.telecom.Call.STATE_DISCONNECTED) {
            com.example.telecom.OngoingCallNotificationHelper.showCallNotification(this, currentCall)
        }
        com.example.telecom.OmniCallRedirectionService.dismissRedirectionNotification(this)
        viewModel.registerCallLogObserver()
        viewModel.refreshDefaultDialerStatus()
        viewModel.refreshCallRedirectionStatus()
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            viewModel.refreshContacts()
        }
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            viewModel.refreshRecentCalls()
        }
        if (com.example.telecom.RoleHelper.isDefaultDialer(this)) {
            try {
                val telecomManager = getSystemService(android.content.Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                telecomManager?.cancelMissedCallsNotification()
            } catch (e: SecurityException) {
                Log.w("MainActivity", "SecurityException trying to cancel missed calls notification: ${e.message}")
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to cancel missed calls notification", e)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CallManager.isCallUiForegrounded = false
        val active = CallManager.activeCall.value
        if (active != null && active.state != android.telecom.Call.STATE_DISCONNECTED) {
            com.example.telecom.OngoingCallNotificationHelper.showCallNotification(this, active)
        }
    }

    override fun onStop() {
        super.onStop()
        CallManager.isCallUiForegrounded = false
        val active = CallManager.activeCall.value
        if (active != null && active.state != android.telecom.Call.STATE_DISCONNECTED) {
            com.example.telecom.OngoingCallNotificationHelper.showCallNotification(this, active)
        }
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP) {
            val active = CallManager.activeCall.value
            if (active != null && active.state == android.telecom.Call.STATE_RINGING) {
                CallManager.silenceRinger(this)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    fun updateLockScreenFlags(hasActiveCall: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(hasActiveCall)
            setTurnScreenOn(hasActiveCall)
        } else {
            @Suppress("DEPRECATION")
            if (hasActiveCall) {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            } else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        }
        if (hasActiveCall) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val active = CallManager.activeCall.value
        if (active != null && (
            active.state == android.telecom.Call.STATE_ACTIVE ||
            active.state == android.telecom.Call.STATE_DIALING ||
            active.state == android.telecom.Call.STATE_CONNECTING ||
            active.state == android.telecom.Call.STATE_RINGING
        )) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    enterPictureInPictureMode(buildPipParams())
                } catch (e: Exception) {
                    Log.w("MainActivity", "Failed to enter PiP mode", e)
                }
            }
        }
    }

    fun updatePipParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPipMode) {
            try {
                setPictureInPictureParams(buildPipParams())
            } catch (e: Exception) {
                Log.w("MainActivity", "Failed to update PiP parameters", e)
            }
        }
    }

    private fun buildPipParams(): PictureInPictureParams {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val isMuted = CallManager.isMuted.value
            val isSpeaker = CallManager.isSpeakerOn.value

            val muteIntent = Intent(this, CallNotificationReceiver::class.java).apply {
                action = CallNotificationReceiver.ACTION_TOGGLE_MUTE
            }
            val mutePendingIntent = PendingIntent.getBroadcast(
                this,
                201,
                muteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val muteIcon = android.graphics.drawable.Icon.createWithResource(
                this,
                if (isMuted) android.R.drawable.stat_notify_call_mute else android.R.drawable.ic_btn_speak_now
            )
            val muteAction = RemoteAction(
                muteIcon,
                if (isMuted) "Unmute" else "Mute",
                if (isMuted) "Unmute microphone" else "Mute microphone",
                mutePendingIntent
            )

            val speakerIntent = Intent(this, CallNotificationReceiver::class.java).apply {
                action = CallNotificationReceiver.ACTION_TOGGLE_SPEAKER
            }
            val speakerPendingIntent = PendingIntent.getBroadcast(
                this,
                202,
                speakerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val speakerIcon = android.graphics.drawable.Icon.createWithResource(
                this,
                android.R.drawable.stat_sys_speakerphone
            )
            val speakerAction = RemoteAction(
                speakerIcon,
                if (isSpeaker) "Speaker Off" else "Speaker On",
                if (isSpeaker) "Switch to earpiece" else "Switch to speaker",
                speakerPendingIntent
            )

            val hangupIntent = Intent(this, CallNotificationReceiver::class.java).apply {
                action = CallNotificationReceiver.ACTION_HANGUP
            }
            val hangupPendingIntent = PendingIntent.getBroadcast(
                this,
                203,
                hangupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val hangupIcon = android.graphics.drawable.Icon.createWithResource(
                this,
                android.R.drawable.ic_menu_close_clear_cancel
            )
            val hangupAction = RemoteAction(
                hangupIcon,
                "End Call",
                "End ongoing call",
                hangupPendingIntent
            )

            return PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .setActions(listOf(muteAction, speakerAction, hangupAction))
                .build()
        }
        throw IllegalStateException("Requires API 26+")
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    private fun handleDialIntent(intent: Intent?) {
        if (intent == null) return
        val rawNumber = com.example.util.ContactHelper.extractPhoneNumberFromIntent(intent)
        if (!rawNumber.isNullOrBlank()) {
            viewModel.setDialerNumber(rawNumber)
        }
        if (com.example.util.ContactHelper.isDialOrTelIntent(intent) || !rawNumber.isNullOrBlank()) {
            viewModel.navigateToKeypad(rawNumber)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    initialTab: Int = -1,
    onOpenDialNumber: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var ruleNumberToCreate by remember { mutableStateOf<String?>(null) }

    // Collect States
    val dialerNumber by viewModel.dialerNumber.collectAsStateWithLifecycle()
    val isDefaultDialer by viewModel.isDefaultDialer.collectAsStateWithLifecycle()
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsStateWithLifecycle()
    val automationStep by viewModel.automationState.collectAsStateWithLifecycle()
    val lastDtmfKey by viewModel.lastDtmfKey.collectAsStateWithLifecycle()
    val showInCallKeypad by viewModel.showInCallKeypad.collectAsStateWithLifecycle()
    val currentAudioRoute by viewModel.currentAudioRoute.collectAsStateWithLifecycle()
    val supportedAudioRoutes by viewModel.supportedAudioRoutes.collectAsStateWithLifecycle()
    val bluetoothDeviceName by viewModel.bluetoothDeviceName.collectAsStateWithLifecycle()
    val availableBluetoothDevices by viewModel.availableBluetoothDevices.collectAsStateWithLifecycle()
    val activeBluetoothDeviceAddress by viewModel.activeBluetoothDeviceAddress.collectAsStateWithLifecycle()

    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val recentCalls by viewModel.recentCalls.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val ignoredContacts by viewModel.ignoredContacts.collectAsStateWithLifecycle()
    val spamNumbers by viewModel.spamNumbers.collectAsStateWithLifecycle()
    val automationLogs by viewModel.automationLogs.collectAsStateWithLifecycle()
    val selectedSimSlot by viewModel.selectedSimSlot.collectAsStateWithLifecycle()
    val activeSims by viewModel.activeSims.collectAsStateWithLifecycle()
    val deviceContacts by viewModel.deviceContacts.collectAsStateWithLifecycle()
    val searchContacts by viewModel.searchContacts.collectAsStateWithLifecycle()
    val pendingCloudConfirmation by viewModel.pendingCloudConfirmation.collectAsStateWithLifecycle()
    val pendingCallMethodChoice by viewModel.pendingCallMethodChoice.collectAsStateWithLifecycle()
    val pendingSimChoice by viewModel.pendingSimChoicePrompt.collectAsStateWithLifecycle()
    val dismissModalsTrigger by viewModel.dismissModalsTrigger.collectAsStateWithLifecycle()

    val isFlipToShhhEnabled by viewModel.isFlipToShhhEnabled.collectAsStateWithLifecycle()
    val isShhhActive by viewModel.isShhhActive.collectAsStateWithLifecycle()
    val isCallScreenMinimized by viewModel.isCallScreenMinimized.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val whatsAppCallMode by viewModel.whatsAppCallMode.collectAsStateWithLifecycle()
    val globalSimPreferenceMode by viewModel.globalSimPreferenceMode.collectAsStateWithLifecycle()
    val learnedCallModes by viewModel.learnedCallModes.collectAsStateWithLifecycle()
    val defaultStartTab by viewModel.defaultStartTab.collectAsStateWithLifecycle()
    val confirmFavoritesCall by viewModel.confirmFavoritesCall.collectAsStateWithLifecycle()
    val confirmSpeedDialCall by viewModel.confirmSpeedDialCall.collectAsStateWithLifecycle()
    val askToAssignUnassignedSpeedDial by viewModel.askToAssignUnassignedSpeedDial.collectAsStateWithLifecycle()
    val speedDialKeypadDisplay by viewModel.speedDialKeypadDisplay.collectAsStateWithLifecycle()
    val showDialerQuickActions by viewModel.showDialerQuickActions.collectAsStateWithLifecycle()
    val callAnswerStyle by viewModel.callAnswerStyle.collectAsStateWithLifecycle()
    val swipeToSwitchPanels by viewModel.swipeToSwitchPanels.collectAsStateWithLifecycle()
    val navBarStyle by viewModel.navBarStyle.collectAsStateWithLifecycle()
    val localBackups by viewModel.localBackups.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = (if (initialTab in 0..4) initialTab else 0)) { 5 }
    val selectedTab = pagerState.currentPage

    fun navigateToTab(targetPage: Int) {
        coroutineScope.launch {
            pagerState.scrollToPage(targetPage.coerceIn(0, 4))
        }
    }

    var hasAppliedDefaultTab by remember { mutableStateOf(false) }
    var highlightNumber by remember { mutableStateOf<String?>(null) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val pendingNavTab by viewModel.pendingNavTab.collectAsStateWithLifecycle()
    val pendingHighlightNumber by viewModel.pendingHighlightNumber.collectAsStateWithLifecycle()

    // Clear focus on startup and tab change so keyboard never pops unexpectedly
    LaunchedEffect(selectedTab) {
        focusManager.clearFocus()
        if (selectedTab == 1) {
            viewModel.refreshRecentCalls()
        }
    }

    LaunchedEffect(activeCall?.state) {
        val hasActive = activeCall != null &&
                activeCall?.state != Call.STATE_DISCONNECTED &&
                activeCall?.state != Call.STATE_DISCONNECTING
        (context as? MainActivity)?.updateLockScreenFlags(hasActive)
        if (!hasActive) {
            viewModel.refreshRecentCalls()
        }
    }

    LaunchedEffect(pendingNavTab) {
        pendingNavTab?.let { targetPage ->
            navigateToTab(targetPage)
            viewModel.clearPendingNavTab()
        }
    }

    LaunchedEffect(pendingHighlightNumber) {
        val num = pendingHighlightNumber
        if (!num.isNullOrBlank()) {
            highlightNumber = num
            navigateToTab(1)
            viewModel.clearPendingHighlight()
        }
    }

    val activity = context as? android.app.Activity
    LaunchedEffect(activity?.intent) {
        val currentIntent = activity?.intent
        if (currentIntent != null) {
            val navTab = currentIntent.getStringExtra("EXTRA_NAV_TAB")
            val navTabIndex = currentIntent.getIntExtra("EXTRA_NAV_TAB_INDEX", -1)
            val initialTabExtra = currentIntent.getIntExtra("EXTRA_INITIAL_TAB", -1)
            val highlightNum = currentIntent.getStringExtra("EXTRA_HIGHLIGHT_NUMBER")
            val isDial = com.example.util.ContactHelper.isDialOrTelIntent(currentIntent)
            val extracted = com.example.util.ContactHelper.extractPhoneNumberFromIntent(currentIntent)

            if (navTab == "RECENTS" || navTabIndex == 1 || initialTabExtra == 1) {
                navigateToTab(1)
                if (!highlightNum.isNullOrBlank()) {
                    highlightNumber = highlightNum
                }
            } else if (isDial || !extracted.isNullOrBlank() || navTab == "DIALER" || navTab == "KEYPAD" || navTabIndex == 2) {
                if (!extracted.isNullOrBlank()) {
                    viewModel.setDialerNumber(extracted)
                }
                navigateToTab(2)
            }
        }
    }

    LaunchedEffect(initialTab) {
        if (initialTab in 0..4) {
            navigateToTab(initialTab)
        }
    }

    LaunchedEffect(defaultStartTab) {
        if (!hasAppliedDefaultTab) {
            hasAppliedDefaultTab = true
            // If app was launched normally without an explicit intent (initialTab == -1),
            // apply the user-configured default start tab
            if (initialTab == -1) {
                navigateToTab(defaultStartTab.coerceIn(0, 4))
            }
        }
    }

    val channelConfigRepo = remember { ChannelConfigRepository.getInstance(context) }
    val channelDiscoveryManager = remember { ChannelDiscoveryManager.getInstance(context) }
    val channelConfigs by channelConfigRepo.allConfigs.collectAsState(initial = emptyList())
    val allDiscoveredChannels by channelDiscoveryManager.allDiscoveredChannels.collectAsState()
    var showChannelOnboarding by remember { mutableStateOf(!channelConfigRepo.hasCompletedOnboarding()) }

    var showDefaultAppPrompt by remember { mutableStateOf(true) }
    var showOverlayPrompt by remember { mutableStateOf(true) }
    var showFullScreenPrompt by remember { mutableStateOf(true) }

    LaunchedEffect(activeCall?.id, dismissModalsTrigger) {
        // Whenever a call is initiated or incoming, always ensure call screen is maximized and dialogs dismissed
        if (activeCall != null || dismissModalsTrigger > 0L) {
            ruleNumberToCreate = null
            showDefaultAppPrompt = false
            showOverlayPrompt = false
            showFullScreenPrompt = false
            viewModel.maximizeCall()
        }
    }

    BackHandler(enabled = activeCall != null && !isCallScreenMinimized) {
        viewModel.minimizeCall()
    }

    var hasOverlayPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else true
        )
    }
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    var hasFullScreenPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.canUseFullScreenIntent() ?: true
            } else true
        )
    }

    val fullScreenPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasFullScreenPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.canUseFullScreenIntent() ?: true
        } else true
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        com.example.telecom.OmniCallRedirectionService.dismissRedirectionNotification(context)
        viewModel.registerCallLogObserver()
        channelDiscoveryManager.refreshChannels()
        hasOverlayPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
        hasFullScreenPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.canUseFullScreenIntent() ?: true
        } else true
    }

    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshDefaultDialerStatus()
        viewModel.refreshSimCards()
        channelDiscoveryManager.refreshChannels()
    }

    // Request necessary runtime permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.registerCallLogObserver()
        viewModel.refreshDefaultDialerStatus()
        viewModel.refreshSimCards()
        viewModel.refreshRecentCalls()
        viewModel.refreshContacts()
        viewModel.syncWithDeviceContacts()
        channelDiscoveryManager.refreshChannels()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.SEND_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val ungranted = permissions.filter {
            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (ungranted.isNotEmpty()) {
            permissionLauncher.launch(ungranted.toTypedArray())
        } else {
            channelDiscoveryManager.refreshChannels()
        }
    }

    LaunchedEffect(showChannelOnboarding) {
        if (showChannelOnboarding) {
            channelDiscoveryManager.refreshChannels()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AnimatedVisibility(
                    visible = activeCall != null &&
                            activeCall?.state != Call.STATE_DISCONNECTED &&
                            activeCall?.state != Call.STATE_DISCONNECTING &&
                            isCallScreenMinimized,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    activeCall?.let { call ->
                        FloatingCallPill(
                            callInfo = call,
                            isMuted = isMuted,
                            isSpeakerOn = isSpeakerOn,
                            onToggleMute = { viewModel.toggleMute() },
                            onToggleSpeaker = { viewModel.toggleSpeaker() },
                            onMaximize = { viewModel.maximizeCall() },
                            onDisconnect = { viewModel.disconnectCall() }
                        )
                    }
                }
            },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (swipeToSwitchPanels) {
                                Modifier.draggable(
                                    state = rememberDraggableState { delta ->
                                        // tracked on drag stopped
                                    },
                                    orientation = Orientation.Horizontal,
                                    onDragStopped = { velocity ->
                                        val thresholdVelocity = 300f
                                        if (velocity < -thresholdVelocity) {
                                            navigateToTab(selectedTab + 1)
                                        } else if (velocity > thresholdVelocity) {
                                            navigateToTab(selectedTab - 1)
                                        }
                                    }
                                )
                            } else Modifier
                        )
                ) {
                    when (navBarStyle) {
                        "indicator" -> {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .height(38.dp)
                                    .testTag("bottom_nav_bar")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val navTabs = listOf(
                                        0 to "Favorites",
                                        1 to "Recents",
                                        2 to "Keypad",
                                        3 to "Contacts",
                                        4 to "Rules"
                                    )
                                    navTabs.forEach { (tabIdx, tabLabel) ->
                                        val isSelected = selectedTab == tabIdx
                                        Box(
                                            modifier = Modifier
                                                .height(28.dp)
                                                .clickable {
                                                    if (tabIdx == 4) ruleNumberToCreate = null
                                                    navigateToTab(tabIdx)
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                                .testTag("nav_${tabLabel.lowercase()}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(
                                                        width = if (isSelected) 30.dp else 7.dp,
                                                        height = 5.dp
                                                    )
                                                    .background(
                                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                                        RoundedCornerShape(3.dp)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        "compact" -> {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .height(52.dp)
                                    .testTag("bottom_nav_bar")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val navItems = listOf(
                                        Triple(0, Icons.Default.Star, "Favorites"),
                                        Triple(1, Icons.Default.History, "Recents"),
                                        Triple(2, Icons.Default.Dialpad, "Keypad"),
                                        Triple(3, Icons.Default.Contacts, "Contacts"),
                                        Triple(4, Icons.Default.SmartToy, "Rules")
                                    )
                                    navItems.forEach { (tabIdx, icon, name) ->
                                        val isSelected = selectedTab == tabIdx
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clickable {
                                                    if (tabIdx == 4) ruleNumberToCreate = null
                                                    navigateToTab(tabIdx)
                                                }
                                                .testTag("nav_${name.lowercase()}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .width(52.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = name,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { navigateToTab(0) },
                                    icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                                    label = { Text("Favorites") },
                                    modifier = Modifier.testTag("nav_favorites")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { navigateToTab(1) },
                                    icon = { Icon(Icons.Default.History, contentDescription = "Recents") },
                                    label = { Text("Recents") },
                                    modifier = Modifier.testTag("nav_recents")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = { navigateToTab(2) },
                                    icon = { Icon(Icons.Default.Dialpad, contentDescription = "Keypad") },
                                    label = { Text("Keypad") },
                                    modifier = Modifier.testTag("nav_keypad")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = { navigateToTab(3) },
                                    icon = { Icon(Icons.Default.Contacts, contentDescription = "Contacts") },
                                    label = { Text("Contacts") },
                                    modifier = Modifier.testTag("nav_contacts")
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 4,
                                    onClick = {
                                        ruleNumberToCreate = null
                                        navigateToTab(4)
                                    },
                                    icon = { Icon(Icons.Default.SmartToy, contentDescription = "Rules") },
                                    label = { Text("Rules") },
                                    modifier = Modifier.testTag("nav_rules")
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = swipeToSwitchPanels,
                    beyondViewportPageCount = 4,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                    0 -> FavoritesScreen(
                        favorites = favorites,
                        recentCalls = recentCalls,
                        ignoredContacts = ignoredContacts,
                        confirmFavoritesCall = confirmFavoritesCall,
                        getPreferredCallingMode = { num -> viewModel.getPreferredCallingMode(num) },
                        onSaveLearnedCallMode = { num, mode -> viewModel.saveLearnedCallMode(num, mode) },
                        learnedCallModes = learnedCallModes,
                        whatsAppCallMode = whatsAppCallMode,
                        onSelectNumber = { num ->
                            viewModel.setDialerNumber(num)
                            navigateToTab(2)
                        },
                        onCallNumber = { num ->
                            viewModel.initiateCall(context, num)
                        },
                        onCallNumberDirect = { num, slot ->
                            viewModel.placeCall(context, num, overrideSimSlot = slot)
                        },
                        onCallWhatsApp = { num ->
                            viewModel.placeWhatsAppCall(context, num)
                        },
                        onCallGoogleVoice = { num ->
                            viewModel.placeGoogleVoiceCall(context, num)
                        },
                        onCreateRule = { num ->
                            ruleNumberToCreate = num
                            navigateToTab(4)
                        },
                        onDeleteFavorite = { fav -> viewModel.deleteFavorite(fav) },
                        onAddFavorite = { name, num, label, photoUri, nickname ->
                            viewModel.addFavorite(name, num, label, photoUri, nickname)
                        },
                        onAddNewContact = { name, number, label, destination, addToFavorites ->
                            val saveToDevice = (destination == ContactSaveDestination.PHONE_CONTACTS)
                            viewModel.createNewContact(name, number, label, saveToDevice, addToFavorites)
                        },
                        onAssignSpeedDial = { contact, slot ->
                            viewModel.assignSpeedDial(contact, slot)
                        },
                        onMoveFavorite = { fromIndex, toIndex ->
                            viewModel.moveFavorite(fromIndex, toIndex)
                        },
                        onReorderFavorites = { newFavorites ->
                            viewModel.reorderFavorites(newFavorites)
                        },
                        onEditFavorite = { contact, newNickname ->
                            viewModel.updateFavorite(contact, newNickname)
                        },
                        onUpdateFavoriteNumber = { contact, newNum, newLabel ->
                            viewModel.updateFavoritePhoneNumber(contact, newNum, newLabel)
                        },
                        onIgnoreContact = { num, name, cat, tag ->
                            viewModel.ignorePopularContact(num, name, cat, tag)
                        },
                        onUnignoreContact = { num ->
                            viewModel.unignorePopularContact(num)
                        },
                        onUpdateIgnoredContactTag = { num, tag, name ->
                            viewModel.updateIgnoredContactTag(num, tag, name)
                        },
                        isFlipToShhhEnabled = isFlipToShhhEnabled,
                        isShhhActive = isShhhActive,
                        onToggleFlipToShhh = { viewModel.toggleFlipToShhh() },
                        onSetDefaultContactNumber = { contact, num, label -> viewModel.setDefaultContactNumber(contact, num, label) },
                        onDeleteContact = { viewModel.deleteContact(it) },
                        deviceContacts = deviceContacts,
                        activeSims = activeSims,
                        getPreferredSimSlot = { num -> viewModel.getPreferredSimSlot(num) },
                        onSetPreferredSimSlot = { num, slot -> viewModel.setPreferredSimSlot(num, slot) },
                        globalSimPreferenceMode = globalSimPreferenceMode,
                        dismissModalsTrigger = dismissModalsTrigger
                    )
                    1 -> CallLogScreen(
                        recentCalls = recentCalls,
                        spamNumbers = spamNumbers,
                        favorites = favorites,
                        rules = rules,
                        deviceContacts = deviceContacts,
                        activeSims = activeSims,
                        highlightNumber = highlightNumber,
                        isSpamNumber = { num -> viewModel.isSpamNumber(num) },
                        getPreferredCallingMode = { num -> viewModel.getPreferredCallingMode(num) },
                        onSaveLearnedCallMode = { num, mode -> viewModel.saveLearnedCallMode(num, mode) },
                        getPreferredSimSlot = { num -> viewModel.getPreferredSimSlot(num) },
                        onSetPreferredSimSlot = { num, slot -> viewModel.setPreferredSimSlot(num, slot) },
                        globalSimPreferenceMode = globalSimPreferenceMode,
                        onCallBack = { recentCall ->
                            if (recentCall.callReason?.contains("WhatsApp Business", ignoreCase = true) == true) {
                                viewModel.placeWhatsAppCall(context, recentCall.phoneNumber, isBusiness = true)
                            } else if (recentCall.callReason?.contains("WhatsApp", ignoreCase = true) == true) {
                                viewModel.placeWhatsAppCall(context, recentCall.phoneNumber, isBusiness = false)
                            } else if (recentCall.callReason?.contains("Google Voice", ignoreCase = true) == true) {
                                viewModel.placeGoogleVoiceCall(context, recentCall.phoneNumber)
                            } else {
                                viewModel.placeCall(context, recentCall.phoneNumber, null, overrideSimSlot = recentCall.simSlot)
                            }
                        },
                        onCreateRuleForNumber = { num ->
                            ruleNumberToCreate = num
                            navigateToTab(4)
                        },
                        onMarkSpam = { num -> viewModel.markAsSpam(num) },
                        onRemoveSpam = { num -> viewModel.removeSpam(num) },
                        onToggleFavorite = { name, num, label, photoUri ->
                            viewModel.toggleFavorite(name, num, label, photoUri)
                        },
                        onUpdateFavoriteNumber = { contact, newNum, newLabel ->
                            viewModel.updateFavoritePhoneNumber(contact, newNum, newLabel)
                        },
                        onAddNewContact = { name, num, label, saveToDevice, addToFavs ->
                            viewModel.createNewContact(
                                name = name,
                                phoneNumber = num,
                                label = label,
                                saveToDevice = saveToDevice,
                                addToFavorites = addToFavs
                            )
                        },
                        onUpdateNoteAndReminder = { call, note, rem ->
                            viewModel.updateRecentCallNoteAndReminder(call, note, rem)
                        },
                        onUpdateContact = { oldNum, name, number, label, nickname ->
                            viewModel.updateContact(oldNum, name, number, label, nickname)
                        },
                        onDeleteContact = { contact ->
                            viewModel.deleteContact(contact)
                        },
                        onDeleteCall = { call ->
                            viewModel.deleteRecentCall(call)
                        },
                        onDeleteCallsForNumber = { phoneNumber ->
                            viewModel.deleteRecentCallsForNumber(phoneNumber)
                        },
                        onSetDefaultContactNumber = { contact, num, label -> viewModel.setDefaultContactNumber(contact, num, label) },
                        dismissModalsTrigger = dismissModalsTrigger
                    )
                    2 -> DialerScreen(
                        number = dialerNumber,
                        favorites = favorites,
                        recentCalls = recentCalls,
                        isDefaultDialer = isDefaultDialer,
                        context = context,
                        simSlot = selectedSimSlot,
                        activeSims = activeSims,
                        onToggleSim = { viewModel.toggleSimSlot() },
                        onRoleChanged = { viewModel.refreshDefaultDialerStatus() },
                        onDigitPress = { viewModel.appendDigit(it) },
                        onDeleteDigit = { viewModel.deleteLastDigit() },
                        onClearDigits = { viewModel.clearDigits() },
                        onSelectContactNumber = { num -> viewModel.setDialerNumber(num) },
                        onPlaceCall = { num, reason -> viewModel.placeCall(context, num, reason) },
                        onPlaceCallDirect = { num, slot -> viewModel.placeCall(context, num, overrideSimSlot = slot) },
                        onPlaceWhatsAppCall = { num -> viewModel.placeWhatsAppCall(context, num) },
                        onPlaceWhatsAppCallWithBusiness = { num, isBiz -> viewModel.placeWhatsAppCall(context, num, isBiz) },
                        onPlaceGoogleVoiceCall = { num -> viewModel.placeGoogleVoiceCall(context, num) },
                        whatsAppCallMode = whatsAppCallMode,
                        onSimulateCall = { num, name -> viewModel.simulateIncomingCall(context, num, name) },
                        getPreferredCallingMode = { num -> viewModel.getPreferredCallingMode(num) },
                        onCreateRuleForNumber = { num ->
                            ruleNumberToCreate = num
                            navigateToTab(4)
                        },
                        onAddFavorite = { name, num, label, photoUri ->
                            viewModel.addFavorite(name, num, label, photoUri)
                        },
                        onAddNewContact = { name, number, label, destination, addToFavorites ->
                            val saveToDevice = (destination == ContactSaveDestination.PHONE_CONTACTS)
                            viewModel.createNewContact(name, number, label, saveToDevice, addToFavorites)
                        },
                        onDeleteFavorite = { fav ->
                            viewModel.deleteFavorite(fav)
                        },
                        onAssignSpeedDialSlot = { slot, name, num, photoUri ->
                            viewModel.assignSpeedDialSlot(slot, name, num, photoUri)
                        },
                        onClearSpeedDialSlot = { slot ->
                            viewModel.clearSpeedDialSlot(slot)
                        },
                        confirmSpeedDialCall = confirmSpeedDialCall,
                        askToAssignUnassignedSpeedDial = askToAssignUnassignedSpeedDial,
                        speedDialKeypadDisplay = speedDialKeypadDisplay,
                        showDialerQuickActions = showDialerQuickActions,
                        deviceContacts = deviceContacts,
                        precomputedSearchContacts = searchContacts
                    )
                    3 -> ContactsScreen(
                        favorites = favorites,
                        recentCalls = recentCalls,
                        deviceContacts = deviceContacts,
                        activeSims = activeSims,
                        onRefreshContacts = { viewModel.refreshContacts() },
                        onPlaceWhatsAppCall = { num -> viewModel.placeWhatsAppCall(context, num) },
                        getPreferredCallingMode = { num -> viewModel.getPreferredCallingMode(num) },
                        onSaveLearnedCallMode = { num, mode -> viewModel.saveLearnedCallMode(num, mode) },
                        whatsAppCallMode = whatsAppCallMode,
                        getPreferredSimSlot = { num -> viewModel.getPreferredSimSlot(num) },
                        onSetPreferredSimSlot = { num, slot -> viewModel.setPreferredSimSlot(num, slot) },
                        globalSimPreferenceMode = globalSimPreferenceMode,
                        onSelectNumber = { num ->
                            viewModel.setDialerNumber(num)
                            navigateToTab(2)
                        },
                        onCallNumber = { num ->
                            viewModel.initiateCall(context, num)
                        },
                        onCallNumberDirect = { num, slot ->
                            viewModel.placeCall(context, num, overrideSimSlot = slot)
                        },
                        onCreateRule = { num ->
                            ruleNumberToCreate = num
                            navigateToTab(4)
                        },
                        onToggleFavorite = { name, num, label, photoUri ->
                            viewModel.toggleFavorite(name, num, label, photoUri)
                        },
                        onDeleteFavorite = { fav ->
                            viewModel.deleteFavorite(fav)
                        },
                        onAddFavorite = { name, num, label, photoUri ->
                            viewModel.addFavorite(name, num, label, photoUri)
                        },
                        onUpdateFavoriteNumber = { contact, newNum, newLabel ->
                            viewModel.updateFavoritePhoneNumber(contact, newNum, newLabel)
                        },
                        onAddNewContact = { name, num, label, destination, addToFavs ->
                            viewModel.createNewContact(
                                name = name,
                                phoneNumber = num,
                                label = label,
                                saveToDevice = destination == ContactSaveDestination.PHONE_CONTACTS,
                                addToFavorites = addToFavs
                            )
                        },
                        onAddNewContactMulti = { name, numbers, destination, addToFavs ->
                            viewModel.createNewContact(
                                name = name,
                                phoneNumbers = numbers,
                                saveToDevice = destination == ContactSaveDestination.PHONE_CONTACTS,
                                addToFavorites = addToFavs
                            )
                        },
                        onSyncContactToPhone = { contact ->
                            viewModel.syncAppContactToPhone(contact)
                        },
                        onSyncAllAppContactsToDevice = {
                            viewModel.syncAllAppContactsToDevice()
                        },
                        onSetDefaultContactNumber = { contact, num, label -> viewModel.setDefaultContactNumber(contact, num, label) },
                        onDeleteContact = { viewModel.deleteContact(it) },
                        dismissModalsTrigger = dismissModalsTrigger
                    )
                    4 -> RulesScreen(
                        rules = rules,
                        automationLogs = automationLogs,
                        favorites = favorites,
                        themeMode = themeMode,
                        onSetThemeMode = { viewModel.setThemeMode(it) },
                        whatsAppCallMode = whatsAppCallMode,
                        onSetWhatsAppCallMode = { viewModel.setWhatsAppCallMode(it) },
                        onResetWhatsAppChoices = { viewModel.resetWhatsAppChoices() },
                        learnedChoicesCount = learnedCallModes.size,
                        spamNumbers = spamNumbers,
                        onAddSpam = { num, tag -> viewModel.markAsSpam(num, tag) },
                        onRemoveSpam = { viewModel.removeSpam(it) },
                        confirmFavoritesCall = confirmFavoritesCall,
                        onSetConfirmFavoritesCall = { viewModel.setConfirmFavoritesCall(it) },
                        confirmSpeedDialCall = confirmSpeedDialCall,
                        onSetConfirmSpeedDialCall = { viewModel.setConfirmSpeedDialCall(it) },
                        askToAssignUnassignedSpeedDial = askToAssignUnassignedSpeedDial,
                        onSetAskToAssignUnassignedSpeedDial = { viewModel.setAskToAssignUnassignedSpeedDial(it) },
                        speedDialKeypadDisplay = speedDialKeypadDisplay,
                        onSetSpeedDialKeypadDisplay = { viewModel.setSpeedDialKeypadDisplay(it) },
                        showDialerQuickActions = showDialerQuickActions,
                        onSetShowDialerQuickActions = { viewModel.setShowDialerQuickActions(it) },
                        defaultStartTab = defaultStartTab,
                        onSetDefaultStartTab = { viewModel.setDefaultStartTab(it) },
                        callAnswerStyle = callAnswerStyle,
                        onSetCallAnswerStyle = { viewModel.setCallAnswerStyle(it) },
                        onToggleRule = { viewModel.toggleRuleEnabled(it) },
                        onSaveRule = { viewModel.saveRule(it) },
                        onDeleteRule = { viewModel.deleteRule(it) },
                        onClearLogs = { viewModel.clearLogs() },
                        onTestRule = { rule ->
                            val targetNumber = when {
                                rule.phoneNumberPattern.isNotBlank() && rule.phoneNumberPattern != "*" -> rule.phoneNumberPattern
                                else -> "+15550199"
                            }
                            viewModel.simulateIncomingCall(context, targetNumber, rule.name, "Rule Simulation Test")
                        },
                        onDuplicateRule = { rule ->
                            viewModel.saveRule(rule.copy(id = 0L, name = "${rule.name} (Copy)"))
                        },
                        initiallyShowAddRuleWithNumber = ruleNumberToCreate,
                        onConsumeAddRuleNumber = { ruleNumberToCreate = null },
                        deviceContacts = deviceContacts,
                        swipeToSwitchPanels = swipeToSwitchPanels,
                        onSetSwipeToSwitchPanels = { viewModel.setSwipeToSwitchPanels(it) },
                        navBarStyle = navBarStyle,
                        onSetNavBarStyle = { viewModel.setNavBarStyle(it) },
                        onExportBackup = { uri, onDone -> viewModel.exportBackup(uri, onDone) },
                        onImportBackup = { uri, onDone -> viewModel.importBackup(uri, onDone) },
                        localBackups = localBackups,
                        onCreateLocalBackup = { onDone -> viewModel.createLocalBackup(onDone) },
                        onRestoreLocalBackup = { file, onDone -> viewModel.restoreLocalBackup(file, onDone) },
                        onDeleteLocalBackup = { file -> viewModel.deleteLocalBackup(file) },
                        globalSimPreferenceMode = globalSimPreferenceMode,
                        onSetGlobalSimPreferenceMode = { viewModel.setGlobalSimPreferenceMode(it) },
                        activeSims = activeSims,
                        channelConfigs = channelConfigs,
                        discoveredChannels = allDiscoveredChannels,
                        onSaveChannelConfigs = { configs ->
                            coroutineScope.launch {
                                channelConfigRepo.saveConfigs(configs)
                                channelDiscoveryManager.refreshChannels()
                                viewModel.refreshSimCards()
                                viewModel.refreshRecentCalls()
                            }
                        },
                        dismissModalsTrigger = dismissModalsTrigger
                    )
                }
            }

        // Active In-Call Overlay Screen (Appears seamlessly over UI whenever call is active/ringing)
        AnimatedVisibility(
            visible = activeCall != null && !isCallScreenMinimized,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            activeCall?.let { call ->
                InCallScreen(
                    callInfo = call,
                    isMuted = isMuted,
                    isSpeakerOn = isSpeakerOn,
                    automationStep = automationStep,
                    lastDtmfKey = lastDtmfKey,
                    showKeypad = showInCallKeypad,
                    onToggleKeypad = { viewModel.toggleInCallKeypad() },
                    onAnswer = { viewModel.answerCall() },
                    onDecline = { viewModel.declineCall() },
                    onDisconnect = { viewModel.disconnectCall() },
                    onToggleMute = { viewModel.toggleMute() },
                    onToggleSpeaker = { viewModel.toggleSpeaker() },
                    audioRoute = currentAudioRoute,
                    supportedAudioRoutes = supportedAudioRoutes,
                    bluetoothDeviceName = bluetoothDeviceName,
                    availableBluetoothDevices = availableBluetoothDevices,
                    activeBluetoothDeviceAddress = activeBluetoothDeviceAddress,
                    onSelectAudioRoute = { route -> viewModel.setAudioRoute(route) },
                    onSelectBluetoothDevice = { address -> viewModel.selectBluetoothDevice(address) },
                    onPlayDtmf = { viewModel.playDtmf(it) },
                    onStopDtmf = { viewModel.stopDtmf() },
                    onDeclineWithSms = { msg -> viewModel.declineWithSms(msg) },
                    onSavePostCallNote = { note, reminderMinutes ->
                        val reminderTime = reminderMinutes?.let { System.currentTimeMillis() + it * 60 * 1000 }
                        viewModel.savePostCallNote(call.phoneNumber, note, reminderTime)
                    },
                    onMarkSpam = { num -> viewModel.markAsSpam(num) },
                    onUnblockSpam = { num -> viewModel.removeSpam(num) },
                    onDismiss = { viewModel.minimizeCall() },
                    onClosePostCall = { viewModel.dismissCall() },
                    callAnswerStyle = callAnswerStyle
                )
            }
        }

        val isCallScreenVisible = activeCall != null && !isCallScreenMinimized

        // Explicit Confirmation Dialog before making any changes to Google Account Contacts in the Cloud
        if (!isCallScreenVisible) {
            pendingCloudConfirmation?.let { conf ->
                CloudContactSyncDialog(
                    confirmation = conf,
                    onConfirm = {
                        conf.onConfirmCloudAction()
                        viewModel.clearCloudConfirmation()
                    },
                    onSecondary = conf.onSecondaryAction?.let { sec ->
                        {
                            sec.invoke()
                            viewModel.clearCloudConfirmation()
                        }
                    },
                    onDismiss = {
                        conf.onDismissOrCancel()
                        viewModel.clearCloudConfirmation()
                    }
                )
            }
        }

        // WhatsApp vs Cellular Call Choice Dialog (Ask Always & Ask and Learn)
        if (!isCallScreenVisible) {
            pendingCallMethodChoice?.let { prompt ->
                WhatsAppChoiceDialog(
                    prompt = prompt,
                    onChooseMethod = { method, rememberChoice ->
                        viewModel.chooseCallMethod(context, method, rememberChoice)
                    },
                    onDismiss = { viewModel.dismissCallMethodChoice() }
                )
            }
        }

        // Dual-SIM Outgoing Call Choice Dialog (Always Ask)
        if (!isCallScreenVisible) {
            pendingSimChoice?.let { prompt ->
                SimChoiceDialog(
                    prompt = prompt,
                    activeSims = activeSims,
                    onSelectSim = { slot ->
                        viewModel.confirmSimChoiceAndPlaceCall(context, slot)
                    },
                    onDismiss = { viewModel.cancelSimChoice() }
                )
            }
        }

        // Check if OmniDial is the default app on startup, and prompt user if not
        if (!isCallScreenVisible && !isDefaultDialer && showDefaultAppPrompt) {
            DefaultAppPromptDialog(
                onRequestSetDefault = {
                    val intent = RoleHelper.createDefaultDialerIntent(context)
                    if (intent != null) {
                        defaultDialerLauncher.launch(intent)
                    }
                    showDefaultAppPrompt = false
                },
                onDismiss = { showDefaultAppPrompt = false }
            )
        }

        // Channel Discovery & First-Launch Onboarding Dialog
        if (!isCallScreenVisible && showChannelOnboarding && allDiscoveredChannels.isNotEmpty()) {
            ChannelSetupDialog(
                discoveredChannels = allDiscoveredChannels,
                existingConfigs = channelConfigs,
                isOnboarding = true,
                onSave = { configs ->
                    coroutineScope.launch {
                        channelConfigRepo.saveConfigs(configs)
                        channelConfigRepo.setCompletedOnboarding(true)
                        channelDiscoveryManager.refreshChannels()
                        showChannelOnboarding = false
                    }
                },
                onDismiss = {
                    channelConfigRepo.setCompletedOnboarding(true)
                    showChannelOnboarding = false
                }
            )
        }

        // Check Display Over Other Apps permission on startup for car & bluetooth call redirection
        if (!isCallScreenVisible && !hasOverlayPermission && showOverlayPrompt && (!showDefaultAppPrompt || isDefaultDialer)) {
            OverlayPermissionPromptDialog(
                onRequestPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            overlayPermissionLauncher.launch(intent)
                        } catch (_: Exception) {
                            try {
                                val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                overlayPermissionLauncher.launch(fallback)
                            } catch (_: Exception) {}
                        }
                    }
                    showOverlayPrompt = false
                },
                onDismiss = { showOverlayPrompt = false }
            )
        }

        // Check Full Screen Intent permission (Android 14+ / API 34+) to wake screen for background calls
        if (!isCallScreenVisible && hasOverlayPermission && !hasFullScreenPermission && showFullScreenPrompt && (!showDefaultAppPrompt || isDefaultDialer)) {
            FullScreenPermissionPromptDialog(
                onRequestPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        try {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                Uri.parse("package:${context.packageName}")
                            )
                            fullScreenPermissionLauncher.launch(intent)
                        } catch (_: Exception) {
                            try {
                                val fallback = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
                                fullScreenPermissionLauncher.launch(fallback)
                            } catch (_: Exception) {}
                        }
                    }
                    showFullScreenPrompt = false
                },
                onDismiss = { showFullScreenPrompt = false }
            )
        }
    }
}
}
}

@Composable
private fun FloatingCallPill(
    callInfo: ActiveCallInfo,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onMaximize: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var elapsedSeconds by remember { mutableStateOf(0L) }

    LaunchedEffect(callInfo.connectTimeMillis, callInfo.state) {
        val connectTime = callInfo.connectTimeMillis
        if (connectTime > 0L && callInfo.state == Call.STATE_ACTIVE) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - connectTime) / 1000).coerceAtLeast(0L)
                delay(1000)
            }
        } else {
            elapsedSeconds = 0L
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timerText = if (callInfo.connectTimeMillis > 0L && callInfo.state == Call.STATE_ACTIVE) {
        String.format("%02d:%02d", minutes, seconds)
    } else when (callInfo.state) {
        Call.STATE_RINGING -> "Incoming call"
        Call.STATE_DIALING, Call.STATE_CONNECTING -> "Calling..."
        Call.STATE_HOLDING -> "On hold"
        else -> "In Call"
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF15803D), // Rich notification-bar emerald green
        contentColor = Color.White,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        modifier = modifier
            .statusBarsPadding()
            .padding(top = 8.dp, bottom = 8.dp, start = 12.dp, end = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .testTag("floating_call_pill")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clickable left side (Avatar + Name & Status / Duration) that maximizes call
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onMaximize() }
                    .padding(vertical = 2.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar circle with photo or caller initial
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF166534),
                    modifier = Modifier.size(38.dp)
                ) {
                    if (!callInfo.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = callInfo.photoUri,
                            contentDescription = callInfo.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            val initialChar = (callInfo.displayName.takeIf { it.isNotBlank() } ?: callInfo.phoneNumber)
                                .take(1).uppercase()
                            Text(
                                text = initialChar,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Caller Name & Live Duration Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    val displayName = buildString {
                        append(callInfo.displayName.ifBlank { callInfo.phoneNumber })
                        if (!callInfo.nickname.isNullOrBlank() && !callInfo.nickname.equals(callInfo.displayName, ignoreCase = true)) {
                            append(" (${callInfo.nickname})")
                        }
                    }
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Live green pulsing status dot
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(
                                    if (callInfo.state == Call.STATE_ACTIVE) Color(0xFF4ADE80) else Color(0xFFFBBF24),
                                    CircleShape
                                )
                        )
                        val labelPrefix = if (!callInfo.numberLabel.isNullOrBlank()) "${callInfo.numberLabel} • " else ""
                        Text(
                            text = if (callInfo.state == Call.STATE_ACTIVE) {
                                "${labelPrefix}Active • $timerText"
                            } else {
                                "${labelPrefix}$timerText"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Notification-bar action controls: Speakerphone, Mute, End Call
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Speakerphone button
                IconButton(
                    onClick = onToggleSpeaker,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.22f),
                            CircleShape
                        )
                        .testTag("floating_pill_speaker_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = if (isSpeakerOn) "Speaker Off" else "Speaker On",
                        tint = if (isSpeakerOn) Color(0xFF15803D) else Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Mute Microphone button
                IconButton(
                    onClick = onToggleMute,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isMuted) Color(0xFFEF4444) else Color.White.copy(alpha = 0.22f),
                            CircleShape
                        )
                        .testTag("floating_pill_mute_btn")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // End Call button
                IconButton(
                    onClick = onDisconnect,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFDC2626), CircleShape)
                        .testTag("floating_pill_hangup_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Rich Picture-in-Picture notification-style floating call pill showing
 * caller avatar, name, duration, and speakerphone, mute, and hangup controls
 * when user is doing something else on the phone.
 */
@Composable
fun PipCallContent(viewModel: MainViewModel) {
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsStateWithLifecycle()
    var elapsedSeconds by remember { mutableStateOf(0L) }

    LaunchedEffect(activeCall?.connectTimeMillis, activeCall?.state) {
        val connectTime = activeCall?.connectTimeMillis ?: 0L
        if (connectTime > 0L && activeCall?.state == Call.STATE_ACTIVE) {
            while (true) {
                elapsedSeconds = ((System.currentTimeMillis() - connectTime) / 1000).coerceAtLeast(0L)
                delay(1000)
            }
        } else {
            elapsedSeconds = 0L
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    val timerText = String.format("%02d:%02d", minutes, seconds)
    val call = activeCall

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pip_call_container"),
        color = Color(0xFF15803D)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left info: Avatar + Name + Timer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF166534),
                    modifier = Modifier.size(32.dp)
                ) {
                    if (!call?.photoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = call?.photoUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (call?.displayName?.takeIf { it.isNotBlank() } ?: call?.phoneNumber ?: "C").take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = call?.displayName?.takeIf { it.isNotBlank() } ?: call?.phoneNumber ?: "Ongoing call",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Active • $timerText",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right actions: Speaker, Mute, Hangup
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Speakerphone
                IconButton(
                    onClick = { viewModel.toggleSpeaker() },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.22f),
                            CircleShape
                        )
                        .testTag("pip_speaker_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = if (isSpeakerOn) "Speaker Off" else "Speaker On",
                        tint = if (isSpeakerOn) Color(0xFF15803D) else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Mute
                IconButton(
                    onClick = { viewModel.toggleMute() },
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isMuted) Color(0xFFEF4444) else Color.White.copy(alpha = 0.22f),
                            CircleShape
                        )
                        .testTag("pip_mute_btn")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Hang up
                IconButton(
                    onClick = { viewModel.disconnectCall() },
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFDC2626), CircleShape)
                        .testTag("pip_hangup_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Hang up",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
