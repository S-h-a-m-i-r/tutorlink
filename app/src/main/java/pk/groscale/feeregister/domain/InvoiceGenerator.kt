package pk.groscale.feeregister.domain

import java.time.LocalDate

/** Everything the generator needs about one enrollment. No Android, no DB. */
data class EnrollmentInput(
    val enrollmentId: Long,
    val batchDaysOfWeekMask: Int,
    val fee: Int,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val anchor: BillingAnchor,
    val mergeStubIntoNext: Boolean,
    val holidays: Set<LocalDate>,
)

/** One bill the generator decided should exist. */
data class GeneratedInvoice(
    val enrollmentId: Long,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val dueDate: LocalDate,
    val feeSnapshot: Int,
    val classDaysInPeriod: Int,
    val classDaysEnrolled: Int,
    val amountDue: Int,
)

/**
 * Decides which invoices should exist, up to [today]. Pure, so the rules are
 * testable without a database.
 *
 * Callers insert the result with INSERT OR IGNORE against the unique
 * (enrollmentId, periodStart) index, which is what makes running this on every
 * app open both cheap and impossible to double-bill with.
 */
object InvoiceGenerator {

    fun generate(input: EnrollmentInput, today: LocalDate, graceDays: Int, roundingStep: Int): List<GeneratedInvoice> {
        val periods = Periods.generate(
            anchor = input.anchor,
            startDate = input.startDate,
            endDate = input.endDate,
            graceDays = graceDays,
            upTo = today,
            mergeStubIntoNext = input.mergeStubIntoNext,
        )

        return periods.mapNotNull { period ->
            // The whole month or cycle, never the billed slice - see [BillingPeriod].
            val inPeriod = FeeCalculator.classDays(
                input.batchDaysOfWeekMask, period.wholeStart, period.wholeEnd, input.holidays,
            )
            val enrolledFrom = maxOf(period.start, input.startDate)
            val enrolledTo = minOf(period.end, input.endDate ?: period.end)
            val enrolled = FeeCalculator.classDays(
                input.batchDaysOfWeekMask, enrolledFrom, enrolledTo, input.holidays,
            )

            // Nothing was taught to this student in this period: no bill at all,
            // rather than a bill for zero. A zero-rupee row is noise the teacher
            // would have to read past every month.
            if (enrolled <= 0) return@mapNotNull null

            GeneratedInvoice(
                enrollmentId = input.enrollmentId,
                periodStart = period.start,
                periodEnd = period.end,
                dueDate = period.dueDate,
                feeSnapshot = input.fee,
                classDaysInPeriod = inPeriod,
                classDaysEnrolled = enrolled,
                amountDue = FeeCalculator.amountDue(input.fee, inPeriod, enrolled, roundingStep),
            )
        }
    }
}
