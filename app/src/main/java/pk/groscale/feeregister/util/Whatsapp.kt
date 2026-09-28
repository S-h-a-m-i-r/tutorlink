package pk.groscale.feeregister.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import pk.groscale.feeregister.domain.formatRs

/**
 * Messages are Roman Urdu, not formal Urdu: highest actual comprehension in
 * Pakistan, no font dependency, and it cannot render badly on any phone.
 *
 * Nothing is ever sent automatically. The teacher reads and sends every message
 * himself - a trust decision, and also what keeps the app clear of Play's spam
 * policy.
 */
object Messages {

    fun feeReminder(studentName: String, monthLabel: String, amount: Int, tuitionName: String): String =
        buildString {
            append("Assalam o Alaikum. ")
            append("$studentName ki $monthLabel ki fees ${formatRs(amount)} abhi baqi hai. ")
            append("Meherbani farma kar jama karwa dein. Shukriya.")
            if (tuitionName.isNotBlank()) append("\n— $tuitionName")
        }

    fun overdueReminder(
        studentName: String,
        monthLabel: String,
        amount: Int,
        monthsDue: Int,
        tuitionName: String,
    ): String = buildString {
        append("Assalam o Alaikum. ")
        append("$studentName ki fees $monthsDue mahine se baqi hai. ")
        append("Total ${formatRs(amount)}. ")
        append("Meherbani farma kar jama karwa dein. Shukriya.")
        if (tuitionName.isNotBlank()) append("\n— $tuitionName")
    }

    /**
     * Sent the moment a student is marked absent, if the teacher chooses to.
     *
     * Asks rather than reports: a parent who gets told his son missed class wants
     * to say why, and a teacher who hears back the same evening is the reason
     * this is worth sending at all. Nothing about fees appears here - an absence
     * never changes what is owed, and mixing the two makes it read as a threat.
     *
     * Worded around "ghair haazri" so it is correct for a son or a daughter: the
     * app does not know which, and "nahi aaya" to a girl's parent reads as careless.
     */
    fun absenceNotice(studentName: String, dateLabel: String, tuitionName: String): String =
        buildString {
            append("Assalam o Alaikum. ")
            append("$studentName ki $dateLabel ko class mein ghair haazri thi. ")
            append("Agar koi masla hai to bata dein. Shukriya.")
            if (tuitionName.isNotBlank()) append("\n— $tuitionName")
        }

    fun receipt(studentName: String, monthLabel: String, amount: Int, tuitionName: String): String =
        buildString {
            append("$studentName ki $monthLabel ki fees ${formatRs(amount)} mausool ho gayi hai. Shukriya.")
            if (tuitionName.isNotBlank()) append("\n— $tuitionName")
        }
}

object Whatsapp {

    /**
     * Opens WhatsApp with the message pre-filled, addressed to one parent. The
     * teacher still presses send.
     *
     * Deep links open one chat at a time - there is no bulk send without the
     * Business API - so the caller loops one parent per tap.
     */
    fun openChat(context: Context, phone: String, message: String) {
        val digits = normalise(phone)
        if (digits == null) {
            Toast.makeText(context, "That phone number doesn't look right", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "WhatsApp isn't installed on this phone", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Pakistani numbers to E.164 without the plus, which is what wa.me wants.
     * Handles the three ways a teacher will actually have typed it:
     * 03001234567, 3001234567, +923001234567.
     */
    fun normalise(raw: String): String? {
        val d = raw.filter { it.isDigit() }
        return when {
            d.length == 12 && d.startsWith("92") -> d
            d.length == 11 && d.startsWith("0") -> "92" + d.drop(1)
            d.length == 10 && d.startsWith("3") -> "92$d"
            d.length >= 11 -> d // already international, some other country
            else -> null
        }
    }
}
