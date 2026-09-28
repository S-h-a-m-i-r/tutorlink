package pk.groscale.feeregister.domain

import java.time.LocalDate

/**
 * A bill as the leaving settlement sees it.
 *
 * [classDaysTaught] is counted from the start of the period up to and including
 * the leaving date, so it is what the student actually got - not what he was
 * enrolled for when the bill was written.
 */
data class LeavingInvoice(
    val invoiceId: Long,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val feeSnapshot: Int,
    val classDaysInPeriod: Int,
    val classDaysTaught: Int,
    val amountDue: Int,
    val adjustment: Int,
)

/** A bill the leaving date shortens, and what it becomes. */
data class Proration(
    val invoiceId: Long,
    val periodStart: LocalDate,
    val classDaysTaught: Int,
    val classDaysInPeriod: Int,
    val amountBefore: Int,
    val amountAfter: Int,
    /**
     * Signed and negative. Stored on the invoice's `adjustment`, never by
     * rewriting `amountDue`: the bill must keep saying what the parent was
     * actually shown, with the days-not-taught coming off it as a visible line.
     */
    val adjustment: Int,
)

/**
 * Where a leaving student and his teacher stand with each other, once the last
 * part-month is cut down to the days really taught.
 *
 * One number in one direction or the other, because that is the only question
 * being asked: does he still owe, or is he owed?
 */
data class Settlement(
    val prorations: List<Proration>,
    /** Everything he was ever charged, after the prorations above. */
    val billed: Int,
    /** Everything he ever handed over, advance credit included. */
    val received: Int,
) {
    val duesToCollect: Int get() = (billed - received).coerceAtLeast(0)

    /**
     * What the teacher owes back. Shown to him and to nobody else - it is his
     * call whether to give it, and a parent must never be told by an app that
     * money is coming to him.
     */
    val toGiveBack: Int get() = (received - billed).coerceAtLeast(0)

    val isClear: Boolean get() = billed == received
}

/**
 * Settles a student who is leaving on [leavingOn]. Pure, like every other rule
 * that moves money.
 *
 * design.md section 12 listed the leave-mid-period rule as open - "refund,
 * prorate, or forfeit". This is the prorate answer, and it is the mirror of the
 * mid-month join it was said to be: a part month costs a part fee, counted in
 * class days, whichever end of the month the part is taken off.
 *
 * Two rules keep it fair in both directions:
 *
 *  1. **Only periods still running are cut** (`periodEnd` after the leaving
 *     date). A month he was taught in full stays billed in full, so leaving in
 *     October never rewrites September.
 *  2. **A bill is never raised on the way out.** Proration can only reduce, so a
 *     student cannot be charged more for leaving than for staying.
 *
 * Anything he paid beyond the result - including advance credit sitting
 * unallocated - comes back as [Settlement.toGiveBack].
 */
fun settleLeaving(
    invoices: List<LeavingInvoice>,
    received: Int,
    leavingOn: LocalDate,
    roundingStep: Int,
): Settlement {
    val prorations = invoices.mapNotNull { it.prorate(leavingOn, roundingStep) }
    val cutDown = prorations.associate { it.invoiceId to it.amountAfter }
    val billed = invoices.sumOf { cutDown[it.invoiceId] ?: (it.amountDue + it.adjustment) }
    return Settlement(prorations, billed, received)
}

private fun LeavingInvoice.prorate(leavingOn: LocalDate, roundingStep: Int): Proration? {
    // The period finished before he left, so he was taught all of it.
    if (!periodEnd.isAfter(leavingOn)) return null

    val after = FeeCalculator.amountDue(feeSnapshot, classDaysInPeriod, classDaysTaught, roundingStep)
    val before = amountDue + adjustment
    if (after >= before) return null

    return Proration(
        invoiceId = invoiceId,
        periodStart = periodStart,
        classDaysTaught = classDaysTaught,
        classDaysInPeriod = classDaysInPeriod,
        amountBefore = before,
        amountAfter = after,
        // Against amountDue, not against `before`, so re-running this settles to
        // the same numbers rather than compounding its own discount.
        adjustment = after - amountDue,
    )
}
