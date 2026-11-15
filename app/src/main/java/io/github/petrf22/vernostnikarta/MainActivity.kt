package io.github.petrf22.vernostnikarta

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.petrf22.vernostnikarta.ui.AppNavigation
import io.github.petrf22.vernostnikarta.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private var debugRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        acceptDebugRoute(intent)
        setContent {
            AppTheme {
                AppNavigation(debugRoute = debugRoute, onDebugRouteConsumed = { debugRoute = null })
            }
        }
    }

    // `ui.py open` sends the intent with --activity-single-top, so a running app receives it here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptDebugRoute(intent)
    }

    /**
     * Jump to a screen from ADB (`tools/mobile/ui.py open "edit/0?code=123"`), so the app can be
     * checked in the emulator as text, without clicking through and without screenshots.
     * Read in debuggable builds only — a released app ignores the extra.
     */
    private fun acceptDebugRoute(intent: Intent?) {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE == 0) return
        val route = intent?.getStringExtra(EXTRA_ROUTE)?.takeIf { it.isNotBlank() } ?: return
        intent.removeExtra(EXTRA_ROUTE)
        debugRoute = route
    }

    private companion object {
        const val EXTRA_ROUTE = "route"
    }
}
