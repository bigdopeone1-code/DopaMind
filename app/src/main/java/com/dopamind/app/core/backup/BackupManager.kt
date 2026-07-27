package com.dopamind.app.core.backup

import android.content.Context
import android.net.Uri
import com.dopamind.app.core.database.DopaMindDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface BackupResult {
    data object Success : BackupResult
    data class Failure(val message: String) : BackupResult
}

/**
 * Exports/imports the already-encrypted SQLCipher database file as-is via the
 * Storage Access Framework, so the user picks the destination/source
 * themselves — no extra storage permission needed.
 *
 * IMPORTANT limitation, surfaced in the UI: the database is encrypted with a
 * passphrase generated in this device's hardware Keystore, which by design
 * never leaves the device and is wiped if the app is uninstalled. This makes
 * the export a genuine *local* backup (protects against accidentally
 * clearing app data, a bad update, etc.) — it is **not** a portable backup
 * you can restore on a different phone or after reinstalling the app.
 */
class BackupManager(
    private val context: Context,
    private val database: DopaMindDatabase,
) {
    suspend fun export(destination: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Flush the write-ahead log into the main file so the copy is self-contained.
            database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL);")

            val dbFile = DopaMindDatabase.databaseFile(context)
            context.contentResolver.openOutputStream(destination)?.use { output ->
                dbFile.inputStream().use { input -> input.copyTo(output) }
            } ?: return@withContext BackupResult.Failure("could_not_open_destination")

            BackupResult.Success
        } catch (t: Throwable) {
            BackupResult.Failure(t.message ?: "unknown_error")
        }
    }

    /**
     * Overwrites the live database file with the picked backup. The caller
     * must prompt the user to fully close and reopen the app afterward —
     * this does not attempt to hot-swap the already-open database instance.
     */
    suspend fun import(source: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            database.close()

            val dbFile = DopaMindDatabase.databaseFile(context)
            context.contentResolver.openInputStream(source)?.use { input ->
                dbFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext BackupResult.Failure("could_not_open_source")

            // Stale WAL/SHM sidecar files would otherwise shadow the freshly imported data.
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()

            BackupResult.Success
        } catch (t: Throwable) {
            BackupResult.Failure(t.message ?: "unknown_error")
        }
    }
}
