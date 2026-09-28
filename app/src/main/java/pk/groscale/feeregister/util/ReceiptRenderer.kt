package pk.groscale.feeregister.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import pk.groscale.feeregister.domain.formatRs
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class ReceiptData(
    val tuitionName: String,
    val teacherPhone: String?,
    val receiptNo: Int,
    val date: LocalDate,
    val studentName: String,
    val classAndBatch: String,
    val monthLabel: String,
    val classDaysEnrolled: Int,
    val classDaysInPeriod: Int,
    val fee: Int,
    val previousBalance: Int,
    val discount: Int,
    val received: Int,
    val balance: Int,
)

/**
 * Renders the receipt as a PNG for sharing on WhatsApp.
 *
 * Drawn on a raw Canvas rather than through Compose because the output has to be
 * a bitmap of exact pixel dimensions regardless of the device's screen density
 * or font scale - the same receipt must look identical on every phone, since it
 * goes to parents.
 *
 * Two rules from design.md hold here:
 *  - The teacher's tuition name is the header. Ours is a footnote.
 *  - Plain. No logo, no QR, no ornament. A receipt is trusted because it is plain.
 */
object ReceiptRenderer {

    private const val W = 1080
    private const val H = 1440
    private const val PAD = 72f

    private val teal = Color.parseColor("#2C7383")
    private val ink = Color.parseColor("#141E22")
    private val inkSoft = Color.parseColor("#53646A")
    private val inkFaint = Color.parseColor("#8A9BA1")
    private val rule = Color.parseColor("#DCE3E6")
    private val paid = Color.parseColor("#2E7D5B")

    fun render(context: Context, d: ReceiptData): File {
        val bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)

        val bold = Typeface.create("sans-serif", Typeface.BOLD)
        val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        val regular = Typeface.create("sans-serif", Typeface.NORMAL)

        fun paint(size: Float, tf: Typeface, colour: Int, align: Paint.Align = Paint.Align.LEFT) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size; typeface = tf; color = colour; textAlign = align
            }

        // --- header band ---------------------------------------------------
        val bandH = 210f
        c.drawRect(0f, 0f, W.toFloat(), bandH, Paint().apply { color = teal })
        c.drawText(
            d.tuitionName.ifBlank { "Tuition" }.uppercase(),
            PAD, 106f,
            paint(46f, bold, Color.WHITE),
        )
        c.drawText("Fee Receipt  ·  Fees ki Raseed", PAD, 162f, paint(30f, regular, Color.parseColor("#DCEAEE")))

        // --- receipt no / date ---------------------------------------------
        var y = bandH + 84f
        c.drawText("Receipt No. ${d.receiptNo.toString().padStart(4, '0')}", PAD, y, paint(30f, medium, inkSoft))
        c.drawText(
            d.date.format(DateTimeFormatter.ofPattern("d MMM yyyy")),
            W - PAD, y,
            paint(30f, medium, inkSoft, Paint.Align.RIGHT),
        )

        // --- detail rows ----------------------------------------------------
        y += 78f
        val labelP = paint(32f, regular, inkSoft)
        val valueP = paint(34f, medium, ink, Paint.Align.RIGHT)

        fun row(label: String, value: String, gap: Float = 66f) {
            c.drawText(label, PAD, y, labelP)
            c.drawText(value, W - PAD, y, valueP)
            y += gap
        }

        row("Student", d.studentName)
        row("Class / Batch", d.classAndBatch)
        row("Month", d.monthLabel)
        // The number that ends an argument with a parent.
        row("Days taught", "${d.classDaysEnrolled} of ${d.classDaysInPeriod}")

        y += 12f
        c.drawLine(PAD, y, W - PAD, y, Paint().apply { color = rule; strokeWidth = 2f })
        y += 66f

        row("Fee", formatRs(d.fee))
        if (d.previousBalance != 0) row("Previous balance", formatRs(d.previousBalance))
        if (d.discount != 0) row("Discount", formatRs(-d.discount))

        y += 12f
        c.drawLine(PAD, y, W - PAD, y, Paint().apply { color = rule; strokeWidth = 2f })
        y += 92f

        // --- the amount received -------------------------------------------
        c.drawText("RECEIVED", PAD, y, paint(30f, bold, paid).apply { letterSpacing = 0.09f })
        c.drawText(formatRs(d.received), W - PAD, y + 6f, paint(56f, bold, paid, Paint.Align.RIGHT))
        y += 78f

        c.drawText(
            if (d.balance <= 0) "Balance" else "Balance still due",
            PAD, y,
            paint(32f, regular, inkSoft),
        )
        c.drawText(formatRs(d.balance), W - PAD, y, paint(34f, medium, if (d.balance > 0) ink else inkSoft, Paint.Align.RIGHT))

        // --- footer ---------------------------------------------------------
        val footY = H - 96f
        c.drawLine(PAD, footY - 56f, W - PAD, footY - 56f, Paint().apply { color = rule; strokeWidth = 2f })
        if (!d.teacherPhone.isNullOrBlank()) {
            c.drawText(d.teacherPhone, PAD, footY, paint(28f, regular, inkSoft))
        }
        // Ours is the footnote - and the only distribution channel this app has.
        c.drawText("TutorLink · Fee Register", W - PAD, footY, paint(26f, regular, inkFaint, Paint.Align.RIGHT))

        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        // One file per receipt number: re-sharing overwrites rather than piling up.
        val file = File(dir, "receipt-${d.receiptNo}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
        return file
    }

    fun uriFor(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.files", file)
}
