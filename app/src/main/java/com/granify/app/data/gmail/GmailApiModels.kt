package com.granify.app.data.gmail

import kotlinx.serialization.Serializable

/**
 * DTOs for the Gmail REST API v1 (https://developers.google.com/gmail/api/reference/rest).
 * Field names match the API's JSON exactly so no custom serializers are needed.
 */

@Serializable
data class GmailMessageListResponse(
    val messages: List<GmailMessageRef> = emptyList(),
    val nextPageToken: String? = null,
    val resultSizeEstimate: Int = 0,
)

@Serializable
data class GmailMessageRef(
    val id: String,
    val threadId: String? = null,
)

@Serializable
data class GmailMessage(
    val id: String,
    val threadId: String? = null,
    val labelIds: List<String> = emptyList(),
    val snippet: String = "",
    val payload: GmailMessagePart? = null,
    // Documented by Google as a string-encoded epoch-millisecond value.
    val internalDate: String? = null,
)

@Serializable
data class GmailMessagePart(
    val partId: String? = null,
    val mimeType: String? = null,
    val filename: String? = null,
    val headers: List<GmailHeader> = emptyList(),
    val body: GmailMessagePartBody? = null,
    val parts: List<GmailMessagePart> = emptyList(),
)

@Serializable
data class GmailHeader(
    val name: String,
    val value: String,
)

@Serializable
data class GmailMessagePartBody(
    val attachmentId: String? = null,
    val size: Int = 0,
    val data: String? = null,
)

@Serializable
data class GmailAttachmentData(
    val attachmentId: String? = null,
    val size: Int = 0,
    val data: String? = null,
)

@Serializable
data class GmailProfile(
    val emailAddress: String = "",
)

@Serializable
data class GmailModifyRequest(
    val addLabelIds: List<String> = emptyList(),
    val removeLabelIds: List<String> = emptyList(),
)
