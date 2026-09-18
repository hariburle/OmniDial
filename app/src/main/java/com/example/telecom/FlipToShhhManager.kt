package com.example.telecom

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.telecom.Call
import android.telecom.TelecomManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

object FlipToShhhManager : SensorEventListener {
    private const val TAG = "FlipToShhhManager"
    private const val PREFS_NAME = "flip_to_shhh_prefs"
    private const val KEY_ENABLED = "flip_to_shhh_enabled"

    private var sensorManager: SensorManager? = null
    private var accelSensor: Sensor? = null
    private var proximitySensor: Sensor? = null
    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isFlipToShhhEnabled = MutableStateFlow(true)
    val isFlipToShhhEnabled: StateFlow<Boolean> = _isFlipToShhhEnabled.asStateFlow()

    private val _isShhhActive = MutableStateFlow(false)
    val isShhhActive: StateFlow<Boolean> = _isShhhActive.asStateFlow()

    private val _isFaceDown = MutableStateFlow(false)
    val isFaceDown: StateFlow<Boolean> = _isFaceDown.asStateFlow()

    private var lastZ = 0f
    private var lastX = 0f
    private var lastY = 0f
    private var isProximityNear = false
    private var previousInterruptionFilter: Int? = null
    private var previousRingerMode: Int? = null

    private var faceDownDebounceJob: Job? = null
    private var faceUpDebounceJob: Job? = null

    // Ringing lift-to-silence state
    private var initialRingingZ: Float? = null
    private var initialRingingTimestamp = 0L
    private var wasRingingFlat = false

