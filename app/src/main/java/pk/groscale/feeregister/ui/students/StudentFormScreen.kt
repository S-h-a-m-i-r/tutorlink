package pk.groscale.feeregister.ui.students

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import pk.groscale.feeregister.data.db.BatchEntity
import pk.groscale.feeregister.data.prefs.Profile
import pk.groscale.feeregister.domain.GeneratedInvoice
import pk.groscale.feeregister.domain.JoiningCycle
import pk.groscale.feeregister.domain.formatRs
import pk.groscale.feeregister.domain.previewBills
import pk.groscale.feeregister.ui.components.AppTextField
import pk.groscale.feeregister.ui.components.OptionCard
import pk.groscale.feeregister.ui.components.PrimaryButton
import pk.groscale.feeregister.ui.components.SelectableChip
import pk.groscale.feeregister.ui.components.formatTime
import pk.groscale.feeregister.ui.theme.Space
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class StudentDraft(
    val name: String,
    val classLabel: String,
    val guardianPhone: String,
    val batchId: Long,
    val fee: Int,
    val cycle: JoiningCycle,
)

/**
 * Four fields, and that is the whole form - on the 1st of the month.
 *
 * A teacher with thirty existing students pays this form's cost thirty times
 * before the app gives him anything back, so every extra field here is the most
 * expensive field in the app. Everything else is editable later.
 *
 * The one exception is the joining cycle, and only on a day that is not the 1st.
 * It cannot wait: it decides the boundaries every future bill is cut on, and it
 * is the question the parent is standing there asking. On the 1st all three
 * answers are the same bill, so it is not shown and the form stays four fields.
 */
@Composable
fun StudentFormScreen(
    batches: List<BatchEntity>,
    title: String = "Add a student",
    joiningOn: LocalDate = LocalDate.now(),
    graceDays: Int = Profile().graceDays,
    roundingStep: Int = Profile().roundingStep,
    onSave: (StudentDraft) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var batchId by remember { mutableLongStateOf(batches.firstOrNull()?.id ?: 0L) }
    var fee by remember { mutableStateOf("") }
    var feeEdited by remember { mutableStateOf(false) }
    var cycle by remember { mutableStateOf(JoiningCycle.PART_MONTH_NOW) }

    val batch = batches.firstOrNull { it.id == batchId }

    // `batches` arrives from the database one frame after first composition, so
    // the initial value above is always 0. Re-select once the list lands, or the
    // form stays permanently disabled with no visible reason why.
    LaunchedEffect(batches) {
        if (batches.none { it.id == batchId }) {
            batchId = batches.firstOrNull()?.id ?: 0L
        }
    }

    // Pre-fill the fee from the batch, but stop the moment he types his own -
    // per-student fees are the normal case, not the exception.
    LaunchedEffect(batch?.id) {
        if (!feeEdited && batch != null) fee = batch.defaultFee.toString()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.xl),
        verticalArrangement = Arrangement.spacedBy(Space.xl),
    ) {
        Spacer(Modifier.height(Space.lg))
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)

        if (batches.isEmpty()) {
            Text(
                "Add a batch first — a student has to belong to one.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        AppTextField(
            value = name,
            onValueChange = { name = it },
            label = "Student's name",
            placeholder = "Ahmed Raza",
        )
        AppTextField(
            value = phone,
            onValueChange = { new -> phone = new.filter { it.isDigit() || it == '+' }.take(15) },
            label = "Parent's WhatsApp number",
            hint = "Reminders and receipts go here.",
            placeholder = "03001234567",
            keyboardType = KeyboardType.Phone,
        )

        if (batches.size > 1) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("Batch", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Space.sm),
                    verticalArrangement = Arrangement.spacedBy(Space.sm),
                ) {
                    batches.forEach { b ->
                        SelectableChip(
                            text = "${b.name} · ${formatTime(b.startMinute)}",
                            selected = b.id == batchId,
                            onClick = { batchId = b.id },
                        )
                    }
                }
            }
        }

        AppTextField(
            value = fee,
            onValueChange = { new -> fee = new.filter { it.isDigit() }.take(6); feeEdited = true },
            label = "Monthly fee",
            placeholder = "2500",
            keyboardType = KeyboardType.Number,
        )

        // Needs the batch's days and a fee before it can quote real amounts, and
        // the amounts are the whole point of showing it.
        val feeValue = fee.toIntOrNull() ?: 0
        if (JoiningCycle.isAskedFor(joiningOn) && batch != null && feeValue > 0) {
            JoiningCyclePicker(
                joiningOn = joiningOn,
                daysOfWeekMask = batch.daysOfWeekMask,
                fee = feeValue,
                graceDays = graceDays,
                roundingStep = roundingStep,
                selected = cycle,
                onSelect = { cycle = it },
            )
        }

        PrimaryButton(
            text = "Add student",
            enabled = name.isNotBlank() && phone.isNotBlank() && batchId != 0L && fee.isNotBlank(),
            onClick = {
                onSave(
                    StudentDraft(
                        name = name,
                        classLabel = batch?.classLabel.orEmpty(),
                        guardianPhone = phone,
                        batchId = batchId,
                        fee = fee.toIntOrNull() ?: 0,
                        cycle = cycle,
                    ),
                )
            },
        )
        Spacer(Modifier.height(Space.xl))
    }
}

