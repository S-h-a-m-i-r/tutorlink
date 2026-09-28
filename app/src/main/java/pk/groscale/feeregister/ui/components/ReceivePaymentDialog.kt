package pk.groscale.feeregister.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
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
import pk.groscale.feeregister.domain.formatRs
import pk.groscale.feeregister.ui.theme.Space

/**
 * Pre-filled with the full outstanding amount, because that is what happens most
 * of the time. Editing it down is the partial-payment case, which the allocation
 * handles without any special code path.
 *
 * [outstanding] is everything the student owes, so [carriedOver] of it may belong
 * to earlier months. Naming that split matters: the money is spent oldest bill
 * first, so a teacher who is not told would expect a part payment to settle this
 * month and find it settled the last one instead.
 */
@Composable
fun ReceivePaymentDialog(
    studentName: String,
    outstanding: Int,
    carriedOver: Int,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var amount by remember { mutableStateOf(outstanding.toString()) }
    // take(7) in the field caps this at 9,999,999 - no overflow to guard against.
    val value = amount.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Receive payment", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.lg)) {
                Text(
                    buildString {
                        append("$studentName owes ${formatRs(outstanding)}")
                        if (carriedOver > 0) append(", ${formatRs(carriedOver)} of it from earlier months")
                        append(".")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppTextField(
                    value = amount,
                    onValueChange = { new -> amount = new.filter { it.isDigit() }.take(7) },
                    label = "Amount received",
                    keyboardType = KeyboardType.Number,
                )
                if (value > outstanding) {
                    Text(
                        "${formatRs(value - outstanding)} will be kept as advance for next month.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (value in 1 until outstanding) {
                    Text(
                        "${formatRs(outstanding - value)} will stay pending.",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalStatusColorsOrDefault().pending,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value) },
                enabled = value > 0 && !saving,
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
    )
}

@Composable
private fun LocalStatusColorsOrDefault() = pk.groscale.feeregister.ui.theme.LocalStatusColors.current
