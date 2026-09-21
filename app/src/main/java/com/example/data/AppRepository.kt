package com.example.data

import kotlinx.coroutines.flow.Flow

class AppRepository(private val appDao: AppDao) {
    val allRules: Flow<List<CallerRule>> = appDao.getAllRules()
    val recentLogs: Flow<List<AutomationLog>> = appDao.getRecentAutomationLogs()
    val recentCalls: Flow<List<RecentCall>> = appDao.getRecentCalls()
    val favorites: Flow<List<FavoriteContact>> = appDao.getAllFavorites()
    val spamNumbers: Flow<List<SpamNumber>> = appDao.getAllSpamNumbers()

    suspend fun getEnabledRules(): List<CallerRule> = appDao.getEnabledRules()

    suspend fun insertRule(rule: CallerRule): Long = appDao.insertRule(rule)
    suspend fun clearAllRules() = appDao.clearAllRules()

    suspend fun updateRule(rule: CallerRule) = appDao.updateRule(rule)

    suspend fun deleteRule(rule: CallerRule) = appDao.deleteRule(rule)

    suspend fun insertAutomationLog(log: AutomationLog): Long = appDao.insertAutomationLog(log)

    suspend fun clearAutomationLogs() = appDao.clearAutomationLogs()

    suspend fun insertRecentCall(call: RecentCall): Long = appDao.insertRecentCall(call)

    suspend fun deleteRecentCall(call: RecentCall) = appDao.deleteRecentCall(call)

    suspend fun deleteRecentCallById(callId: Long) = appDao.deleteRecentCallById(callId)

    suspend fun deleteRecentCallsForNumber(phoneNumber: String) =
        appDao.deleteRecentCallsForNumber(phoneNumber, com.example.util.PhoneNumberNormalizer.toE164(phoneNumber))

    suspend fun deleteRecentCallByNumberAndTimestamp(phoneNumber: String, timestamp: Long) =
        appDao.deleteRecentCallByNumberAndTimestamp(phoneNumber, com.example.util.PhoneNumberNormalizer.toE164(phoneNumber), timestamp)

    fun getCallHistoryForContact(numbers: List<String>, name: String): Flow<List<RecentCall>> =
        appDao.getCallHistoryForContact(numbers, name)

    suspend fun getCallHistoryForContactList(numbers: List<String>, name: String): List<RecentCall> =
        appDao.getCallHistoryForContactList(numbers, name)

    suspend fun getLatestRecentCallForNumber(phoneNumber: String): RecentCall? =
        appDao.getLatestRecentCallForNumber(phoneNumber, com.example.util.PhoneNumberNormalizer.toE164(phoneNumber))

    suspend fun updateRecentCall(call: RecentCall) = appDao.updateRecentCall(call)
    suspend fun getAllRecentCallsList(): List<RecentCall> = appDao.getAllRecentCallsList()
    suspend fun updateRecentCallSpamStatus(phoneNumber: String, isSpam: Boolean) = appDao.updateRecentCallSpamStatus(phoneNumber, isSpam)

    suspend fun getAllFavoritesList(): List<FavoriteContact> = appDao.getAllFavoritesList()

    suspend fun insertFavorite(contact: FavoriteContact): Long = appDao.insertFavorite(contact)
    suspend fun clearAllFavorites() = appDao.clearAllFavorites()

    suspend fun updateFavorite(contact: FavoriteContact) = appDao.updateFavorite(contact)

    suspend fun updateFavorites(contacts: List<FavoriteContact>) = appDao.updateFavorites(contacts)

    suspend fun deleteFavorite(contact: FavoriteContact) = appDao.deleteFavorite(contact)

    suspend fun getSpamByNumber(number: String): SpamNumber? =
        appDao.getSpamByNumber(number, com.example.util.PhoneNumberNormalizer.toE164(number))
    suspend fun getAllSpamNumbersList(): List<SpamNumber> = appDao.getAllSpamNumbersList()

    suspend fun insertSpamNumber(spam: SpamNumber): Long = appDao.insertSpamNumber(spam)
    suspend fun clearAllSpamNumbers() = appDao.clearAllSpamNumbers()

    suspend fun deleteSpamNumber(spam: SpamNumber) = appDao.deleteSpamNumber(spam)

    suspend fun deleteSpamByNumber(number: String) = appDao.deleteSpamByNumber(number)

    suspend fun getIgnoredContactByNumber(number: String): IgnoredContact? = appDao.getIgnoredContactByNumber(number)

    val ignoredContacts: Flow<List<IgnoredContact>> = appDao.getAllIgnoredContacts()

    suspend fun insertIgnoredContact(ignored: IgnoredContact) = appDao.insertIgnoredContact(ignored)
    suspend fun clearAllIgnoredContacts() = appDao.clearAllIgnoredContacts()

    suspend fun deleteIgnoredContact(ignored: IgnoredContact) = appDao.deleteIgnoredContact(ignored)

    suspend fun deleteIgnoredContactByNumber(number: String) = appDao.deleteIgnoredContactByNumber(number)

