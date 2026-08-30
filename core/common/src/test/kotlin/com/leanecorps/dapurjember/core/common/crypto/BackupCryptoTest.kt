package com.leanecorps.dapurjember.core.common.crypto

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BackupCryptoTest {

    private val passphrase = "correct horse battery staple".toCharArray()
    private val payload = "the database bytes".repeat(100).toByteArray()

    @Test
    fun `a backup round-trips with the right passphrase`() {
        val encrypted = BackupCrypto.encrypt(payload, passphrase)
        assertArrayEquals(payload, BackupCrypto.decrypt(encrypted, passphrase))
    }

    @Test
    fun `the plaintext never appears in the encrypted file`() {
        val encrypted = BackupCrypto.encrypt(payload, passphrase)
        assertFalse(String(encrypted, Charsets.ISO_8859_1).contains("the database bytes"))
    }

    @Test
    fun `the wrong passphrase is rejected, not silently mis-decrypted`() {
        val encrypted = BackupCrypto.encrypt(payload, passphrase)
        val failure = assertThrows(BackupCrypto.InvalidBackupException::class.java) {
            BackupCrypto.decrypt(encrypted, "wrong passphrase".toCharArray())
        }
        assertEquals("Wrong passphrase, or the backup file is damaged.", failure.message)
    }

    @Test
    fun `a single flipped byte in the ciphertext is detected`() {
        val encrypted = BackupCrypto.encrypt(payload, passphrase)
        encrypted[encrypted.lastIndex - 5] = (encrypted[encrypted.lastIndex - 5] + 1).toByte()
        assertThrows(BackupCrypto.InvalidBackupException::class.java) {
            BackupCrypto.decrypt(encrypted, passphrase)
        }
    }

    @Test
    fun `tampering with the authenticated header is detected`() {
        val encrypted = BackupCrypto.encrypt(payload, passphrase)
        encrypted[8] = (encrypted[8] + 1).toByte() // inside the salt
        assertThrows(BackupCrypto.InvalidBackupException::class.java) {
            BackupCrypto.decrypt(encrypted, passphrase)
        }
    }

    @Test
    fun `a foreign file is refused with a clear message`() {
        // Long enough to clear the header-length check, so we exercise the magic check itself.
        val foreign = "PK this is actually a zip file, not a backup".repeat(4).toByteArray()
        val failure = assertThrows(BackupCrypto.InvalidBackupException::class.java) {
            BackupCrypto.decrypt(foreign, passphrase)
        }
        assertEquals("This is not a DapurJember backup file.", failure.message)
    }

    @Test
    fun `a truncated file is refused rather than crashing`() {
        val encrypted = BackupCrypto.encrypt(payload, passphrase)
        assertThrows(BackupCrypto.InvalidBackupException::class.java) {
            BackupCrypto.decrypt(encrypted.copyOfRange(0, 20), passphrase)
        }
    }

    @Test
    fun `a newer format version is refused by name so the user knows to update`() {
        val encrypted = BackupCrypto.encrypt(payload, passphrase)
        encrypted[4] = 99 // version byte
        val failure = assertThrows(BackupCrypto.InvalidBackupException::class.java) {
            BackupCrypto.decrypt(encrypted, passphrase)
        }
        assertEquals("This backup was written by a newer version (format 99).", failure.message)
    }

    @Test
    fun `each backup uses a fresh salt and nonce, so two backups of the same data differ`() {
        val first = BackupCrypto.encrypt(payload, passphrase)
        val second = BackupCrypto.encrypt(payload, passphrase)
        assertFalse(first.contentEquals(second))
        assertArrayEquals(payload, BackupCrypto.decrypt(second, passphrase))
    }

    @Test
    fun `the file is self-describing so a future release can recognise it`() {
        val encrypted = BackupCrypto.encrypt(payload, passphrase)
        assertEquals("DJBK", String(encrypted.copyOfRange(0, 4), Charsets.US_ASCII))
        assertEquals(BackupCrypto.VERSION, encrypted[4].toInt())
    }
}
