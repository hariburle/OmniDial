package com.example

import com.example.util.PhoneNumberNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberDetectionTest {

    @Test
    fun testEmptyAndBlankStringsRejected() {
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber(null))
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber(""))
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber("   "))
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber("\n\t"))

        assertNull(PhoneNumberNormalizer.extractLikelyPhoneNumber(null))
        assertNull(PhoneNumberNormalizer.extractLikelyPhoneNumber(""))
        assertNull(PhoneNumberNormalizer.extractLikelyPhoneNumber("   "))
    }

    @Test
    fun testNonPhoneTextRejected() {
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber("Hello world"))
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber("Call me later"))
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber("https://google.com"))
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber("john@example.com"))
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber("Order #1234 on 5th Ave"))
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber("Meeting at 3pm"))
    }

    @Test
    fun testArbitraryDigitsRejected() {
        // Too short for a phone number
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber("1"))
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber("42"))
        // 5-digit ZIP or 6-digit OTP codes are not phone numbers
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber("90210"))
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber("847291"))
        // Excessive digits (> 15 base digits) e.g. credit card or tracking numbers
        assertFalse(PhoneNumberNormalizer.isLikelyPhoneNumber("12345678901234567890"))
    }

    @Test
    fun testStandardPhoneNumbersAccepted() {
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("+1 (415) 555-2671"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("415-555-2671"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("(415) 555-2671"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("4155552671"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("555-1234"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("+91 98765 43210"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("+44 20 7946 0958"))
    }

    @Test
    fun testSpecialDialerNumbersAccepted() {
        // USSD and carrier feature codes
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("*86"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("*#06#"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("#123#"))
        // Emergency numbers
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("911"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("112"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("999"))
        // Service codes
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("611"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("411"))
    }

    @Test
    fun testNumbersWithPausesAndExtensions() {
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("1-800-555-1234,1234#"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("555-234-5678;99"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("+1-555-555-5555 ext 102"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("+1-555-555-5555 x404"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("tel:+14155552671"))
    }

    @Test
    fun testExtractLikelyPhoneNumber() {
        assertEquals("+14155552671", PhoneNumberNormalizer.extractLikelyPhoneNumber("tel:+14155552671"))
        assertEquals("+1 (415) 555-2671", PhoneNumberNormalizer.extractLikelyPhoneNumber("+1 (415) 555-2671"))
        assertEquals("*86", PhoneNumberNormalizer.extractLikelyPhoneNumber("*86"))
        assertEquals("911", PhoneNumberNormalizer.extractLikelyPhoneNumber("911"))

        // Embedded phone number in short text
        val embedded = PhoneNumberNormalizer.extractLikelyPhoneNumber("Call: (415) 555-2671")
        assertNotNull(embedded)
        assertTrue(embedded!!.contains("415"))

        assertNull(PhoneNumberNormalizer.extractLikelyPhoneNumber("Hello world, see you tomorrow"))
        assertNull(PhoneNumberNormalizer.extractLikelyPhoneNumber(""))
    }

    @Test
    fun testInvisibleFormattingAndQuotes() {
        // Bi-directional marks (LRM / RLM) injected by Chromium/TextClassifier
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("‎911‎"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("‎+1 (415) 555-2671‎"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("‪+14155552671‬"))

        // Non-breaking spaces commonly found in European/international number formatting
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("+1 415 555 2671"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("+33 1 42 68 55 55"))

        // Quotes copied from formatted text
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("\"911\""))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("'415-555-2671'"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("“+14155552671”"))

        // International exit codes
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("011 44 20 7946 0958"))
        assertTrue(PhoneNumberNormalizer.isLikelyPhoneNumber("00 44 20 7946 0958"))

        assertEquals("911", PhoneNumberNormalizer.extractLikelyPhoneNumber("‎911‎"))
        assertEquals("+1 (415) 555-2671", PhoneNumberNormalizer.extractLikelyPhoneNumber("‎+1 (415) 555-2671‎"))
    }
}
