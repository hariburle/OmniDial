package com.example.domain.usecase

import android.content.Context
import com.example.data.AppRepository
import com.example.util.CommunityCallerIdService
import com.example.util.CommunityCallerInfo
import com.example.util.ContactHelper
import com.example.util.DeviceContact

data class CallerIdentity(
    val phoneNumber: String,
    val displayName: String,
    val photoUri: String?,
    val nickname: String?,
    val label: String?,
    val isVoicemail: Boolean,
    val isSpam: Boolean,
    val spamScore: Int,
    val communityInfo: CommunityCallerInfo?
)

/**
 * Resolves caller identity with prioritized local cache, address book, and offline spam detection.
 */
class ResolveCallerIdentityUseCase(
    private val repository: AppRepository
) {
    suspend operator fun invoke(context: Context, phoneNumber: String): CallerIdentity {
        val isVoicemail = ContactHelper.isVoicemailNumber(context, phoneNumber)
        if (isVoicemail) {
            return CallerIdentity(
                phoneNumber = phoneNumber,
                displayName = "Voicemail",
                photoUri = null,
                nickname = null,
                label = "Voicemail",
                isVoicemail = true,
                isSpam = false,
                spamScore = 0,
                communityInfo = null
            )
        }

        // 1. Check Address Book
        val deviceContact: DeviceContact? = ContactHelper.lookupContactByNumber(context, phoneNumber)
        if (deviceContact != null) {
            return CallerIdentity(
                phoneNumber = phoneNumber,
                displayName = deviceContact.name,
                photoUri = deviceContact.photoUri,
                nickname = deviceContact.nickname?.ifBlank { null },
                label = deviceContact.label?.ifBlank { null } ?: "Mobile",
                isVoicemail = false,
                isSpam = false,
                spamScore = 0,
                communityInfo = null
            )
        }

        // 2. Check Local Favorites
        val favorites = try { repository.getAllFavoritesList() } catch (_: Exception) { emptyList() }
        val fav = favorites.firstOrNull { ContactHelper.isSamePhoneNumber(it.phoneNumber, phoneNumber) }
        if (fav != null) {
            return CallerIdentity(
                phoneNumber = phoneNumber,
                displayName = fav.name,
                photoUri = fav.photoUri,
                nickname = fav.nickname?.ifBlank { null },
                label = fav.label.ifBlank { "Mobile" },
                isVoicemail = false,
                isSpam = false,
                spamScore = 0,
                communityInfo = null
            )
        }

        // 3. Check Community / Offline Spam Directory
        val community = CommunityCallerIdService.lookup(phoneNumber)
        val isSpam = community?.verificationType?.contains("Spam", ignoreCase = true) == true
        val spamScore = community?.spamScore ?: 0
        val displayName = community?.name ?: phoneNumber

        return CallerIdentity(
            phoneNumber = phoneNumber,
            displayName = displayName,
            photoUri = null,
            nickname = null,
            label = if (isSpam) "Spam / Robocall" else "Unknown",
            isVoicemail = false,
            isSpam = isSpam,
            spamScore = spamScore,
            communityInfo = community
        )
    }
}
