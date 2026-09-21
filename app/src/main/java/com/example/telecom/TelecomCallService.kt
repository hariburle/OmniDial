package com.example.telecom

import android.content.Intent
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import com.example.MainActivity

class TelecomCallService : InCallService() {

    companion object {
        private const val TAG = "TelecomCallService"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "InCallService created")
        CallManager.setTelecomService(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "InCallService destroyed")
        CallManager.setTelecomService(null)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "onCallAdded: $call")
        CallManager.onCallAdded(call, applicationContext)

        // Show ongoing call notification for background/switch-apps support
        CallManager.activeCall.value?.let {
            OngoingCallNotificationHelper.showCallNotification(applicationContext, it)
        }

        // Bring In-Call UI forward
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_IN_CALL", true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch MainActivity on call added", e)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "onCallRemoved: $call")
        CallManager.onCallRemoved(call, applicationContext)
        OngoingCallNotificationHelper.cancelCallNotification(applicationContext)
    }

    @Deprecated("Deprecated in Java", ReplaceWith("onCallEndpointChanged"))
    @Suppress("DEPRECATION")
    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        Log.d(TAG, "onCallAudioStateChanged: $audioState")
        CallManager.onCallAudioStateChanged(audioState)
    }

    override fun onBringToForeground(showDialpad: Boolean) {
        super.onBringToForeground(showDialpad)
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_SHOW_DIALPAD", showDialpad)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bring to foreground", e)
        }
    }
}
