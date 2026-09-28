package pk.groscale.feeregister.ui.theme

import androidx.compose.ui.unit.dp

/** Spacing scale from design.md section 4. Nothing off-scale. */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val screen = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
}

/**
 * A card and a button must never share a corner radius — that sameness is what
 * makes a screen read as generated. Buttons are full pills; cards are not.
 */
object Radius {
    val card = 16.dp
    val input = 12.dp
    val sheet = 24.dp
    val pill = 999.dp
}

/** Tap targets are 56dp, not 48. These users are 30-55 on a 6.5" screen. */
val TapTarget = 56.dp
val RowMinHeight = 72.dp
