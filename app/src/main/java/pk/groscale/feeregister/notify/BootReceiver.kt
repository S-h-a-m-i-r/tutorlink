package pk.groscale.feeregister.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import pk.groscale.feeregister.util.safely

/**
 * Books the reminders again after a restart or an update.
 *
 * Alarms do not survive either, and a reminder that quietly stops the first time
 * the phone reboots is worse than no reminder - the teacher trusts it and it is
 * not there.
 *
 * Separate from [ReminderReceiver] on purpose. This one has to be exported to
 * hear the system's boot broadcast, so it is kept to the one harmless action;
 * the receiver that actually posts notifications stays unexported and reachable
 * only by this app's own alarms. Both boot and package-replaced are protected
 * broadcasts, so only the system can send them.
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> Unit
            else -> return
        }
        val app = context.applicationContext
        val done = goAsync()
        scope.launch {
            try {
                safely("reschedule reminders after boot") { ReminderSync.rescheduleAll(app) }
            } finally {
                done.finish()
            }
        }
    }
}
