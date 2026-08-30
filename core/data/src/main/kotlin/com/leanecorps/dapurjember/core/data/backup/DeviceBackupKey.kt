package com.leanecorps.dapurjember.core.data.backup

import com.leanecorps.dapurjember.core.data.crypto.DatabasePassphrase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The passphrase used for *automatic* nightly backups (FR-D3).
 *
 * A scheduled backup cannot prompt anyone, and we deliberately never store the owner's manual
 * passphrase, so automatic backups are encrypted with a device-held key instead. That makes
 * them exactly what FR-D3 asks for — a **local** rolling safety net, good for "restore
 * yesterday" after a bad day of data entry — but *not* a substitute for a manual backup
 * shared off the tablet, which is the one that survives the tablet itself (Risk R1).
 *
 * Reuses the same Keystore-wrapped secret as the database, so there is one key to lose, not
 * two: if the Keystore is gone the database is unreadable anyway.
 */
@Singleton
class DeviceBackupKey @Inject constructor(
    private val databasePassphrase: DatabasePassphrase,
) {
    /** Stable for the life of the install; changes only on a factory reset or data wipe. */
    fun passphrase(): CharArray =
        databasePassphrase.getOrCreate().joinToString("") { "%02x".format(it) }.toCharArray()
}
