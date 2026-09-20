package com.example.data

import android.content.Context
import com.example.domain.model.CallingChannel
import com.example.util.PhoneNumberNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ChannelPreferenceRepository(
    private val appRepository: AppRepository
) {
    val allPreferences: Flow<List<NumberChannelPreference>> =
        appRepository.allNumberChannelPreferences

    private val cachedPreferences = java.util.concurrent.ConcurrentHashMap<String, String>()

    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            allPreferences.collect { list ->
                cachedPreferences.clear()
                list.forEach { pref ->
                    cachedPreferences[pref.normalizedNumber] = pref.preferredChannelId
                }
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
        return getPreferenceForNumber(phoneNumber)?.preferredChannelId
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
                val db = AppDatabase.getInstance(context)
                val repo = AppRepository(db.appDao())
                val instance = ChannelPreferenceRepository(repo)
                INSTANCE = instance
                instance
            }
        }
    }
}
