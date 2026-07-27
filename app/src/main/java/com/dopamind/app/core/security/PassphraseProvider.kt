package com.dopamind.app.core.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

/**
 * Generates and stores the SQLCipher database passphrase, itself protected by
 * a hardware-backed Android Keystore key (via [EncryptedSharedPreferences]).
 * The raw passphrase never leaves the device and is never logged.
 */
object PassphraseProvider {

    private const val PREFS_FILE_NAME = "dopamind_secure_prefs"
    private const val KEY_DB_PASSPHRASE = "db_passphrase_v1"
    private const val PASSPHRASE_BYTE_LENGTH = 32 // 256-bit

    fun getOrCreateDatabasePassphrase(context: Context): CharArray {
        val prefs = encryptedPrefs(context)
        val existing = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (existing != null) return existing.toCharArray()

        val generated = generateRandomPassphrase()
        prefs.edit().putString(KEY_DB_PASSPHRASE, generated).apply()
        return generated.toCharArray()
    }

    private fun encryptedPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun generateRandomPassphrase(): String {
        val bytes = ByteArray(PASSPHRASE_BYTE_LENGTH)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
