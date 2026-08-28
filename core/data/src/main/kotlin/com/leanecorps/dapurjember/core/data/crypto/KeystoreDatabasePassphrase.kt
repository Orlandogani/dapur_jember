package com.leanecorps.dapurjember.core.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

/**
 * Generates a 256-bit random passphrase on first run and stores it encrypted:
 * an `AndroidKeyStore` AES/GCM key wraps the passphrase, and the `iv || ciphertext` blob
 * lives in a plain [android.content.SharedPreferences] file. The plaintext passphrase never
 * touches disk. Losing the Keystore key (factory reset, app data wipe) means the database
 * can only be recovered from a backup — this is stated to the user during setup (FR-A5).
 */
class KeystoreDatabasePassphrase @Inject constructor(
    @ApplicationContext private val context: Context,
) : DatabasePassphrase {

    private val lock = Any()

    override fun getOrCreate(): ByteArray = synchronized(lock) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(PREF_KEY, null)
        if (stored != null) {
            return decrypt(Base64.decode(stored, Base64.NO_WRAP))
        }

        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        prefs.edit().putString(PREF_KEY, Base64.encodeToString(encrypt(passphrase), Base64.NO_WRAP)).apply()
        passphrase
    }

    private fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plaintext)
        return cipher.iv + ciphertext
    }

    private fun decrypt(blob: ByteArray): ByteArray {
        val iv = blob.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = blob.copyOfRange(GCM_IV_BYTES, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "dapurjember_db_key"
        const val PREFS_NAME = "dapurjember_db"
        const val PREF_KEY = "passphrase"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PASSPHRASE_BYTES = 32
        const val AES_KEY_BITS = 256
        const val GCM_IV_BYTES = 12
        const val GCM_TAG_BITS = 128
    }
}
