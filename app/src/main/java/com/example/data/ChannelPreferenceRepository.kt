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
            }
        }
    }

    fun getCachedPreference(phoneNumber: String): String? {
        val normalized = PhoneNumberNormalizer.toE164(phoneNumber)
        return cachedPreferences[normalized]
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
                    val db = AppDatabase.getInstance(context)
                    val repo = AppRepository(db.appDao())
                    ChannelPreferenceRepository(repo).also { INSTANCE = it }
                }
            }
        }
    }
}
