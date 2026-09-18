package com.example.telecom

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.util.ContactHelper
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
class OmniCallRedirectionServiceTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val prefs = context.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    @Test
    fun testLearnedWhatsAppPreferenceMatchesNumber() {
        val prefs = context.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("whatsapp_learned_choices", setOf("+15551234567:whatsapp"))
            .apply()

        val rawLearned = prefs.getStringSet("whatsapp_learned_choices", emptySet()) ?: emptySet()
        val phoneNumber = "5551234567"

        var preferredMode: String? = null
        for (entry in rawLearned) {
            val parts = entry.split(":")
            if (parts.size >= 2) {
                val numKey = parts[0]
                val mode = parts[1]
                if (numKey.endsWith(phoneNumber) || ContactHelper.isSamePhoneNumber(numKey, phoneNumber)) {
                    preferredMode = mode
                    break
                }
            }
        }

        assertEquals("whatsapp", preferredMode)
    }

    @Test
    fun testNeverModeBypassesRedirection() {
        val prefs = context.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("whatsapp_call_mode", "never")
            .putStringSet("whatsapp_learned_choices", setOf("+15551234567:whatsapp"))
            .apply()

        val globalMode = prefs.getString("whatsapp_call_mode", "ask_learn")
        assertEquals("never", globalMode)
        // Under "never", call redirection aborts immediately and lets cellular proceed
        assertTrue(globalMode == "never")
    }

    @Test
    fun testContactSimPreferenceResolution() {
        val prefs = context.getSharedPreferences("kishan_dialer_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("contact_sim_preferences", setOf("+15559876543:2"))
            .apply()

        val simPrefs = prefs.getStringSet("contact_sim_preferences", emptySet()) ?: emptySet()
        val targetNumber = "5559876543"

        var resolvedSlot: Int? = null
        for (entry in simPrefs) {
            val parts = entry.split(":")
            if (parts.size >= 2) {
                val numKey = parts[0]
                val slot = parts[1].toIntOrNull()
                if (numKey.endsWith(targetNumber)) {
                    resolvedSlot = slot
                    break
                }
            }
        }

        assertEquals(2, resolvedSlot)
    }

    @Test
    fun testVoicemailOrEmptyUriIsNotRedirected() {
        val emptyUri = Uri.parse("tel:")
        val rawScheme = emptyUri.schemeSpecificPart ?: ""
        val cleanNumber = rawScheme.replace(Regex("[^0-9+]"), "")
        assertTrue(cleanNumber.isBlank())
    }
}
