package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM caller_rules ORDER BY id DESC")
    fun getAllRules(): Flow<List<CallerRule>>

    @Query("SELECT * FROM caller_rules ORDER BY id DESC")
    suspend fun getAllRulesList(): List<CallerRule>

    @Query("SELECT * FROM caller_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<CallerRule>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: CallerRule): Long

    @Update
    suspend fun updateRule(rule: CallerRule)

    @Delete
    suspend fun deleteRule(rule: CallerRule)

    @Query("DELETE FROM caller_rules")
    suspend fun clearAllRules()

    @Query("SELECT * FROM automation_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentAutomationLogs(): Flow<List<AutomationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutomationLog(log: AutomationLog): Long

    @Query("DELETE FROM automation_logs")
    suspend fun clearAutomationLogs()

    @Query("SELECT * FROM recent_calls ORDER BY timestamp DESC LIMIT 100")
    fun getRecentCalls(): Flow<List<RecentCall>>

    @Query("SELECT * FROM recent_calls WHERE phoneNumber IN (:numbers) OR callerName = :name ORDER BY timestamp DESC LIMIT 100")
    fun getCallHistoryForContact(numbers: List<String>, name: String): Flow<List<RecentCall>>

    @Query("SELECT * FROM recent_calls WHERE phoneNumber IN (:numbers) OR callerName = :name ORDER BY timestamp DESC LIMIT 100")
    suspend fun getCallHistoryForContactList(numbers: List<String>, name: String): List<RecentCall>

    @Query("SELECT * FROM recent_calls WHERE phoneNumber = :phoneNumber OR normalized_number = :normalizedNumber ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRecentCallForNumber(phoneNumber: String, normalizedNumber: String = phoneNumber): RecentCall?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentCall(call: RecentCall): Long

    @Update
    suspend fun updateRecentCall(call: RecentCall)

    @Delete
    suspend fun deleteRecentCall(call: RecentCall)

    @Query("DELETE FROM recent_calls WHERE id = :callId")
    suspend fun deleteRecentCallById(callId: Long)

    @Query("DELETE FROM recent_calls WHERE phoneNumber = :phoneNumber OR normalized_number = :normalizedNumber")
    suspend fun deleteRecentCallsForNumber(phoneNumber: String, normalizedNumber: String = phoneNumber)

    @Query("DELETE FROM recent_calls WHERE (phoneNumber = :phoneNumber OR normalized_number = :normalizedNumber) AND ABS(timestamp - :timestamp) < 60000")
    suspend fun deleteRecentCallByNumberAndTimestamp(phoneNumber: String, normalizedNumber: String, timestamp: Long)

    @Query("SELECT * FROM recent_calls ORDER BY timestamp DESC")
    suspend fun getAllRecentCallsList(): List<RecentCall>

    @Query("UPDATE recent_calls SET isSpam = :isSpam WHERE phoneNumber = :phoneNumber")
    suspend fun updateRecentCallSpamStatus(phoneNumber: String, isSpam: Boolean)

    @Query("SELECT * FROM favorite_contacts ORDER BY sortOrder ASC, id ASC")
    fun getAllFavorites(): Flow<List<FavoriteContact>>

    @Query("SELECT * FROM favorite_contacts ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllFavoritesList(): List<FavoriteContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(contact: FavoriteContact): Long

    @Update
    suspend fun updateFavorite(contact: FavoriteContact)

    @Update
    suspend fun updateFavorites(contacts: List<FavoriteContact>)

    @Delete
    suspend fun deleteFavorite(contact: FavoriteContact)

    @Query("DELETE FROM favorite_contacts")
    suspend fun clearAllFavorites()

    @Query("SELECT * FROM offline_spam_numbers ORDER BY reportCount DESC")
    fun getAllSpamNumbers(): Flow<List<SpamNumber>>

    @Query("SELECT * FROM offline_spam_numbers ORDER BY reportCount DESC")
    suspend fun getAllSpamNumbersList(): List<SpamNumber>

    @Query("SELECT * FROM offline_spam_numbers WHERE phoneNumber = :number OR normalized_number = :normalizedNumber LIMIT 1")
    suspend fun getSpamByNumber(number: String, normalizedNumber: String = number): SpamNumber?

    @Query("SELECT * FROM offline_spam_numbers WHERE normalized_number = :normalizedNumber LIMIT 1")
    suspend fun getSpamByNormalizedNumber(normalizedNumber: String): SpamNumber?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamNumber(spam: SpamNumber): Long

    @Delete
    suspend fun deleteSpamNumber(spam: SpamNumber)

    @Query("DELETE FROM offline_spam_numbers WHERE phoneNumber = :number")
    suspend fun deleteSpamByNumber(number: String)

    @Query("DELETE FROM offline_spam_numbers")
    suspend fun clearAllSpamNumbers()

    @Query("SELECT * FROM ignored_contacts ORDER BY timestamp DESC")
    fun getAllIgnoredContacts(): Flow<List<IgnoredContact>>

    @Query("SELECT * FROM ignored_contacts ORDER BY timestamp DESC")
    suspend fun getAllIgnoredContactsList(): List<IgnoredContact>

    @Query("SELECT * FROM ignored_contacts WHERE phoneNumber = :number LIMIT 1")
    suspend fun getIgnoredContactByNumber(number: String): IgnoredContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIgnoredContact(ignored: IgnoredContact): Long

    @Delete
    suspend fun deleteIgnoredContact(ignored: IgnoredContact)

    @Query("DELETE FROM ignored_contacts WHERE phoneNumber = :number")
    suspend fun deleteIgnoredContactByNumber(number: String)

    @Query("DELETE FROM ignored_contacts")
    suspend fun clearAllIgnoredContacts()

    @Query("SELECT * FROM local_contacts ORDER BY name ASC")
    fun getAllLocalContacts(): Flow<List<LocalContact>>

    @Query("SELECT * FROM local_contacts ORDER BY name ASC")
    suspend fun getAllLocalContactsList(): List<LocalContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocalContact(contact: LocalContact): Long

    @Update
    suspend fun updateLocalContact(contact: LocalContact)

    @Delete
    suspend fun deleteLocalContact(contact: LocalContact)

    @Query("DELETE FROM local_contacts WHERE id = :id")
    suspend fun deleteLocalContactById(id: Long)

    @Query("DELETE FROM local_contacts WHERE phoneNumber = :phoneNumber")
    suspend fun deleteLocalContactByNumber(phoneNumber: String)

    @Query("DELETE FROM local_contacts WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name))")
    suspend fun deleteLocalContactByName(name: String)

    @Query("DELETE FROM local_contacts")
    suspend fun clearAllLocalContacts()

    @Query("SELECT * FROM contact_sim_preferences")
    fun getAllContactSimPreferences(): Flow<List<ContactSimPreference>>

    @Query("SELECT * FROM contact_sim_preferences WHERE normalized_number = :normalizedNumber LIMIT 1")
    suspend fun getContactSimPreference(normalizedNumber: String): ContactSimPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setContactSimPreference(pref: ContactSimPreference)

    @Query("DELETE FROM contact_sim_preferences WHERE normalized_number = :normalizedNumber")
    suspend fun deleteContactSimPreference(normalizedNumber: String)

    @Query("DELETE FROM contact_sim_preferences")
    suspend fun clearAllContactSimPreferences()

    @Query("SELECT * FROM number_channel_preferences")
    fun getAllNumberChannelPreferences(): Flow<List<NumberChannelPreference>>

    @Query("SELECT * FROM number_channel_preferences")
    suspend fun getAllNumberChannelPreferencesList(): List<NumberChannelPreference>

    @Query("SELECT * FROM number_channel_preferences WHERE normalized_number = :normalizedNumber LIMIT 1")
    suspend fun getNumberChannelPreference(normalizedNumber: String): NumberChannelPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setNumberChannelPreference(pref: NumberChannelPreference)

    @Query("DELETE FROM number_channel_preferences WHERE normalized_number = :normalizedNumber")
    suspend fun deleteNumberChannelPreference(normalizedNumber: String)

    @Query("DELETE FROM number_channel_preferences")
    suspend fun clearAllNumberChannelPreferences()

    @Query("SELECT * FROM channel_configurations ORDER BY order_index ASC")
    fun getAllChannelConfigs(): Flow<List<ChannelConfig>>

    @Query("SELECT * FROM channel_configurations ORDER BY order_index ASC")
    suspend fun getAllChannelConfigsList(): List<ChannelConfig>

    @Query("SELECT * FROM channel_configurations WHERE channel_id = :channelId LIMIT 1")
    suspend fun getChannelConfig(channelId: String): ChannelConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChannelConfig(config: ChannelConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChannelConfigs(configs: List<ChannelConfig>)

    @Query("UPDATE channel_configurations SET is_enabled = :isEnabled, updated_timestamp = :timestamp WHERE channel_id = :channelId")
    suspend fun setChannelEnabled(channelId: String, isEnabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE channel_configurations SET custom_name = :customName, updated_timestamp = :timestamp WHERE channel_id = :channelId")
    suspend fun setChannelCustomName(channelId: String, customName: String?, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM channel_configurations WHERE channel_id = :channelId")
    suspend fun deleteChannelConfig(channelId: String)

    @Query("DELETE FROM channel_configurations")
    suspend fun clearAllChannelConfigs()
}
