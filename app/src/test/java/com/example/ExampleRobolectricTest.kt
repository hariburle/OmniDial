package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CallerRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("OmniDial", appName)
  }

  @Test
  fun `verify caller rule matching logic`() {
    val rule = CallerRule(
      name = "Apartment Intercom",
      phoneNumberPattern = "5550199",
      isEnabled = true,
      autoAnswer = true,
      dtmfSequence = "9#"
    )
    val incomingCaller = "+1 (555) 0199"
    val normalizedCaller = incomingCaller.filter { it.isDigit() }
    val normalizedPattern = rule.phoneNumberPattern.filter { it.isDigit() }

    assertTrue(normalizedCaller.contains(normalizedPattern))
  }

  @Test
  fun `verify favorite contact creation`() {
    val fav = com.example.data.FavoriteContact(
      name = "Mom",
      phoneNumber = "+15552345678",
      label = "Family"
    )
    assertEquals("Mom", fav.name)
    assertEquals("+15552345678", fav.phoneNumber)
  }

  @Test
  fun `verify ongoing call notification banner has chronometer and public visibility`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val callInfo = com.example.telecom.ActiveCallInfo(
      id = "test_1",
      phoneNumber = "+15551234567",
      displayName = "Kishan",
      state = android.telecom.Call.STATE_ACTIVE,
      isIncoming = false,
      connectTimeMillis = System.currentTimeMillis() - 45000,
      isSimulated = true
    )

    val notification = com.example.telecom.OngoingCallNotificationHelper.buildCallNotification(context, callInfo)
    assertEquals(android.app.Notification.VISIBILITY_PUBLIC, notification.visibility)
    assertTrue((notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0)
    assertTrue(notification.extras.getBoolean(androidx.core.app.NotificationCompat.EXTRA_SHOW_CHRONOMETER))
    assertEquals("Kishan", notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString())
  }

  @Test
  fun `verify external web dial intent is recognized and extracted correctly`() {
    // Typical intent sent when tapping a phone number link on a website: ACTION_VIEW with tel: URI
    val webTelIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("tel:+15551234567"))
    assertTrue(com.example.util.ContactHelper.isDialOrTelIntent(webTelIntent))
    assertEquals("+15551234567", com.example.util.ContactHelper.extractPhoneNumberFromIntent(webTelIntent))

    // Intent with encoded characters or query parameters
    val encodedTelIntent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:+1%20800%20555%200199?ext=101"))
    assertTrue(com.example.util.ContactHelper.isDialOrTelIntent(encodedTelIntent))
    assertEquals("+1 800 555 0199", com.example.util.ContactHelper.extractPhoneNumberFromIntent(encodedTelIntent))

    // Intent with extra phone number
    val extraPhoneIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
      putExtra(android.content.Intent.EXTRA_PHONE_NUMBER, "4155552671")
    }
    assertTrue(com.example.util.ContactHelper.isDialOrTelIntent(extraPhoneIntent))
    assertEquals("4155552671", com.example.util.ContactHelper.extractPhoneNumberFromIntent(extraPhoneIntent))
  }
}
