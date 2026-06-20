package ua.danichapps.radiantdays.ui.common

import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier

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

    fun aiChatInputBarModifier(base: Modifier, useImePadding: Boolean = true): Modifier {
        val withNavBar = base.navigationBarsPadding()
        return if (useImePadding) {
            withNavBar.imePadding()
        } else {
            withNavBar
        }
    }
}
