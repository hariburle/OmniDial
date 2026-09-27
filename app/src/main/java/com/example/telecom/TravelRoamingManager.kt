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

    fun setHomeCountryIso(countryIso: String) {
        prefs.edit().putString(KEY_HOME_COUNTRY_ISO, countryIso.trim().lowercase()).apply()
    }

    fun getPrimaryDomesticSimSlot(): Int {
        return prefs.getInt(KEY_PRIMARY_DOMESTIC_SIM_SLOT, 1)
    }

    fun setPrimaryDomesticSimSlot(slot: Int) {
        prefs.edit().putInt(KEY_PRIMARY_DOMESTIC_SIM_SLOT, slot).apply()
    }

    fun isUsCallsViaWhatsAppBizEnabled(): Boolean {
        return prefs.getBoolean(KEY_US_CALLS_WA_BIZ, true)
    }

    fun setUsCallsViaWhatsAppBizEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_US_CALLS_WA_BIZ, enabled).apply()
    }

    fun isIndiaCallsViaWhatsAppEnabled(): Boolean {
        return prefs.getBoolean(KEY_INDIA_CALLS_WA_PERSONAL, true)
    }

    fun setIndiaCallsViaWhatsAppEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INDIA_CALLS_WA_PERSONAL, enabled).apply()
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
    fun isTraveling(): Boolean {
        val activeSims = discoveryManager.availableChannels.value.filterIsInstance<CallingChannel.CellularSim>()
        if (activeSims.any { it.isRoaming }) return true

        val currentCountry = getCurrentCountryIso()
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
        availableChannels: List<CallingChannel>? = null
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

        // ====================================================================
        // RULE SET A: Device is physically in INDIA ("in")
        // ====================================================================
        if (currentCountry == "in") {
            // Case A1: Calling USA (+1) from India -> WhatsApp Business (US #)
            if (targetCountry == "US") {
                val targetWa = if (isUsCallsViaWhatsAppBizEnabled()) {
                    waBizChannel ?: waPersonalChannel
                } else {
                    waPersonalChannel ?: waBizChannel
                }
                if (targetWa != null) {
                    val label = if (targetWa.isBusiness) "WhatsApp Business (US #)" else "WhatsApp"
                    return TravelRoutingDecision(
                        recommendedChannel = targetWa,
                        reason = "Travel Mode (India): Calling US (+1) routed to $label to prevent international roaming charges",
                        isTravelOptimized = true
                    )
                }
            }

            // Case A2: Calling India (+91) while in India -> WhatsApp Personal or Domestic India SIM
            if (targetCountry == "IN") {
                if (isIndiaCallsViaWhatsAppEnabled() && waPersonalChannel != null) {
                    return TravelRoutingDecision(
                        recommendedChannel = waPersonalChannel,
                        reason = "Travel Mode (India): Calling India (+91) routed to WhatsApp Personal VoIP",
                        isTravelOptimized = true
                    )
                }
                val domesticIndiaSim = findDomesticSim("in", channels)
                if (domesticIndiaSim != null) {
                    return TravelRoutingDecision(
                        recommendedChannel = domesticIndiaSim,
                        reason = "Travel Mode (India): Domestic India call routed to non-roaming India SIM",
                        isTravelOptimized = (pinnedChannel is CallingChannel.CellularSim && pinnedChannel.isRoaming)
                    )
                }
            }

            // Case A3: Calling other international destination while in India
            if (targetCountry != "IN" && fallbackWa != null) {
                return TravelRoutingDecision(
                    recommendedChannel = fallbackWa,
                    reason = "Travel Mode (India): International call routed to WhatsApp VoIP",
                    isTravelOptimized = true
                )
            }
        }

        // ====================================================================
        // RULE SET B: Device is physically in USA ("us")
        // ====================================================================
        if (currentCountry == "us") {
            // Case B1: Calling India (+91) from USA
            if (targetCountry == "IN") {
                if (fallbackWa != null) {
                    return TravelRoutingDecision(
                        recommendedChannel = fallbackWa,
                        reason = "Home Mode (US): Calling India (+91) routed to WhatsApp VoIP to prevent international long-distance rates",
                        isTravelOptimized = true
                    )
                }
            }

            // Case B2: Calling USA (+1) while in USA
            if (targetCountry == "US") {
                val domesticUsSim = findDomesticSim("us", channels)
                if (domesticUsSim != null) {
                    return TravelRoutingDecision(
                        recommendedChannel = pinnedChannel ?: domesticUsSim,
                        reason = "Home Mode (US): Domestic US call routed to domestic US SIM",
                        isTravelOptimized = false
                    )
                }
            }
        }

        // ====================================================================
        // RULE SET C: General Roaming Safeguard
        // ====================================================================
        // If the pinned cellular SIM is actively in roaming mode:
        if (pinnedChannel is CallingChannel.CellularSim && pinnedChannel.isRoaming) {
            // If calling destination country matches local network: switch to local non-roaming SIM
            val localSim = activeSims.firstOrNull { !it.isRoaming }
            if (localSim != null && targetCountry.equals(currentCountry, ignoreCase = true)) {
                return TravelRoutingDecision(
                    recommendedChannel = localSim,
                    reason = "Roaming Safeguard: Switched from roaming SIM to domestic local SIM",
                    isTravelOptimized = true
                )
            }
            // If calling cross-border and WhatsApp is available: protect with WhatsApp VoIP
            if (fallbackWa != null && !targetCountry.equals(currentCountry, ignoreCase = true)) {
                return TravelRoutingDecision(
                    recommendedChannel = fallbackWa,
                    reason = "Roaming Safeguard: Cross-border call on roaming SIM redirected to WhatsApp VoIP",
                    isTravelOptimized = true
                )
            }
        }

        // Fall back to base preference or default
        return TravelRoutingDecision(
            recommendedChannel = pinnedChannel,
            reason = "Standard channel resolution",
            isTravelOptimized = false
        )
    }

    companion object {
        private const val PREFS_NAME = "travel_roaming_prefs"
        private const val KEY_SMART_ROAMING_ENABLED = "smart_roaming_enabled"
        private const val KEY_HOME_COUNTRY_ISO = "home_country_iso"
        private const val KEY_PRIMARY_DOMESTIC_SIM_SLOT = "primary_domestic_sim_slot"
        private const val KEY_US_CALLS_WA_BIZ = "us_calls_wa_biz"
        private const val KEY_INDIA_CALLS_WA_PERSONAL = "india_calls_wa_personal"
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
