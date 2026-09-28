package pk.groscale.feeregister.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pk.groscale.feeregister.data.db.Arrears
import pk.groscale.feeregister.data.repo.FeeRepository
import pk.groscale.feeregister.ui.components.FeeStatus
import pk.groscale.feeregister.util.ReceiptData
import pk.groscale.feeregister.util.safely
import pk.groscale.feeregister.util.userMessage
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class DueRowUi(
    val invoiceId: Long,
    val studentId: Long,
    val name: String,
    val subtitle: String,
    /** Everything owed as of this month: this month's bill plus [carriedOver]. */
    val outstanding: Int,
    /** Unpaid from months before this one. Never dropped, only ever collected. */
    val carriedOver: Int,
    val status: FeeStatus,
    val monthsDue: Int,
    val guardianPhone: String?,
    val remindedToday: Boolean,
)

data class HomeUiState(
    val monthLabel: String = "",
    /** Everything still owed as of this month, arrears included. */
    val pending: Int = 0,
    val pendingStudents: Int = 0,
    /**
     * [received] and [expected] stay scoped to this month's bills - they are what
     * the collection bar means by "how is this month going". [pending] is the
     * wider number, so the two differ exactly by [broughtForward].
     */
    val received: Int = 0,
    val expected: Int = 0,
    val broughtForward: Int = 0,
    val unpaid: List<DueRowUi> = emptyList(),
    val paid: List<DueRowUi> = emptyList(),
    val hasStudents: Boolean = true,
    val standing: MonthStanding = MonthStanding.CURRENT,
    val tuitionName: String = "",
    val loading: Boolean = true,
    val saving: Boolean = false,
) {
    /** The next parent to message. Reminders are one at a time - see [nextToRemind]. */
    val nextToRemind: DueRowUi?
        get() = unpaid.firstOrNull { !it.remindedToday && !it.guardianPhone.isNullOrBlank() }

    val remainingToRemind: Int
        get() = unpaid.count { !it.remindedToday && !it.guardianPhone.isNullOrBlank() }
}

/** Where the month being looked at sits relative to today. */
enum class MonthStanding { PAST, CURRENT, FUTURE }

sealed interface HomeEvent {
    data class Message(val text: String) : HomeEvent
    data class OfferReceipt(val data: ReceiptData) : HomeEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(private val repo: FeeRepository) : ViewModel() {

    private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val month = MutableStateFlow(YearMonth.now())
    private val saving = MutableStateFlow(false)

