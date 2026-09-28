package pk.groscale.feeregister.domain

import kotlin.math.roundToInt

/**
 * Money is whole rupees as [Int], everywhere. Never Double: nobody charges paisa,
 * and float drift on someone's fee record is not acceptable.
 */

/** Half-up to the nearest [step]. Applied only when prorating - see [FeeCalculator]. */
fun roundToStep(value: Double, step: Int): Int =
    if (step <= 1) value.roundToInt() else (value / step).roundToInt() * step

/**
 * "Rs 2,500" - space after Rs, comma grouping, never decimals, never "PKR".
 *
 * Grouping is done by hand rather than with NumberFormat so that a device set to
 * another locale cannot change how a receipt reads. Receipts go to parents; they
 * must look identical on every phone.
 */
fun formatRs(amount: Int): String = "Rs " + groupDigits(amount)

fun groupDigits(amount: Int): String {
    val negative = amount < 0
    val digits = kotlin.math.abs(amount).toString()
    val out = StringBuilder()
    for ((i, c) in digits.withIndex()) {
        if (i > 0 && (digits.length - i) % 3 == 0) out.append(',')
        out.append(c)
    }
    return if (negative) "-$out" else out.toString()
}
