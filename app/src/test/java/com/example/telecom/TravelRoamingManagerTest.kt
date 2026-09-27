package com.example.telecom

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.CallingChannel
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
class TravelRoamingManagerTest {

    private lateinit var context: Context
    private lateinit var manager: TravelRoamingManager

    private val usSim = CallingChannel.CellularSim(
        slotIndex = 0,
        subscriptionId = 1,
        carrierName = "Spectrum Mobile",
        isRoaming = false,
        deviceSimName = "US",
        countryIso = "us",
        isAvailable = true
    )

    private val usSimRoaming = CallingChannel.CellularSim(
        slotIndex = 0,
        subscriptionId = 1,
        carrierName = "Spectrum Mobile",
        isRoaming = true,
        deviceSimName = "US",
        countryIso = "us",
        isAvailable = true
    )

    private val indiaSim = CallingChannel.CellularSim(
        slotIndex = 1,
        subscriptionId = 2,
        carrierName = "Airtel",
        isRoaming = false,
        deviceSimName = "IN",
        countryIso = "in",
        isAvailable = true
    )

    private val whatsAppChannel = CallingChannel.WhatsApp(
        isBusiness = false,
        isAvailable = true
    )

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        manager = TravelRoamingManager(context)
        manager.setSmartRoamingEnabled(true)
    }

    @Test
    fun testWhenInIndia_callingUsNumber_routesToWhatsApp() {
        val testChannels = listOf(usSimRoaming, indiaSim, whatsAppChannel)

        val testManager = object : TravelRoamingManager(context) {
            override fun getCurrentCountryIso(): String = "in"
        }

        val decision = testManager.evaluateTravelRouting(
            phoneNumber = "+14085551234",
            pinnedChannel = usSimRoaming,
            userExplicitOverride = null,
            availableChannels = testChannels
        )

        assertTrue("Should be travel optimized", decision.isTravelOptimized)
        assertTrue("Recommended channel should be WhatsApp", decision.recommendedChannel is CallingChannel.WhatsApp)
    }

    @Test
    fun testWhenRoaming_callingDomesticHostNumber_routesToDomesticLocalSim() {
        val testChannels = listOf(usSimRoaming, indiaSim, whatsAppChannel)

        val testManager = object : TravelRoamingManager(context) {
            override fun getCurrentCountryIso(): String = "in"
        }

        val decision = testManager.evaluateTravelRouting(
            phoneNumber = "+919876543210",
            pinnedChannel = usSimRoaming,
            userExplicitOverride = null,
            availableChannels = testChannels
        )

        assertNotNull(decision.recommendedChannel)
        assertTrue(decision.recommendedChannel is CallingChannel.CellularSim)
        val cellular = decision.recommendedChannel as CallingChannel.CellularSim
        assertEquals(1, cellular.slotIndex) // slotIndex 1 = India SIM
        assertFalse(cellular.isRoaming)
    }

    @Test
    fun testExplicitUserOverrideTakesPrecedenceOverTravelOverlay() {
        val testChannels = listOf(usSimRoaming, indiaSim, whatsAppChannel)

        val decision = manager.evaluateTravelRouting(
            phoneNumber = "+14085551234",
            pinnedChannel = whatsAppChannel,
            userExplicitOverride = usSimRoaming,
            availableChannels = testChannels
        )

        assertFalse("User override is not auto-optimized", decision.isTravelOptimized)
        assertEquals("Explicit user selection must be honored", usSimRoaming, decision.recommendedChannel)
    }

    @Test
    fun testEmergencyNumberAlwaysRoutesToCellular() {
        val testChannels = listOf(usSimRoaming, indiaSim, whatsAppChannel)

        val decision911 = manager.evaluateTravelRouting(
            phoneNumber = "911",
            pinnedChannel = whatsAppChannel,
            availableChannels = testChannels
        )
        assertTrue(decision911.recommendedChannel is CallingChannel.CellularSim)

        val decision112 = manager.evaluateTravelRouting(
            phoneNumber = "112",
            pinnedChannel = whatsAppChannel,
            availableChannels = testChannels
        )
        assertTrue(decision112.recommendedChannel is CallingChannel.CellularSim)
    }

    @Test
    fun testDisabledSmartRoamingHonorsPinnedChannelDirectly() {
        manager.setSmartRoamingEnabled(false)
        val testChannels = listOf(usSimRoaming, indiaSim, whatsAppChannel)

        val decision = manager.evaluateTravelRouting(
            phoneNumber = "+14085551234",
            pinnedChannel = usSimRoaming,
            availableChannels = testChannels
        )

        assertFalse(decision.isTravelOptimized)
        assertEquals(usSimRoaming, decision.recommendedChannel)
    }

    @Test
    fun testZeroMutationInvariant_PreferencesRemainUntouched() {
        // Room DB / preference store is never modified by TravelRoamingManager
        val prefRepo = com.example.data.ChannelPreferenceRepository.getInstance(context)
        val testNumber = "+14085559999"

        // Baseline: no preference
        val initialPref = prefRepo.getCachedPreference(testNumber)

        // Run travel evaluations multiple times
        val testChannels = listOf(usSimRoaming, indiaSim, whatsAppChannel)
        manager.evaluateTravelRouting(testNumber, pinnedChannel = usSimRoaming, availableChannels = testChannels)
        manager.evaluateTravelRouting("+919876543210", pinnedChannel = usSimRoaming, availableChannels = testChannels)

        // Verify preference cache is unchanged
        val afterPref = prefRepo.getCachedPreference(testNumber)
        assertEquals("Preference in database/cache must remain completely unmutated", initialPref, afterPref)
    }
}