    val localContacts: Flow<List<LocalContact>> = appDao.getAllLocalContacts()
    suspend fun getAllLocalContactsList(): List<LocalContact> = appDao.getAllLocalContactsList()
    suspend fun insertLocalContact(contact: LocalContact): Long = appDao.insertLocalContact(contact)
    suspend fun updateLocalContact(contact: LocalContact) = appDao.updateLocalContact(contact)
    suspend fun deleteLocalContact(contact: LocalContact) = appDao.deleteLocalContact(contact)
    suspend fun deleteLocalContactById(id: Long) = appDao.deleteLocalContactById(id)
    suspend fun deleteLocalContactByNumber(phoneNumber: String) = appDao.deleteLocalContactByNumber(phoneNumber)
    suspend fun deleteLocalContactByName(name: String) = appDao.deleteLocalContactByName(name)
    suspend fun clearAllLocalContacts() = appDao.clearAllLocalContacts()

    val allContactSimPreferences: Flow<List<ContactSimPreference>> = appDao.getAllContactSimPreferences()
    suspend fun getContactSimPreference(phoneNumber: String): ContactSimPreference? =
        appDao.getContactSimPreference(com.example.util.PhoneNumberNormalizer.toE164(phoneNumber))
    suspend fun setContactSimPreference(phoneNumber: String, simSlot: Int) =
        appDao.setContactSimPreference(ContactSimPreference(com.example.util.PhoneNumberNormalizer.toE164(phoneNumber), simSlot))
    suspend fun deleteContactSimPreference(phoneNumber: String) =
        appDao.deleteContactSimPreference(com.example.util.PhoneNumberNormalizer.toE164(phoneNumber))
    suspend fun clearAllContactSimPreferences() = appDao.clearAllContactSimPreferences()

    val allNumberChannelPreferences: Flow<List<NumberChannelPreference>> = appDao.getAllNumberChannelPreferences()
    suspend fun getNumberChannelPreference(phoneNumber: String): NumberChannelPreference? =
        appDao.getNumberChannelPreference(com.example.util.PhoneNumberNormalizer.toE164(phoneNumber))
    suspend fun setNumberChannelPreference(phoneNumber: String, channelId: String, customLabel: String? = null) =
        appDao.setNumberChannelPreference(
            NumberChannelPreference(
                normalizedNumber = com.example.util.PhoneNumberNormalizer.toE164(phoneNumber),
                preferredChannelId = channelId,
                customLabel = customLabel
            )
        )
    suspend fun deleteNumberChannelPreference(phoneNumber: String) =
        appDao.deleteNumberChannelPreference(com.example.util.PhoneNumberNormalizer.toE164(phoneNumber))
    suspend fun clearAllNumberChannelPreferences() = appDao.clearAllNumberChannelPreferences()

    val allChannelConfigs: Flow<List<ChannelConfig>> = appDao.getAllChannelConfigs()
    suspend fun getAllChannelConfigsList(): List<ChannelConfig> = appDao.getAllChannelConfigsList()
    suspend fun getChannelConfig(channelId: String): ChannelConfig? = appDao.getChannelConfig(channelId)
    suspend fun setChannelConfig(config: ChannelConfig) = appDao.insertOrUpdateChannelConfig(config)
    suspend fun setChannelConfigs(configs: List<ChannelConfig>) = appDao.insertOrUpdateChannelConfigs(configs)
    suspend fun setChannelEnabled(channelId: String, isEnabled: Boolean) = appDao.setChannelEnabled(channelId, isEnabled)
    suspend fun setChannelCustomName(channelId: String, customName: String?) = appDao.setChannelCustomName(channelId, customName)
    suspend fun deleteChannelConfig(channelId: String) = appDao.deleteChannelConfig(channelId)
    suspend fun clearAllChannelConfigs() = appDao.clearAllChannelConfigs()

    val allDefaultNumbers: Flow<List<ContactDefaultNumber>> = appDao.getAllDefaultNumbers()
    suspend fun getAllDefaultNumbersList(): List<ContactDefaultNumber> = appDao.getAllDefaultNumbersList()

    suspend fun getDefaultNumberForContact(contactId: Long?, phoneNumbers: List<String>): ContactDefaultNumber? {
        if (contactId != null && contactId > 0L) {
            val byId = appDao.getDefaultNumberByContactId(contactId)
            if (byId != null) return byId
        }
        for (num in phoneNumbers) {
            val norm = com.example.util.PhoneNumberNormalizer.toE164(num)
            if (norm.isNotBlank()) {
                val byNum = appDao.getDefaultNumber(norm)
                if (byNum != null) return byNum
            }
        }
        return null
    }

    suspend fun setDefaultNumberForContact(
        contactId: Long?,
        phoneNumbers: List<String>,
        chosenNumber: String,
        chosenLabel: String = "Mobile"
    ) {
        val entries = mutableListOf<ContactDefaultNumber>()
        val normChosen = com.example.util.PhoneNumberNormalizer.toE164(chosenNumber)
        if (normChosen.isNotBlank()) {
            entries.add(
                ContactDefaultNumber(
                    normalizedNumber = normChosen,
                    contactId = contactId,
                    defaultNumber = chosenNumber,
                    defaultLabel = chosenLabel
                )
            )
        }
        for (num in phoneNumbers) {
            val norm = com.example.util.PhoneNumberNormalizer.toE164(num)
            if (norm.isNotBlank() && entries.none { it.normalizedNumber == norm }) {
                entries.add(
                    ContactDefaultNumber(
                        normalizedNumber = norm,
                        contactId = contactId,
                        defaultNumber = chosenNumber,
                        defaultLabel = chosenLabel
                    )
                )
            }
        }
        if (entries.isNotEmpty()) {
            appDao.insertDefaultNumbers(entries)
        }
    }

    suspend fun clearAllDefaultNumbers() = appDao.clearAllDefaultNumbers()
}

