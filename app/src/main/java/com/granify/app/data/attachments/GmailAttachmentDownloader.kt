package com.granify.app.data.attachments

import android.content.Context
import com.granify.app.auth.AuthManager
import com.granify.app.auth.AuthorizeOutcome
import com.granify.app.data.Base64Url
import com.granify.app.data.MailAttachment
import com.granify.app.data.MailAuthException
import com.granify.app.data.gmail.GmailApiService
import com.granify.app.data.gmail.GmailMailRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class GmailAttachmentDownloader(
    private val context: Context,
    private val api: GmailApiService,
    private val authManager: AuthManager,
) : AttachmentDownloader {

    override suspend fun download(messageId: String, attachment: MailAttachment): String =
        withContext(Dispatchers.IO) {
            val token = when (val outcome = authManager.authorize(GmailMailRepository.REQUIRED_SCOPES)) {
                is AuthorizeOutcome.Granted -> "Bearer ${outcome.accessToken}"
                is AuthorizeOutcome.ResolutionRequired ->
                    throw MailAuthException("Please sign in again to continue.")
                is AuthorizeOutcome.Failed -> throw MailAuthException(outcome.message)
            }

            val data = api.getAttachment(token, messageId, attachment.id).data
                ?: throw MailAuthException("This document could not be downloaded.")
            val bytes = Base64Url.decodeBytes(data)

            val file = File(
                AttachmentCache.directory(context),
                AttachmentCache.sanitizedFileName(attachment.id, attachment.name),
            )
            file.writeBytes(bytes)
            AttachmentCache.uriFor(context, file).toString()
        }
}
