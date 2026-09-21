package com.example.telecom

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat

@Immutable
data class SimInfo(
    val slotIndex: Int,          // 0-based: 0 for SIM 1, 1 for SIM 2
    val subscriptionId: Int,
    val displayName: String,     // e.g. "Spectrum Mobile", "Verizon", "T-Mobile", "Jio"
    val carrierName: String,     // e.g. "Spectrum", "Verizon"
    val number: String? = null,
    val isDefault: Boolean = false,
    val isRoaming: Boolean = false,
    val deviceSimName: String? = null // custom name from device's SIM management (e.g., "US", "IN")
)

object SimHelper {

    /**
     * Reads active SIM cards and carrier/display names directly from Android's SubscriptionManager.
     */
    fun getActiveSimCards(context: Context): List<SimInfo> {
        val simList = mutableListOf<SimInfo>()
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            return emptyList()
        }

        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val activeList: List<SubscriptionInfo>? = subscriptionManager?.activeSubscriptionInfoList

            if (!activeList.isNullOrEmpty()) {
                val defaultSubId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    SubscriptionManager.getDefaultVoiceSubscriptionId()
                } else {
                    -1
                }

                for (info in activeList) {
                    val slot = info.simSlotIndex // 0 for SIM 1, 1 for SIM 2
                    if (slot < 0) continue

                    val display = info.displayName?.toString()?.trim()
                    val carrier = info.carrierName?.toString()?.trim()
                    val customName = try {
                        com.example.data.ChannelConfigRepository.getInstance(context).getCustomNameSync("sim_${slot + 1}")
                    } catch (_: Exception) {
                        null
                    }

                    // Extract device-managed custom name (e.g. "US", "IN") if not generic CARD index
                    val deviceSimName = when {
                        !display.isNullOrBlank() && !display.equals("CARD $slot", ignoreCase = true) && !display.equals("CARD ${slot + 1}", ignoreCase = true) -> display
                        !carrier.isNullOrBlank() -> carrier
                        else -> "SIM ${slot + 1}"
                    }

                    val name = customName?.takeIf { it.isNotBlank() } ?: deviceSimName

                    val isRoaming = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            telephonyManager?.createForSubscriptionId(info.subscriptionId)?.isNetworkRoaming == true
                        } else {
                            telephonyManager?.isNetworkRoaming == true
                        }
                    } catch (_: Exception) {
                        false
                    }

                    val simNumber: String? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        try {
                            subscriptionManager.getPhoneNumber(info.subscriptionId).takeIf { it.isNotBlank() }
                        } catch (_: SecurityException) {
                            @Suppress("DEPRECATION")
                            info.number?.takeIf { it.isNotBlank() }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        info.number?.takeIf { it.isNotBlank() }
                    }

                    simList.add(
                        SimInfo(
                            slotIndex = slot,
                            subscriptionId = info.subscriptionId,
                            displayName = name,
                            carrierName = carrier ?: name,
                            number = simNumber,
                            isDefault = (info.subscriptionId == defaultSubId),
                            isRoaming = isRoaming,
                            deviceSimName = deviceSimName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return simList.sortedBy { it.slotIndex }
    }

    /**
     * Resolves 1-based SIM slot index (1 for SIM 1, 2 for SIM 2) from a PhoneAccountHandle or account ID string.
     */
    fun resolveSimSlot(context: Context, accountHandle: PhoneAccountHandle? = null, accountId: String? = null): Int {
        val targetId = accountHandle?.id ?: accountId ?: return 1
        val simCards = getActiveSimCards(context)
        if (simCards.isEmpty()) return 1
        
        // Match by subscription ID in account ID string
        for (sim in simCards) {
            if (targetId.contains(sim.subscriptionId.toString())) {
                return sim.slotIndex + 1
            }
        }
        
        // Match by TelecomManager account index
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val accounts = telecomManager?.callCapablePhoneAccounts ?: emptyList()
            val index = accounts.indexOfFirst { it.id == targetId }
            if (index != -1 && index < simCards.size) {
                return simCards[index].slotIndex + 1
            }
        } catch (_: Exception) {}

        return 1
    }

    /**
     * Resolves complete SimInfo (including display name, carrier name, and roaming status) for a PhoneAccountHandle or account ID.
     */
    fun resolveSimInfo(context: Context, accountHandle: PhoneAccountHandle? = null, accountId: String? = null): SimInfo? {
        val simCards = getActiveSimCards(context)
        if (simCards.isEmpty()) return null
        val targetId = accountHandle?.id ?: accountId
        if (targetId != null) {
            for (sim in simCards) {
                if (targetId.contains(sim.subscriptionId.toString())) {
                    return sim
                }
            }
        }
        val slot = resolveSimSlot(context, accountHandle, accountId)
        return simCards.firstOrNull { it.slotIndex == (slot - 1) } ?: simCards.firstOrNull()
    }

    /**
     * Resolves the Telecom PhoneAccountHandle associated with a given SIM slot index.
     */
    fun getPhoneAccountForSimSlot(context: Context, slotIndex: Int): PhoneAccountHandle? {
        return try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return null
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return null

            val accounts = telecomManager.callCapablePhoneAccounts
            // Try matching by subscription ID or slot index
            val simCards = getActiveSimCards(context)
            val targetSim = simCards.firstOrNull { it.slotIndex == slotIndex }

            if (targetSim != null) {
                accounts.firstOrNull { it.id.contains(targetSim.subscriptionId.toString()) }
                    ?: accounts.getOrNull(slotIndex)
                    ?: accounts.firstOrNull()
            } else {
                accounts.getOrNull(slotIndex) ?: accounts.firstOrNull()
            }
        } catch (e: Exception) {
            null
        }
    }
}
