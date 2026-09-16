package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.DialerScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [34])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    composeTestRule.setContent {
      MyApplicationTheme {
        DialerScreen(
          number = "555-0199",
          favorites = listOf(
            com.example.data.FavoriteContact(
              id = 1,
              name = "Apartment Gate",
              phoneNumber = "5550199",
              label = "Intercom"
            )
          ),
          isDefaultDialer = true,
          context = context,
          onRoleChanged = {},
          onDigitPress = {},
          onDeleteDigit = {},
          onClearDigits = {},
          onSelectContactNumber = {},
          onPlaceCall = { _, _ -> },
          onSimulateCall = { _, _ -> },
          onCreateRuleForNumber = {},
          onAddFavorite = { _, _, _, _ -> },
          onDeleteFavorite = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
