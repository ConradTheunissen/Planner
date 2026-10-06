package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Google Drive API Models ---

@JsonClass(generateAdapter = true)
data class GoogleDriveUser(
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "emailAddress") val emailAddress: String? = null
)

@JsonClass(generateAdapter = true)
data class GoogleDriveFileItem(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "size") val size: String? = null,
    @Json(name = "modifiedTime") val modifiedTime: String? = null,
    @Json(name = "webViewLink") val webViewLink: String? = null,
    @Json(name = "starred") val starred: Boolean? = false,
    @Json(name = "owners") val owners: List<GoogleDriveUser>? = null,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GoogleDriveFileListResponse(
    @Json(name = "files") val files: List<GoogleDriveFileItem>?,
    @Json(name = "nextPageToken") val nextPageToken: String? = null
)

@JsonClass(generateAdapter = true)
data class GoogleDriveCreateFileRequest(
    @Json(name = "name") val name: String,
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "description") val description: String? = null
)

// --- Gmail API Models ---

@JsonClass(generateAdapter = true)
data class GmailMessageRef(
    @Json(name = "id") val id: String,
    @Json(name = "threadId") val threadId: String
)

@JsonClass(generateAdapter = true)
data class GmailListMessagesResponse(
    @Json(name = "messages") val messages: List<GmailMessageRef>?,
    @Json(name = "resultSizeEstimate") val resultSizeEstimate: Int? = 0
)

@JsonClass(generateAdapter = true)
data class GmailHeader(
    @Json(name = "name") val name: String,
    @Json(name = "value") val value: String
)

@JsonClass(generateAdapter = true)
data class GmailPartBody(
    @Json(name = "size") val size: Long? = 0L,
    @Json(name = "data") val data: String? = null,
    @Json(name = "attachmentId") val attachmentId: String? = null
)

@JsonClass(generateAdapter = true)
data class GmailPayloadPart(
    @Json(name = "partId") val partId: String? = null,
    @Json(name = "mimeType") val mimeType: String? = null,
    @Json(name = "filename") val filename: String? = null,
    @Json(name = "body") val body: GmailPartBody? = null,
    @Json(name = "parts") val parts: List<GmailPayloadPart>? = null
)

@JsonClass(generateAdapter = true)
data class GmailPayload(
    @Json(name = "mimeType") val mimeType: String? = null,
    @Json(name = "headers") val headers: List<GmailHeader>? = null,
    @Json(name = "body") val body: GmailPartBody? = null,
    @Json(name = "parts") val parts: List<GmailPayloadPart>? = null
)

@JsonClass(generateAdapter = true)
data class GmailMessageDetail(
    @Json(name = "id") val id: String,
    @Json(name = "threadId") val threadId: String,
    @Json(name = "snippet") val snippet: String? = null,
    @Json(name = "internalDate") val internalDate: String? = null,
    @Json(name = "labelIds") val labelIds: List<String>? = null,
    @Json(name = "payload") val payload: GmailPayload? = null
)

@JsonClass(generateAdapter = true)
data class GmailSendMessageRequest(
    @Json(name = "raw") val raw: String,
    @Json(name = "threadId") val threadId: String? = null
)

// --- Retrofit Interfaces ---

interface GoogleDriveService {
    @GET("drive/v3/files")
    suspend fun listFiles(
        @Header("Authorization") authHeader: String,
        @Query("pageSize") pageSize: Int = 30,
        @Query("fields") fields: String = "files(id,name,mimeType,size,modifiedTime,webViewLink,starred,owners,description)"
    ): GoogleDriveFileListResponse

    @POST("drive/v3/files")
    suspend fun createFile(
        @Header("Authorization") authHeader: String,
        @Body request: GoogleDriveCreateFileRequest
    ): GoogleDriveFileItem
}

interface GmailService {
    @GET("gmail/v1/users/me/messages")
    suspend fun listMessages(
        @Header("Authorization") authHeader: String,
        @Query("maxResults") maxResults: Int = 20,
        @Query("q") query: String? = null
    ): GmailListMessagesResponse

    @GET("gmail/v1/users/me/messages/{id}")
    suspend fun getMessage(
        @Header("Authorization") authHeader: String,
        @Path("id") id: String,
        @Query("format") format: String = "full"
    ): GmailMessageDetail

    @POST("gmail/v1/users/me/messages/send")
    suspend fun sendMessage(
        @Header("Authorization") authHeader: String,
        @Body request: GmailSendMessageRequest
    ): GmailMessageRef
}

object WorkspaceApiClient {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val driveService: GoogleDriveService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoogleDriveService::class.java)
    }

    val gmailService: GmailService by lazy {
        Retrofit.Builder()
            .baseUrl("https://gmail.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GmailService::class.java)
    }
}
