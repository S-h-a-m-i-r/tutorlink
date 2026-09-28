package pk.groscale.feeregister.domain

import java.time.LocalDate

/** An existing bill, as far as re-pricing cares. */
data class Repriceable(
    val invoiceId: Long,
    val periodEnd: LocalDate,
    val feeSnapshot: Int,
    val classDaysInPeriod: Int,
    val classDaysEnrolled: Int,
    val paidSoFar: Int,
)

/** A bill the fee change is allowed to reach, at its corrected price. */
data class Repriced(val invoiceId: Long, val feeSnapshot: Int, val amountDue: Int)

/**
 * Which already-generated bills a fee change may correct. Pure, like every other
 * rule that moves money.
 *
 * Two conditions, and they are the whole of it:
 *
 *  1. **The period is still running** (`periodEnd` is today or later). A closed
 *     month was billed at the rate that applied then, and arrears for it must
 *     stay at that rate - otherwise raising the fee in October silently inflates
 *     what a parent owes for August. Phrasing the test on `periodEnd` rather
 *     than on a month boundary means it holds for both anchors: a calendar month
 *     and an 18-to-18 cycle are each "still running" while their end is ahead.
 *  2. **Nothing has been received against it** (`paidSoFar == 0`). Once a payment
 *     is taken a receipt has gone to the parent with that fee printed on it, and
 *     the receipt must stay true. A change after that goes on the next bill.
 *
 * So a fee typed wrong and corrected the same month lands where the teacher
 * expects - on the bill in front of him - while a mid-year rise still only
 * touches what has not been settled.
 *
 * Day counts come from the invoice's own snapshots, so a prorated mid-month stub
 * stays prorated at the new fee.
 */
fun reprice(
    invoices: List<Repriceable>,
    newFee: Int,
    today: LocalDate,
    roundingStep: Int,
): List<Repriced> = invoices
    .filter { it.feeSnapshot != newFee && it.paidSoFar == 0 && !it.periodEnd.isBefore(today) }
    .map {
        Repriced(
            invoiceId = it.invoiceId,
            feeSnapshot = newFee,
            amountDue = FeeCalculator.amountDue(
                feeSnapshot = newFee,
                classDaysInPeriod = it.classDaysInPeriod,
                classDaysEnrolled = it.classDaysEnrolled,
                roundingStep = roundingStep,
            ),
        )
    }
