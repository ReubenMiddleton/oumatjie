package com.granify.app.data.gmail

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The token is passed explicitly per call (rather than injected by an OkHttp interceptor)
 * because [com.granify.app.data.gmail.GmailMailRepository] already re-authorizes right
 * before each call, which is also how it gets a fresh token silently.
 */
interface GmailApiService {
    @GET("users/me/profile")
    suspend fun getProfile(@Header("Authorization") token: String): GmailProfile

    @GET("users/me/messages")
    suspend fun listMessages(
        @Header("Authorization") token: String,
        @Query("labelIds") labelIds: String = "INBOX",
        @Query("maxResults") maxResults: Int = 25,
    ): GmailMessageListResponse

    @GET("users/me/messages/{id}")
    suspend fun getMessage(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Query("format") format: String = "full",
    ): GmailMessage

    @GET("users/me/messages/{messageId}/attachments/{attachmentId}")
    suspend fun getAttachment(
        @Header("Authorization") token: String,
        @Path("messageId") messageId: String,
        @Path("attachmentId") attachmentId: String,
    ): GmailAttachmentData

    @POST("users/me/messages/{id}/modify")
    suspend fun modifyMessage(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: GmailModifyRequest,
    ): GmailMessage

    @POST("users/me/messages/{id}/trash")
    suspend fun trashMessage(
        @Header("Authorization") token: String,
        @Path("id") id: String,
    ): GmailMessage

    companion object {
        const val BASE_URL = "https://gmail.googleapis.com/gmail/v1/"
    }
}
