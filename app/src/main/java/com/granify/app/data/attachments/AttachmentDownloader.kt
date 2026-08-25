package com.granify.app.data.attachments

import com.granify.app.data.MailAttachment

interface AttachmentDownloader {
    /**
     * Saves the attachment into the app cache and returns a content:// Uri (as a string) for
     * viewing it. Returned as a string rather than [android.net.Uri] so this interface — and
     * the ViewModel that calls it — stays free of Android framework types and easy to test
     * with plain fakes.
     */
    suspend fun download(messageId: String, attachment: MailAttachment): String
}
