package com.example.telecom

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.telecom.Call
import android.util.Log
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * High-priority Foreground Service that keeps ongoing calls alive when screen is locked
 * or when the user switches to other apps.
 * Keeps the ongoing call notification banner ticking with the elapsed time in the status bar
 * and lock screen, just like standard Android Phone.
 */
class CallForegroundService : Service() {

    companion object {
        private const val TAG = "CallForegroundService"
        const val ACTION_START = "com.example.telecom.START_CALL_FOREGROUND"
        const val ACTION_STOP = "com.example.telecom.STOP_CALL_FOREGROUND"

        fun start(context: Context) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start CallForegroundService", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop CallForegroundService", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickerJob: Job? = null
    private var collectorJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        OngoingCallNotificationHelper.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopServiceInternal()
            return START_NOT_STICKY
        }

        startForegroundWithCall()
        startObservingCallState()

        return START_STICKY
    }

    private fun startForegroundWithCall() {
        val currentCall = CallManager.activeCall.value
        if (currentCall == null || currentCall.state == Call.STATE_DISCONNECTED) {
            stopServiceInternal()
            return
        }

        val notification = OngoingCallNotificationHelper.buildCallNotification(this, currentCall)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    OngoingCallNotificationHelper.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
            } else {
                startForeground(OngoingCallNotificationHelper.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting foreground service", e)
        }
    }

    private fun startObservingCallState() {
        collectorJob?.cancel()
        collectorJob = serviceScope.launch {
            CallManager.activeCall.collect { call ->
                if (call == null || call.state == Call.STATE_DISCONNECTED) {
                    stopServiceInternal()
                } else {
                    if (!CallManager.isCallUiForegrounded) {
                        OngoingCallNotificationHelper.showCallNotification(this@CallForegroundService, call)
                    }
                    manageTicker(call)
                }
            }
        }
    }

    private fun manageTicker(call: ActiveCallInfo) {
        if (call.state == Call.STATE_ACTIVE) {
            if (tickerJob?.isActive != true) {
                tickerJob = serviceScope.launch {
                    while (isActive) {
                        delay(1000)
                        val active = CallManager.activeCall.value
                        if (active != null && active.state == Call.STATE_ACTIVE) {
                            if (!CallManager.isCallUiForegrounded) {
                                OngoingCallNotificationHelper.showCallNotification(this@CallForegroundService, active)
                            }
                        } else {
                            break
                        }
                    }
                }
            }
        } else {
            tickerJob?.cancel()
            tickerJob = null
        }
    }

    private fun stopServiceInternal() {
        tickerJob?.cancel()
        tickerJob = null
        collectorJob?.cancel()
        collectorJob = null
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping foreground", e)
        }
        OngoingCallNotificationHelper.cancelCallNotification(this)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        tickerJob?.cancel()
        collectorJob?.cancel()
        serviceScope.cancel()
        OngoingCallNotificationHelper.cancelCallNotification(this)
    }
}
