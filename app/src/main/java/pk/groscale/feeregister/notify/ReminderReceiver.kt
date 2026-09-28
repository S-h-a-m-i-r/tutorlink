package pk.groscale.feeregister.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pk.groscale.feeregister.TutorLinkApp
import pk.groscale.feeregister.data.repo.FeeRepository
import pk.groscale.feeregister.domain.Reminders
import pk.groscale.feeregister.notify.ReminderSync.reminderBatches
import pk.groscale.feeregister.util.safely
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Posts a reminder and books the next one.
 *
 * Also generates any bills that have come due. A teacher who has not opened the
 * app since the 28th would otherwise be told on the 1st that nothing is pending,
 * because generation is lazy and nothing has run - which is exactly the person
 * these reminders exist for. Generation is idempotent, so doing it here costs a
 * query and can never double-bill.
 *
 * Unexported: reachable only by this app's own alarm PendingIntents. Surviving a
 * reboot is [BootReceiver]'s job, because that one has to be exported.
 */
class ReminderReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? TutorLinkApp ?: return
        val action = intent.action ?: return
        val scheduledFor = intent.getLongExtra(ReminderScheduler.EXTRA_SCHEDULED_FOR, 0L)

        // onReceive is main-thread and short-lived; this keeps the process alive
        // while the database work finishes.
        val done = goAsync()
        scope.launch {
            try {
                safely("reminder") { handle(app, action, scheduledFor) }
                safely("reschedule reminders") { ReminderSync.rescheduleAll(app) }
            } finally {
                done.finish()
            }
        }
    }

    private suspend fun handle(app: TutorLinkApp, action: String, scheduledFor: Long) {
        val profile = app.profileStore.profile.first()
        val today = LocalDate.now()

        // Bring the register up to date before saying anything about it.
        FeeRepository(app.db, app.profileStore).generateInvoices(today)

        val pending = ReminderSync.pendingToday(app, today)

        when (action) {
            ReminderScheduler.ACTION_ATTENDANCE -> {
                if (!profile.attendanceReminder) return
                val at = scheduledFor.asLocalDateTime() ?: LocalDateTime.now()
                val sitting = Reminders.batchesStartingAt(app.reminderBatches(), at)
                ReminderNotifications.attendance(
                    context = app,
                    batchNames = sitting.map { it.name },
                    pending = pending.amount,
                    pendingStudents = pending.students,
                )
            }

            ReminderScheduler.ACTION_FEE_DAY -> {
                if (!profile.feeReminder) return
                val day = scheduledFor.asLocalDateTime()?.toLocalDate() ?: today
                ReminderNotifications.feeDay(
                    context = app,
                    studentCount = app.db.reports().studentsDueOn(day),
                    pending = pending.amount,
                )
            }
        }
    }

    private fun Long.asLocalDateTime(): LocalDateTime? =
        if (this <= 0L) null
        else LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
}
