package pk.groscale.feeregister.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pk.groscale.feeregister.MainActivity
import pk.groscale.feeregister.R
import pk.groscale.feeregister.domain.formatRs

/**
 * The two reminders, and nothing else.
 *
 * Play policy shapes every decision here (play-store-policy.md section 1):
 *
 *  - **Off until asked for.** Both reminders default to false and
 *    POST_NOTIFICATIONS is requested at the moment the teacher turns one on,
 *    never at first launch.
 *  - **Two channels, not one.** He can silence fee reminders and keep attendance
 *    ones from Android's own settings, without uninstalling.
 *  - **Nothing promotional.** Each notification is about his own register and
 *    opens the app at the screen it is talking about.
 *  - **Private on the lock screen.** What a parent owes is not something a
 *    stranger glancing at the teacher's phone should be able to read.
 */
object ReminderNotifications {

    const val CHANNEL_ATTENDANCE = "attendance"
    const val CHANNEL_FEES = "fees"

    private const val ID_ATTENDANCE = 1001
    private const val ID_FEES = 1002

    /**
     * Channels must exist before anything is posted. Creating one twice is a
     * no-op, so this is called on every path that might notify.
     */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ATTENDANCE,
                "Attendance reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "When a batch starts, a reminder to mark who did not come." },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FEES,
                "Fee day reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "On the day a student's fee falls due." },
        )
    }

    /**
     * True once the teacher has allowed notifications. Below Android 13 there is
     * no runtime permission, and the system settings toggle is the only gate.
     */
    fun allowed(context: Context): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }

    /** "Evening Batch starts now." Plus what is owed, if anything is. */
    fun attendance(context: Context, batchNames: List<String>, pending: Int, pendingStudents: Int) {
        if (batchNames.isEmpty()) return
        val title = when (batchNames.size) {
            1 -> "${batchNames.first()} starts now"
            else -> "${batchNames.joinToString(" and ")} start now"
        }
        // One notification does all three jobs the teacher opens the app for:
        // mark who is missing, see what is owed, message a parent. Two competing
        // notifications would train him to swipe both away.
        val body = buildString {
            append("Tap to mark who did not come.")
            if (pending > 0) {
                append(" ${formatRs(pending)} still pending from ")
                append(if (pendingStudents == 1) "1 student." else "$pendingStudents students.")
            }
        }
        post(context, CHANNEL_ATTENDANCE, ID_ATTENDANCE, title, body)
    }

    /** "Fee day for 3 students." */
    fun feeDay(context: Context, studentCount: Int, pending: Int) {
        if (studentCount <= 0) return
        val title = if (studentCount == 1) "Fee day for 1 student" else "Fee day for $studentCount students"
        post(
            context,
            CHANNEL_FEES,
            ID_FEES,
            title,
            "${formatRs(pending)} pending. Tap to see who, and send a reminder.",
        )
    }

    private fun post(context: Context, channel: String, id: Int, title: String, body: String) {
        if (!allowed(context)) return
        ensureChannels(context)

        val open = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            // Amounts and names stay behind the lock screen.
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        // Permission can be revoked between the check above and here; the
        // framework would throw rather than warn.
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }
}
