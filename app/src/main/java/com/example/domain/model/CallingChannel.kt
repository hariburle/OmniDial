package com.example.domain.model

import android.telecom.PhoneAccountHandle
import androidx.compose.runtime.Immutable

enum class ChannelCategory {
    CELLULAR,
    MESSAGING_VOIP,
    CLOUD_VOIP
}

@Immutable
sealed interface CallingChannel {
    val id: String
    val displayName: String
    val shortLabel: String
    val brandColorHex: Long
    val isAvailable: Boolean
    val category: ChannelCategory

    fun withCustomName(customName: String?): CallingChannel {
        return when (this) {
            is CellularSim -> copy(customName = customName)
            is WhatsApp -> copy(customName = customName)
            is GoogleVoice -> copy(customName = customName)
            is SystemDefault -> this
            is AskAlways -> this
        }
    }

    @Immutable
    data class CellularSim(
        val slotIndex: Int,          // 0 = SIM 1, 1 = SIM 2
        val subscriptionId: Int,
        val carrierName: String,
        val isRoaming: Boolean,
        val customName: String? = null,
        val deviceSimName: String? = null,
        override val isAvailable: Boolean = true
    ) : CallingChannel {
        override val id: String = if (slotIndex == 0) "sim_1" else "sim_2"

        private val baseLabel: String
            get() = customName?.takeIf { it.isNotBlank() }
                ?: deviceSimName?.takeIf { it.isNotBlank() }
                ?: "SIM ${slotIndex + 1}"

        override val displayName: String
            get() {
                val base = baseLabel
                val carrier = carrierName.trim()
                return when {
                    carrier.isBlank() || base.equals(carrier, ignoreCase = true) -> base
                    base.contains(carrier, ignoreCase = true) -> base
                    else -> "$base ($carrier)"
                }
            }

        override val shortLabel: String
            get() = baseLabel

        override val brandColorHex: Long = if (slotIndex == 0) 0xFF3B82F6 else 0xFF8B5CF6 // Blue for SIM 1, Purple for SIM 2
        override val category: ChannelCategory = ChannelCategory.CELLULAR
    }

    @Immutable
    data class WhatsApp(
        val isBusiness: Boolean = false,
        val packageName: String = if (isBusiness) "com.whatsapp.w4b" else "com.whatsapp",
        val customName: String? = null,
        override val isAvailable: Boolean = true
    ) : CallingChannel {
        override val id: String = if (isBusiness) "whatsapp_business" else "whatsapp"
        override val displayName: String = customName?.takeIf { it.isNotBlank() } ?: (if (isBusiness) "WhatsApp Business" else "WhatsApp")
        override val shortLabel: String = customName?.takeIf { it.isNotBlank() } ?: (if (isBusiness) "WA Biz" else "WhatsApp")
        override val brandColorHex: Long = if (isBusiness) 0xFF128C7E else 0xFF25D366 // Teal for WA Biz, Bright Green for WA
        override val category: ChannelCategory = ChannelCategory.MESSAGING_VOIP
    }

    @Immutable
    data class GoogleVoice(
        val packageName: String = "com.google.android.apps.googlevoice",
        val phoneAccountHandle: PhoneAccountHandle? = null,
        val customName: String? = null,
        override val isAvailable: Boolean = true
    ) : CallingChannel {
        override val id: String = "google_voice"
        override val displayName: String = customName?.takeIf { it.isNotBlank() } ?: "Google Voice"
        override val shortLabel: String = customName?.takeIf { it.isNotBlank() } ?: "G Voice"
        override val brandColorHex: Long = 0xFF0F9D58 // Google Green
        override val category: ChannelCategory = ChannelCategory.CLOUD_VOIP
    }

    @Immutable
    object SystemDefault : CallingChannel {
        override val id: String = "system"
        override val displayName: String = "System Default"
        override val shortLabel: String = "Default"
        override val brandColorHex: Long = 0xFF64748B // Slate
        override val isAvailable: Boolean = true
        override val category: ChannelCategory = ChannelCategory.CELLULAR
    }

    @Immutable
    object AskAlways : CallingChannel {
        override val id: String = "ask"
        override val displayName: String = "Ask Every Time"
        override val shortLabel: String = "Ask"
        override val brandColorHex: Long = 0xFFF59E0B // Amber
        override val isAvailable: Boolean = true
        override val category: ChannelCategory = ChannelCategory.CELLULAR
    }
}
