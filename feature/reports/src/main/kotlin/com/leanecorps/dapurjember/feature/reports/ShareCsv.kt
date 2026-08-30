package com.leanecorps.dapurjember.feature.reports

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/**
 * Writes [export] to the app's cache and opens the Android share sheet (FR-R3). Uses a
 * [FileProvider] so the receiving app gets a temporary read grant rather than a `file://` URI.
 * Nothing leaves the device unless the user picks a target — see NFR12.
 */
internal fun shareCsv(context: Context, export: CsvExport) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, export.fileName).apply { writeText(export.content) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, export.fileName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, null))
}