    fun initialize(context: Context) {
        val app = context.applicationContext
        this.appContext = app

        val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isFlipToShhhEnabled.value = prefs.getBoolean(KEY_ENABLED, true)

        sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        if (_isFlipToShhhEnabled.value) {
            startListening()
        }

        scope.launch {
            CallManager.activeCall.collect { call ->
                if (call != null && call.state == Call.STATE_RINGING) {
                    startListening()
                    initialRingingZ = null
                    wasRingingFlat = false
                } else if (!_isFlipToShhhEnabled.value) {
                    stopListening()
                    initialRingingZ = null
                    wasRingingFlat = false
                }
            }
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        _isFlipToShhhEnabled.value = enabled
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()

        if (enabled) {
            startListening()
        } else {
            stopListening()
            if (_isShhhActive.value) {
                deactivateShhh()
            }
        }
    }

    private fun startListening() {
        accelSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        proximitySensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun stopListening() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val activeCall = CallManager.activeCall.value
        val isCallRinging = activeCall != null && activeCall.state == Call.STATE_RINGING

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                lastX = event.values[0]
                lastY = event.values[1]
                lastZ = event.values[2]

                if (isCallRinging) {
                    evaluateRingingPickupGesture()
                }

                if (_isFlipToShhhEnabled.value) {
                    evaluateFaceOrientation()
                }
            }
            Sensor.TYPE_PROXIMITY -> {
                val distance = event.values[0]
                val maxRange = event.sensor.maximumRange
                val wasNear = isProximityNear
                isProximityNear = distance < 4.0f || distance < maxRange

                if (isCallRinging && wasNear && !isProximityNear) {
                    // Proximity uncovered while ringing (e.g. pulled out of pocket or lifted off table)
                    appContext?.let { ctx ->
                        if (!CallManager.isRingerSilenced.value) {
                            CallManager.silenceRinger(ctx)
                            Log.d(TAG, "Incoming call ringer silenced: Proximity sensor uncovered")
                        }
                    }
                }

                if (_isFlipToShhhEnabled.value) {
                    evaluateFaceOrientation()
                }
            }
        }
    }

    private fun evaluateRingingPickupGesture() {
        val ctx = appContext ?: return
        if (CallManager.isRingerSilenced.value) return

        if (initialRingingZ == null) {
            initialRingingZ = lastZ
            initialRingingTimestamp = System.currentTimeMillis()
            // Check if phone was resting horizontally flat (face up or face down)
            wasRingingFlat = abs(lastZ) > 6.0f && abs(lastX) < 5.0f && abs(lastY) < 5.0f
            return
        }

        val elapsed = System.currentTimeMillis() - initialRingingTimestamp
        if (elapsed < 350L) return // Debounce initial incoming vibration burst

        val totalA = kotlin.math.sqrt(lastX * lastX + lastY * lastY + lastZ * lastZ)
        val isDynamicLiftJerk = abs(totalA - 9.8f) > 2.8f
        val isTiltedUp = abs(lastZ) < 6.0f && abs(lastY) > 3.5f

        if (wasRingingFlat && (isDynamicLiftJerk || isTiltedUp)) {
            CallManager.silenceRinger(ctx)
            Log.d(TAG, "Incoming call ringer silenced: Lift gesture detected (Z: $lastZ, Y: $lastY, totalA: $totalA)")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun evaluateFaceOrientation() {
        // Face-down condition:
        // Screen facing down -> Z is negative (~ -9.8 m/s²) and X/Y are near horizontal plane
        val isFaceDownOrientation = lastZ < -6.5f && abs(lastX) < 4.5f && abs(lastY) < 4.5f
        val isConfirmedFaceDown = isFaceDownOrientation && (proximitySensor == null || isProximityNear)

        if (isConfirmedFaceDown) {
            faceUpDebounceJob?.cancel()
            faceUpDebounceJob = null
            if (!_isFaceDown.value && faceDownDebounceJob == null) {
                faceDownDebounceJob = scope.launch {
                    delay(300) // Debounce to prevent accidental triggers while in transit
                    _isFaceDown.value = true
                    activateShhh()
                    faceDownDebounceJob = null
                }
            }
        } else {
            faceDownDebounceJob?.cancel()
            faceDownDebounceJob = null
            if (_isFaceDown.value && faceUpDebounceJob == null) {
                faceUpDebounceJob = scope.launch {
                    delay(250) // Debounce when picking phone up
                    _isFaceDown.value = false
                    deactivateShhh()
                    faceUpDebounceJob = null
                }
            }
        }
    }

    private fun activateShhh() {
        val context = appContext ?: return
        _isShhhActive.value = true
        Log.d(TAG, "Flip to Shhh activated: Phone placed face down")

        // 1. Double buzz haptic feedback confirming Flip to Shhh
        vibrate(context, longArrayOf(0, 50, 70, 50))

        // 2. Silence incoming ringing call if one is active
        silenceIncomingCallIfRinging(context)

        // 3. Apply Do Not Disturb filter or Silent Ringer
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (nm != null && nm.isNotificationPolicyAccessGranted) {
                previousInterruptionFilter = nm.currentInterruptionFilter
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            } else {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                if (am != null) {
                    previousRingerMode = am.ringerMode
                    am.ringerMode = AudioManager.RINGER_MODE_SILENT
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set DND filter", e)
        }
    }

    private fun deactivateShhh() {
        val context = appContext ?: return
        _isShhhActive.value = false
        Log.d(TAG, "Flip to Shhh deactivated: Phone lifted face up")

        // Single buzz haptic feedback
        vibrate(context, longArrayOf(0, 50))

        // Restore previous Do Not Disturb filter or Ringer Mode
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val prevFilter = previousInterruptionFilter
            if (nm != null && prevFilter != null && nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(prevFilter)
                previousInterruptionFilter = null
            }

            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val prevRinger = previousRingerMode
            if (am != null && prevRinger != null) {
                am.ringerMode = prevRinger
                previousRingerMode = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore DND filter", e)
        }
    }

    fun silenceIncomingCallIfRinging(context: Context) {
        CallManager.silenceRinger(context)
    }

    fun isNotificationPolicyAccessGranted(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return nm?.isNotificationPolicyAccessGranted ?: false
    }

    fun openNotificationPolicyAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open DND settings", e)
        }
    }

    fun isDndActive(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        val filter = nm.currentInterruptionFilter
        return filter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
                filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                filter == NotificationManager.INTERRUPTION_FILTER_ALARMS
    }

    fun isCallerAllowedUnderCurrentDnd(context: Context, isFavorite: Boolean): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return true
        val filter = nm.currentInterruptionFilter
        return when (filter) {
            NotificationManager.INTERRUPTION_FILTER_ALL -> true
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> isFavorite
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> false
            else -> true
        }
    }

    private fun vibrate(context: Context, pattern: LongArray) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (_: Exception) {}
    }
}
