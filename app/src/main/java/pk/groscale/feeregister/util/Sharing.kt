package pk.groscale.feeregister.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object Sharing {

    /**
     * Shares an image through the system sheet. WhatsApp is the destination in
     * practice, but going through the sheet means the teacher can also save it or
     * send it anywhere else, and there is nothing to fail if WhatsApp is missing.
     */
    fun shareImage(context: Context, file: File, caption: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Send receipt").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(context, "Nothing on this phone can share an image", Toast.LENGTH_LONG).show()
        }
    }
}
