package com.leanecorps.dapurjember.core.common.crypto

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * The encrypted-backup envelope (FR-D1, arch §7). A backup must be restorable on a *different*
 * tablet, so it cannot use the Android Keystore key that protects the live database — it is
 * re-encrypted under a key derived from a passphrase the owner supplies and keeps.
 *
 * File layout, all big-endian:
 * ```
 * "DJBK" | version:u8 | salt:16 | nonce:12 | AES-256-GCM(ciphertext ‖ tag)
 * ```
 * The magic and version are written in the clear so a future release can still recognise and
 * read a v1 file — the header is authenticated as GCM associated data, so it cannot be
 * tampered with silently.
 *
 * Pure `javax.crypto`; no third-party dependency and no Android import, so it is testable on
 * the JVM in milliseconds.
 */
object BackupCrypto {

    private const val MAGIC = "DJBK"
    const val VERSION: Int = 1

    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val KEY_BITS = 256
    private const val TAG_BITS = 128

    /** OWASP's 2023 floor for PBKDF2-HMAC-SHA256. Roughly a second on a cheap tablet. */
    const val ITERATIONS: Int = 210_000

    private const val HEADER_BYTES = 4 + 1 + SALT_BYTES + NONCE_BYTES

    class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

    fun encrypt(plaintext: ByteArray, passphrase: CharArray, random: SecureRandom = SecureRandom()): ByteArray {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val header = header(salt, nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(header)
        }
        return ByteArrayOutputStream().apply {
            write(header)
            write(cipher.doFinal(plaintext))
        }.toByteArray()
    }

    /**
     * @throws InvalidBackupException if this is not a DapurJember backup, is a newer version
     *   than this build understands, or the passphrase is wrong / the file was tampered with.
     *   A wrong passphrase and a corrupted file are deliberately indistinguishable.
     */
    @Suppress("ThrowsCount")
    fun decrypt(backup: ByteArray, passphrase: CharArray): ByteArray {
        if (backup.size <= HEADER_BYTES) throw InvalidBackupException("File is too short to be a backup.")

        val buffer = ByteBuffer.wrap(backup)
        val magic = ByteArray(MAGIC.length).also(buffer::get)
        if (String(magic, Charsets.US_ASCII) != MAGIC) {
            throw InvalidBackupException("This is not a DapurJember backup file.")
        }
        val version = buffer.get().toInt()
        if (version != VERSION) {
            throw InvalidBackupException("This backup was written by a newer version (format $version).")
        }
        val salt = ByteArray(SALT_BYTES).also(buffer::get)
        val nonce = ByteArray(NONCE_BYTES).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, deriveKey(passphrase, salt), GCMParameterSpec(TAG_BITS, nonce))
            updateAAD(backup.copyOfRange(0, HEADER_BYTES))
        }
        return try {
            cipher.doFinal(ciphertext)
        } catch (e: AEADBadTagException) {
            throw InvalidBackupException("Wrong passphrase, or the backup file is damaged.", e)
        }
    }

    private fun header(salt: ByteArray, nonce: ByteArray): ByteArray =
        ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC.toByteArray(Charsets.US_ASCII))
            .put(VERSION.toByte())
            .put(salt)
            .put(nonce)
            .array()

    private fun deriveKey(passphrase: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
