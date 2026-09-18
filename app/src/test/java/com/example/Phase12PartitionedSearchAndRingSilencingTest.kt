package com.example

import android.content.Context
import android.telecom.Call
import androidx.test.core.app.ApplicationProvider
import com.example.telecom.ActiveCallInfo
import com.example.telecom.CallManager
import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
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
class Phase12PartitionedSearchAndRingSilencingTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        CallManager.init(context)
    }

    @Test
    fun `test partitioned search with nicknames filter active`() {
        val contacts = listOf(
            DeviceContact(
                name = "John Doe",
                phoneNumber = "1234567890",
                nickname = "Johnny"
            ),
            DeviceContact(
                name = "John Smith",
                phoneNumber = "9876543210",
                nickname = null
            ),
            DeviceContact(
                name = "Alice Wonder",
                phoneNumber = "5551234567",
                nickname = null
            )
        )

        val searchQuery = "John"
        val qLower = searchQuery.lowercase()

        // 1. All matches across phonebook
        val allMatches = contacts.filter {
            it.name.lowercase().contains(qLower) ||
            (it.nickname != null && it.nickname.lowercase().contains(qLower)) ||
            ContactHelper.matchesNumberQuery(it.phoneNumber, searchQuery)
        }

        // 2. Filtered matches (Nicknames only)
        val filteredMatches = allMatches.filter { !it.nickname.isNullOrBlank() }

        // 3. Partitioned excluded matches
        val excludedMatches = allMatches.filter { it.nickname.isNullOrBlank() }

        assertEquals(2, allMatches.size)
        assertEquals(1, filteredMatches.size)
        assertEquals("John Doe", filteredMatches[0].name)
        assertEquals("Johnny", filteredMatches[0].nickname)

        assertEquals(1, excludedMatches.size)
        assertEquals("John Smith", excludedMatches[0].name)
    }

    @Test
    fun `test partitioned search with favorites filter active`() {
        val fastFavorites = setOf("1234567890")

        val contacts = listOf(
            DeviceContact(
                name = "Robert Taylor",
                phoneNumber = "1234567890" // Favorite
            ),
            DeviceContact(
                name = "Robert Miller",
                phoneNumber = "8881234567" // Not favorite
            )
        )

        val searchQuery = "Robert"
        val qLower = searchQuery.lowercase()

        val allMatches = contacts.filter {
            it.name.lowercase().contains(qLower)
        }

        val favoriteMatches = allMatches.filter { fastFavorites.contains(it.phoneNumber) }
        val excludedMatches = allMatches.filter { !fastFavorites.contains(it.phoneNumber) }

        assertEquals(1, favoriteMatches.size)
        assertEquals("Robert Taylor", favoriteMatches[0].name)

        assertEquals(1, excludedMatches.size)
        assertEquals("Robert Miller", excludedMatches[0].name)
    }

    @Test
    fun `test ringer silencing state during incoming call`() {
        // Initial state before incoming call
        assertFalse(CallManager.isRingerSilenced.value)

        // Simulate incoming ringing call
        CallManager.startSimulatedIncomingCall(
            context = context,
            number = "+15550001",
            name = "Test Ringing Caller"
        )

        // When call arrives, it should not be silenced yet
        assertFalse(CallManager.isRingerSilenced.value)

        // Trigger silence ringer (e.g. via screen tap, lift gesture, or button)
        CallManager.silenceRinger(context)
        assertTrue(CallManager.isRingerSilenced.value)

        // When call ends, ringer silenced state should reset to false
        CallManager.disconnectCall()
        assertFalse(CallManager.isRingerSilenced.value)
    }
}
