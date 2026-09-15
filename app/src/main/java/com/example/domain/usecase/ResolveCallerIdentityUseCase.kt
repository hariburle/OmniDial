package com.example.domain.usecase

import android.content.Context
import com.example.data.AppRepository
import com.example.util.CommunityCallerIdService
import com.example.util.CommunityCallerInfo
import com.example.util.ContactHelper
import com.example.util.DeviceContact

enum class TrustTier {
    VERIFIED_BUSINESS,     // Green - Verified businesses, banks, emergency/official services, saved contacts
    PRIORITY_LOGISTICS,    // Amber - Delivery drivers, ride-shares, building intercoms, appointment reminders
    HIGH_RISK_SPAM,        // Red - High report count, fraud/scam warnings, community blacklists
    NEUTRAL_UNKNOWN        // Grey - Standard unknown numbers
}

data class CallerIdentity(
    val phoneNumber: String,
    val displayName: String,
    val photoUri: String?,
    val nickname: String?,
    val label: String?,
    val isVoicemail: Boolean,
    val isSpam: Boolean,
    val spamScore: Int,
    val communityInfo: CommunityCallerInfo?,
    val trustTier: TrustTier = TrustTier.NEUTRAL_UNKNOWN,
    val trustBadgeLabel: String? = null
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
                communityInfo = null,
                trustTier = TrustTier.VERIFIED_BUSINESS,
                trustBadgeLabel = "System Voicemail"
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
                communityInfo = null,
                trustTier = TrustTier.VERIFIED_BUSINESS,
                trustBadgeLabel = "Saved Contact"
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
                communityInfo = null,
                trustTier = TrustTier.VERIFIED_BUSINESS,
                trustBadgeLabel = "Favorite"
            )
        }

        // 3. Check Community / Offline Spam Directory
        val community = CommunityCallerIdService.lookup(phoneNumber)
        val isSpam = community?.verificationType?.contains("Spam", ignoreCase = true) == true || (community?.spamScore ?: 0) >= 50
        val spamScore = community?.spamScore ?: 0
        val displayName = community?.name ?: phoneNumber

        val (tier, badgeLabel) = when {
            isSpam -> Pair(TrustTier.HIGH_RISK_SPAM, "High Spam Risk (${spamScore}%)")
            community != null && (community.category.contains("Delivery", ignoreCase = true) ||
                community.category.contains("Logistics", ignoreCase = true) ||
                community.verificationType.contains("Delivery", ignoreCase = true) ||
                community.name.contains("Delivery", ignoreCase = true) ||
                community.defaultCallReason?.contains("Buzzer", ignoreCase = true) == true) ->
                Pair(TrustTier.PRIORITY_LOGISTICS, "Priority Delivery")
            community != null && (community.isVerified ||
                community.verificationType.contains("Verified", ignoreCase = true) ||
                community.verificationType.contains("Financial", ignoreCase = true)) ->
                Pair(TrustTier.VERIFIED_BUSINESS, community.verificationType)
            community != null -> Pair(TrustTier.NEUTRAL_UNKNOWN, "Community Identified")
            else -> Pair(TrustTier.NEUTRAL_UNKNOWN, null)
        }

        return CallerIdentity(
            phoneNumber = phoneNumber,
            displayName = displayName,
            photoUri = null,
            nickname = null,
            label = if (isSpam) "Spam / Robocall" else "Unknown",
            isVoicemail = false,
            isSpam = isSpam,
            spamScore = spamScore,
            communityInfo = community,
            trustTier = tier,
            trustBadgeLabel = badgeLabel
        )
    }
}
