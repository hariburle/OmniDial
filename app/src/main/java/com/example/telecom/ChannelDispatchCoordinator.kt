package com.example.telecom

import android.content.Context
import com.example.data.ChannelPreferenceRepository
import com.example.domain.model.CallingChannel
import com.example.util.ContactHelper
import com.example.util.PhoneNumberNormalizer

/**
 * Result of a channel dispatch execution.
 */
sealed interface DispatchResult {
    data class Dispatched(val channel: CallingChannel) : DispatchResult
    data class ShowPicker(val channels: List<CallingChannel>) : DispatchResult
    data class Error(val message: String) : DispatchResult
}

/**
 * Coordinates outgoing call dispatching across multi-channel transports:
 * - Cellular SIM 1 & SIM 2 via TelecomManager / Carrier
 * - WhatsApp VoIP via ContactsContract / URL scheme
 * - Future extensible channels (WhatsApp Business, Google Voice)
 */
class ChannelDispatchCoordinator(
    private val context: Context,
    private val discoveryManager: ChannelDiscoveryManager = ChannelDiscoveryManager.getInstance(context),
    private val preferenceRepository: ChannelPreferenceRepository = ChannelPreferenceRepository.getInstance(context)
) {

    /**
     * Resolves the target CallingChannel following the 4-Tier MCCE Pipeline:
     * 1. Explicit user override (e.g., user tapped a pill in KeypadChannelDock)
     * 2. Pinned per-number preference in Room DB
     * 3. Learned call mode bias (e.g. WhatsApp vs Cellular)
     * 4. Default active Cellular SIM or SystemDefault
     */
    suspend fun resolveChannel(
        phoneNumber: String,
        explicitChannel: CallingChannel? = null,
        learnedMode: String? = null
    ): CallingChannel {
        // 0. Safety Guardrail: Emergency numbers ALWAYS route to Domestic Cellular SIM
        if (discoveryManager.isEmergencyNumber(phoneNumber)) {
            val emergencySim = discoveryManager.getEmergencyCellularChannel()
            return emergencySim ?: CallingChannel.SystemDefault
        }

        // 1. Explicit channel selection (unless AskAlways)
        if (explicitChannel != null && explicitChannel !is CallingChannel.AskAlways) {
            return explicitChannel
        }

        // 2. Pinned Per-Number Preference
        val normalized = PhoneNumberNormalizer.toE164(phoneNumber)
        if (normalized.isNotBlank()) {
            val pref = preferenceRepository.getPreferenceForNumber(normalized)
            if (pref != null) {
                if (pref.preferredChannelId.equals("ask", ignoreCase = true)) {
                    return CallingChannel.AskAlways
                }
                val found = discoveryManager.getChannelById(pref.preferredChannelId)
                if (found != null) {
                    return found
                }
            }
        }

        // 3. Learned Call Mode Bias
        if (learnedMode.equals("whatsapp", ignoreCase = true)) {
            val waChannel = discoveryManager.availableChannels.value
                .filterIsInstance<CallingChannel.WhatsApp>()
                .firstOrNull { !it.isBusiness && it.isAvailable }
            if (waChannel != null) return waChannel
        }

        // 4. Default Cellular SIM 1 or System Default
        val activeSims = discoveryManager.availableChannels.value
            .filterIsInstance<CallingChannel.CellularSim>()
        return activeSims.firstOrNull() ?: CallingChannel.SystemDefault
    }

    /**
     * Dispatches a call through the resolved or provided channel.
     */
    fun dispatchCall(
        phoneNumber: String,
        channel: CallingChannel,
        reason: String? = null,
        onCellularCall: (simSlot: Int?, reason: String?) -> Unit,
        onWhatsAppCall: (phoneNumber: String) -> Unit,
        onShowPicker: (channels: List<CallingChannel>) -> Unit
    ): DispatchResult {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isBlank()) {
            return DispatchResult.Error("Phone number cannot be empty")
        }

        return when (channel) {
            is CallingChannel.CellularSim -> {
                onCellularCall(channel.slotIndex + 1, reason)
                DispatchResult.Dispatched(channel)
            }
            is CallingChannel.WhatsApp -> {
                onWhatsAppCall(cleanNumber)
                DispatchResult.Dispatched(channel)
            }
            is CallingChannel.SystemDefault -> {
                onCellularCall(null, reason)
                DispatchResult.Dispatched(channel)
            }
            is CallingChannel.AskAlways -> {
                val available = discoveryManager.availableChannels.value
                onShowPicker(available)
                DispatchResult.ShowPicker(available)
            }
            is CallingChannel.GoogleVoice -> {
                // Future Task 14.2 integration: fallback to cellular or direct package intent
                onCellularCall(null, reason)
                DispatchResult.Dispatched(channel)
            }
        }
    }

    /**
     * Dispatches an SMS or WhatsApp instant message based on the selected channel.
     */
    fun dispatchMessage(
        phoneNumber: String,
        channel: CallingChannel,
        onSms: (phoneNumber: String) -> Unit,
        onWhatsAppMessage: (phoneNumber: String) -> Unit
    ): DispatchResult {
        val clean = phoneNumber.trim()
        if (clean.isBlank()) return DispatchResult.Error("Phone number cannot be empty")
        return when (channel) {
            is CallingChannel.WhatsApp -> {
                onWhatsAppMessage(clean)
                DispatchResult.Dispatched(channel)
            }
            else -> {
                onSms(clean)
                DispatchResult.Dispatched(channel)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ChannelDispatchCoordinator? = null

        fun getInstance(context: Context): ChannelDispatchCoordinator {
            return INSTANCE ?: synchronized(this) {
                val instance = ChannelDispatchCoordinator(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
