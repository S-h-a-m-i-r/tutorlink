package pk.groscale.feeregister.ui.students

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import pk.groscale.feeregister.data.db.StudentEditRow
import pk.groscale.feeregister.domain.BillingAnchor
import pk.groscale.feeregister.domain.JoiningCycle
import pk.groscale.feeregister.domain.Settlement
import pk.groscale.feeregister.domain.formatRs
import pk.groscale.feeregister.ui.components.AppTextField
import pk.groscale.feeregister.ui.components.PrimaryButton
import pk.groscale.feeregister.ui.components.SecondaryButton
import pk.groscale.feeregister.ui.theme.Ink200
import pk.groscale.feeregister.ui.theme.LocalStatusColors
import pk.groscale.feeregister.ui.theme.Space
import java.time.format.DateTimeFormatter

@Composable
fun StudentEditScreen(
    student: StudentEditRow,
    /** Worked out before the dialog opens, so the teacher never waits on a spinner. */
    leaving: Settlement?,
    onSave: (name: String, phone: String, fee: Int) -> Unit,
    onMarkLeft: () -> Unit,
) {
    var name by remember(student.studentId) { mutableStateOf(student.name) }
    var phone by remember(student.studentId) { mutableStateOf(student.guardianPhone.orEmpty()) }
    var fee by remember(student.studentId) { mutableStateOf(student.fee.toString()) }
    var confirmLeave by remember { mutableStateOf(false) }

    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Mark ${student.name} as left?", style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
                    Text(
                        "He stops appearing in this month's list and no new bills are made. " +
                            "His past bills and payments are kept.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (leaving != null) LeavingSummary(leaving)
                }
            },
            confirmButton = {
                TextButton(onClick = { confirmLeave = false; onMarkLeft() }) { Text("Mark as left") }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("Cancel") } },
        )
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
        Text(student.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            listOf(student.classLabel, student.batchName).filter { it.isNotBlank() }.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        AppTextField(value = name, onValueChange = { name = it }, label = "Student's name")
        AppTextField(
            value = phone,
            onValueChange = { new -> phone = new.filter { it.isDigit() || it == '+' }.take(15) },
            label = "Parent's WhatsApp number",
            keyboardType = KeyboardType.Phone,
        )
        AppTextField(
            value = fee,
            onValueChange = { new -> fee = new.filter { it.isDigit() }.take(6) },
            label = "Monthly fee",
            hint = "This month's bill changes too, unless money has already come in for it. " +
                "Earlier bills keep their old fee.",
            keyboardType = KeyboardType.Number,
        )

        // Read-only on purpose: the cycle decides the boundaries every bill was
        // cut on, so changing it now would strand the invoices already written.
        // Shown because "which cycle is this boy on?" is what the parent asks.
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(
                "Fee cycle",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                cycleSummary(student),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PrimaryButton(
            text = "Save changes",
            enabled = name.isNotBlank() && (fee.toIntOrNull() ?: 0) > 0,
            onClick = { onSave(name, phone, fee.toIntOrNull() ?: 0) },
        )

        HorizontalDivider(color = Ink200)
        SecondaryButton(text = "This student has left", onClick = { confirmLeave = true })
        Spacer(Modifier.height(Space.xxl))
    }
}

private val DayMonth = DateTimeFormatter.ofPattern("d MMM yyyy")
private val MonthYear = DateTimeFormatter.ofPattern("MMMM")

/** Set once when the student was added, and fixed from then on. */
private fun cycleSummary(student: StudentEditRow): String {
    val anchor = runCatching { BillingAnchor.valueOf(student.billingAnchor) }
        .getOrDefault(BillingAnchor.CALENDAR)
    val joined = "Joined ${student.startDate.format(DayMonth)}"
    return when (JoiningCycle.of(anchor, student.mergeStubIntoNext)) {
        JoiningCycle.PART_MONTH_NOW -> "1st of each month · $joined"
        JoiningCycle.PART_MONTH_WITH_NEXT ->
            "1st of each month, part month collected with the next bill · $joined"
        JoiningCycle.OWN_DATE -> "${student.startDate.dayOfMonth} to " +
            "${student.startDate.dayOfMonth} of each month · $joined"
    }
}

/**
 * The two questions a teacher cannot answer once the boy has walked out: has he
 * cleared his dues, and is anything owed back to him?
 *
 * The give-back figure is for the teacher alone. Nothing here is ever put into a
 * reminder or a receipt - an app must not tell a parent that money is coming to
 * him, because whether it is given is the teacher's call to make face to face.
 */
@Composable
private fun LeavingSummary(settlement: Settlement) {
    val status = LocalStatusColors.current
    Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        settlement.prorations.forEach { p ->
            Text(
                "${p.periodStart.format(MonthYear)} is cut to ${p.classDaysTaught} of " +
                    "${p.classDaysInPeriod} days taught — " +
                    "${formatRs(p.amountBefore)} becomes ${formatRs(p.amountAfter)}.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when {
            settlement.duesToCollect > 0 -> Text(
                "${formatRs(settlement.duesToCollect)} still to collect from him.",
                style = MaterialTheme.typography.bodyLarge,
                color = status.pending,
            )

            settlement.toGiveBack > 0 -> Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(
                    "${formatRs(settlement.toGiveBack)} to give back to him.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = status.paid,
                )
                Text(
                    "This is for you only — nothing about it is sent to the parent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> Text(
                "Nothing owed either way.",
                style = MaterialTheme.typography.bodyLarge,
                color = status.paid,
            )
        }
    }
}