private val DayMonth = DateTimeFormatter.ofPattern("d MMM")

/**
 * The mid-month joining question, asked in money rather than in vocabulary.
 *
 * Every option is priced by the real invoice generator, so the teacher can read
 * a line straight out to the parent standing in front of him and know the app
 * will bill exactly that. The two things he must not do are surprise the parent
 * and undercharge himself, and both of those are decided right here.
 */
@Composable
private fun JoiningCyclePicker(
    joiningOn: LocalDate,
    daysOfWeekMask: Int,
    fee: Int,
    graceDays: Int,
    roundingStep: Int,
    selected: JoiningCycle,
    onSelect: (JoiningCycle) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text(
            "How should the first month be billed?",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            "He joins on ${joiningOn.format(DayMonth)}, so the first bill is not a whole " +
                "month. Ask the parent which one suits them — the teacher is owed the same " +
                "either way.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        JoiningCycle.entries.forEach { option ->
            val bills = remember(option, joiningOn, daysOfWeekMask, fee, graceDays, roundingStep) {
                previewBills(option, joiningOn, daysOfWeekMask, fee, graceDays, roundingStep)
            }
            OptionCard(
                title = cycleTitle(option, joiningOn),
                detail = quote(bills),
                selected = option == selected,
                onClick = { onSelect(option) },
            )
        }
    }
}

private fun cycleTitle(cycle: JoiningCycle, joiningOn: LocalDate): String = when (cycle) {
    JoiningCycle.PART_MONTH_NOW -> "Part month now, then the 1st of each month"
    JoiningCycle.PART_MONTH_WITH_NEXT -> "Part month added to next month's bill"
    JoiningCycle.OWN_DATE -> "The ${ordinal(joiningOn.dayOfMonth)} of every month"
}

/**
 * The two bills, in the order they will arrive. Merged options print as one
 * collection because that is what the parent will be asked for.
 */
private fun quote(bills: List<GeneratedInvoice>): String {
    val first = bills.firstOrNull() ?: return ""
    val next = bills.getOrNull(1)

    val firstLine = buildString {
        append("${formatRs(first.amountDue)} for ${period(first)}")
        // "11 of 26 days" is the sentence that ends an argument with a parent.
        if (first.classDaysEnrolled < first.classDaysInPeriod) {
            append(" · ${first.classDaysEnrolled} of ${first.classDaysInPeriod} days")
        }
    }
    if (next == null) return firstLine

    return if (next.dueDate == first.dueDate) {
        "$firstLine\nplus ${formatRs(next.amountDue)} for ${period(next)} — " +
            "${formatRs(first.amountDue + next.amountDue)} together on ${next.dueDate.format(DayMonth)}"
    } else {
        "$firstLine, due ${first.dueDate.format(DayMonth)}\n" +
            "then ${formatRs(next.amountDue)} for ${period(next)}, due ${next.dueDate.format(DayMonth)}"
    }
}

private fun period(bill: GeneratedInvoice): String =
    "${bill.periodStart.format(DayMonth)} – ${bill.periodEnd.format(DayMonth)}"

private fun ordinal(day: Int): String {
    val suffix = if (day in 11..13) "th" else when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}
