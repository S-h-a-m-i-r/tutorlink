package pk.groscale.feeregister.ui.home

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import pk.groscale.feeregister.domain.formatRs
import pk.groscale.feeregister.ui.components.CollectionBar
import pk.groscale.feeregister.ui.components.DrawnTick
import pk.groscale.feeregister.ui.components.FeeStatus
import pk.groscale.feeregister.ui.components.Motion
import pk.groscale.feeregister.ui.components.PrimaryButton
import pk.groscale.feeregister.ui.components.ReceivePaymentDialog
import pk.groscale.feeregister.ui.components.SecondaryButton
import pk.groscale.feeregister.ui.components.StudentDueRow
import pk.groscale.feeregister.ui.components.throttledClickable
import pk.groscale.feeregister.ui.theme.HeaderTint
import pk.groscale.feeregister.ui.theme.Ink200
import pk.groscale.feeregister.ui.theme.LocalStatusColors
import pk.groscale.feeregister.ui.theme.Money
import pk.groscale.feeregister.ui.theme.Radius
import pk.groscale.feeregister.ui.theme.Space
import pk.groscale.feeregister.ui.theme.Teal700
import pk.groscale.feeregister.util.Messages
import pk.groscale.feeregister.util.ReceiptData
import pk.groscale.feeregister.util.ReceiptRenderer
import pk.groscale.feeregister.util.Sharing
import pk.groscale.feeregister.util.Whatsapp

@Composable
fun HomeScreen(
    state: HomeUiState,
    events: SharedFlow<HomeEvent>,
    onResumed: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onReceivePayment: (studentId: Long, amount: Int) -> Unit,
    onMarkReminded: (invoiceId: Long) -> Unit,
    onAddStudent: () -> Unit,
) {
    // Not LaunchedEffect: that fires once and would miss the month rolling over
    // under an app that was only backgrounded, never killed.
    LifecycleResumeEffect(Unit) {
        onResumed()
        onPauseOrDispose { }
    }

    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var payingFor by remember { mutableStateOf<DueRowUi?>(null) }
    var receiptOffer by remember { mutableStateOf<ReceiptData?>(null) }
    // The receipt being rendered and shared. Held apart from receiptOffer because
    // the offer dialog closes on the same tap, and an effect living inside it
    // would leave composition before it ever ran.
    var sharing by remember { mutableStateOf<ReceiptData?>(null) }

    LaunchedEffect(events) {
        events.collect { event ->
            when (event) {
                is HomeEvent.Message -> snackbar.showSnackbar(event.text)
                is HomeEvent.OfferReceipt -> {
                    payingFor = null
                    receiptOffer = event.data
                }
            }
        }
    }

    payingFor?.let { row ->
        ReceivePaymentDialog(
            studentName = row.name,
            outstanding = row.outstanding,
            carriedOver = row.carriedOver,
            saving = state.saving,
            onDismiss = { if (!state.saving) payingFor = null },
            onConfirm = { amount -> onReceivePayment(row.studentId, amount) },
        )
    }

    receiptOffer?.let { data ->
        AlertDialog(
            onDismissRequest = { if (sharing == null) receiptOffer = null },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = { DrawnTick(color = LocalStatusColors.current.paid) },
            title = { Text("Payment saved", style = MaterialTheme.typography.titleMedium) },
            text = {
                Text(
                    "${formatRs(data.received)} received from ${data.studentName}. " +
                        "Send the receipt to the parent?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = sharing == null,
                    onClick = {
                        sharing = data
                        receiptOffer = null
                    },
                ) { Text("Send receipt") }
            },
            dismissButton = {
                TextButton(enabled = sharing == null, onClick = { receiptOffer = null }) { Text("Not now") }
            },
        )
    }

    sharing?.let { data ->
        // Rendering a 1080x1440 bitmap on the main thread would drop frames on a
        // cheap phone, so it happens off-main and the UI stays responsive.
        LaunchedEffect(data) {
            val result = runCatching {
                withContext(Dispatchers.Default) { ReceiptRenderer.render(context, data) }
            }
            result
                .onSuccess {
                    Sharing.shareImage(
                        context,
                        it,
                        Messages.receipt(data.studentName, data.monthLabel, data.received, data.tuitionName),
                    )
                }
                .onFailure { snackbar.showSnackbar("Could not make the receipt image.") }
            sharing = null
        }
    }

    fun remind(row: DueRowUi) {
        val phone = row.guardianPhone
        if (phone.isNullOrBlank()) return
        val message = if (row.monthsDue > 0) {
            Messages.overdueReminder(row.name, state.monthLabel, row.outstanding, row.monthsDue, state.tuitionName)
        } else {
            Messages.feeReminder(row.name, state.monthLabel, row.outstanding, state.tuitionName)
        }
        Whatsapp.openChat(context, phone, message)
        onMarkReminded(row.invoiceId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Surface, not background: the sheet only wraps its content, so the
                // area below a short list would otherwise show a grey band.
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState()),
        ) {
            Header(state, onPreviousMonth, onNextMonth)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-18).dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet),
                    )
                    .padding(horizontal = Space.screen, vertical = Space.lg)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(Space.lg),
            ) {
                when {
                    !state.hasStudents -> EmptyState(onAddStudent)

                    // Not `expected == 0`: a month can bill nothing new and still
                    // be owed money carried in from before.
                    state.unpaid.isEmpty() && state.paid.isEmpty() -> Centered(nothingBilled(state))

                    else -> {
                        // Nothing billed this month means nothing to collect: the
                        // bar would read "Rs 0 of Rs 0 · 0% collected", which
                        // looks like a failure rather than an empty month.
                        if (state.expected > 0) {
                            CollectionBar(received = state.received, expected = state.expected)
                        }

                        if (state.unpaid.isNotEmpty()) {
                            SectionLabel("NOT PAID YET")
                            Rows(state.unpaid, ::remind) { payingFor = it }

                            val next = state.nextToRemind
                            when {
                                next != null -> PrimaryButton(
                                    text = if (state.remainingToRemind > 1) {
                                        "Remind ${next.name} · ${state.remainingToRemind} left"
                                    } else {
                                        "Remind ${next.name}"
                                    },
                                    onClick = { remind(next) },
                                )
                                // Deep links open one chat at a time, so there is no
                                // "send to all". Saying so beats a button that
                                // silently drops every message but the first.
                                else -> Centered("All reminders sent today.")
                            }
                        } else {
                            Centered("Everyone has paid this month.")
                        }

                        if (state.paid.isNotEmpty()) {
                            SectionLabel("PAID")
                            Rows(state.paid, null) { }
                        }
                    }
                }
                Spacer(Modifier.height(Space.sm))
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(Space.lg),
        )
    }
}

