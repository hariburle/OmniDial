package com.example.ui.components

/**
 * Who actually dropped the call, derived from the markers OmniDial writes
 * when it declines a call itself.
 *
 * The old "Carrier Auto-Dropped" badge was misleading: the word "Carrier"
 * referred to the *signal* (the carrier flagged the call as spam), but
 * OmniDial was the one that declined it. These labels name the actor
 * truthfully and keep the reason as a detail.
 */
enum class DropAttribution {
    NONE,

    /** OmniDial declined it because the carrier flagged the call as spam. */
    BLOCKED_BY_OMNIDIAL_CARRIER_FLAG,

    /** OmniDial declined it because the number is in the spam list. */
    BLOCKED_BY_OMNIDIAL_SPAM_LIST;

    val badgeText: String
        get() = when (this) {
            BLOCKED_BY_OMNIDIAL_CARRIER_FLAG -> "Blocked by OmniDial · Carrier flagged as spam"
            BLOCKED_BY_OMNIDIAL_SPAM_LIST -> "Blocked by OmniDial · Number in spam list"
            NONE -> ""
        }
}

object CallDropAttribution {

    /**
     * Only OmniDial writes the "auto-dropped" marker, in its own decline
     * path (CallManager). A true carrier network-level block leaves no
     * on-device record, so it is never mislabeled as ours.
     */
    fun forCall(
        isSpam: Boolean,
        note: String?,
        ruleMatched: String?,
        callReason: String?
    ): DropAttribution {
        if (!isSpam) return DropAttribution.NONE
        val omniDialDropped = note?.contains("auto-dropped", ignoreCase = true) == true ||
            callReason?.contains("auto-dropped", ignoreCase = true) == true
        if (!omniDialDropped) return DropAttribution.NONE
        val carrierFlagged = ruleMatched?.contains("Carrier Spam Filter", ignoreCase = true) == true ||
            callReason?.contains("Carrier Spam Filter", ignoreCase = true) == true
        return if (carrierFlagged) DropAttribution.BLOCKED_BY_OMNIDIAL_CARRIER_FLAG
        else DropAttribution.BLOCKED_BY_OMNIDIAL_SPAM_LIST
    }
}
