package com.yashraj.cadence.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Reads and writes the backup file in a folder the user picked with the system folder picker. The picker grants
 * access to that one folder only, so the app needs no storage permission, and the file stays there after an uninstall.
 */
object BackupFolder {
    /** Keeps access to [tree] across restarts. Call with the URI the picker returned. */
    fun remember(context: Context, tree: Uri) {
        context.contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    /** Lets go of a folder the user switched away from. */
    fun forget(context: Context, tree: String) {
        try {
            context.contentResolver.releasePersistableUriPermission(Uri.parse(tree), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } catch (e: SecurityException) {
            // Already gone; nothing to release.
        }
    }

    /** The text of a file the user picked, or null when it cannot be read. */
    suspend fun read(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    /** Writes [json] to [BackupFormat.FILE_NAME] in [tree], replacing the previous backup. Throws [IOException] on failure. */
    fun write(context: Context, tree: String, json: String) {
        val resolver = context.contentResolver
        val treeUri = Uri.parse(tree)
        try {
            val treeId = DocumentsContract.getTreeDocumentId(treeUri)
            val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeId)
            var target: Uri? = null
            resolver.query(
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeId),
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    if (c.getString(1) == BackupFormat.FILE_NAME) {
                        target = DocumentsContract.buildDocumentUriUsingTree(treeUri, c.getString(0))
                        break
                    }
                }
            }
            val file = target ?: DocumentsContract.createDocument(resolver, parent, "application/json", BackupFormat.FILE_NAME)
                ?: throw IOException("Couldn't create the backup file")
            // "wt" truncates, and the whole file is already in memory, so a failure leaves at worst a short write.
            resolver.openOutputStream(file, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                ?: throw IOException("Couldn't open the backup file")
        } catch (e: SecurityException) {
            throw IOException("No access to the backup folder", e)
        } catch (e: IllegalArgumentException) {
            throw IOException("The backup folder is not available", e)
        }
    }
}
