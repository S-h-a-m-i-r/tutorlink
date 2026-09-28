package pk.groscale.feeregister.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Status colours are deliberately NOT in the Material colour scheme. Material's
 * slots are for brand roles; putting "paid" in `tertiary` would let it be reused
 * decoratively, which is exactly what destroys the meaning. They live here, named
 * for what they mean.
 */
@Immutable
data class StatusColors(
    val paid: Color, val paidContainer: Color, val onPaidContainer: Color,
    val pending: Color, val pendingContainer: Color, val onPendingContainer: Color,
    val overdue: Color, val overdueContainer: Color, val onOverdueContainer: Color,
)

private val statusColors = StatusColors(
    paid = Paid, paidContainer = PaidContainer, onPaidContainer = OnPaidContainer,
    pending = Pending, pendingContainer = PendingContainer, onPendingContainer = OnPendingContainer,
    overdue = Overdue, overdueContainer = OverdueContainer, onOverdueContainer = OnOverdueContainer,
)

val LocalStatusColors = staticCompositionLocalOf { statusColors }

/**
 * Light only in v1 - see design.md section 2.
 *
 * EVERY slot is set on purpose. Any slot left unset falls back to Material's
 * purple baseline, and it will surface somewhere you did not look - a dialog, a
 * bottom sheet, a text-selection handle. That is exactly how `#6750A4` ends up
 * shipping in an app that never names it.
 *
 * There is no third brand colour, so `tertiary` points into the teal ramp rather
 * than being left to default.
 */
private val LightScheme = lightColorScheme(
    primary = Teal600,
    onPrimary = Color.White,
    primaryContainer = Teal100,
    onPrimaryContainer = Teal900,
    inversePrimary = Teal300,

    secondary = Teal500,
    onSecondary = Color.White,
    secondaryContainer = Teal50,
    onSecondaryContainer = Teal900,

    tertiary = Teal500,
    onTertiary = Color.White,
    tertiaryContainer = Teal100,
    onTertiaryContainer = Teal900,

    background = Ink50,
    onBackground = Ink900,

    surface = Surface,
    onSurface = Ink900,
    surfaceVariant = Ink100,
    onSurfaceVariant = Ink600,
    surfaceTint = Teal600,

    // Dialogs and sheets pull from these. White, not a tinted container, because
    // the design uses borders rather than elevation overlays.
    surfaceBright = Surface,
    surfaceDim = Color(0xFFE7ECEE),
    surfaceContainerLowest = Surface,
    surfaceContainerLow = Color(0xFFFCFDFD),
    surfaceContainer = Ink50,
    surfaceContainerHigh = Surface,
    surfaceContainerHighest = Ink100,

    inverseSurface = Ink900,
    inverseOnSurface = Surface,

    error = Overdue,
    onError = Color.White,
    errorContainer = OverdueContainer,
    onErrorContainer = OnOverdueContainer,

    outline = Ink200,
    outlineVariant = Ink200,
    scrim = Color.Black,
)

@Composable
fun TutorLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = AppTypography,
        content = content,
    )
}
