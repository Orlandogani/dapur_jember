package com.leanecorps.dapurjember.feature.settings.backup

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.leanecorps.dapurjember.core.domain.backup.BackupFile
import java.io.File

/**
 * Hands a backup to the Android share sheet — Drive, email, a USB stick (FR-D1). This is the
 * step that actually satisfies Risk R1: a backup sitting only on the tablet dies with the
 * tablet. The file is already encrypted under the owner's passphrase, so it is safe to send
 * through an untrusted channel.
 */
internal fun shareBackup(context: Context, backup: BackupFile) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        File(backup.path),
    )
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, backup.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, null))
}
