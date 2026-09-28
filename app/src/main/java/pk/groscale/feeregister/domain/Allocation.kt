package pk.groscale.feeregister.domain

/** An invoice as far as allocation cares: what is owed, and what is already paid. */
data class Payable(
    val invoiceId: Long,
    val net: Int,
    val alreadyPaid: Int,
) {
    val outstanding: Int get() = (net - alreadyPaid).coerceAtLeast(0)
}

data class Allocated(val invoiceId: Long, val amount: Int)

data class AllocationResult(val allocations: List<Allocated>, val unallocated: Int)

/**
 * Oldest invoice first. This single rule is why partial payments, sibling
 * combined payments and arrears are one code path instead of three special cases.
 *
 * [payables] must already be ordered oldest first.
 */
fun allocate(amount: Int, payables: List<Payable>): AllocationResult {
    var remaining = amount
    val out = mutableListOf<Allocated>()
    for (p in payables) {
        if (remaining <= 0) break
        if (p.outstanding <= 0) continue
        val take = minOf(remaining, p.outstanding)
        out += Allocated(p.invoiceId, take)
        remaining -= take
    }
    // Anything left over is advance credit, applied when the next bill appears.
    return AllocationResult(out, remaining)
}
