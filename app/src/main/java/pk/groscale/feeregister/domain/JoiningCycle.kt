package pk.groscale.feeregister.domain

import java.time.LocalDate

/**
 * What the parents of a mid-month joiner actually agreed to, per v1-pitch.md
 * section 3: "the teacher chooses, per student".
 *
 * Stored as the two columns the generator reads, [anchor] and
 * [mergeStubIntoNext]. Choosing through this enum rather than setting those two
 * separately is what keeps the meaningless fourth combination - an anniversary
 * cycle carrying a stub to merge - from ever reaching the database.
 */
enum class JoiningCycle(val anchor: BillingAnchor, val mergeStubIntoNext: Boolean) {

    /** 18-30 Sep billed and collected now, then the 1st-of-month cycle. */
    PART_MONTH_NOW(BillingAnchor.CALENDAR, mergeStubIntoNext = false),

    /**
     * The same two bills - so the teacher is owed exactly the same money - but
     * the part month is collected together with the next one. Asking a parent
     * for an odd amount twelve days before the real bill is what makes him feel
     * chased; this is the answer to that, and it costs the teacher nothing.
     */
    PART_MONTH_WITH_NEXT(BillingAnchor.CALENDAR, mergeStubIntoNext = true),

    /** The 18th to the 18th, for ever. Always a whole cycle, so never prorated. */
    OWN_DATE(BillingAnchor.JOINING_DATE, mergeStubIntoNext = false),
    ;

    companion object {
        /** Reads back the cycle an existing enrollment was set up with. */
        fun of(anchor: BillingAnchor, mergeStubIntoNext: Boolean): JoiningCycle =
            entries.firstOrNull { it.anchor == anchor && it.mergeStubIntoNext == mergeStubIntoNext }
                ?: PART_MONTH_NOW

        /**
         * Joining on the 1st, all three cycles bill the identical thing for ever,
         * so there is nothing to ask and the add-student form stays four fields.
         */
        fun isAskedFor(startDate: LocalDate): Boolean = startDate.dayOfMonth != 1
    }
}

/**
 * The first two bills this student will really receive under [cycle] - what the
 * teacher reads out to the parent before either of them agrees to anything.
 *
 * Runs the real generator against a clock one month ahead rather than
 * re-deriving the arithmetic, so the quote cannot drift from what the app bills
 * afterwards. One month ahead is exactly two periods under either anchor.
 */
fun previewBills(
    cycle: JoiningCycle,
    startDate: LocalDate,
    daysOfWeekMask: Int,
    fee: Int,
    graceDays: Int,
    roundingStep: Int,
    holidays: Set<LocalDate> = emptySet(),
): List<GeneratedInvoice> = InvoiceGenerator.generate(
    input = EnrollmentInput(
        enrollmentId = 0,
        batchDaysOfWeekMask = daysOfWeekMask,
        fee = fee,
        startDate = startDate,
        endDate = null,
        anchor = cycle.anchor,
        mergeStubIntoNext = cycle.mergeStubIntoNext,
        holidays = holidays,
    ),
    today = startDate.plusMonths(1),
    graceDays = graceDays,
    roundingStep = roundingStep,
)
