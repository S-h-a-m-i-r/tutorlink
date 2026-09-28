package pk.groscale.feeregister.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pk.groscale.feeregister.ui.theme.Radius
import pk.groscale.feeregister.ui.theme.Space
import java.time.DayOfWeek

@Composable
fun SelectableChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        modifier = modifier
            .background(
                if (selected) scheme.primaryContainer else scheme.surface,
                RoundedCornerShape(Radius.pill),
            )
            .border(
                width = 1.dp,
                color = if (selected) scheme.primary else scheme.outline,
                shape = RoundedCornerShape(Radius.pill),
            )
            .throttledClickable(windowMs = 250L, onClick = onClick)
            .padding(horizontal = Space.lg, vertical = Space.md),
    )
}

/**
 * A choice that has to be understood before it can be made, so it carries its
 * consequence underneath instead of only a label. Full width and stacked, not
 * chips: these are read, and read out to a parent, before they are tapped.
 */
@Composable
fun OptionCard(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (selected) scheme.primaryContainer else scheme.surface,
                RoundedCornerShape(Radius.card),
            )
            .border(
                width = 1.dp,
                color = if (selected) scheme.primary else scheme.outline,
                shape = RoundedCornerShape(Radius.card),
            )
            .throttledClickable(windowMs = 250L, onClick = onClick)
            .padding(Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.xs),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
        )
        Text(
            detail,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        )
    }
}

/** Mon..Sun as circular toggles. Mon = bit 0, matching java.time. */
@Composable
fun DayOfWeekPicker(mask: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text("Which days?", style = MaterialTheme.typography.labelLarge, color = scheme.onSurface)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            DayOfWeek.entries.forEachIndexed { i, day ->
                val bit = 1 shl (day.value - 1)
                val on = mask and bit != 0
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(if (on) scheme.primary else scheme.surface, CircleShape)
                        .border(1.dp, if (on) scheme.primary else scheme.outline, CircleShape)
                        .throttledClickable(windowMs = 250L) { onChange(mask xor bit) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        labels[i],
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) scheme.onPrimary else scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Common evening slots as chips rather than a time picker. A teacher's batches
 * start on the hour, and a spinner is three interactions where this is one.
 */
@Composable
fun StartTimePicker(startMinute: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val slots = listOf(14, 15, 16, 17, 18, 19, 20)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
        Text("Start time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            slots.forEach { hour ->
                val label = if (hour <= 12) "$hour PM" else "${hour - 12} PM"
                SelectableChip(
                    text = if (hour == 12) "12 PM" else label,
                    selected = startMinute == hour * 60,
                    onClick = { onChange(hour * 60) },
                )
            }
        }
    }
}

fun formatTime(startMinute: Int): String {
    val h24 = startMinute / 60
    val suffix = if (h24 < 12) "AM" else "PM"
    val h = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    return "$h $suffix"
}
