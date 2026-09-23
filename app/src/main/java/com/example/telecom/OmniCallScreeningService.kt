package com.example.telecom

import android.content.Context
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.data.AppDatabase
import com.example.util.PhoneNumberNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Makes OmniDial eligible for Android's "Caller ID & spam app" default
 * (Settings → Apps → Default apps → Caller ID & spam app).
 *
 * Screening philosophy: never silently drop a call the user might have
 * wanted. Numbers on the spam blocklist are *silenced and logged* — the
 * call appears as a missed call with a notification, so it can be called
 * back. Only if the user explicitly enables "Block spam calls automatically"
 * in settings are spam calls rejected outright.
 *
 * Non-spam calls are always allowed through untouched.
 *
 * Requires the manifest declaration:
 *
 * <service
 *     android:name=".telecom.OmniCallScreeningService"
 *     android:permission="android.permission.BIND_CALL_SCREENING_SERVICE"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="android.telecom.CallScreeningService" />
 *     </intent-filter>
 * </service>
 *
 * …and the ROLE_CALL_SCREENING role, requested via
 * [RoleHelper.createCallScreeningRoleIntent].
 */
class OmniCallScreeningService : CallScreeningService() {

    companion object {
        private const val TAG = "OmniCallScreening"
        const val PREFS = "kishan_dialer_prefs"
        const val KEY_SPAM_AUTO_BLOCK = "spam_auto_block"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onScreenCall(details: Call.Details) {
        val rawNumber = details.handle?.schemeSpecificPart?.trim().orEmpty()

        serviceScope.launch {
            var silence = false
            var disallow = false
            var label: String? = null

            if (rawNumber.isNotEmpty()) {
                try {
                    val dao = AppDatabase.getInstance(applicationContext).appDao()
                    val normalized = PhoneNumberNormalizer.toE164(rawNumber)
                    val entry = dao.getSpamByNumber(rawNumber, rawNumber)
                        ?: dao.getSpamByNormalizedNumber(normalized)
                    if (entry != null && entry.isBlocked) {
                        label = entry.label
                        val autoBlock = applicationContext
                            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .getBoolean(KEY_SPAM_AUTO_BLOCK, false)
                        if (autoBlock) disallow = true else silence = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Spam lookup failed for $rawNumber, allowing call", e)
                }
            }

            Log.d(TAG, "Screening '$rawNumber': disallow=$disallow silence=$silence label=$label")
            val response = CallResponse.Builder()
                .setDisallowCall(disallow)
                .setRejectCall(false)
                .setSilenceCall(silence)
                .setSkipCallLog(false)
                .setSkipNotification(false)
                .build()
            respondToCall(details, response)
        }
    }
}
