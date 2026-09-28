package pk.groscale.feeregister.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

// TODO(fonts): bundle IBM Plex Sans in res/font and swap FontFamily.Default.
// Until then the scale and weights below are already correct, so swapping the
// family is a one-line change and nothing else shifts.
private val Plex = FontFamily.Default

/**
 * Scale from design.md section 3. Body never goes below 16sp.
 * Every money style must also carry tabular figures - see [Money].
 */
val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Plex, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = Plex, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = Plex, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontFamily = Plex, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = Plex, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = Plex, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Plex, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 23.sp),
    labelLarge = TextStyle(fontFamily = Plex, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Plex, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(
        fontFamily = Plex, fontWeight = FontWeight.Medium, fontSize = 12.sp,
        lineHeight = 16.sp, letterSpacing = 1.0.sp,
    ),
)

/**
 * Money styles. Separated from [AppTypography] because they carry a constraint
 * the rest of the scale does not: figures must be tabular so rupee columns line
 * up on one right edge. A wobbling column is the fastest way to look amateur.
 */
object Money {
    val hero = TextStyle(
        fontFamily = Plex, fontWeight = FontWeight.Bold, fontSize = 40.sp, lineHeight = 44.sp,
        fontFeatureSettings = "tnum", textAlign = TextAlign.Start,
    )
    val large = TextStyle(
        fontFamily = Plex, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp,
        fontFeatureSettings = "tnum",
    )
    val row = TextStyle(
        fontFamily = Plex, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 20.sp,
        fontFeatureSettings = "tnum", textAlign = TextAlign.End,
    )
    val small = TextStyle(
        fontFamily = Plex, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp,
        fontFeatureSettings = "tnum",
    )
}
