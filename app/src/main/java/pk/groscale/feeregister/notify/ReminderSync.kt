package pk.groscale.feeregister.notify

import android.content.Context
import pk.groscale.feeregister.TutorLinkApp
import pk.groscale.feeregister.data.prefs.Profile
import pk.groscale.feeregister.domain.ReminderBatch
import pk.groscale.feeregister.domain.Reminders
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Works out when each reminder should next fire and books the alarm.
 *
 * Called from three places, all of them cheap and idempotent: when the app
 * resumes, when a reminder has just fired, and after a reboot. Rebooking rather
 * than tracking state is what makes a batch added this afternoon, or a toggle
 * switched off, take effect without any bookkeeping to get wrong.
 */
object ReminderSync {

    suspend fun rescheduleAll(context: Context) {
        val app = context.applicationContext as? TutorLinkApp ?: return
        val profile = app.profileStore.profile.first()
        val now = LocalDateTime.now()

        scheduleAttendance(context, app, profile, now)
        scheduleFeeDay(context, app, profile, now)
    }

    private suspend fun scheduleAttendance(
        context: Context,
        app: TutorLinkApp,
        profile: Profile,
        now: LocalDateTime,
    ) {
        if (!profile.attendanceReminder) {
            ReminderScheduler.cancelAttendance(context)
            return
        }
        ReminderScheduler.scheduleAttendance(context, Reminders.nextAttendanceAt(app.reminderBatches(), now))
    }

    private suspend fun scheduleFeeDay(
        context: Context,
        app: TutorLinkApp,
        profile: Profile,
        now: LocalDateTime,
    ) {
        if (!profile.feeReminder) {
            ReminderScheduler.cancelFeeDay(context)
            return
        }
        val dates = app.db.reports().dueDatesFrom(now.toLocalDate())
        ReminderScheduler.scheduleFeeDay(
            context,
            Reminders.nextFeeDayAt(dates, now, profile.feeReminderHour),
        )
    }

    suspend fun TutorLinkApp.reminderBatches(): List<ReminderBatch> =
        db.batches().allActive().map {
            ReminderBatch(
                batchId = it.id,
                name = it.name,
                daysOfWeekMask = it.daysOfWeekMask,
                startMinute = it.startMinute,
            )
        }

    /** Everything owed as of today, for the line a reminder carries. */
    suspend fun pendingToday(app: TutorLinkApp, today: LocalDate) =
        app.db.reports().pendingAsOf(today)
}
