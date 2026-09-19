package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelConfigRepository(
    private val appRepository: AppRepository,
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val allConfigs: Flow<List<ChannelConfig>> = appRepository.allChannelConfigs

    private val clockCounter = java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())
    private fun nextTimestamp(): Long = clockCounter.updateAndGet { maxOf(System.currentTimeMillis(), it + 1) }

    init {
        scope.launch {
            allConfigs.collect { configs ->
                syncCustomNamesToPrefs(configs)
            }
        }
    }

    private fun syncCustomNamesToPrefs(configs: List<ChannelConfig>) {
        val editor = prefs.edit()
        for (cfg in configs) {
            val tsKey = "channel_custom_name_ts_${cfg.channelId.lowercase()}"
            val currentTs = prefs.getLong(tsKey, 0L)
            if (cfg.updatedTimestamp >= currentTs) {
                val name = cfg.customName?.trim()
                val key = "channel_custom_name_${cfg.channelId.lowercase()}"
                if (!name.isNullOrBlank()) {
                    editor.putString(key, name)
                } else {
                    editor.remove(key)
                }
                editor.putLong(tsKey, cfg.updatedTimestamp)
            }
        }
        editor.commit()
    }

    suspend fun getAllConfigs(): List<ChannelConfig> = appRepository.getAllChannelConfigsList()

    suspend fun getConfig(channelId: String): ChannelConfig? = appRepository.getChannelConfig(channelId)

    suspend fun saveConfigs(configs: List<ChannelConfig>) {
        val now = nextTimestamp()
        val configsWithTs = configs.map { it.copy(updatedTimestamp = nextTimestamp()) }
        val editor = prefs.edit()
        for (cfg in configsWithTs) {
            val name = cfg.customName?.trim()
            val key = "channel_custom_name_${cfg.channelId.lowercase()}"
            val tsKey = "channel_custom_name_ts_${cfg.channelId.lowercase()}"
            if (!name.isNullOrBlank()) {
                editor.putString(key, name)
            } else {
                editor.remove(key)
            }
            editor.putLong(tsKey, cfg.updatedTimestamp)
        }
        editor.commit()
        appRepository.setChannelConfigs(configsWithTs)
    }

    suspend fun saveConfig(config: ChannelConfig) {
        val configWithTs = config.copy(updatedTimestamp = nextTimestamp())
        val name = configWithTs.customName?.trim()
        val key = "channel_custom_name_${configWithTs.channelId.lowercase()}"
        val tsKey = "channel_custom_name_ts_${configWithTs.channelId.lowercase()}"
        val editor = prefs.edit()
        if (!name.isNullOrBlank()) {
            editor.putString(key, name)
        } else {
            editor.remove(key)
        }
        editor.putLong(tsKey, configWithTs.updatedTimestamp)
        editor.commit()
        appRepository.setChannelConfig(configWithTs)
    }

    suspend fun setChannelEnabled(channelId: String, isEnabled: Boolean) =
        appRepository.setChannelEnabled(channelId, isEnabled)

    suspend fun renameChannel(channelId: String, customName: String?) {
        val name = customName?.trim()?.ifBlank { null }
        val now = nextTimestamp()
        val key = "channel_custom_name_${channelId.lowercase()}"
        val tsKey = "channel_custom_name_ts_${channelId.lowercase()}"
        val editor = prefs.edit()
        if (!name.isNullOrBlank()) {
            editor.putString(key, name)
        } else {
            editor.remove(key)
        }
        editor.putLong(tsKey, now)
        editor.commit()
        appRepository.setChannelCustomName(channelId, name)
    }

    fun getCustomNameSync(channelId: String): String? =
        prefs.getString("channel_custom_name_${channelId.lowercase()}", null)?.takeIf { it.isNotBlank() }

    fun hasCompletedOnboarding(): Boolean =
        prefs.getBoolean(KEY_HAS_COMPLETED_CHANNEL_ONBOARDING, false)

    fun setCompletedOnboarding(completed: Boolean = true) {
        prefs.edit().putBoolean(KEY_HAS_COMPLETED_CHANNEL_ONBOARDING, completed).commit()
    }

    suspend fun clearAllConfigs() {
        val editor = prefs.edit()
        val keysToRemove = prefs.all.keys.filter { it.startsWith("channel_custom_name_") }
        for (k in keysToRemove) {
            editor.remove(k)
        }
        editor.commit()
        appRepository.clearAllChannelConfigs()
    }

    companion object {
        private const val PREFS_NAME = "kishan_dialer_prefs"
        const val KEY_HAS_COMPLETED_CHANNEL_ONBOARDING = "has_completed_channel_onboarding"

        @Volatile
        private var INSTANCE: ChannelConfigRepository? = null

        fun getInstance(context: Context): ChannelConfigRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val repo = AppRepository(db.appDao())
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val instance = ChannelConfigRepository(repo, prefs)
                INSTANCE = instance
                instance
            }
        }
    }
}