@Composable
private fun Header(state: HomeUiState, onPrevious: () -> Unit, onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderTint)
            .statusBarsPadding()
            .padding(start = Space.screen, end = Space.screen, top = Space.lg, bottom = Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.lg)) {
            Text(
                "‹",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .throttledClickable(windowMs = 200L, onClick = onPrevious)
                    .padding(horizontal = Space.sm),
            )
            Text(
                state.monthLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .throttledClickable(windowMs = 200L, onClick = onNext)
                    .padding(horizontal = Space.sm),
            )
        }
        Text(
            "Pending",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Space.lg),
        )
        // Counts to the new figure after a payment or a month change, so the
        // teacher sees the amount move rather than a number silently swapped.
        val pendingShown by animateIntAsState(
            targetValue = state.pending,
            animationSpec = tween(Motion.LONG, easing = Motion.Easing),
            label = "pending",
        )
        Text(formatRs(pendingShown), style = Money.hero, color = Teal700)
        Text(
            if (state.pendingStudents == 1) "from 1 student" else "from ${state.pendingStudents} students",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Says out loud why the hero is larger than the collection bar below it.
        // Without this the two numbers look like a mistake.
        if (state.broughtForward > 0) {
            Text(
                "${formatRs(state.broughtForward)} of it is from earlier months",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Space.sm),
    )
}

@Composable
private fun Centered(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.lg),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Rows(rows: List<DueRowUi>, onRemind: ((DueRowUi) -> Unit)?, onTap: (DueRowUi) -> Unit) {
    Column {
        rows.forEachIndexed { index, row ->
            StudentDueRow(
                name = row.name,
                subtitle = row.subtitle,
                amount = row.outstanding,
                status = row.status,
                monthsDue = row.monthsDue,
                onRemind = if (onRemind != null && !row.guardianPhone.isNullOrBlank()) {
                    { onRemind(row) }
                } else {
                    null
                },
                onClick = { onTap(row) },
            )
            if (index != rows.lastIndex) HorizontalDivider(color = Ink200)
        }
    }
}

/**
 * An empty month has to say why it is empty, or it reads as lost data.
 *
 * Paging forward to a month with no bills in it is the one that frightens a
 * teacher: bills are never made ahead of time, so next month is always blank
 * until its 1st, and a bare "nothing here" invites him to conclude that his
 * students have to be entered again. They never do.
 */
private fun nothingBilled(state: HomeUiState): String = when (state.standing) {
    MonthStanding.FUTURE ->
        "Bills for ${state.monthLabel} are made on the 1st. Nothing to do — your " +
            "students carry over on their own."
    MonthStanding.PAST -> "Nothing was billed in ${state.monthLabel}."
    MonthStanding.CURRENT -> "No fees due this month yet."
}

/** Empty states instruct and offer the fix. Never a bare empty screen. */
@Composable
private fun EmptyState(onAddStudent: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        Text(
            "No students yet.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "Add your students and this screen will show you exactly who has paid and who has not.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryButton(text = "Add your first student", onClick = onAddStudent)
    }
}
