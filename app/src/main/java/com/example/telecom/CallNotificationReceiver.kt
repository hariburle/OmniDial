package com.example.telecom

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_HANGUP = "com.example.telecom.ACTION_HANGUP"
        const val ACTION_ANSWER = "com.example.telecom.ACTION_ANSWER"
        const val ACTION_TOGGLE_MUTE = "com.example.telecom.ACTION_TOGGLE_MUTE"
        const val ACTION_TOGGLE_SPEAKER = "com.example.telecom.ACTION_TOGGLE_SPEAKER"
        const val ACTION_UNSPAM = "com.example.telecom.ACTION_UNSPAM"
        const val EXTRA_PHONE_NUMBER = "com.example.telecom.EXTRA_PHONE_NUMBER"
        const val EXTRA_NOTIFICATION_ID = "com.example.telecom.EXTRA_NOTIFICATION_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_UNSPAM -> {
                val number = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: return
                val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
                val manager = context.getSystemService(android.app.NotificationManager::class.java)
                if (notifId != 0) {
                    manager?.cancel(notifId)
                }

                // Add to persistent not_spam_whitelist in SharedPreferences
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val currentWl = prefs.getStringSet("not_spam_whitelist", emptySet())?.toMutableSet() ?: mutableSetOf()
                currentWl.add(number)
                val cleanDigits = number.filter { it.isDigit() }.takeLast(10)
                if (cleanDigits.isNotBlank()) currentWl.add(cleanDigits)
                prefs.edit().putStringSet("not_spam_whitelist", currentWl).apply()

                // Remove from database spam table and update call log in background with structured concurrency
                val pendingResult = goAsync()
                com.example.util.AppCoroutineScope.launch {
                    try {
                        val dao = com.example.data.AppDatabase.getInstance(context).appDao()
                        dao.deleteSpamByNumber(number)
                        val allSpam = dao.getAllSpamNumbersList()
                        for (sp in allSpam) {
                            val spDigits = sp.phoneNumber.filter { it.isDigit() }.takeLast(10)
                            if (sp.phoneNumber == number || (cleanDigits.length >= 7 && spDigits == cleanDigits)) {
                                dao.deleteSpamNumber(sp)
                            }
                        }
                        dao.updateRecentCallSpamStatus(number, false)
                        val allCalls = dao.getAllRecentCallsList()
                        for (c in allCalls) {
                            val callDigits = c.phoneNumber.filter { it.isDigit() }.takeLast(10)
                            if (c.phoneNumber == number || (cleanDigits.length >= 7 && callDigits == cleanDigits)) {
                                if (c.isSpam) {
                                    dao.updateRecentCall(c.copy(isSpam = false, note = "Unmarked by user"))
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CallNotificationReceiver", "Error unmarking spam", e)
                    } finally {
                        pendingResult.finish()
                    }
                }

                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    android.widget.Toast.makeText(context, "$number unspammed & whitelisted", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            ACTION_ANSWER -> {
                CallManager.answerCall()
                CallForegroundService.start(context)
                CallManager.activeCall.value?.let {
                    OngoingCallNotificationHelper.showCallNotification(context, it)
                }
            }
            ACTION_HANGUP -> {
                CallManager.disconnectCall()
                CallForegroundService.stop(context)
                OngoingCallNotificationHelper.cancelCallNotification(context)
            }
            ACTION_TOGGLE_MUTE -> {
                CallManager.toggleMute()
                CallManager.activeCall.value?.let {
                    OngoingCallNotificationHelper.showCallNotification(context, it)
                }
            }
            ACTION_TOGGLE_SPEAKER -> {
                CallManager.toggleSpeaker()
                CallManager.activeCall.value?.let {
                    OngoingCallNotificationHelper.showCallNotification(context, it)
                }
            }
        }
    }
}
