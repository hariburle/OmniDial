package com.example.data

import android.content.Context
import com.example.domain.model.CallingChannel
import com.example.util.PhoneNumberNormalizer
import kotlinx.coroutines.flow.Flow

class ChannelPreferenceRepository(
    private val appRepository: AppRepository
) {
    val allPreferences: Flow<List<NumberChannelPreference>> =
        appRepository.allNumberChannelPreferences

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
        appRepository.deleteNumberChannelPreference(normalized)
    }

    suspend fun clearAllPreferences() {
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
