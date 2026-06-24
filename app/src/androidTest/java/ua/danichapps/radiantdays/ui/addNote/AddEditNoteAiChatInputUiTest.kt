package ua.danichapps.radiantdays.ui.addNote

import android.content.Context
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ua.danichapps.radiantdays.ui.common.KeyboardInsetsPolicy
import ua.danichapps.radiantdays.ui.theme.RadiantDaysTheme

/**
 * Mirrors production layout: Scaffold bottomBar + [KeyboardInsetsPolicy].
 */
@RunWith(AndroidJUnit4::class)
class AddEditNoteAiChatInputUiTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUpActivityWindow() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            KeyboardInsetsPolicy.applySoftInputMode(activity.window)
        }
    }

    @Test
    fun aiChatInput_isDisplayed_whenShowAiChatTrue() {
        setProductionLikeLayout(showAiChat = true)
        composeRule.onNodeWithTag(AiChatInputTestTags.BAR).assertIsDisplayed()
        composeRule.onNodeWithTag(AiChatInputTestTags.ACTIONS).assertIsDisplayed()
        composeRule.onNodeWithTag(AiChatInputTestTags.FIELD).assertIsDisplayed()
    }

    @Test
    fun aiChatInput_isHidden_whenShowAiChatFalse() {
        setProductionLikeLayout(showAiChat = false)
        composeRule.onNodeWithTag(AiChatInputTestTags.BAR).assertIsNotDisplayed()
    }

    @Test
    fun aiChatInput_movesUp_whenFocused_productionLayout() {
        setProductionLikeLayout(showAiChat = true)
        val metrics = focusAndMeasure()
        logMetrics("production_bottom_bar", metrics)

        assertTrue(
            "Input should move up when keyboard opens, upward=${metrics.upwardMovePx}px",
            metrics.upwardMovePx >= MIN_UPWARD_MOVE_PX,
        )
        assertTrue(
            "Gap above keyboard too large: ${metrics.gapAboveKeyboardPx}px",
            metrics.gapAboveKeyboardPx <= EXCESSIVE_GAP_ABOVE_KEYBOARD_PX,
        )
    }

    @Test
    fun aiChatInput_withoutImePadding_staysNearBottom_whenFocused_onApi30Plus() {
        assumeTrue(
            "Legacy adjustResize moves the bar without imePadding",
            !KeyboardInsetsPolicy.usesLegacyAdjustResize,
        )
        setProductionLikeLayout(showAiChat = true, useImePadding = false)
        val metrics = focusAndMeasure(waitForUpwardMove = false)
        logMetrics("without_ime_padding", metrics)
        assertTrue(
            "Without imePadding bar should barely move (${metrics.upwardMovePx}px)",
            metrics.upwardMovePx < MIN_UPWARD_MOVE_PX,
        )
    }

    private fun setProductionLikeLayout(showAiChat: Boolean, useImePadding: Boolean = true) {
        composeRule.setContent {
            RadiantDaysTheme(dynamicColor = false) {
                ProductionLikeAiChatScaffold(
                    showAiChat = showAiChat,
                    useImePadding = useImePadding,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun focusAndMeasure(waitForUpwardMove: Boolean = true): FocusMetrics {
        val displayHeightPx = composeRule.activity.resources.displayMetrics.heightPixels.toFloat()
        val barBottomBefore = barBottomPx()
        composeRule.onNodeWithTag(AiChatInputTestTags.FIELD).performClick()
        showKeyboardExplicitly()

        if (waitForUpwardMove) {
            composeRule.waitUntil(timeoutMillis = 6_000L) {
                barBottomPx() < barBottomBefore - MIN_UPWARD_MOVE_PX
            }
        } else {
            composeRule.waitForIdle()
        }
        composeRule.waitForIdle()

        val barBottomAfter = barBottomPx()
        val upwardMove = barBottomBefore - barBottomAfter
        val keyboardHeightEstimate = upwardMove.coerceAtLeast(displayHeightPx * 0.15f)
        val keyboardTopPx = displayHeightPx - keyboardHeightEstimate

        return FocusMetrics(
            displayHeightPx = displayHeightPx,
            barBottomBeforePx = barBottomBefore,
            barBottomPx = barBottomAfter,
            upwardMovePx = upwardMove,
            gapAboveKeyboardPx = keyboardTopPx - barBottomAfter,
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

    private fun logMetrics(tag: String, metrics: FocusMetrics) {
        Log.i(
            METRICS_TAG,
            JSONObject(
                mapOf(
                    "tag" to tag,
                    "displayHeightPx" to metrics.displayHeightPx,
                    "barBottomBeforePx" to metrics.barBottomBeforePx,
                    "barBottomPx" to metrics.barBottomPx,
                    "upwardMovePx" to metrics.upwardMovePx,
                    "gapAboveKeyboardPx" to metrics.gapAboveKeyboardPx,
                ),
            ).toString(),
        )
    }

    private data class FocusMetrics(
        val displayHeightPx: Float,
        val barBottomBeforePx: Float,
        val barBottomPx: Float,
        val upwardMovePx: Float,
        val gapAboveKeyboardPx: Float,
    )

    private companion object {
        const val METRICS_TAG = "AiChatUiTest"
        const val MIN_UPWARD_MOVE_PX = 80f
        const val KEYBOARD_TOP_MAX_FRACTION = 0.72f
        const val EXCESSIVE_GAP_ABOVE_KEYBOARD_PX = 120f
    }
}

@androidx.compose.runtime.Composable
private fun ProductionLikeAiChatScaffold(
    showAiChat: Boolean,
    useImePadding: Boolean,
) {
    Scaffold(
        bottomBar = {
            if (showAiChat) {
                val inputModifier = KeyboardInsetsPolicy.aiChatInputBarModifier(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    useImePadding = useImePadding,
                )
                InlineAiChatInput(
                    loading = false,
                    onSend = {},
                    onAiActionsClick = {},
                    modifier = inputModifier,
                )
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("editor_content"),
        )
    }
}
