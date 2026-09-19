package com.example.telecom

import android.content.Context
import com.example.data.ChannelConfig
import com.example.data.ChannelConfigRepository
import com.example.domain.model.CallingChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChannelDiscoveryManager(
    private val context: Context,
    private val configRepository: ChannelConfigRepository = ChannelConfigRepository.getInstance(context),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val _allDiscoveredChannels = MutableStateFlow<List<CallingChannel>>(emptyList())
    val allDiscoveredChannels: StateFlow<List<CallingChannel>> = _allDiscoveredChannels.asStateFlow()

    private val _availableChannels = MutableStateFlow<List<CallingChannel>>(listOf(CallingChannel.SystemDefault))
    val availableChannels: StateFlow<List<CallingChannel>> = _availableChannels.asStateFlow()

    init {
        refreshChannels()
        scope.launch {
            configRepository.allConfigs.collect { configs ->
                applyConfigs(configs)
            }
        }
    }

    fun refreshChannels() {
        scope.launch {
            val discovered = mutableListOf<CallingChannel>()

            // 1. Discover Active Cellular SIMs
            val activeSims = SimHelper.getActiveSimCards(context)
            if (activeSims.isNotEmpty()) {
                activeSims.forEach { sim ->
                    discovered.add(
                        CallingChannel.CellularSim(
                            slotIndex = sim.slotIndex,
                            subscriptionId = sim.subscriptionId,
                            carrierName = sim.carrierName.ifBlank { sim.displayName },
                            isRoaming = sim.isRoaming,
                            isAvailable = true
                        )
                    )
                }
            } else {
                // Fallback to SystemDefault if SIM discovery returns empty
                discovered.add(CallingChannel.SystemDefault)
            }

            // 2. Discover WhatsApp (Personal)
            val isWhatsAppInstalled = isPackageInstalled("com.whatsapp")
            if (isWhatsAppInstalled) {
                discovered.add(
                    CallingChannel.WhatsApp(
                        isBusiness = false,
                        packageName = "com.whatsapp",
                        isAvailable = true
                    )
                )
            }

            // 3. Discover WhatsApp Business (if installed)
            val isWhatsAppBizInstalled = isPackageInstalled("com.whatsapp.w4b")
            if (isWhatsAppBizInstalled) {
                discovered.add(
                    CallingChannel.WhatsApp(
                        isBusiness = true,
                        packageName = "com.whatsapp.w4b",
                        isAvailable = true
                    )
                )
            }

            // 4. Discover Google Voice (if installed)
            val isGoogleVoiceInstalled = isPackageInstalled("com.google.android.apps.googlevoice")
            if (isGoogleVoiceInstalled) {
                discovered.add(
                    CallingChannel.GoogleVoice(
                        packageName = "com.google.android.apps.googlevoice",
                        isAvailable = true
                    )
                )
            }

            _allDiscoveredChannels.value = discovered

            val currentConfigs = configRepository.getAllConfigs()
            applyConfigs(currentConfigs)
        }
    }

    private fun applyConfigs(configs: List<ChannelConfig>) {
        val discovered = _allDiscoveredChannels.value
        if (discovered.isEmpty()) return

        if (!configRepository.hasCompletedOnboarding() || configs.isEmpty()) {
            _availableChannels.value = discovered
            return
        }

        val configMap = configs.associateBy { it.channelId }
        val updatedDiscovered = discovered.map { channel ->
            val custom = configMap[channel.id]?.customName ?: configRepository.getCustomNameSync(channel.id)
            if (custom != null) channel.withCustomName(custom) else channel
        }
        _allDiscoveredChannels.value = updatedDiscovered

        val filtered = updatedDiscovered.mapNotNull { channel ->
            val cfg = configMap[channel.id]
            if (cfg != null) {
                if (cfg.isEnabled) {
                    channel.withCustomName(cfg.customName)
                } else {
                    null
                }
            } else {
                channel
            }
        }

        _availableChannels.value = if (filtered.isNotEmpty()) {
            filtered
        } else {
            // Guard: Always keep at least the default cellular channel
            updatedDiscovered.filterIsInstance<CallingChannel.CellularSim>().take(1).ifEmpty {
                listOf(CallingChannel.SystemDefault)
            }
        }
    }

    fun getChannelById(channelId: String): CallingChannel? {
        // First check in currently active/configured channels
        _availableChannels.value.firstOrNull { it.id.equals(channelId, ignoreCase = true) }?.let {
            return it
        }
        // Then check all discovered channels
        _allDiscoveredChannels.value.firstOrNull { it.id.equals(channelId, ignoreCase = true) }?.let {
            return it
        }
        // Fallback checks for fixed IDs with custom name applied
        val custom = configRepository.getCustomNameSync(channelId)
        val fallback = when (channelId.lowercase()) {
            "system" -> CallingChannel.SystemDefault
            "ask" -> CallingChannel.AskAlways
            "sim_1" -> CallingChannel.CellularSim(slotIndex = 0, subscriptionId = 1, carrierName = "SIM 1", isRoaming = false)
            "sim_2" -> CallingChannel.CellularSim(slotIndex = 1, subscriptionId = 2, carrierName = "SIM 2", isRoaming = false)
            "whatsapp" -> CallingChannel.WhatsApp(isBusiness = false, isAvailable = true)
            "whatsapp_business" -> CallingChannel.WhatsApp(isBusiness = true, isAvailable = true)
            "google_voice" -> CallingChannel.GoogleVoice(isAvailable = true)
            else -> null
        }
        return if (custom != null) fallback?.withCustomName(custom) else fallback
    }

    fun isEmergencyNumber(number: String): Boolean {
        val clean = number.trim().filter { it.isDigit() || it == '+' }
        if (clean.isBlank()) return false
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && telephonyManager != null) {
            try {
                if (telephonyManager.isEmergencyNumber(clean)) return true
            } catch (_: Throwable) {}
        }
        return try {
            android.telephony.PhoneNumberUtils.isEmergencyNumber(clean)
        } catch (_: Throwable) {
            clean in setOf("911", "112", "999", "000", "100", "101", "102", "108")
        }
    }

    fun getEmergencyCellularChannel(): CallingChannel.CellularSim? {
        val sims = _allDiscoveredChannels.value.filterIsInstance<CallingChannel.CellularSim>()
        // Prioritize active domestic/non-roaming SIM, then any available cellular SIM
        return sims.firstOrNull { !it.isRoaming } ?: sims.firstOrNull()
    }

    fun isInternationalNumber(number: String): Boolean {
        val clean = number.trim()
        return clean.startsWith("+") || clean.startsWith("011") || clean.startsWith("00")
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ChannelDiscoveryManager? = null

        fun getInstance(context: Context): ChannelDiscoveryManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ChannelDiscoveryManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
