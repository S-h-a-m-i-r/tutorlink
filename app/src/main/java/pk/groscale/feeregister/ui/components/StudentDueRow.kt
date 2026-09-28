package pk.groscale.feeregister.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pk.groscale.feeregister.domain.groupDigits
import pk.groscale.feeregister.ui.theme.Money
import pk.groscale.feeregister.ui.theme.RowMinHeight
import pk.groscale.feeregister.ui.theme.Space
import pk.groscale.feeregister.ui.theme.Teal100
import pk.groscale.feeregister.ui.theme.Teal700

/** Wide enough for "2 months due", the longest chip label. */
private val AmountColumnWidth = 120.dp

/**
 * The most-used component in the app.
 *
 * Amounts are right-aligned with tabular figures so the rupee column lines up on
 * a single edge - the difference between reading as a register and reading as a
 * list of guesses.
 */
@Composable
fun StudentDueRow(
    name: String,
    subtitle: String,
    amount: Int,
    status: FeeStatus,
    monthsDue: Int = 0,
    onRemind: (() -> Unit)? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .throttledClickable(onClick = onClick)
            .defaultMinSize(minHeight = RowMinHeight)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.md),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Fixed width, not wrap-content: otherwise each row's right edge is set
        // by its own chip and the rupee column wobbles down the list.
        Column(
            modifier = Modifier.width(AmountColumnWidth),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Text(
                groupDigits(amount),
                style = Money.row,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
            StatusChip(status, monthsDue)
        }

        if (onRemind != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Teal100, CircleShape)
                    .throttledClickable(onClick = onRemind),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Send reminder to $name's parent",
                    tint = Teal700,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
