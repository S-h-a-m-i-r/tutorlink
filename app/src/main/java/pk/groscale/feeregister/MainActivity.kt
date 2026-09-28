package pk.groscale.feeregister

import android.os.Bundle
import android.os.SystemClock
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
        // A short fade rather than the system's cut. The splash ground is the same
        // tint as the home header, so the splash melts into the app. It waits for
        // the quill's dip to finish (at most its 800ms) - the app is usually ready
        // sooner, and cutting the gesture halfway looks like a glitch. Below API 31
        // there is no icon animation and the remaining time is zero.
        installSplashScreen().setOnExitAnimationListener { splash ->
            val iconEndsAt = splash.iconAnimationStartMillis + splash.iconAnimationDurationMillis
            val wait = (iconEndsAt - SystemClock.uptimeMillis()).coerceIn(0L, 800L)
            splash.view.animate()
                .alpha(0f)
                .setStartDelay(wait)
                .setDuration(220L)
                .withEndAction { splash.remove() }
                .start()
        }
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
