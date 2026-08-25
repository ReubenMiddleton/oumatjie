package com.granify.app.data.attachments

import android.content.Context
import com.granify.app.data.MailAttachment
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Serves the same bundled sample PDF for every demo attachment, so "Open document" works
 * end-to-end in the offline demo. */
class MockAttachmentDownloader(private val context: Context) : AttachmentDownloader {

    override suspend fun download(messageId: String, attachment: MailAttachment): String =
        withContext(Dispatchers.IO) {
            delay(300)
            val file = File(
                AttachmentCache.directory(context),
                AttachmentCache.sanitizedFileName(attachment.id, attachment.name),
            )
            context.assets.open(SAMPLE_ASSET_NAME).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            AttachmentCache.uriFor(context, file).toString()
        }

    private companion object {
        const val SAMPLE_ASSET_NAME = "sample_document.pdf"
    }
}
