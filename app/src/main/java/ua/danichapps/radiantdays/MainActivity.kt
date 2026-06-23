package ua.danichapps.radiantdays

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import ua.danichapps.radiantdays.notification.ReminderContract
import ua.danichapps.radiantdays.ui.common.KeyboardInsetsPolicy
import ua.danichapps.radiantdays.ui.navigation.AppNavigation
import ua.danichapps.radiantdays.ui.theme.AppThemeMode
import ua.danichapps.radiantdays.ui.theme.AppThemeStore
import ua.danichapps.radiantdays.ui.theme.RadiantDaysTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeyboardInsetsPolicy.applySoftInputMode(window)
        enableEdgeToEdge()
        setContent {
            val themeStore: AppThemeStore = koinInject()
            val themeMode by themeStore.mode.collectAsStateWithLifecycle()
            val systemInDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                AppThemeMode.SYSTEM -> systemInDarkTheme
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            RadiantDaysTheme(darkTheme = darkTheme) {
                AppNavigation(
                    pendingEditEventId = consumePendingEditEventId(),
                    onPendingEditEventConsumed = { clearPendingEditEventId() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val eventId = intent.getLongExtra(ReminderContract.EXTRA_EVENT_ID, -1L)
        if (eventId >= 0L) {
            recreate()
        }
    }

    private fun consumePendingEditEventId(): Long? {
        val eventId = intent.getLongExtra(ReminderContract.EXTRA_EVENT_ID, -1L)
        return eventId.takeIf { it >= 0L }
    }

    private fun clearPendingEditEventId() {
        intent.removeExtra(ReminderContract.EXTRA_EVENT_ID)
    }
}
