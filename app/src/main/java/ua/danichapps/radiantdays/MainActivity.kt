package ua.danichapps.radiantdays

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ua.danichapps.radiantdays.notification.ReminderContract
import ua.danichapps.radiantdays.ui.navigation.AppNavigation
import ua.danichapps.radiantdays.ui.theme.RadiantDaysTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RadiantDaysTheme {
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
