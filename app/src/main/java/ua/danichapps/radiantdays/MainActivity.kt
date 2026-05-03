package ua.danichapps.radiantdays

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ua.danichapps.radiantdays.ui.navigation.AppNavigation
import ua.danichapps.radiantdays.ui.theme.RadiantDaysTheme

/**
 * Single-activity host.
 *
 * All navigation is handled by [AppNavigation] (Compose Navigation).
 * No business logic lives here вЂ” the Activity is only a thin container.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RadiantDaysTheme {
                AppNavigation()
            }
        }
    }
}
