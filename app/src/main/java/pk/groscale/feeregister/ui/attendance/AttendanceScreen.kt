package pk.groscale.feeregister.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import pk.groscale.feeregister.data.db.BatchEntity
import pk.groscale.feeregister.data.db.RollRow
import pk.groscale.feeregister.ui.components.SelectableChip
import pk.groscale.feeregister.ui.components.StatusChipLike
import pk.groscale.feeregister.ui.components.formatTime
import pk.groscale.feeregister.ui.components.throttledClickable
import pk.groscale.feeregister.ui.theme.Ink200
import pk.groscale.feeregister.ui.theme.RowMinHeight
import pk.groscale.feeregister.ui.theme.Space
import pk.groscale.feeregister.util.Messages
import pk.groscale.feeregister.util.Whatsapp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Everyone is present until tapped.
 *
 * On a normal day two students are missing, so this is two taps - not twenty.
 * Marking every student present daily is the pattern that gets these apps
 * abandoned in week three, so it is deliberately not offered.
 */
@Composable
fun AttendanceScreen(
    batches: List<BatchEntity>,
    selectedBatchId: Long,
    onSelectBatch: (Long) -> Unit,
    date: LocalDate,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    roll: List<RollRow>,
    onToggle: (RollRow) -> Unit,
    tuitionName: String = "",
) {
    val context = LocalContext.current

    // Set only when a student has just been marked absent - never when the
    // teacher un-marks one, because there is nothing to tell a parent about a
    // tap that was a mistake.
    var tellParentAbout by remember { mutableStateOf<RollRow?>(null) }

    tellParentAbout?.let { student ->
        val dayLabel = date.format(DateTimeFormatter.ofPattern("d MMM"))
        AlertDialog(
            onDismissRequest = { tellParentAbout = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("Tell ${student.studentName}'s parent?", style = MaterialTheme.typography.titleMedium)
            },
            text = {
                Text(
                    "He is marked absent for $dayLabel. WhatsApp opens with the message " +
                        "written — you still press send.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val phone = student.guardianPhone
                        tellParentAbout = null
                        if (!phone.isNullOrBlank()) {
                            Whatsapp.openChat(
                                context,
                                phone,
                                Messages.absenceNotice(student.studentName, dayLabel, tuitionName),
                            )
                        }
                    },
                ) { Text("Tell the parent") }
            },
            dismissButton = {
                TextButton(onClick = { tellParentAbout = null }) { Text("Not now") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = Space.screen),
        verticalArrangement = Arrangement.spacedBy(Space.lg),
    ) {
        Spacer(Modifier.height(Space.lg))
        Text("Attendance", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.throttledClickable(200L, onPreviousDay).padding(horizontal = Space.md),
            )
            Text(
                if (date == LocalDate.now()) {
                    "Today · ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
                } else {
                    date.format(DateTimeFormatter.ofPattern("EEEE, d MMM"))
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.throttledClickable(200L, onNextDay).padding(horizontal = Space.md),
            )
        }

        if (batches.size > 1) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalArrangement = Arrangement.spacedBy(Space.sm),
            ) {
                batches.forEach { b ->
                    SelectableChip(
                        text = "${b.name} · ${formatTime(b.startMinute)}",
                        selected = b.id == selectedBatchId,
                        onClick = { onSelectBatch(b.id) },
                    )
                }
            }
        }

        when {
            batches.isEmpty() -> Hint("Add a batch first.")
            roll.isEmpty() -> Hint("Nobody is enrolled in this batch on this day.")
            else -> {
                Text(
                    "Tap a student who did not come. Everyone else counts as present.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column {
                    roll.forEachIndexed { i, r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .throttledClickable(250L) {
                                    onToggle(r)
                                    // Offer only on the present -> absent
                                    // direction, and only if there is a number
                                    // to send to.
                                    if (!r.absent && !r.guardianPhone.isNullOrBlank()) {
                                        tellParentAbout = r
                                    }
                                }
                                .height(RowMinHeight)
                                .padding(vertical = Space.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                r.studentName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            StatusChipLike(
                                text = if (r.absent) "Absent" else "Present",
                                emphasised = r.absent,
                            )
                        }
                        if (i != roll.lastIndex) HorizontalDivider(color = Ink200)
                    }
                }
                Text(
                    "${roll.count { it.absent }} absent of ${roll.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(Space.xxl))
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = Space.xl),
        textAlign = TextAlign.Center,
    )
}
