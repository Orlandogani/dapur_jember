package com.leanecorps.dapurjember.core.data.crypto

/**
 * Supplies the SQLCipher passphrase for the local database. The bytes are generated once on
 * first run and must survive app restarts without ever being stored in plaintext.
 */
interface DatabasePassphrase {
    /** Returns the passphrase, creating and persisting it on first call. */
    fun getOrCreate(): ByteArray
}
