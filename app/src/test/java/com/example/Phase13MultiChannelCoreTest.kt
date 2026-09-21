package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.ChannelPreferenceRepository
import com.example.data.NumberChannelPreference
import com.example.data.ChannelConfig
import com.example.data.ChannelConfigRepository
import com.example.domain.model.CallingChannel
import com.example.domain.model.ChannelCategory
import com.example.telecom.ChannelDiscoveryManager
import com.example.telecom.ChannelDispatchCoordinator
import com.example.telecom.DispatchResult
import com.example.util.PhoneNumberNormalizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase13MultiChannelCoreTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var channelRepo: ChannelPreferenceRepository
    private lateinit var configRepo: ChannelConfigRepository
    private lateinit var discoveryManager: ChannelDiscoveryManager
    private lateinit var coordinator: ChannelDispatchCoordinator

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val appRepo = AppRepository(database.appDao())
        val prefs = context.getSharedPreferences("test_kishan_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        channelRepo = ChannelPreferenceRepository(appRepo)
        configRepo = ChannelConfigRepository(appRepo, prefs)
        discoveryManager = ChannelDiscoveryManager(context, configRepo)
        coordinator = ChannelDispatchCoordinator(context, discoveryManager, channelRepo)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `test calling channel model contracts and hierarchy`() {
        // Cellular SIM 1 & SIM 2
        val sim1 = CallingChannel.CellularSim(
            slotIndex = 0,
            subscriptionId = 101,
            carrierName = "Verizon",
            isRoaming = false
        )
        assertEquals("sim_1", sim1.id)
        assertEquals("SIM 1", sim1.shortLabel)
        assertEquals("SIM 1 (Verizon)", sim1.displayName)
        assertEquals(ChannelCategory.CELLULAR, sim1.category)

        val sim2 = CallingChannel.CellularSim(
            slotIndex = 1,
            subscriptionId = 102,
            carrierName = "T-Mobile",
            isRoaming = true
        )
        assertEquals("sim_2", sim2.id)
        assertEquals("SIM 2", sim2.shortLabel)
        assertEquals("SIM 2 (T-Mobile)", sim2.displayName)
        assertEquals(ChannelCategory.CELLULAR, sim2.category)
        assertTrue(sim2.isRoaming)

        // WhatsApp Personal & Business
        val waPersonal = CallingChannel.WhatsApp(isBusiness = false)
        assertEquals("whatsapp", waPersonal.id)
        assertEquals("WhatsApp", waPersonal.displayName)
        assertEquals("WhatsApp", waPersonal.shortLabel)
        assertEquals("com.whatsapp", waPersonal.packageName)
        assertEquals(ChannelCategory.MESSAGING_VOIP, waPersonal.category)

        val waBusiness = CallingChannel.WhatsApp(isBusiness = true)
        assertEquals("whatsapp_business", waBusiness.id)
        assertEquals("WhatsApp Business", waBusiness.displayName)
        assertEquals("WA Biz", waBusiness.shortLabel)
        assertEquals("com.whatsapp.w4b", waBusiness.packageName)
        assertEquals(ChannelCategory.MESSAGING_VOIP, waBusiness.category)

        // Google Voice
        val gVoice = CallingChannel.GoogleVoice(isAvailable = true)
        assertEquals("google_voice", gVoice.id)
        assertEquals("Google Voice", gVoice.displayName)
        assertEquals("G Voice", gVoice.shortLabel)
        assertEquals(ChannelCategory.CLOUD_VOIP, gVoice.category)

        // System Default and Ask
        assertEquals("system", CallingChannel.SystemDefault.id)
        assertEquals("ask", CallingChannel.AskAlways.id)
    }

    @Test
    fun `test channel discovery manager resolution by ID`() {
        val sim1 = discoveryManager.getChannelById("sim_1")
        assertNotNull(sim1)
        assertEquals("sim_1", sim1?.id)

        val sim2 = discoveryManager.getChannelById("sim_2")
        assertNotNull(sim2)
        assertEquals("sim_2", sim2?.id)

        val wa = discoveryManager.getChannelById("whatsapp")
        assertNotNull(wa)
        assertEquals("whatsapp", wa?.id)

        val waBiz = discoveryManager.getChannelById("whatsapp_business")
        assertNotNull(waBiz)
        assertEquals("whatsapp_business", waBiz?.id)

        val gv = discoveryManager.getChannelById("google_voice")
        assertNotNull(gv)
        assertEquals("google_voice", gv?.id)

        val sys = discoveryManager.getChannelById("system")
        assertNotNull(sys)
        assertEquals(CallingChannel.SystemDefault, sys)

        val ask = discoveryManager.getChannelById("ask")
        assertNotNull(ask)
        assertEquals(CallingChannel.AskAlways, ask)

        val unknown = discoveryManager.getChannelById("unknown_xyz")
        assertNull(unknown)
    }

    @Test
    fun `test per-phone-number channel preferences granularity`() = runBlocking {
        // A single contact with 3 numbers has independent channel preferences
        val mobileNumber = "+14155550100"
        val workNumber = "+14155550200"
        val homeNumber = "+14155550300"

        // Set preferences per number
        channelRepo.setPreferenceForNumber(mobileNumber, "whatsapp", customLabel = "Mobile")
        channelRepo.setPreferenceForNumber(workNumber, "sim_1", customLabel = "Work")
        channelRepo.setPreferenceForNumber(homeNumber, "sim_2", customLabel = "Home")

        // Verify independent retrieval
        val mobilePref = channelRepo.getPreferenceForNumber(mobileNumber)
        assertNotNull(mobilePref)
        assertEquals("whatsapp", mobilePref?.preferredChannelId)
        assertEquals("Mobile", mobilePref?.customLabel)

        val workPref = channelRepo.getPreferenceForNumber(workNumber)
        assertNotNull(workPref)
        assertEquals("sim_1", workPref?.preferredChannelId)
        assertEquals("Work", workPref?.customLabel)

        val homePref = channelRepo.getPreferenceForNumber(homeNumber)
        assertNotNull(homePref)
        assertEquals("sim_2", homePref?.preferredChannelId)
        assertEquals("Home", homePref?.customLabel)

        // Verify Flow emits all 3
        val allPrefs = channelRepo.allPreferences.first()
        assertEquals(3, allPrefs.size)

        // Update mobileNumber to WhatsApp Business
        channelRepo.setPreferenceForNumber(mobileNumber, "whatsapp_business", customLabel = "Work WA")
        val updatedMobile = channelRepo.getPreferenceForNumber(mobileNumber)
        assertEquals("whatsapp_business", updatedMobile?.preferredChannelId)
        assertEquals("Work WA", updatedMobile?.customLabel)

        // Remove workNumber preference
        channelRepo.removePreferenceForNumber(workNumber)
        val deletedWork = channelRepo.getPreferenceForNumber(workNumber)
        assertNull(deletedWork)

        // Clear all
        channelRepo.clearAllPreferences()
        val emptyPrefs = channelRepo.allPreferences.first()
        assertTrue(emptyPrefs.isEmpty())
    }

    @Test
    fun `test number normalization during channel preference operations`() = runBlocking {
        // Input with formatting, spaces, and hyphens should normalize to same E.164
        val rawInput = "(415) 555-0199"
        val normalized = PhoneNumberNormalizer.toE164(rawInput)

        channelRepo.setPreferenceForNumber(rawInput, "whatsapp")

        // Retrieve using normalized form
        val prefByNormalized = channelRepo.getPreferenceForNumber(normalized)
        assertNotNull(prefByNormalized)
        assertEquals("whatsapp", prefByNormalized?.preferredChannelId)

        // Retrieve using raw form
        val prefByRaw = channelRepo.getPreferenceForNumber(rawInput)
        assertNotNull(prefByRaw)
        assertEquals("whatsapp", prefByRaw?.preferredChannelId)
    }

    @Test
    fun `test channel dispatch coordinator resolution pipeline`() = runBlocking {
        val testNumber = "+15551234567"

        // 1. Explicit override takes precedence
        val explicitSim = CallingChannel.CellularSim(slotIndex = 1, subscriptionId = 2, carrierName = "T-Mobile", isRoaming = false)
        val resolvedExplicit = coordinator.resolveChannel(testNumber, explicitChannel = explicitSim)
        assertEquals("sim_2", resolvedExplicit.id)

        // 2. Per-number pinned preference in Room DB takes precedence over defaults
        channelRepo.setPreferenceForNumber(testNumber, "whatsapp")
        val resolvedPinned = coordinator.resolveChannel(testNumber)
        assertEquals("whatsapp", resolvedPinned.id)

        // 3. Pinned preference set to 'ask' resolves to AskAlways
        channelRepo.setPreferenceForNumber(testNumber, "ask")
        val resolvedAsk = coordinator.resolveChannel(testNumber)
        assertEquals(CallingChannel.AskAlways, resolvedAsk)

        // 4. No preference -> fallback to SystemDefault / Cellular
        channelRepo.removePreferenceForNumber(testNumber)
        val resolvedFallback = coordinator.resolveChannel(testNumber)
        assertTrue(resolvedFallback is CallingChannel.CellularSim || resolvedFallback == CallingChannel.SystemDefault)
    }

    @Test
    fun `test channel dispatch coordinator execution`() {
        val testNumber = "+15559876543"
        var cellularSlotCalled: Int? = -1
        var cellularReasonCalled: String? = null
        var whatsAppNumberCalled: String? = null
        var pickerShown = false

        // 1. Dispatch Cellular SIM 1
        val sim1 = CallingChannel.CellularSim(slotIndex = 0, subscriptionId = 1, carrierName = "Carrier", isRoaming = false)
        val res1 = coordinator.dispatchCall(
            phoneNumber = testNumber,
            channel = sim1,
            reason = "Urgent",
            onCellularCall = { slot, reason ->
                cellularSlotCalled = slot
                cellularReasonCalled = reason
            },
            onWhatsAppCall = { whatsAppNumberCalled = it },
            onShowPicker = { pickerShown = true }
        )
        assertTrue(res1 is DispatchResult.Dispatched)
        assertEquals(1, cellularSlotCalled)
        assertEquals("Urgent", cellularReasonCalled)

        // 2. Dispatch WhatsApp
        val wa = CallingChannel.WhatsApp(isBusiness = false)
        val res2 = coordinator.dispatchCall(
            phoneNumber = testNumber,
            channel = wa,
            onCellularCall = { _, _ -> },
            onWhatsAppCall = { whatsAppNumberCalled = it },
            onShowPicker = { pickerShown = true }
        )
        assertTrue(res2 is DispatchResult.Dispatched)
        assertEquals(testNumber, whatsAppNumberCalled)

        // 3. Dispatch AskAlways triggers showPicker
        val res3 = coordinator.dispatchCall(
            phoneNumber = testNumber,
            channel = CallingChannel.AskAlways,
            onCellularCall = { _, _ -> },
            onWhatsAppCall = { },
            onShowPicker = { pickerShown = true }
        )
        assertTrue(res3 is DispatchResult.ShowPicker)
        assertTrue(pickerShown)

        // 4. Blank phone number returns Error
        val res4 = coordinator.dispatchCall(
            phoneNumber = "   ",
            channel = sim1,
            onCellularCall = { _, _ -> },
            onWhatsAppCall = { },
            onShowPicker = { }
        )
        assertTrue(res4 is DispatchResult.Error)
    }

    @Test
    fun `test channel config persistence and renaming`() = runBlocking {
        // 1. Initial empty configs
        val initialConfigs = configRepo.getAllConfigs()
        assertTrue(initialConfigs.isEmpty())

        // 2. Save configs for multiple channels
        val configs = listOf(
            ChannelConfig(channelId = "sim_1", isEnabled = true, customName = "Personal Jio", orderIndex = 0),
            ChannelConfig(channelId = "sim_2", isEnabled = true, customName = "Work Airtel", orderIndex = 1),
            ChannelConfig(channelId = "whatsapp", isEnabled = false, customName = "WhatsApp Family", orderIndex = 2)
        )
        configRepo.saveConfigs(configs)

        // 3. Verify retrieval
        val saved = configRepo.getAllConfigs()
        assertEquals(3, saved.size)
        assertEquals("Personal Jio", configRepo.getConfig("sim_1")?.customName)
        assertEquals(true, configRepo.getConfig("sim_1")?.isEnabled)
        assertEquals("Work Airtel", configRepo.getConfig("sim_2")?.customName)
        assertEquals(false, configRepo.getConfig("whatsapp")?.isEnabled)

        // 4. Update enablement
        configRepo.setChannelEnabled("whatsapp", true)
        assertEquals(true, configRepo.getConfig("whatsapp")?.isEnabled)

        // 5. Rename channel
        configRepo.renameChannel("sim_1", "Primary Jio 5G")
        assertEquals("Primary Jio 5G", configRepo.getConfig("sim_1")?.customName)

        // 6. Verify Flow emits
        val flowList = configRepo.allConfigs.first()
        assertEquals(3, flowList.size)
    }

    @Test
    fun `test channel discovery manager respects enabled and custom name configurations`() = runBlocking {
        // Configure SIM 1 with custom name, and disable WhatsApp
        configRepo.saveConfigs(listOf(
            ChannelConfig(channelId = "sim_1", isEnabled = true, customName = "Personal SIM", orderIndex = 0),
            ChannelConfig(channelId = "whatsapp", isEnabled = false, customName = "Hidden WA", orderIndex = 1)
        ))
        configRepo.setCompletedOnboarding(true)

        // Trigger channel refresh
        discoveryManager.refreshChannels()

        // Verify available channels
        val available = discoveryManager.availableChannels.first()
        // WhatsApp should be filtered out
        assertFalse(available.any { it.id == "whatsapp" })

        // If SIM 1 is present, its display name should reflect the custom name
        val sim1 = available.firstOrNull { it.id == "sim_1" }
        if (sim1 != null) {
            assertEquals("Personal SIM", sim1.displayName)
            assertEquals("Personal SIM", sim1.shortLabel)
        }
    }

    @Test
    fun `test first run onboarding state tracking`() {
        assertFalse(configRepo.hasCompletedOnboarding())

        configRepo.setCompletedOnboarding(true)
        assertTrue(configRepo.hasCompletedOnboarding())

        configRepo.setCompletedOnboarding(false)
        assertFalse(configRepo.hasCompletedOnboarding())
    }

    @Test
    fun `test synchronous custom name caching and retrieval`() = runBlocking {
        configRepo.saveConfigs(listOf(
            ChannelConfig(channelId = "sim_1", isEnabled = true, customName = "US Spectrum", orderIndex = 0),
            ChannelConfig(channelId = "sim_2", isEnabled = true, customName = "India Roaming", orderIndex = 1),
            ChannelConfig(channelId = "whatsapp", isEnabled = true, customName = "WaIN", orderIndex = 2)
        ))

        assertEquals("US Spectrum", configRepo.getCustomNameSync("sim_1"))
        assertEquals("India Roaming", configRepo.getCustomNameSync("sim_2"))
        assertEquals("WaIN", configRepo.getCustomNameSync("whatsapp"))
        assertNull(configRepo.getCustomNameSync("non_existent"))

        // Updating single config
        configRepo.saveConfig(ChannelConfig(channelId = "whatsapp", isEnabled = true, customName = "WhatsApp Direct", orderIndex = 2))
        assertEquals("WhatsApp Direct", configRepo.getCustomNameSync("whatsapp"))

        // Renaming single channel
        configRepo.renameChannel("sim_1", "US Ultra")
        assertEquals("US Ultra", configRepo.getCustomNameSync("sim_1"))
    }

    @Test
    fun `test emergency number discovery and cellular routing`() {
        // Standard emergency numbers
        assertTrue(discoveryManager.isEmergencyNumber("911"))
        assertTrue(discoveryManager.isEmergencyNumber("112"))
        assertFalse(discoveryManager.isEmergencyNumber("5551234"))

        // International numbers
        assertTrue(discoveryManager.isInternationalNumber("+44123456789"))
        assertTrue(discoveryManager.isInternationalNumber("01144123456789"))
        assertTrue(discoveryManager.isInternationalNumber("0044123456789"))
        assertFalse(discoveryManager.isInternationalNumber("5551234567"))
    }
}

