package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.CallerRule
import com.example.domain.usecase.EvaluateSimRuleUseCase
import com.example.domain.usecase.TrustTier
import com.example.telecom.CallManager
import com.example.telecom.SimInfo
import com.example.telecom.TelecomVoipHelper
import com.example.util.CommunityCallerIdService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase10TelecomTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db.appDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test DAG rule conflict resolution gives precedence to exact match over prefix`() = runBlocking {
        // Insert a generic prefix rule
        repository.insertRule(
            CallerRule(
                name = "All US Numbers",
                phoneNumberPattern = "+1*",
                isEnabled = true
            )
        )
        // Insert a specific exact-match rule
        repository.insertRule(
            CallerRule(
                name = "Specific Partner",
                phoneNumberPattern = "+14155550199",
                isEnabled = true
            )
        )

        val dualSims = listOf(
            SimInfo(slotIndex = 0, subscriptionId = 1, displayName = "SIM 1", carrierName = "Carrier A", isDefault = true, isRoaming = false),
            SimInfo(slotIndex = 1, subscriptionId = 2, displayName = "SIM 2", carrierName = "Carrier B", isDefault = false, isRoaming = false)
        )

        val useCase = EvaluateSimRuleUseCase(repository)
        val result = useCase(phoneNumber = "+14155550199", sims = dualSims, context = context)

        // Exact match should win DAG resolution
        assertEquals("Specific Partner", result.matchedRule?.name)
        assertTrue(result.ruleWeight > 1000)
    }

    @Test
    fun `test roaming-aware SIM selection protects against bill shock`() = runBlocking {
        val dualSimsWithRoaming = listOf(
            SimInfo(slotIndex = 0, subscriptionId = 1, displayName = "Home SIM", carrierName = "Home Tel", isDefault = true, isRoaming = true),
            SimInfo(slotIndex = 1, subscriptionId = 2, displayName = "Travel Local eSIM", carrierName = "Local Tel", isDefault = false, isRoaming = false)
        )

        val useCase = EvaluateSimRuleUseCase(repository)
        val result = useCase(phoneNumber = "+442071234567", sims = dualSimsWithRoaming, context = context)

        // Should automatically avoid roaming and select the non-roaming eSIM
        assertTrue(result.isRoamingActive)
        assertTrue(result.isRoamingAvoided)
        assertEquals(1, result.targetSlotIndex)
        assertTrue(result.skipDualPrompt)
    }

    @Test
    fun `test trust badge assignment logic for spam, delivery and verified callers`() {
        val spamResult = CallManager.resolveTrustBadge(
            isVoicemail = false,
            hasContact = false,
            hasFav = false,
            isSpam = true,
            communityInfo = null
        )
        assertEquals(TrustTier.HIGH_RISK_SPAM, spamResult.first)
        assertEquals("High Spam Risk", spamResult.second)

        val deliveryInfo = CommunityCallerIdService.lookup("+18003662255")
        val deliveryResult = CallManager.resolveTrustBadge(
            isVoicemail = false,
            hasContact = false,
            hasFav = false,
            isSpam = false,
            communityInfo = deliveryInfo
        )
        assertEquals(TrustTier.PRIORITY_LOGISTICS, deliveryResult.first)

        val bankInfo = CommunityCallerIdService.lookup("+18009359935")
        val bankResult = CallManager.resolveTrustBadge(
            isVoicemail = false,
            hasContact = false,
            hasFav = false,
            isSpam = false,
            communityInfo = bankInfo
        )
        assertEquals(TrustTier.VERIFIED_BUSINESS, bankResult.first)

        val contactResult = CallManager.resolveTrustBadge(
            isVoicemail = false,
            hasContact = true,
            hasFav = false,
            isSpam = false,
            communityInfo = null
        )
        assertEquals(TrustTier.VERIFIED_BUSINESS, contactResult.first)
        assertEquals("Saved Contact", contactResult.second)
    }

    @Test
    fun `test VoIP continuity lifecycle helper`() {
        assertFalse(TelecomVoipHelper.isVoipCallActive())

        TelecomVoipHelper.startVoipCall(
            context = context,
            phoneNumber = "+14155550123",
            displayName = "VoIP Test Partner",
            isIncoming = false
        )
        assertTrue(TelecomVoipHelper.isVoipCallActive())

        TelecomVoipHelper.endVoipCall()
        assertFalse(TelecomVoipHelper.isVoipCallActive())
    }
}
