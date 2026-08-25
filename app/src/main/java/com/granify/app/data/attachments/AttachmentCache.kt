package com.granify.app.data.attachments

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * A single cache folder for every downloaded attachment, so it can be swept in one place.
 * Oumatjie never keeps a copy of an attachment longer than it has to
 * (docs/PRODUCT_PRINCIPLES.md, "Privacy rules").
 */
object AttachmentCache {
    private const val DIR_NAME = "attachments"

    fun directory(context: Context): File =
        File(context.cacheDir, DIR_NAME).apply { mkdirs() }

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun sanitizedFileName(id: String, name: String): String {
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return "${id}_$safeName"
    }

    /** Deletes every cached attachment. Called on app startup so a crash or force-stop never
     * leaves a downloaded document sitting on disk. */
    fun clear(context: Context) {
        directory(context).listFiles()?.forEach { it.delete() }
    }
}
