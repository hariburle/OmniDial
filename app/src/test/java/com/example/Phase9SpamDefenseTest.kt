package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.SpamNumber
import com.example.util.PhoneNumberNormalizer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase9SpamDefenseTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `test E164 number normalization`() {
        val rawNumber1 = "(555) 019-2831"
        val rawNumber2 = "+1 555-019-2831"
        val rawNumber3 = "5550192831"

        val e164_1 = PhoneNumberNormalizer.toE164(rawNumber1, "US")
        val e164_2 = PhoneNumberNormalizer.toE164(rawNumber2, "US")
        val e164_3 = PhoneNumberNormalizer.toE164(rawNumber3, "US")

        assertEquals("+15550192831", e164_1)
        assertEquals("+15550192831", e164_2)
        assertEquals("+15550192831", e164_3)
    }

    @Test
    fun `test spam database normalized lookup`() = runBlocking {
        val rawInput = "(555) 999-8888"
        val normalized = PhoneNumberNormalizer.toE164(rawInput, "US")

        val spamEntry = SpamNumber(
            phoneNumber = rawInput,
            normalizedNumber = normalized,
            label = "Telemarketer",
            reportCount = 5
        )

        db.appDao().insertSpamNumber(spamEntry)

        val retrievedByFormatted = db.appDao().getSpamByNumber(rawInput, normalized)
        assertNotNull(retrievedByFormatted)
        assertEquals("Telemarketer", retrievedByFormatted?.label)

        val retrievedByE164 = db.appDao().getSpamByNormalizedNumber("+15559998888")
        assertNotNull(retrievedByE164)
        assertEquals(rawInput, retrievedByE164?.phoneNumber)
    }

    @Test
    fun `test spam preset preferences storage`() {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("auto_block_carrier_spam", true)
            .putBoolean("block_telemarketers_robocalls", true)
            .putBoolean("silence_unknown_private", true)
            .apply()

        assertTrue(prefs.getBoolean("auto_block_carrier_spam", false))
        assertTrue(prefs.getBoolean("block_telemarketers_robocalls", false))
        assertTrue(prefs.getBoolean("silence_unknown_private", false))
    }
}
