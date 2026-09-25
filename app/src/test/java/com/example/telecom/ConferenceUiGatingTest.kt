package com.example.telecom

import android.telecom.Call
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the conference control gating matrix. Pure logic — no Android framework needed
 * (Call.STATE_* constants are compile-time inlined).
 *
 * Visibility is driven by the calls OmniDial tracks, not by carrier-advertised capabilities:
 * Merge/Swap appear whenever a native telecom call has a second tracked call, even if the
 * carrier never set CAPABILITY_MERGE_CONFERENCE / CAPABILITY_SWAP_CONFERENCE.
 */
class ConferenceUiGatingTest {

    @Test
    fun `single active native call shows only Add call`() {
        val c = conferenceControls(
            callState = Call.STATE_ACTIVE,
            isConference = false,
            hasNativeCall = true,
            extraCallCount = 0
        )
        assertTrue(c.showAddCall)
        assertFalse(c.showMerge)
        assertFalse(c.showSwap)
        assertFalse(c.showManage)
    }

    @Test
    fun `two tracked native calls show Merge and Swap and hide Add call`() {
        val c = conferenceControls(
            callState = Call.STATE_ACTIVE,
            isConference = false,
            hasNativeCall = true,
            extraCallCount = 1
        )
        assertFalse(c.showAddCall)
        assertTrue(c.showMerge)
        assertTrue(c.showSwap)
        assertFalse(c.showManage)
    }

    @Test
    fun `second call tracked but not native hides Merge and Swap`() {
        // E.g. simulated/emulator call: nothing to merge through telecom.
        val c = conferenceControls(
            callState = Call.STATE_ACTIVE,
            isConference = false,
            hasNativeCall = false,
            extraCallCount = 1
        )
        assertFalse(c.showMerge)
        assertFalse(c.showSwap)
        assertFalse(c.showAddCall)
        assertFalse(c.showManage)
    }

    @Test
    fun `no second call shows no Merge or Swap`() {
        val c = conferenceControls(
            callState = Call.STATE_ACTIVE,
            isConference = false,
            hasNativeCall = true,
            extraCallCount = 0
        )
        assertFalse(c.showMerge)
        assertFalse(c.showSwap)
        assertTrue(c.showAddCall)
    }

    @Test
    fun `merged conference keeps Add call and shows Manage`() {
        val c = conferenceControls(
            callState = Call.STATE_ACTIVE,
            isConference = true,
            hasNativeCall = true,
            extraCallCount = 0
        )
        assertTrue(c.showAddCall)
        assertTrue(c.showManage)
        assertFalse(c.showMerge)
        assertFalse(c.showSwap)
    }

    @Test
    fun `non-native single call still shows Add call`() {
        val c = conferenceControls(
            callState = Call.STATE_ACTIVE,
            isConference = false,
            hasNativeCall = false,
            extraCallCount = 0
        )
        assertTrue(c.showAddCall)
        assertFalse(c.showMerge)
        assertFalse(c.showSwap)
    }

    @Test
    fun `non-active call hides Add call`() {
        for (state in listOf(Call.STATE_DIALING, Call.STATE_RINGING, Call.STATE_DISCONNECTED)) {
            val c = conferenceControls(
                callState = state,
                isConference = false,
                hasNativeCall = true,
                extraCallCount = 0
            )
            assertFalse("Add call must hide in state $state", c.showAddCall)
        }
    }

    @Test
    fun `merged conference with 2 participants hides Add call at 3-way limit`() {
        val c = conferenceControls(
            callState = Call.STATE_ACTIVE,
            isConference = true,
            hasNativeCall = true,
            extraCallCount = 0,
            participantCount = 2
        )
        assertFalse(c.showAddCall)
        assertTrue(c.showManage)
    }
}
