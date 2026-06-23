package ua.danichapps.radiantdays.ui.common

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * API < 30: [SOFT_INPUT_ADJUST_RESIZE] so IME insets are dispatched; [imePadding] on the bar.
 * API 30+: [SOFT_INPUT_ADJUST_NOTHING] + [imePadding] on the input bar.
 */
object KeyboardInsetsPolicy {

    val usesLegacyAdjustResize: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.R

    fun applySoftInputMode(window: Window) {
        window.setSoftInputMode(
            if (usesLegacyAdjustResize) {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            } else {
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            },
        )
    }

    /** Enables IME insets in a Compose [androidx.compose.ui.window.Dialog] window. */
    fun configureDialogWindow(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        applySoftInputMode(window)
    }

    /**
     * IME bottom inset in pixels. Works in Activity and Dialog windows where [WindowInsets.ime]
     * may stay zero until [ViewCompat.OnApplyWindowInsetsListener] is attached.
     */
    @Composable
    fun rememberImeBottomPx(): Int {
        val view = LocalView.current
        var imeBottomPx by remember(view) { mutableIntStateOf(0) }

        DisposableEffect(view) {
            ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
                imeBottomPx = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
                insets
            }
            view.requestApplyInsets()
            onDispose {
                ViewCompat.setOnApplyWindowInsetsListener(view, null)
            }
        }

        return imeBottomPx
    }

    fun aiChatInputBarModifier(base: Modifier, useImePadding: Boolean = true): Modifier {
        val withNavBar = base.navigationBarsPadding()
        return if (useImePadding) {
            withNavBar.imePadding()
        } else {
            withNavBar
        }
    }
}
