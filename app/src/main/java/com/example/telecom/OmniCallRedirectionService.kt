package com.example.telecom

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.util.ContactHelper

/**
 * OmniCallRedirectionService intercepts outgoing calls initiated from external interfaces—
 * such as Bluetooth vehicle infotainment systems (e.g. car head units), smartwatches,
 * voice assistants, or third-party dialers—and redirects them to WhatsApp VoIP when WhatsApp
 * is configured as the contact's preferred calling channel.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class OmniCallRedirectionService : CallRedirectionService() {

    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean
    ) {
        val rawScheme = handle.schemeSpecificPart ?: ""
        val cleanNumber = rawScheme.replace(Regex("[^0-9+]"), "")
        val digitsOnly = cleanNumber.filter { it.isDigit() }

        if (cleanNumber.isBlank() || ContactHelper.isVoicemailNumber(this, cleanNumber)) {
            placeCallUnmodified()
            return
        }

        val prefs = getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)
        val legacyPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val globalMode = prefs.getString("whatsapp_call_mode", null)
            ?: legacyPrefs.getString("whatsapp_call_mode", "ask_learn")
            ?: "ask_learn"

        // Check contact-specific learned calling channel from all preference stores
        val rawLearned = (prefs.getStringSet("whatsapp_learned_choices", emptySet()) ?: emptySet()) +
                         (prefs.getStringSet("learned_call_modes", emptySet()) ?: emptySet()) +
                         (legacyPrefs.getStringSet("whatsapp_learned_choices", emptySet()) ?: emptySet()) +
                         (legacyPrefs.getStringSet("learned_call_modes", emptySet()) ?: emptySet())
        val suffix10 = if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else digitsOnly

        var preferredMode: String? = null
        for (entry in rawLearned) {
            val parts = entry.split(":")
            if (parts.size >= 2) {
                val numKey = parts[0]
                val mode = parts[1]
                val numKeyDigits = numKey.filter { it.isDigit() }
                val numKeySuffix10 = if (numKeyDigits.length >= 10) numKeyDigits.takeLast(10) else numKeyDigits
                if (ContactHelper.isSamePhoneNumber(numKey, cleanNumber) ||
                    numKey == cleanNumber ||
                    (suffix10.isNotEmpty() && (numKey.endsWith(suffix10) || numKeySuffix10 == suffix10))) {
                    preferredMode = mode
                    break
                }
            }
        }

        val isInternational = ContactHelper.isInternationalNumber(this, cleanNumber)

        // Explicit per-number cellular/SIM pins, mirrored by ChannelPreferenceRepository (the
        // Room table cannot be read from this binder thread). A deliberate "always cellular"
        // pin outranks the global all-international rule; a stale implicit learned "cellular"
        // entry — which may simply record that a cellular call was once placed while in
        // ask-and-learn mode — does not. This mirrors MainViewModel.initiateCall, where the
        // per-number pin (step 0) is consulted before the global rule (step 1) and the
        // learned choice (step 3).
        fun isPinnedNumber(pinnedSet: Set<String>): Boolean {
            for (entry in pinnedSet) {
                val numKey = entry.substringBefore(":")
                val numKeyDigits = numKey.filter { it.isDigit() }
                val numKeySuffix10 = if (numKeyDigits.length >= 10) numKeyDigits.takeLast(10) else numKeyDigits
                if (ContactHelper.isSamePhoneNumber(numKey, cleanNumber) ||
                    numKey == cleanNumber ||
                    (suffix10.isNotEmpty() && (numKey.endsWith(suffix10) || numKeySuffix10 == suffix10))) {
                    return true
                }
            }
            return false
        }
        val pinnedCellular = isPinnedNumber(prefs.getStringSet("pinned_cellular_numbers", emptySet()) ?: emptySet())

        // 1. Google Voice Redirection
        if (!pinnedCellular && preferredMode == "google_voice") {
            Log.i(TAG, "External outgoing call for $cleanNumber redirected to Google Voice")
            cancelCall()
            try {
                ContactHelper.launchGoogleVoiceCall(applicationContext, cleanNumber)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch Google Voice call from redirection service", e)
            }
            return
        }

        // 2. WhatsApp Redirection (Only if WhatsApp is not globally set to "never")
        val shouldRedirectToWhatsApp = when {
            globalMode == "never" -> false
            pinnedCellular -> false
            preferredMode == "whatsapp" || preferredMode == "whatsapp_business" -> true
            globalMode == "all_international" && isInternational -> true
            else -> false
        }

        if (shouldRedirectToWhatsApp) {
            val useBusiness = preferredMode == "whatsapp_business"
            Log.i(TAG, "External outgoing call for $cleanNumber redirected to WhatsApp (preferredMode=$preferredMode)")
            // Abort cellular network call
            cancelCall()

            // Trigger WhatsApp Call immediately
            try {
                ContactHelper.launchWhatsAppCall(
                    applicationContext,
                    cleanNumber,
                    isBusiness = if (useBusiness) true else null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Direct WhatsApp launch error, dispatching notification fallback", e)
            }
            // Always post full-screen call notification to guarantee execution from background / car mode
            showWhatsAppRedirectionNotification(cleanNumber)
            return
        }

        // 3. Preferred SIM slot selection (SIM 1 vs SIM 2)
        val simPrefsSet = (prefs.getStringSet("contact_sim_preferences", emptySet()) ?: emptySet()) +
                          (legacyPrefs.getStringSet("contact_sim_preferences", emptySet()) ?: emptySet())
        val pinnedSimSet = prefs.getStringSet("pinned_sim_numbers", emptySet()) ?: emptySet()
        var preferredSimSlot: Int? = null
        for (entry in pinnedSimSet.toList() + simPrefsSet.toList()) {
            val parts = entry.split(":")
            if (parts.size >= 2) {
                val numKey = parts[0]
                val slot = parts[1].toIntOrNull()
                val numKeyDigits = numKey.filter { it.isDigit() }
                val numKeySuffix10 = if (numKeyDigits.length >= 10) numKeyDigits.takeLast(10) else numKeyDigits
                if (ContactHelper.isSamePhoneNumber(numKey, cleanNumber) ||
                    numKey == cleanNumber ||
                    (suffix10.isNotEmpty() && (numKey.endsWith(suffix10) || numKeySuffix10 == suffix10))) {
                    if (slot != null && slot > 0) {
                        preferredSimSlot = slot
                    } else if (slot == -2) {
                        val activeSims = SimHelper.getActiveSimCards(this)
                        val intlSim = activeSims.firstOrNull {
                            it.isRoaming || it.displayName.contains("intl", ignoreCase = true) || it.displayName.contains("international", ignoreCase = true)
                        }
                        preferredSimSlot = intlSim?.let { it.slotIndex + 1 } ?: (if (activeSims.size > 1) 2 else null)
                    }
                    break
                }
            }
        }

        if (preferredSimSlot != null) {
            val targetAccount = SimHelper.getPhoneAccountForSimSlot(this, preferredSimSlot - 1)
            if (targetAccount != null && targetAccount != initialPhoneAccount) {
                Log.i(TAG, "External outgoing call redirected to preferred SIM $preferredSimSlot ($targetAccount)")
                redirectCall(handle, targetAccount, false)
                return
            }
        }

        // Let standard cellular call proceed
        placeCallUnmodified()
    }

    private fun showWhatsAppRedirectionNotification(phoneNumber: String) {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channelId = "call_redirection_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Outgoing Call Redirection",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for redirected car & bluetooth calls"
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(channel)
            }

            val digitsOnly = phoneNumber.filter { it.isDigit() }
            val waIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("whatsapp://call?phone=$digitsOnly")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                `package` = "com.whatsapp"
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                109,
                waIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle("Redirecting to WhatsApp Call")
                .setContentText("Outgoing call to $phoneNumber routed via WhatsApp")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setTimeoutAfter(4000L)
                .setContentIntent(pendingIntent)
                .addAction(
                    android.R.drawable.sym_action_call,
                    "Open WhatsApp Call",
                    pendingIntent
                )
                .build()

            nm.notify(902, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display redirection notification", e)
        }
    }

    companion object {
        private const val TAG = "OmniCallRedirection"
        const val NOTIFICATION_ID = 902

        fun dismissRedirectionNotification(context: Context) {
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(NOTIFICATION_ID)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to cancel redirection notification", e)
            }
        }
    }
}
