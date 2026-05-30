package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.CreateChallengeScreen
import com.example.ui.viewmodel.ChallengeViewModel
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Daily Challenge", appName)
  }

  @Test
  fun testCreateChallengeScreenRendering() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = ChallengeViewModel(app)
    composeTestRule.setContent {
      MyApplicationTheme {
        CreateChallengeScreen(viewModel = viewModel, onNavigateBack = {})
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun testCreateChallengeSubmit() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = ChallengeViewModel(app)
    composeTestRule.setContent {
      MyApplicationTheme {
        CreateChallengeScreen(viewModel = viewModel, onNavigateBack = {})
      }
    }
    
    // Type into fields and click submit to trigger full DB and Alarm scheduling pipeline
    composeTestRule.onNodeWithTag("challenge_title_input").performTextInput("Robolectric Exercise Target")
    composeTestRule.onNodeWithTag("challenge_target_days_input").performTextInput("30")
    composeTestRule.onNodeWithTag("save_challenge_button").performClick()
    composeTestRule.waitForIdle()
  }
}
