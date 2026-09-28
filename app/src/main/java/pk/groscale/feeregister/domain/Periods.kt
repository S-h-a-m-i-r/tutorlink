package pk.groscale.feeregister.domain

import java.time.LocalDate

enum class BillingAnchor { CALENDAR, JOINING_DATE }

data class BillingPeriod(
    /** What is billed. Starts on the joining date for a mid-month join. */
    val start: LocalDate,
    /** Ends early when the student leaves part-way through. */
    val end: LocalDate,
    val dueDate: LocalDate,
    /** True for the short first period created by a mid-month join. */
    val isStub: Boolean = false,
    /**
     * The whole period that [start]..[end] is a slice of - the calendar month, or
     * the full anniversary cycle - and therefore the denominator the proration is
     * done against.
     *
     * It has to be carried separately, because a slice measured against itself is
     * always a whole one: bill 18-30 September against 18-30 September and the
     * student has been enrolled for "all" of it, so rule 2 hands him a full
     * month's fee for thirteen days of teaching. The parent gets that bill and
     * another one twelve days later.
     */
    val wholeStart: LocalDate = start,
    val wholeEnd: LocalDate = end,
)

/**
 * Period generation for the two anchors, per data-model.md.
 *
 * Never generates a future period: the teacher must not see a bill for next month.
 */
object Periods {

    fun generate(
        anchor: BillingAnchor,
        startDate: LocalDate,
        endDate: LocalDate?,
        graceDays: Int,
        upTo: LocalDate,
        mergeStubIntoNext: Boolean = false,
    ): List<BillingPeriod> = when (anchor) {
        BillingAnchor.CALENDAR -> calendar(startDate, endDate, graceDays, upTo, mergeStubIntoNext)
        BillingAnchor.JOINING_DATE -> joining(startDate, endDate, graceDays, upTo)
    }

    /**
     * Calendar months, due on the 1st-10th. A mid-month join produces a short
     * first period (the "stub"), prorated by class days.
     *
     * `mergeStubIntoNext` is a DUE-DATE rule only: two invoices still exist, so
     * each can explain itself and arrears stay correct. It just means the teacher
     * collects the stub together with the next month.
     */
    private fun calendar(
        startDate: LocalDate,
        endDate: LocalDate?,
        graceDays: Int,
        upTo: LocalDate,
        mergeStubIntoNext: Boolean,
    ): List<BillingPeriod> {
        val out = mutableListOf<BillingPeriod>()
        var cursor = startDate

        while (!cursor.isAfter(upTo)) {
            val monthStart = cursor.withDayOfMonth(1)
            val monthEnd = cursor.withDayOfMonth(cursor.lengthOfMonth())
            val periodEnd = minOf(monthEnd, endDate ?: monthEnd)
            val isStub = cursor.dayOfMonth != 1
            val due = cursor.plusDays((graceDays - 1).toLong())
            // The month is the denominator whichever end the slice is taken off:
            // a join on the 18th and a leaving on the 14th both prorate against
            // the whole of it.
            out += BillingPeriod(cursor, periodEnd, due, isStub, monthStart, monthEnd)

            if (endDate != null && !periodEnd.isBefore(endDate)) break
            cursor = monthEnd.plusDays(1)
        }

        if (mergeStubIntoNext && out.size >= 2 && out[0].isStub) {
            out[0] = out[0].copy(dueDate = out[1].dueDate)
        }
        return out
    }

    /**
     * Anniversary cycles: 18 Jun - 17 Jul, 18 Jul - 17 Aug, and so on. Always a
     * whole cycle, so proration never applies except to a final part-period when
     * the student leaves mid-cycle.
     *
     * plusMonths clamps month-ends correctly: 31 Jan -> 28 Feb.
     */
    private fun joining(
        startDate: LocalDate,
        endDate: LocalDate?,
        graceDays: Int,
        upTo: LocalDate,
    ): List<BillingPeriod> {
        val out = mutableListOf<BillingPeriod>()
        var n = 0L
        while (true) {
            val start = startDate.plusMonths(n)
            if (start.isAfter(upTo)) break
            val naturalEnd = startDate.plusMonths(n + 1).minusDays(1)
            val end = minOf(naturalEnd, endDate ?: naturalEnd)
            // A whole cycle by construction, so [wholeEnd] only differs from
            // [end] for the final part-cycle when the student leaves mid-month.
            out += BillingPeriod(
                start = start,
                end = end,
                dueDate = start.plusDays((graceDays - 1).toLong()),
                wholeStart = start,
                wholeEnd = naturalEnd,
            )
            if (endDate != null && !end.isBefore(endDate)) break
            n++
        }
        return out
    }
}
