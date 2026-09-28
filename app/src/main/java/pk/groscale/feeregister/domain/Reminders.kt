package pk.groscale.feeregister.domain

import java.time.LocalDate
import java.time.LocalDateTime

/** A batch, as far as the attendance reminder cares. */
data class ReminderBatch(
    val batchId: Long,
    val name: String,
    val daysOfWeekMask: Int,
    val startMinute: Int,
)

/**
 * When the next reminder is due.
 *
 * Pure, so "does it fire on a Sunday?" and "does it skip to next week?" are unit
 * tests rather than a wait until Sunday. The Android side owns only the alarm
 * and the notification; every decision about *when* is decided here.
 *
 * One alarm is kept in flight at a time, for each of the two kinds. When it
 * fires the receiver asks for the next one, so a batch added or removed today is
 * honoured from the next reminder onwards without any bookkeeping.
 */
object Reminders {

    /**
     * How far ahead to look. A batch that meets at all meets weekly, so eight
     * days always contains its next sitting and the loop cannot run away when a
     * batch is saved with no days ticked.
     */
    private const val SEARCH_DAYS = 8L

    /**
     * The next moment any of [batches] starts, strictly after [after].
     *
     * Strictly, so that a reminder firing at 4:00 cannot immediately reschedule
     * itself for the same 4:00 and loop.
     */
    fun nextAttendanceAt(batches: List<ReminderBatch>, after: LocalDateTime): LocalDateTime? {
        var soonest: LocalDateTime? = null
        for (batch in batches) {
            var day = after.toLocalDate()
            val stopBefore = day.plusDays(SEARCH_DAYS)
            while (day.isBefore(stopBefore)) {
                if (batch.daysOfWeekMask.includes(day.dayOfWeek)) {
                    val at = day.atStartOfDay().plusMinutes(batch.startMinute.toLong())
                    if (at.isAfter(after) && (soonest == null || at.isBefore(soonest))) soonest = at
                }
                day = day.plusDays(1)
            }
        }
        return soonest
    }

    /**
     * The batches sitting at exactly [at] - what the reminder that just fired is
     * about. Two batches at the same hour are named in one notification rather
     * than buzzing the phone twice.
     */
    fun batchesStartingAt(batches: List<ReminderBatch>, at: LocalDateTime): List<ReminderBatch> =
        batches.filter {
            it.daysOfWeekMask.includes(at.dayOfWeek) &&
                it.startMinute == at.toLocalTime().toSecondOfDay() / 60
        }

    /**
     * The next fee due date, at [hour] on the morning it falls.
     *
     * Fee day is whenever a bill's own due date lands, which differs per student
     * once anniversary cycles are in use - so this takes the dates rather than
     * assuming the 10th.
     */
    fun nextFeeDayAt(dueDates: List<LocalDate>, after: LocalDateTime, hour: Int): LocalDateTime? =
        dueDates.asSequence()
            .map { it.atStartOfDay().plusHours(hour.toLong()) }
            .filter { it.isAfter(after) }
            .minOrNull()
}
