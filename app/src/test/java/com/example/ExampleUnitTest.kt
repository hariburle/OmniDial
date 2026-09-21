package com.example

import com.example.util.ContactHelper
import com.example.util.ContactPhoneNumber
import com.example.util.DeviceContact
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testCountryIsoForNumber() {
        assertEquals("US", ContactHelper.getCountryIsoForNumber("+14155550199"))
        assertEquals("IN", ContactHelper.getCountryIsoForNumber("+919876543210"))
        assertEquals("GB", ContactHelper.getCountryIsoForNumber("+442071838750"))
        assertNull(ContactHelper.getCountryIsoForNumber("5550199"))
    }

    @Test
    fun testGetDescriptiveNumberLabel() {
        val contact = DeviceContact(
            name = "John Doe",
            phoneNumber = "+14155550199",
            label = "Mobile",
            phoneNumbers = listOf(
                ContactPhoneNumber("+14155550199", "Mobile"),
                ContactPhoneNumber("+919876543210", "Work")
            )
        )

        val usLabel = ContactHelper.getDescriptiveNumberLabel(contact, "+14155550199")
        assertEquals("Mobile • US", usLabel)

        val inLabel = ContactHelper.getDescriptiveNumberLabel(contact, "+919876543210")
        assertEquals("Work • IN", inLabel)
    }

    @Test
    fun testSingleNumberContactLabel() {
        val contact = DeviceContact(
            name = "Jane Smith",
            phoneNumber = "5551234",
            label = "Home",
            phoneNumbers = listOf(
                ContactPhoneNumber("5551234", "Home")
            )
        )

        val label = ContactHelper.getDescriptiveNumberLabel(contact, "5551234")
        assertEquals("Home", label)
    }

    @Test
    fun testVoicemailAndShortCodeIsolation() {
        // *86 voicemail must NOT match arbitrary contacts with 86 in their number
        assertFalse(ContactHelper.isSamePhoneNumber("*86", "+18605551234"))
        assertFalse(ContactHelper.isSamePhoneNumber("*86", "860-555-1234"))
        assertFalse(ContactHelper.isSamePhoneNumber("*86", "+8613800000000"))
        assertFalse(ContactHelper.isSamePhoneNumber("*86", "555-8686"))
        assertFalse(ContactHelper.isSamePhoneNumber("911", "+12129115555"))
        
        // Exact matches must still pass
        assertTrue(ContactHelper.isSamePhoneNumber("*86", "*86"))
        assertTrue(ContactHelper.isSamePhoneNumber("*86", " *86 "))
        assertTrue(ContactHelper.isSamePhoneNumber("911", "911"))
    }

    @Test
    fun testSmartContactSortNicknames() {
        val c1 = DeviceContact(name = "Alice Smith", phoneNumber = "111", nickname = "Ali")
        val c2 = DeviceContact(name = "Bob Jones", phoneNumber = "222", nickname = null)
        val c3 = DeviceContact(name = "Charlie Brown", phoneNumber = "333", nickname = "Chuck")
        val c4 = DeviceContact(name = "David Miller", phoneNumber = "444", nickname = "")

        val list = listOf(c1, c2, c3, c4)
        val filtered = list.filter { !it.nickname.isNullOrBlank() }
        assertEquals(2, filtered.size)
        assertEquals(listOf("Ali", "Chuck"), filtered.map { it.nickname })

        // Check SmartContactSort enum
        val nicknamesSort = com.example.ui.models.SmartContactSort.NICKNAMES
        assertEquals("Nicknames", nicknamesSort.label)
        assertEquals("Contacts with Nicknames", nicknamesSort.description)
    }

    @Test
    fun testT9SecondaryNumberMatchPreservesMatchingNumberAndLabel() {
        val contact = DeviceContact(
            name = "Kishan Patel",
            phoneNumber = "+919876543210",
            label = "India Mobile",
            phoneNumbers = listOf(
                ContactPhoneNumber("+919876543210", "India Mobile"),
                ContactPhoneNumber("+16505551234", "US Work")
            )
        )

        val results = com.example.util.T9Helper.search(listOf(contact), "6505551234")
        assertEquals(1, results.size)
        val match = results[0]
        assertEquals("Kishan Patel", match.name)
        // Must return the matched US Work number, NOT the default India Mobile number
        assertEquals("+16505551234", match.phoneNumber)
        assertEquals("US Work", match.label)
        assertEquals(2, match.allPhoneNumbers.size)
    }

    @Test
    fun testT9NameSearchReturnsAllPhoneNumbersForContact() {
        val contact = DeviceContact(
            name = "Alice Smith",
            phoneNumber = "+15551111111",
            label = "Mobile",
            phoneNumbers = listOf(
                ContactPhoneNumber("+15551111111", "Mobile"),
                ContactPhoneNumber("+15552222222", "Home"),
                ContactPhoneNumber("+15553333333", "Work")
            )
        )

        // "25423" corresponds to "ALICE"
        val results = com.example.util.T9Helper.search(listOf(contact), "25423")
        assertEquals(1, results.size)
        val match = results[0]
        assertEquals("Alice Smith", match.name)
        assertEquals(3, match.allPhoneNumbers.size)
        assertEquals("+15551111111", match.allPhoneNumbers[0].number)
        assertEquals("+15552222222", match.allPhoneNumbers[1].number)
        assertEquals("+15553333333", match.allPhoneNumbers[2].number)
    }
}
