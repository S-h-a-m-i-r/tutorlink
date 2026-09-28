package pk.groscale.feeregister.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pk.groscale.feeregister.ui.theme.LocalStatusColors
import pk.groscale.feeregister.ui.theme.Radius
import pk.groscale.feeregister.ui.theme.TutorLinkTheme

/**
 * A status is always a WORD plus a colour, never a colour alone: cheap screens
 * render colour badly in daylight, and some parents are colour-blind.
 */
enum class FeeStatus { PAID, PARTIAL, PENDING, OVERDUE }

@Composable
fun StatusChip(status: FeeStatus, monthsDue: Int = 0, modifier: Modifier = Modifier) {
    val c = LocalStatusColors.current
    val (bg, fg, label) = when (status) {
        FeeStatus.PAID -> Triple(c.paidContainer, c.onPaidContainer, "Paid")
        FeeStatus.PARTIAL -> Triple(c.pendingContainer, c.onPendingContainer, "Part paid")
        FeeStatus.PENDING -> Triple(c.pendingContainer, c.onPendingContainer, "Not paid")
        FeeStatus.OVERDUE -> Triple(
            c.overdueContainer,
            c.onOverdueContainer,
            if (monthsDue > 1) "$monthsDue months due" else "1 month due",
        )
    }
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = fg,
        modifier = modifier
            .background(bg, RoundedCornerShape(Radius.pill))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * Same shape as [StatusChip] but for states that are not a fee status - present
 * and absent. Kept separate so the fee status colours keep their single meaning.
 */
@Composable
fun StatusChipLike(text: String, emphasised: Boolean, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val c = LocalStatusColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (emphasised) c.onPendingContainer else scheme.onSurfaceVariant,
        modifier = modifier
            .background(
                if (emphasised) c.pendingContainer else scheme.surfaceVariant,
                RoundedCornerShape(Radius.pill),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun StatusChipPreview() {
    TutorLinkTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.background(Color.White).padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        ) {
            StatusChip(FeeStatus.PAID)
            StatusChip(FeeStatus.PENDING)
            StatusChip(FeeStatus.PARTIAL)
            StatusChip(FeeStatus.OVERDUE, monthsDue = 2)
        }
    }
}