    // Extra buffer + DROP_OLDEST so a burst of taps can never suspend the emitter
    // or wedge the UI waiting for a collector.
    private val _events = MutableSharedFlow<HomeEvent>(
        replay = 0, extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<HomeEvent> = _events

    private var billsMadeFor: LocalDate? = null

    /**
     * Makes this month's bills. Called every time the screen resumes, not once
     * when the ViewModel is built.
     *
     * Generation is lazy and "on open" has to include coming back to a process
     * that never died. A phone left on the home screen overnight on the 30th
     * keeps this ViewModel alive, so building the bills only in `init` means the
     * teacher opens the app on the 1st, finds the month empty, and concludes he
     * has to enter all thirty students again. He does not: students carry over
     * by themselves, for ever, and this is what makes that true.
     *
     * Once per calendar day is enough. Generation is idempotent, but a teacher
     * switching apps twenty times in an afternoon should not pay for it twenty
     * times.
     */
    fun makeBillsForToday() {
        val today = LocalDate.now()
        val lastRun = billsMadeFor
        if (lastRun == today) return

        // The date rolled over underneath a live process. Follow it into the new
        // month - unless he has paged somewhere else himself, which is his choice
        // to keep.
        val lastMonth = lastRun?.let(YearMonth::from)
        if (lastMonth != null && month.value == lastMonth) month.value = YearMonth.from(today)

        // Claimed before the work starts, so two quick resumes cannot both run it.
        billsMadeFor = today
        viewModelScope.launch {
            safely("generate invoices") { repo.generateInvoices(today) }
                .onFailure {
                    billsMadeFor = null // let the next resume try again
                    _events.tryEmit(HomeEvent.Message(it.userMessage("Could not prepare this month's bills.")))
                }
        }
    }

    val state: StateFlow<HomeUiState> = month
        .flatMapLatest { ym ->
            val first = ym.atDay(1)
            combine(
                repo.observeMonth(first),
                repo.observeArrears(first),
                repo.observeStudentCount(),
                repo.profile,
                saving,
            ) { rows, arrears, studentCount, profile, isSaving ->
                val today = LocalDate.now()
                val mapped = rows.map { r ->
                    val net = r.invoice.amountDue + r.invoice.adjustment
                    val thisMonth = (net - r.paidSoFar).coerceAtLeast(0)
                    val behind = arrears[r.invoice.enrollmentId]
                    val carriedOver = behind?.amount ?: 0
                    // What the teacher is actually owed, which is what he should
                    // read out, send in a reminder and be offered on the payment
                    // dialog. Allocation already spends a payment oldest-first
                    // across exactly these bills.
                    val owed = thisMonth + carriedOver
                    DueRowUi(
                        invoiceId = r.invoice.id,
                        studentId = r.studentId,
                        name = r.studentName,
                        // "Left" on the row, because a name still sitting in the
                        // month list after he was removed reads as the removal
                        // having failed. He is here only for the money.
                        subtitle = subtitleFor(r.classLabel, r.batchName, r.hasLeft),
                        // A settled row shows the bill rather than a bare zero.
                        outstanding = if (owed > 0) owed else net,
                        carriedOver = carriedOver,
                        status = when {
                            owed <= 0 -> FeeStatus.PAID
                            carriedOver > 0 -> FeeStatus.OVERDUE
                            r.paidSoFar > 0 -> FeeStatus.PARTIAL
                            today > r.invoice.dueDate -> FeeStatus.OVERDUE
                            else -> FeeStatus.PENDING
                        },
                        monthsDue = behind?.months ?: 0,
                        guardianPhone = r.guardianPhone,
                        remindedToday = r.invoice.lastRemindedAt == today,
                    )
                }
                // A student can owe from earlier months and still have nothing
                // falling due in the month being viewed - an anniversary cycle
                // that bills on the 7th, or someone who has left. Without a row
                // of his own his debt would simply vanish off the screen, which
                // is the one thing arrears must never do.
                val billedThisMonth = rows.map { it.invoice.enrollmentId }.toSet()
                val broughtForwardOnly = arrears.values
                    .filterNot { it.enrollmentId in billedThisMonth }
                    .map { it.asOwedRow(today) }

                val expected = rows.sumOf { it.invoice.amountDue + it.invoice.adjustment }
                val received = rows.sumOf { it.paidSoFar }
                val everyRow = (mapped + broughtForwardOnly).sortedBy { it.name }
                val broughtForward = everyRow.sumOf { it.carriedOver }
                val unpaid = everyRow.filter { it.status != FeeStatus.PAID }

                HomeUiState(
                    monthLabel = ym.atDay(1).format(monthFormat),
                    // Equals the sum of the amounts on the rows below it, so the
                    // hero and the list can never tell the teacher two stories.
                    pending = (expected - received).coerceAtLeast(0) + broughtForward,
                    pendingStudents = unpaid.map { it.studentId }.distinct().size,
                    received = received,
                    expected = expected,
                    broughtForward = broughtForward,
                    unpaid = unpaid,
                    paid = everyRow.filter { it.status == FeeStatus.PAID },
                    hasStudents = studentCount > 0,
                    standing = ym.standingAgainst(YearMonth.now()),
                    tuitionName = profile.tuitionName,
                    loading = false,
                    saving = isSaving,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /**
     * Clamped to a sane window. Without this, holding the arrow walks off into
     * years with no data and no way back except tapping the same number of times.
     */
    fun previousMonth() {
        val floor = YearMonth.now().minusMonths(36)
        if (month.value > floor) month.value = month.value.minusMonths(1)
    }

    fun nextMonth() {
        val ceiling = YearMonth.now().plusMonths(1)
        if (month.value < ceiling) month.value = month.value.plusMonths(1)
    }

    fun receivePayment(studentId: Long, amount: Int) {
        // A second tap while the first write is in flight would create a second
        // payment. The dialog also disables its button; this is the real guard.
        if (saving.value) return
        if (amount <= 0) {
            _events.tryEmit(HomeEvent.Message("Enter an amount first."))
            return
        }
        saving.value = true
        viewModelScope.launch {
            safely("receive payment") { repo.receivePayment(studentId, amount) }
                .onSuccess { receipt ->
                    if (receipt != null) _events.tryEmit(HomeEvent.OfferReceipt(receipt))
                }
                .onFailure {
                    _events.tryEmit(HomeEvent.Message(it.userMessage("Could not save that payment.")))
                }
            saving.value = false
        }
    }

    fun markReminded(invoiceId: Long) {
        viewModelScope.launch { safely("mark reminded") { repo.markReminded(invoiceId) } }
    }
}

/** A debt with no bill of its own this month, shown as a row so it stays visible. */
private fun Arrears.asOwedRow(today: LocalDate) = DueRowUi(
    invoiceId = latestInvoiceId,
    studentId = studentId,
    name = studentName,
    subtitle = subtitleFor(classLabel, batchName, hasLeft),
    outstanding = amount,
    carriedOver = amount,
    status = FeeStatus.OVERDUE,
    monthsDue = months,
    guardianPhone = guardianPhone,
    remindedToday = lastRemindedAt == today,
)

private fun subtitleFor(classLabel: String, batchName: String, hasLeft: Boolean): String =
    (listOf(classLabel, batchName) + if (hasLeft) listOf("Left") else emptyList())
        .filter { it.isNotBlank() }
        .joinToString(" · ")

private fun YearMonth.standingAgainst(now: YearMonth): MonthStanding = when {
    this > now -> MonthStanding.FUTURE
    this < now -> MonthStanding.PAST
    else -> MonthStanding.CURRENT
}
