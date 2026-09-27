package com.example.telecom

import android.content.Context
import android.telephony.TelephonyManager
import com.example.domain.model.CallingChannel
import com.example.util.ContactHelper
import com.example.util.PhoneNumberNormalizer

/**
 * Outcome of a Travel & Roaming routing evaluation.
 */
data class TravelRoutingDecision(
    val recommendedChannel: CallingChannel?,
    val reason: String,
    val isTravelOptimized: Boolean
)

/**
 * Enterprise Travel & Roaming Intelligence Engine.
 *
 * CORE ARCHITECTURAL INVARIANT:
 * Zero-Mutation Runtime Overlay.
 *
 * When a user travels abroad (e.g., US to India with a Dual-SIM setup):
 * - Saved contact preferences in Room DB (number_channel_preferences) and learned modes
 *   are NEVER mutated, altered, or overwritten.
 * - This engine intercepts channel dispatching dynamically at runtime.
 * - Calling home country numbers (e.g., +1 while in India) auto-routes via WhatsApp VoIP,
 *   saving the user from expensive international roaming and ISD tariffs.
 * - Calling host country numbers (e.g., +91 while in India) auto-routes via the domestic
 *   non-roaming SIM slot, avoiding roaming SIM usage.
 * - When the user returns to the US, the cellular network detects "US", the travel overlay
 *   disengages instantaneously, and all saved preferences resume immediately with
 *   ZERO manual intervention or restoration steps.
 */
