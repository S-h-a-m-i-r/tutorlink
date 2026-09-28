package pk.groscale.feeregister.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Keeps at most one alarm in flight per reminder kind.
 *
 * **Deliberately inexact.** `setAndAllowWhileIdle` needs no permission at all,
 * where an exact alarm on Android 12+ means `SCHEDULE_EXACT_ALARM` - a
 * declaration form and a common rejection (play-store-policy.md section 1). A
 * reminder that lands a few minutes after the class bell is worth exactly as
 * much as one that lands on it, so there is nothing to buy with that risk.
 *
 * Each alarm carries the moment it was scheduled for, so the receiver can say
 * which batch it is about without guessing from a clock that has since moved on.
 */
object ReminderScheduler {

    const val ACTION_ATTENDANCE = "pk.groscale.feeregister.REMIND_ATTENDANCE"
    const val ACTION_FEE_DAY = "pk.groscale.feeregister.REMIND_FEE_DAY"
    const val EXTRA_SCHEDULED_FOR = "scheduledFor"

    private const val REQ_ATTENDANCE = 2001
    private const val REQ_FEE_DAY = 2002

    fun scheduleAttendance(context: Context, at: LocalDateTime?) =
        set(context, ACTION_ATTENDANCE, REQ_ATTENDANCE, at)

    fun scheduleFeeDay(context: Context, at: LocalDateTime?) =
        set(context, ACTION_FEE_DAY, REQ_FEE_DAY, at)

    fun cancelAttendance(context: Context) = set(context, ACTION_ATTENDANCE, REQ_ATTENDANCE, null)

    fun cancelFeeDay(context: Context) = set(context, ACTION_FEE_DAY, REQ_FEE_DAY, null)

    /** A null [at] cancels: one path for "no next one" and "switched off". */
    private fun set(context: Context, action: String, requestCode: Int, at: LocalDateTime?) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val app = context.applicationContext

        if (at == null) {
            pendingIntent(app, action, requestCode, 0L, PendingIntent.FLAG_NO_CREATE)
                ?.let { alarms.cancel(it); it.cancel() }
            return
        }

        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = pendingIntent(app, action, requestCode, millis, PendingIntent.FLAG_UPDATE_CURRENT)
            ?: return
        runCatching { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intent) }
    }

    private fun pendingIntent(
        context: Context,
        action: String,
        requestCode: Int,
        scheduledFor: Long,
        flags: Int,
    ): PendingIntent? = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, ReminderReceiver::class.java)
            .setAction(action)
            .putExtra(EXTRA_SCHEDULED_FOR, scheduledFor),
        flags or PendingIntent.FLAG_IMMUTABLE,
    )
}
