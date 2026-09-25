package com.example.telecom

import android.telecom.Call

/**
 * Which conference controls the in-call screen shows. Visibility is driven by the calls
 * OmniDial itself is tracking — not by carrier-advertised capabilities, which many
 * carriers (e.g. MVNOs) never set. If the carrier rejects a merge/swap at tap time,
 * the UI shows a failure toast instead.
 */
data class ConferenceControls(
    val showAddCall: Boolean,
    val showMerge: Boolean,
    val showSwap: Boolean,
    val showManage: Boolean
)

/**
 * Pure gating logic for the in-call conference buttons (unit-testable).
 *
 * - Add call: shown on a live (active) call when merge isn't the pending action. During a
 *   merged conference it stays available per the approved UX; with two unmerged calls already
 *   tracked it hides because a third call can't be placed.
 * - Merge: shown when this is a native telecom (cellular) call and a second call is tracked.
 * - Swap: shown when this is a native telecom call and a second call is tracked to swap with.
 * - Manage: shown for a merged conference call.
 */
fun conferenceControls(
    callState: Int,
    isConference: Boolean,
    hasNativeCall: Boolean,
    extraCallCount: Int,
    participantCount: Int = 0
): ConferenceControls {
    val showMerge = hasNativeCall && extraCallCount > 0
    // Cellular 3-way calling limit: 3 parties total (Host + 2 remote participants).
    // Once 2 participants are merged in the conference, hide Add call so user cannot exceed carrier limit.
    val maxParticipantsReached = isConference && participantCount >= 2
    val showAddCall = callState == Call.STATE_ACTIVE && !showMerge && !maxParticipantsReached &&
        (isConference || extraCallCount == 0)
    return ConferenceControls(
        showAddCall = showAddCall,
        showMerge = showMerge,
        showSwap = hasNativeCall && extraCallCount > 0,
        showManage = isConference
    )
}
