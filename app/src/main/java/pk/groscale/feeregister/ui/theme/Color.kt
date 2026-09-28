package pk.groscale.feeregister.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens from design.md section 2. Do not introduce a colour that is not here.
 *
 * Three rules the whole palette depends on:
 *  - Brand is blue-teal; green means "paid" and nothing else.
 *  - Fills may be soft. Text never is.
 *  - The header is a PALE WASH with dark ink on it, not a dark block with white
 *    on it. That is both the softer look and the higher contrast - 8.8:1 for the
 *    hero number, against 4.0:1 for white on a mid-teal.
 */

// Brand ramp
val Teal900 = Color(0xFF07333A)
val Teal700 = Color(0xFF0E4A52) // ink teal: the hero number, icon glyphs
val Teal600 = Color(0xFF2C7383) // primary: filled buttons, launcher ground
val Teal500 = Color(0xFF3D8394) // links, active nav
val Teal300 = Color(0xFF8FBAC4) // decorative, disabled, type on the teal ground
val Teal100 = Color(0xFFDCEAEE) // chips, icon circles, selected row
val Teal50  = Color(0xFFEFF6F8) // card tint fill
val HeaderTint = Color(0xFFEAF3F5) // the header ground - a pale wash, not a dark block

// Status — reserved meanings
val Paid            = Color(0xFF2E7D5B)
val PaidContainer   = Color(0xFFE6F1EB)
val OnPaidContainer = Color(0xFF12452F)

val Pending            = Color(0xFFB27A3A) // the only warm colour in the app
val PendingContainer   = Color(0xFFF7EEE2)
val OnPendingContainer = Color(0xFF5E3C15)

val Overdue            = Color(0xFFA05244)
val OverdueContainer   = Color(0xFFF6E8E5)
val OnOverdueContainer = Color(0xFF5A241B)

// Neutrals — cool, biased toward the teal
val Ink900 = Color(0xFF141E22) // names, amounts
val Ink600 = Color(0xFF53646A) // captions, labels
val Ink400 = Color(0xFF8A9BA1) // placeholder only — never body text
val Ink200 = Color(0xFFDCE3E6) // dividers, input borders
val Ink100 = Color(0xFFEDF1F2) // input fill
val Ink50  = Color(0xFFF7F9FA) // screen ground
val Surface = Color(0xFFFFFFFF)
