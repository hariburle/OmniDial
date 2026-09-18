package com.example.telecom

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SimHelperTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testSimInfoModelDataIntegrity() {
        val sim1 = SimInfo(
            slotIndex = 0,
            subscriptionId = 1,
            displayName = "Verizon",
            carrierName = "Verizon Wireless",
            number = "+15551112222",
            isDefault = true,
            isRoaming = false
        )

        val sim2 = SimInfo(
            slotIndex = 1,
            subscriptionId = 2,
            displayName = "T-Mobile (Work)",
            carrierName = "T-Mobile",
            number = "+15553334444",
            isDefault = false,
            isRoaming = true
        )

        assertEquals(0, sim1.slotIndex)
        assertEquals("Verizon", sim1.displayName)
        assertTrue(sim1.isDefault)
        assertFalse(sim1.isRoaming)

        assertEquals(1, sim2.slotIndex)
        assertEquals("T-Mobile", sim2.carrierName)
        assertFalse(sim2.isDefault)
        assertTrue(sim2.isRoaming)
    }

    @Test
    fun testResolveSimSlotReturnsDefaultSlotWhenNoSims() {
        // Without active telephony subscriptions, resolveSimSlot safely defaults to 1
        val resolved = SimHelper.resolveSimSlot(context, accountHandle = null, accountId = "dummy_account")
        assertEquals(1, resolved)
    }

    @Test
    fun testGetPhoneAccountForSimSlotSafeOnEmptyTelecom() {
        // Must not throw or crash when TelecomManager has no accounts registered
        val account = SimHelper.getPhoneAccountForSimSlot(context, 0)
        // Under Robolectric without mock phone accounts, account will be null
        assertTrue(account == null || account.componentName != null)
    }
}
