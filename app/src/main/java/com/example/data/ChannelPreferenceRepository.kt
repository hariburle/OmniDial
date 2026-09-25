package com.example.data

import android.content.Context
import com.example.domain.model.CallingChannel
import com.example.util.PhoneNumberNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ChannelPreferenceRepository(
    private val appRepository: AppRepository
) {
    val allPreferences: Flow<List<NumberChannelPreference>> =
        appRepository.allNumberChannelPreferences

    private val cachedPreferences = java.util.concurrent.ConcurrentHashMap<String, String>()

    /**
     * Application context, captured in [getInstance]. Used only for the SharedPreferences
     * mirror below — never for UI.
     */
    private var appContext: Context? = null

    /**
     * False until the Room observer has delivered its first emission. Before that the cache is
     * empty for a reason we cannot distinguish from "this number has no preference", so callers
     * must not treat a miss as authoritative.
     */
    @Volatile
    private var cacheReady = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        scope.launch {
            allPreferences.collect { list ->
                val newMap = list.associate { it.normalizedNumber to it.preferredChannelId }
                cachedPreferences.putAll(newMap)
                if (cacheReady) {
                    cachedPreferences.keys.retainAll(newMap.keys)
                }
                cacheReady = true
                mirrorAllToSharedPreferences(list)
            }
        }
    }

    /**
     * Bulk-synchronizes all Room channel preferences into SharedPreferences so the redirection service
     * (car / Bluetooth / head-unit path) has an up-to-date mirror even after app restart or restore.
     */
    private fun mirrorAllToSharedPreferences(list: List<NumberChannelPreference>) {
        val ctx = appContext ?: return
        try {
            val sp = ctx.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)
            val learnedModes = mutableSetOf<String>()
            val pinnedCellular = mutableSetOf<String>()
            val pinnedSim = mutableSetOf<String>()

            for (pref in list) {
                val num = pref.normalizedNumber
                if (num.isBlank()) continue
                val chan = pref.preferredChannelId.lowercase()
                when (chan) {
                    "whatsapp" -> learnedModes.add("$num:whatsapp")
                    "whatsapp_business" -> learnedModes.add("$num:whatsapp_business")
                    "google_voice" -> learnedModes.add("$num:google_voice")
                    "sim_1" -> {
                        learnedModes.add("$num:cellular")
                        pinnedCellular.add(num)
                        pinnedSim.add("$num:1")
                    }
                    "sim_2" -> {
                        learnedModes.add("$num:cellular")
                        pinnedCellular.add(num)
                        pinnedSim.add("$num:2")
                    }
                    "cellular", "system" -> {
                        learnedModes.add("$num:cellular")
                        pinnedCellular.add(num)
                    }
                }
            }

            sp.edit()
                .putStringSet("whatsapp_learned_choices", learnedModes)
                .putStringSet("learned_call_modes", learnedModes)
                .putStringSet("pinned_cellular_numbers", pinnedCellular)
                .putStringSet("pinned_sim_numbers", pinnedSim)
                .apply()
        } catch (_: Exception) {}
    }

    fun getCachedPreference(phoneNumber: String): String? {
        val normalized = PhoneNumberNormalizer.toE164(phoneNumber)
        cachedPreferences[normalized]?.let { return it }

        val clean = phoneNumber.replace(Regex("[^0-9+]"), "")
        if (clean.isNotBlank()) {
            cachedPreferences[clean]?.let { return it }
        }

        val digits = phoneNumber.filter { it.isDigit() }
        val suffix10 = if (digits.length >= 10) digits.takeLast(10) else digits
        if (suffix10.length >= 7) {
            for ((key, pref) in cachedPreferences) {
                val keyDigits = key.filter { it.isDigit() }
                if (keyDigits.endsWith(suffix10) || (keyDigits.length >= 10 && suffix10.endsWith(keyDigits.takeLast(10)))) {
                    return pref
                }
            }
        }
        return null
    }

    suspend fun getPreferenceForNumber(phoneNumber: String): NumberChannelPreference? {
        val normalized = PhoneNumberNormalizer.toE164(phoneNumber)
        return appRepository.getNumberChannelPreference(normalized)
    }

    suspend fun getPreferredChannelId(phoneNumber: String): String? {
        val normalized = PhoneNumberNormalizer.toE164(phoneNumber)
        // Once warm the cache mirrors the whole table, so a miss is authoritative and the database
        // round-trip on the call-routing hot path is unnecessary.
        if (cacheReady) return cachedPreferences[normalized]
        return appRepository.getNumberChannelPreference(normalized)?.preferredChannelId
    }

    suspend fun setPreferenceForNumber(
        phoneNumber: String,
        channelId: String,
        customLabel: String? = null
    ) {
        val normalized = PhoneNumberNormalizer.toE164(phoneNumber)
        cachedPreferences[normalized] = channelId
        appRepository.setNumberChannelPreference(normalized, channelId, customLabel)
        mirrorToLearnedPrefs(normalized, channelId)
    }

    /**
     * Mirrors a per-number channel pin into SharedPreferences sets that
     * [com.example.telecom.OmniCallRedirectionService] can read.
     *
     * Why: the redirection service (the car / Bluetooth / head-unit path) runs on a system
     * binder thread and cannot do blocking Room reads, so it only consults SharedPreferences.
     * Without this mirror, a "Remember choice" pin or contact-sheet channel pin set in the
     * app was silently invisible to car-initiated calls. This follows the existing precedent
     * of `contact_sim_preferences`, which is mirrored to SharedPreferences for the same reason.
     *
     * Three mirrors are maintained:
     * - the learned-call-mode sets: WhatsApp / WhatsApp Business / Google Voice pins, plus
     *   cellular-ish pins recorded as "cellular" (the vocabulary the service understands);
     * - `pinned_cellular_numbers`: numbers with a deliberate "always cellular / SIM" pin, so
     *   the service can tell them apart from a stale implicit learned "cellular" entry (which
     *   must NOT outrank the global all-international rule, while the pin must);
     * - `pinned_sim_numbers`: "e164:slot" entries so a SIM 1 / SIM 2 pin is honored from the car.
     */
    private fun mirrorToLearnedPrefs(normalizedE164: String, channelId: String) {
        val ctx = appContext ?: return
        if (normalizedE164.isBlank()) return
        // Map the pin to the learned-mode vocabulary the redirection service understands.
        // "ask" has no car equivalent (no UI to prompt with), so it clears every mirror.
        val mappedMode = when (channelId.lowercase()) {
            "whatsapp" -> "whatsapp"
            "whatsapp_business" -> "whatsapp_business"
            "google_voice" -> "google_voice"
            "sim_1", "sim_2", "cellular", "system" -> "cellular"
            else -> null
        }
        val pinnedSimSlot = when (channelId.lowercase()) {
            "sim_1" -> 1
            "sim_2" -> 2
            else -> null
        }
        try {
            val sp = ctx.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)
            val ed = sp.edit()
            for (key in arrayOf("whatsapp_learned_choices", "learned_call_modes")) {
                val current = (sp.getStringSet(key, emptySet()) ?: emptySet()).toMutableSet()
                removeMirrorEntriesForNumber(current, normalizedE164)
                if (mappedMode != null) {
                    current.add("$normalizedE164:$mappedMode")
                }
                ed.putStringSet(key, current)
            }
            val pinnedCellular = (sp.getStringSet("pinned_cellular_numbers", emptySet()) ?: emptySet()).toMutableSet()
            removeMirrorEntriesForNumber(pinnedCellular, normalizedE164)
            if (mappedMode == "cellular") {
                pinnedCellular.add(normalizedE164)
            }
            ed.putStringSet("pinned_cellular_numbers", pinnedCellular)

            val pinnedSim = (sp.getStringSet("pinned_sim_numbers", emptySet()) ?: emptySet()).toMutableSet()
            removeMirrorEntriesForNumber(pinnedSim, normalizedE164)
            if (pinnedSimSlot != null) {
                pinnedSim.add("$normalizedE164:$pinnedSimSlot")
            }
            ed.putStringSet("pinned_sim_numbers", pinnedSim)
            ed.apply()
        } catch (_: Exception) {
            // Mirror is best-effort; the Room pin remains the in-app source of truth.
        }
    }

    /**
     * Removes every mirror entry for the given number (exact E.164 or same-subscriber suffix
     * match), so a pin cannot be shadowed by an older conflicting entry — the sets are
     * unordered and the service takes the first match it finds.
     */
    private fun removeMirrorEntriesForNumber(entries: MutableSet<String>, normalizedE164: String) {
        val suffix10 = normalizedE164.filter { it.isDigit() }.takeLast(10)
        entries.removeAll { entry ->
            val numKey = entry.substringBefore(":")
            val numDigits = numKey.filter { it.isDigit() }
            numKey == normalizedE164 ||
                (suffix10.length >= 7 && numDigits.takeLast(10) == suffix10)
        }
    }

    suspend fun setPreferredChannel(
        phoneNumber: String,
        channel: CallingChannel,
        customLabel: String? = null
    ) {
        setPreferenceForNumber(phoneNumber, channel.id, customLabel)
    }

    suspend fun removePreferenceForNumber(phoneNumber: String) {
        val normalized = PhoneNumberNormalizer.toE164(phoneNumber)
        cachedPreferences.remove(normalized)
        appRepository.deleteNumberChannelPreference(normalized)
        // Clearing the pin ("ask") leaves no mirror behind.
        mirrorToLearnedPrefs(normalized, "ask")
    }

    suspend fun clearAllPreferences() {
        cachedPreferences.clear()
        appRepository.clearAllNumberChannelPreferences()
    }

    companion object {
        @Volatile
        private var INSTANCE: ChannelPreferenceRepository? = null

        fun getInstance(context: Context): ChannelPreferenceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val appCtx = context.applicationContext
                    val db = AppDatabase.getInstance(appCtx)
                    val repo = AppRepository(db.appDao())
                    ChannelPreferenceRepository(repo).also {
                        it.appContext = appCtx
                        INSTANCE = it
                    }
                }
            }
        }
    }
}
