package pk.groscale.feeregister.ui.students

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pk.groscale.feeregister.data.db.BatchEntity
import pk.groscale.feeregister.data.db.StudentEntity
import pk.groscale.feeregister.domain.groupDigits
import pk.groscale.feeregister.ui.components.PrimaryButton
import pk.groscale.feeregister.ui.components.SecondaryButton
import pk.groscale.feeregister.ui.components.formatTime
import pk.groscale.feeregister.ui.components.throttledClickable
import pk.groscale.feeregister.ui.theme.Ink200
import pk.groscale.feeregister.ui.theme.Money
import pk.groscale.feeregister.ui.theme.RowMinHeight
import pk.groscale.feeregister.ui.theme.Space

@Composable
fun StudentsScreen(
    students: List<StudentEntity>,
    batches: List<BatchEntity>,
    feeFor: (Long) -> Int,
    batchFor: (Long) -> String,
    onAddStudent: () -> Unit,
    onAddBatch: () -> Unit,
    onOpenStudent: (Long) -> Unit,
    onMarkAttendance: () -> Unit,
) {
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
        Text("Students", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)

        Text(
            if (students.isEmpty()) {
                "No students yet."
            } else {
                "${students.size} student${if (students.size == 1) "" else "s"} in " +
                    "${batches.size} batch${if (batches.size == 1) "" else "es"}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column {
            students.forEachIndexed { i, s ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .throttledClickable { onOpenStudent(s.id) }
                        .height(RowMinHeight)
                        .padding(vertical = Space.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            s.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            listOf(s.classLabel, batchFor(s.id)).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        groupDigits(feeFor(s.id)),
                        style = Money.row,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (i != students.lastIndex) HorizontalDivider(color = Ink200)
            }
        }

        PrimaryButton(text = "Add student", onClick = onAddStudent)
        SecondaryButton(text = "Mark attendance", onClick = onMarkAttendance)
        SecondaryButton(text = "Add another batch", onClick = onAddBatch)
        Spacer(Modifier.height(Space.xxl))
    }
}
