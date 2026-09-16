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
}
