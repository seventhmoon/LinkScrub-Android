import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.androidfung.linkscrub.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinkScrubUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testStep1_InputUrl_And_ClearInput() {
        // Find input field by testTag and verify empty
        val inputField = composeTestRule.onNodeWithTag("url_input_field")
        inputField.assertIsDisplayed()

        // Button should initially be disabled when input is blank
        composeTestRule.onNodeWithText("Inspect & Scrub Link").assertIsNotEnabled()

        // Input a dirty tracking URL
        inputField.performTextInput("https://example.com/product?utm_source=twitter&utm_medium=social&ref=123")

        // Button should now be enabled
        composeTestRule.onNodeWithText("Inspect & Scrub Link").assertIsEnabled()

        // Clear input via trailing icon
        composeTestRule.onNodeWithContentDescription("Clear input").performClick()

        // Button is disabled again
        composeTestRule.onNodeWithText("Inspect & Scrub Link").assertIsNotEnabled()
    }

    @Test
    fun testStep1_QuickCleanToggle() {
        // Initially in Step-by-step mode
        composeTestRule.onNodeWithText("Quick-Clean Mode").assertIsDisplayed()
        composeTestRule.onNodeWithText("Step-by-step parameter review").assertIsDisplayed()

        // Toggle Quick-Clean Mode
        composeTestRule.onNodeWithText("Quick-Clean Mode").performClick()

        // Subtitle changes to auto-scrub description
        composeTestRule.onNodeWithText("Auto-scrub & copy to clipboard instantly").assertIsDisplayed()

        // Toggle back
        composeTestRule.onNodeWithText("Quick-Clean Mode").performClick()
        composeTestRule.onNodeWithText("Step-by-step parameter review").assertIsDisplayed()
    }

    @Test
    fun testFullWorkflow_Step1ToStep3_And_ParameterToggling() {
        val inputField = composeTestRule.onNodeWithTag("url_input_field")
        inputField.performTextInput("https://example.com/article?utm_source=newsletter&utm_campaign=spring&article_id=987")

        // Proceed to Step 2
        composeTestRule.onNodeWithText("Inspect & Scrub Link").performClick()

        // Verify Step 2 is displayed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Live Cleaned URL Preview").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scrub All").assertIsDisplayed()
        composeTestRule.onNodeWithText("Keep All").assertIsDisplayed()

        // Verify parameter chips are shown
        composeTestRule.onNodeWithText("utm_source").assertIsDisplayed()
        composeTestRule.onNodeWithText("utm_campaign").assertIsDisplayed()
        composeTestRule.onNodeWithText("article_id").assertIsDisplayed()

        // Test Keep All
        composeTestRule.onNodeWithText("Keep All").performClick()
        composeTestRule.waitForIdle()

        // Test Scrub All
        composeTestRule.onNodeWithText("Scrub All").performClick()
        composeTestRule.waitForIdle()

        // Toggle individual parameter (e.g. article_id)
        composeTestRule.onNodeWithText("article_id").performClick()
        composeTestRule.waitForIdle()

        // Proceed to Step 3: Confirm & Preview
        composeTestRule.onNodeWithText("Confirm & Preview").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify Step 3: Preview card, Clean URL card, and Action buttons
        composeTestRule.onNodeWithText("Link Successfully Sanitized!").assertIsDisplayed()
        composeTestRule.onNodeWithText("Scrubbed Clean URL").assertIsDisplayed()
        composeTestRule.onNodeWithText("Copy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Share").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open in Browser").assertIsDisplayed()

        // Test Back/Adjust Parameters
        composeTestRule.onNodeWithText("Adjust Parameters").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Live Cleaned URL Preview").assertIsDisplayed()

        // Back to Step 1
        composeTestRule.onNodeWithText("Back").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("url_input_field").assertIsDisplayed()
    }

    @Test
    fun testHistoryPanel_OpenAndClose() {
        // Click on History top bar button
        composeTestRule.onNodeWithContentDescription("History").performClick()
        composeTestRule.waitForIdle()

        // Check history panel header is visible
        composeTestRule.onNodeWithText("Clean Link History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Search history...").assertIsDisplayed()

        // Try searching in history
        composeTestRule.onNodeWithText("Search history...").performTextInput("test-query")
        composeTestRule.waitForIdle()

        // Clear search
        composeTestRule.onNodeWithContentDescription("Clear Search").performClick()
        composeTestRule.waitForIdle()

        // Close history sheet if close button exists, or dismiss
        val closeBtn = composeTestRule.onNodeWithContentDescription("Close History")
        if (closeBtn.isDisplayed()) {
            closeBtn.performClick()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun testVirtualKeyboardInteraction_NoLayoutCrash() {
        val inputField = composeTestRule.onNodeWithTag("url_input_field")
        inputField.assertIsDisplayed()

        // Click to focus and request IME
        inputField.performClick()
        composeTestRule.waitForIdle()

        // Type text while virtual keyboard is triggered
        inputField.performTextInput("https://keyboard-test.com/path?utm_source=ime_test&session=123")
        composeTestRule.waitForIdle()

        // Verify input text updated without crash or layout collapse
        inputField.assertIsDisplayed()
        composeTestRule.onNodeWithText("Inspect & Scrub Link").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inspect & Scrub Link").assertIsEnabled()

        // Clear text
        inputField.performTextClearance()
        composeTestRule.waitForIdle()
        inputField.assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.isDisplayed(): Boolean {
        return try {
            assertIsDisplayed()
            true
        } catch (e: AssertionError) {
            false
        }
    }
}
