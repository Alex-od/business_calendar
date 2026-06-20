package ua.danichapps.radiantdays.ui.addevent

import android.content.Context
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ua.danichapps.radiantdays.MainActivity
import ua.danichapps.radiantdays.R

/**
 * End-to-end: Calendar → Add event → focus AI chat on real [MainActivity].
 */
@RunWith(AndroidJUnit4::class)
class AddEditEventAiChatInputE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun aiChatInput_movesUp_whenFocused_onMainActivity() {
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.calendar_add_event),
        ).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(AiChatInputTestTags.BAR).assertIsDisplayed()
        composeRule.onNodeWithTag(AiChatInputTestTags.FIELD).assertIsDisplayed()

        val displayHeightPx = composeRule.activity.resources.displayMetrics.heightPixels.toFloat()
        val barBottomBefore = barBottomPx()
        composeRule.onNodeWithTag(AiChatInputTestTags.FIELD).performClick()
        showKeyboardExplicitly()

        composeRule.waitUntil(timeoutMillis = 8_000L) {
            barBottomPx() < barBottomBefore - MIN_UPWARD_MOVE_PX
        }
        composeRule.waitForIdle()

        val barBottomAfter = barBottomPx()
        val upwardMove = barBottomBefore - barBottomAfter
        val keyboardHeightEstimate = upwardMove.coerceAtLeast(displayHeightPx * 0.15f)
        val keyboardTopPx = displayHeightPx - keyboardHeightEstimate
        val gapAboveKeyboardPx = keyboardTopPx - barBottomAfter

        logMetrics(
            mapOf(
                "displayHeightPx" to displayHeightPx,
                "barBottomBeforePx" to barBottomBefore,
                "barBottomPx" to barBottomAfter,
                "upwardMovePx" to upwardMove,
                "gapAboveKeyboardPx" to gapAboveKeyboardPx,
            ),
        )

        assertTrue(
            "Input should move up when keyboard opens, upward=${upwardMove}px",
            upwardMove >= MIN_UPWARD_MOVE_PX,
        )
        assertTrue(
            "Gap above keyboard too large: ${gapAboveKeyboardPx}px",
            gapAboveKeyboardPx <= EXCESSIVE_GAP_ABOVE_KEYBOARD_PX,
        )
        assertTrue(
            "Input should stay above keyboard, barBottom=$barBottomAfter",
            barBottomAfter <= displayHeightPx * KEYBOARD_TOP_MAX_FRACTION,
        )
    }

    private fun showKeyboardExplicitly() {
        composeRule.activityRule.scenario.onActivity { activity ->
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            activity.currentFocus?.let { imm.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT) }
        }
        composeRule.waitForIdle()
    }

    private fun barBottomPx(): Float = composeRule
        .onNodeWithTag(AiChatInputTestTags.BAR)
        .fetchSemanticsNode()
        .boundsInRoot
        .bottom

    private fun logMetrics(data: Map<String, Any>) {
        Log.i(METRICS_TAG, JSONObject(data).toString())
    }

    private companion object {
        const val METRICS_TAG = "AiChatE2ETest"
        const val MIN_UPWARD_MOVE_PX = 80f
        const val KEYBOARD_TOP_MAX_FRACTION = 0.72f
        const val EXCESSIVE_GAP_ABOVE_KEYBOARD_PX = 120f
    }
}