open class TravelRoamingManager(
    private val context: Context,
    private val discoveryManager: ChannelDiscoveryManager = ChannelDiscoveryManager.getInstance(context)
) {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isSmartRoamingEnabled(): Boolean {
        return prefs.getBoolean(KEY_SMART_ROAMING_ENABLED, true)
    }

    fun setSmartRoamingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMART_ROAMING_ENABLED, enabled).apply()
    }

    fun getHomeCountryIso(): String {
        val saved = prefs.getString(KEY_HOME_COUNTRY_ISO, null)
        if (!saved.isNullOrBlank()) return saved.lowercase()
        val sims = discoveryManager.availableChannels.value.filterIsInstance<CallingChannel.CellularSim>()
        val simCountry = sims.firstOrNull { !it.isRoaming }?.countryIso?.lowercase()
            ?: sims.firstOrNull()?.countryIso?.lowercase()
        return simCountry ?: "us"
    }

    fun isHomeCountryExplicitlySet(): Boolean {
        return prefs.contains(KEY_HOME_COUNTRY_ISO)
    }

    fun setHomeCountryIso(countryIso: String) {
        prefs.edit().putString(KEY_HOME_COUNTRY_ISO, countryIso.trim().lowercase()).apply()
    }

    /**
     * Permanent Relocation Assistant:
     * Promotes learned travel preferences (e.g., travel:IN) to become the new home profile (home:IN)
     * if promoteTravelPreferences is true, and updates the home country ISO.
     */
    suspend fun relocateHomeCountry(
        newHomeIso: String,
        promoteTravelPreferences: Boolean,
        appRepository: com.example.data.AppRepository
    ) {
        val targetIso = newHomeIso.trim().lowercase()
        if (promoteTravelPreferences) {
            val travelContext = "travel:${targetIso.uppercase()}"
            val newHomeContext = "home:${targetIso.uppercase()}"
            appRepository.promoteProfilePreferences(travelContext, newHomeContext)
        }
        setHomeCountryIso(targetIso)
    }

    fun getPrimaryDomesticSimSlot(): Int {
        return prefs.getInt(KEY_PRIMARY_DOMESTIC_SIM_SLOT, 1)
    }

    fun setPrimaryDomesticSimSlot(slot: Int) {
        prefs.edit().putInt(KEY_PRIMARY_DOMESTIC_SIM_SLOT, slot).apply()
    }

    fun isRoamingGuardEnabled(): Boolean {
        return prefs.getBoolean(KEY_ROAMING_GUARD_ENABLED, true)
    }

    fun setRoamingGuardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ROAMING_GUARD_ENABLED, enabled).apply()
    }

    fun getActiveProfileContext(): String {
        val current = getCurrentCountryIso().uppercase()
        val home = getHomeCountryIso().uppercase()
        return if (!isTraveling() && current == home) {
            "home:$home"
        } else {
            "travel:$current"
        }
    }

    /**
     * Resolves the current connected physical network country ISO in lowercase (e.g. "us", "in").
     */
    open fun getCurrentCountryIso(): String {
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val networkCountry = tm?.networkCountryIso?.trim()?.lowercase()
            if (!networkCountry.isNullOrBlank()) return networkCountry
        } catch (_: Exception) {}
        return ContactHelper.getDeviceCountryIso(context)
    }

    /**
     * Checks if the device is currently traveling outside its home network or if any active SIM is roaming.
     */
    open fun isTraveling(): Boolean {
        val currentCountry = getCurrentCountryIso().lowercase()
        val homeCountry = getHomeCountryIso().lowercase()
        if (currentCountry.isNotBlank() && homeCountry.isNotBlank() && currentCountry != homeCountry) {
            return true
        }

        val activeSims = discoveryManager.availableChannels.value.filterIsInstance<CallingChannel.CellularSim>()
        if (activeSims.any { it.isRoaming }) return true

        val homeSimCountries = activeSims.mapNotNull { it.countryIso?.lowercase() }
        if (homeSimCountries.isNotEmpty() && !homeSimCountries.contains(currentCountry)) {
            return true
        }

        return false
    }

    /**
     * Resolves the destination country ISO (in uppercase, e.g. "US", "IN") from the dialed number.
     */
    fun getTargetCountryIso(phoneNumber: String): String {
        val clean = phoneNumber.trim()
        val detected = ContactHelper.getCountryIsoForNumber(clean)
        if (detected != null) return detected

        // Normalize and check international prefix
        val e164 = PhoneNumberNormalizer.toE164(clean, getCurrentCountryIso().uppercase())
        val e164Detected = ContactHelper.getCountryIsoForNumber(e164)
        if (e164Detected != null) return e164Detected

        return getCurrentCountryIso().uppercase()
    }

    /**
     * Finds a domestic, non-roaming SIM matching the given country ISO or name tag.
     */
    fun findDomesticSim(countryIso: String, channels: List<CallingChannel>? = null): CallingChannel.CellularSim? {
        val sims = (channels ?: discoveryManager.availableChannels.value)
            .filterIsInstance<CallingChannel.CellularSim>()
        val targetIso = countryIso.lowercase()

        // 1. Explicit countryIso match and not roaming
        sims.firstOrNull { !it.isRoaming && it.countryIso?.equals(targetIso, ignoreCase = true) == true }?.let { return it }

        // 2. Display name or deviceSimName match (e.g. "IN", "India", "Jio", "Airtel")
        sims.firstOrNull {
            !it.isRoaming && (
                it.displayName.contains(targetIso, ignoreCase = true) ||
                it.carrierName.contains(targetIso, ignoreCase = true) ||
                (it.deviceSimName?.contains(targetIso, ignoreCase = true) == true)
            )
        }?.let { return it }

        // 3. Fallback to any non-roaming SIM
        return sims.firstOrNull { !it.isRoaming }
    }

    /**
     * Finds the actively roaming SIM (e.g. US SIM in India).
     */
    fun findRoamingSim(channels: List<CallingChannel>? = null): CallingChannel.CellularSim? {
        val sims = (channels ?: discoveryManager.availableChannels.value)
            .filterIsInstance<CallingChannel.CellularSim>()
        return sims.firstOrNull { it.isRoaming }
    }

    /**
     * Evaluates the non-mutating travel routing recommendation for a dialed phone number.
     */
    fun evaluateTravelRouting(
        phoneNumber: String,
        pinnedChannel: CallingChannel? = null,
        userExplicitOverride: CallingChannel? = null,
        availableChannels: List<CallingChannel>? = null,
        activeRules: List<com.example.data.TelecomRoutingRule>? = null
    ): TravelRoutingDecision {
        // 1. Emergency calls always route to domestic cellular emergency service, never VoIP
        if (discoveryManager.isEmergencyNumber(phoneNumber)) {
            val emergencySim = discoveryManager.getEmergencyCellularChannel()
                ?: availableChannels?.filterIsInstance<CallingChannel.CellularSim>()?.firstOrNull()
            return TravelRoutingDecision(
                recommendedChannel = emergencySim ?: CallingChannel.SystemDefault,
                reason = "Emergency number routed to cellular network",
                isTravelOptimized = false
            )
        }

        // 2. User explicit selection on keypad dock always takes highest priority
        if (userExplicitOverride != null && userExplicitOverride !is CallingChannel.AskAlways) {
            return TravelRoutingDecision(
                recommendedChannel = userExplicitOverride,
                reason = "User explicit dock selection",
                isTravelOptimized = false
            )
        }

        // 3. If smart roaming is disabled by user, honor base preference without travel overlay
        if (!isSmartRoamingEnabled()) {
            return TravelRoutingDecision(
                recommendedChannel = pinnedChannel,
                reason = "Smart roaming disabled",
                isTravelOptimized = false
            )
        }

        val clean = phoneNumber.trim()
        if (clean.isBlank()) {
            return TravelRoutingDecision(pinnedChannel, "Empty phone number", false)
        }

        val channels = availableChannels ?: discoveryManager.availableChannels.value
        val currentCountry = getCurrentCountryIso()
        val targetCountry = getTargetCountryIso(clean)
        val waBizChannel = channels.filterIsInstance<CallingChannel.WhatsApp>().firstOrNull { it.isBusiness && it.isAvailable }
        val waPersonalChannel = channels.filterIsInstance<CallingChannel.WhatsApp>().firstOrNull { !it.isBusiness && it.isAvailable }
        val fallbackWa = waBizChannel ?: waPersonalChannel

        val activeSims = channels.filterIsInstance<CallingChannel.CellularSim>()
        val hasRoamingSim = activeSims.any { it.isRoaming }

        // Tier 2: Dynamic User Routing Rules (@ Slot Composer)
        if (!activeRules.isNullOrEmpty()) {
            for (rule in activeRules.filter { it.isEnabled }.sortedByDescending { it.priority }) {
                if (matchesRule(rule, currentCountry, targetCountry, clean, hasRoamingSim)) {
                    val resolvedChan = resolveRuleChannel(rule.targetChannelId, channels, waBizChannel, waPersonalChannel)
                    if (resolvedChan != null) {
                        return TravelRoutingDecision(
                            recommendedChannel = resolvedChan,
                            reason = "Matched Dynamic Rule: ${rule.name}",
                            isTravelOptimized = true
                        )
                    }
                }
            }
        }

        // Tier 2: Pinned / Profile-Scoped Preference
        if (pinnedChannel != null) {
            // If pinned channel is a roaming cellular SIM:
            if (pinnedChannel is CallingChannel.CellularSim && pinnedChannel.isRoaming) {
                // If calling a local domestic number in current country, prefer a local non-roaming SIM
                val localSim = activeSims.firstOrNull { !it.isRoaming && it.countryIso.equals(currentCountry, ignoreCase = true) }
                    ?: findDomesticSim(currentCountry, channels)
                if (localSim != null && targetCountry.isNotBlank() && targetCountry.equals(currentCountry, ignoreCase = true)) {
                    return TravelRoutingDecision(
                        recommendedChannel = localSim,
                        reason = "Roaming Safeguard: Domestic call routed to local non-roaming SIM",
                        isTravelOptimized = true
                    )
                }
                return TravelRoutingDecision(
                    recommendedChannel = pinnedChannel,
                    reason = "Saved Preference (Roaming SIM Active)",
                    isTravelOptimized = false
                )
            }
            return TravelRoutingDecision(
                recommendedChannel = pinnedChannel,
                reason = "Saved Preference",
                isTravelOptimized = false
            )
        }

        // Tier 3: Domestic Local SIM Safeguard (No rule & No contact preference)
        if (targetCountry.isNotBlank() && targetCountry.equals(currentCountry, ignoreCase = true)) {
            val localSim = activeSims.firstOrNull { !it.isRoaming && it.countryIso.equals(currentCountry, ignoreCase = true) }
                ?: findDomesticSim(currentCountry, channels)
            if (localSim != null) {
                return TravelRoutingDecision(
                    recommendedChannel = localSim,
                    reason = "Domestic call routed to local non-roaming SIM",
                    isTravelOptimized = hasRoamingSim
                )
            }
        }

        // Fall back to default non-roaming SIM or first available channel
        val defaultChannel = activeSims.firstOrNull { !it.isRoaming } ?: activeSims.firstOrNull() ?: channels.firstOrNull()
        return TravelRoutingDecision(
            recommendedChannel = defaultChannel,
            reason = "Standard channel resolution",
            isTravelOptimized = false
        )
    }

    fun matchesRule(
        rule: com.example.data.TelecomRoutingRule,
        currentCountry: String,
        targetCountry: String,
        cleanNumber: String,
        hasRoamingSim: Boolean
    ): Boolean {
        // Location check
        val loc = rule.locationPattern.lowercase()
        val homeIso = getHomeCountryIso().lowercase()
        val locMatch = when {
            loc == "any" -> true
            loc == "roaming" || loc == "any_roaming" -> (currentCountry != homeIso || hasRoamingSim || isTraveling())
            loc.startsWith("travel:") -> {
                val expectedIso = loc.removePrefix("travel:").lowercase()
                currentCountry == expectedIso && (currentCountry != homeIso || hasRoamingSim || isTraveling())
            }
            loc.startsWith("home:") -> {
                val expectedIso = loc.removePrefix("home:").lowercase()
                currentCountry == expectedIso && currentCountry == homeIso && !hasRoamingSim
            }
            loc == "home" -> currentCountry == homeIso && !hasRoamingSim
            loc == "travel" -> currentCountry != homeIso || hasRoamingSim || isTraveling()
            else -> currentCountry == loc
        }
        if (!locMatch) return false

        // Destination prefix check
        val dest = rule.destinationPrefix.trim()
        val destMatch = when {
            dest.equals("any", ignoreCase = true) -> true
            dest.equals("all_intl", ignoreCase = true) -> targetCountry != currentCountry.uppercase()
            dest == "+1" -> cleanNumber.startsWith("+1") || targetCountry == "US"
            dest == "+91" -> cleanNumber.startsWith("+91") || targetCountry == "IN"
            dest.startsWith("+") -> cleanNumber.startsWith(dest)
            else -> cleanNumber.startsWith(dest) || targetCountry.equals(dest, ignoreCase = true)
        }
        return destMatch
    }

    private fun resolveRuleChannel(
        targetChannelId: String,
        channels: List<CallingChannel>,
        waBiz: CallingChannel.WhatsApp?,
        waPersonal: CallingChannel.WhatsApp?
    ): CallingChannel? {
        return when (targetChannelId.lowercase()) {
            "whatsapp_business", "whatsapp_biz", "w4b" -> waBiz ?: waPersonal
            "whatsapp", "whatsapp_personal" -> waPersonal ?: waBiz
            "sim_1", "sim1" -> channels.filterIsInstance<CallingChannel.CellularSim>().firstOrNull { it.slotIndex == 0 }
            "sim_2", "sim2" -> channels.filterIsInstance<CallingChannel.CellularSim>().firstOrNull { it.slotIndex == 1 }
            "google_voice" -> channels.filterIsInstance<CallingChannel.GoogleVoice>().firstOrNull()
            else -> channels.firstOrNull { it.id.equals(targetChannelId, ignoreCase = true) }
        }
    }

    companion object {
        private const val PREFS_NAME = "travel_roaming_prefs"
        private const val KEY_SMART_ROAMING_ENABLED = "smart_roaming_enabled"
        private const val KEY_HOME_COUNTRY_ISO = "home_country_iso"
        private const val KEY_PRIMARY_DOMESTIC_SIM_SLOT = "primary_domestic_sim_slot"
        private const val KEY_ROAMING_GUARD_ENABLED = "roaming_guard_enabled"

        @Volatile
        private var INSTANCE: TravelRoamingManager? = null

        fun getInstance(context: Context): TravelRoamingManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TravelRoamingManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
