package pk.groscale.feeregister.domain

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * The fee rules, as specified in data-model.md. Pure - no Android types, no DB,
 * no clock. This is the one piece of logic where a mistake costs real money for a
 * real teacher, so it is isolated and fully tested.
 */
object FeeCalculator {

    /**
     * Rules, in order:
     *  1. No class days in the period -> nothing to bill.
     *  2. Enrolled for the whole period -> exactly the fee, unrounded. A full
     *     month is always the full fee, whether that month had 22 class days or
     *     27, and regardless of holidays or absences. This is what stops the
     *     teacher having to explain a moving bill to a parent.
     *  3. Part of a period -> prorate on CLASS days (not calendar days), rounded.
     *
     * Student absences never reduce the amount. The seat is sold, not the day.
     * Relief is a deliberate `adjustment` on the invoice, never an automatic deduction.
     */
    fun amountDue(
        feeSnapshot: Int,
        classDaysInPeriod: Int,
        classDaysEnrolled: Int,
        roundingStep: Int = 50,
    ): Int {
        if (classDaysInPeriod <= 0) return 0
        if (classDaysEnrolled >= classDaysInPeriod) return feeSnapshot
        if (classDaysEnrolled <= 0) return 0
        return roundToStep(feeSnapshot.toDouble() * classDaysEnrolled / classDaysInPeriod, roundingStep)
    }

    /**
     * Class days in [from]..[to] inclusive: dates whose day-of-week is in the
     * batch's mask and which are not holidays.
     */
    fun classDays(
        daysOfWeekMask: Int,
        from: LocalDate,
        to: LocalDate,
        holidays: Set<LocalDate> = emptySet(),
    ): Int {
        if (from.isAfter(to)) return 0
        var count = 0
        var d = from
        while (!d.isAfter(to)) {
            if (daysOfWeekMask.includes(d.dayOfWeek) && d !in holidays) count++
            d = d.plusDays(1)
        }
        return count
    }
}

/** Mon = bit 0 ... Sun = bit 6, matching java.time's Mon..Sun ordering. */
fun Int.includes(day: DayOfWeek): Boolean = (this shr (day.value - 1)) and 1 == 1

fun daysOfWeekMaskOf(vararg days: DayOfWeek): Int =
    days.fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }

/** Mon-Sat, the common shape for a home tuition batch. */
val MON_TO_SAT: Int = daysOfWeekMaskOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
)
