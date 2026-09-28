package pk.groscale.feeregister

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pk.groscale.feeregister.notify.ReminderSync
import pk.groscale.feeregister.util.safely
import pk.groscale.feeregister.data.repo.BackupRepository
import pk.groscale.feeregister.data.repo.FeeRepository
import pk.groscale.feeregister.ui.nav.AppNav
import pk.groscale.feeregister.ui.theme.TutorLinkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // The pale header runs behind the status bar on purpose; HomeScreen pads
        // its own content for the insets. `light` means "the bar sits on a light
        // surface", so the clock and icons render dark and stay readable on the
        // header tint.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ),
        )

        val app = application as TutorLinkApp
        val repo = FeeRepository(app.db, app.profileStore)
        val backups = BackupRepository(app.db, app.profileStore)

        setContent {
            TutorLinkTheme { AppNav(repo, backups) }
        }
    }

    /**
     * Rebooks the reminders on every resume.
     *
     * Cheap, idempotent, and the only thing that keeps them honest after a batch
     * is added, its time changed, or a student's cycle shifts the next fee day.
     * Tracking those events individually would be four places to forget; asking
     * again is one.
     */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            safely("schedule reminders") { ReminderSync.rescheduleAll(this@MainActivity) }
        }
    }
}
