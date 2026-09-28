package pk.groscale.feeregister.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import pk.groscale.feeregister.domain.formatRs
import pk.groscale.feeregister.ui.theme.Ink100
import pk.groscale.feeregister.ui.theme.LocalStatusColors
import pk.groscale.feeregister.ui.theme.Money
import pk.groscale.feeregister.ui.theme.Radius
import pk.groscale.feeregister.ui.theme.Space

/**
 * Answers "how is this month going?" before he reads a single name.
 *
 * The only progress indicator in the app. Adding others would stop it meaning
 * anything.
 */
@Composable
fun CollectionBar(received: Int, expected: Int, modifier: Modifier = Modifier) {
    val fraction = if (expected <= 0) 0f else (received.toFloat() / expected).coerceIn(0f, 1f)
    val percent = (fraction * 100).roundToInt()

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text(
            text = "${formatRs(received)} of ${formatRs(expected)}",
            style = Money.small,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Ink100, RoundedCornerShape(Radius.pill)),
        ) {
            Row(
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val width = (constraints.maxWidth * fraction).toInt()
                        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
                        layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                    }
                    .height(8.dp)
                    .background(LocalStatusColors.current.paid, RoundedCornerShape(Radius.pill)),
            ) {}
        }
        Text(
            text = "$percent% collected",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}
