package com.zeus.lineagent.network

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface LineApi {
    @POST("v2/bot/message/push")
    suspend fun push(
        @Header("Authorization") bearer: String,
        @Body body: PushRequest,
    ): Response<Unit>

    @POST("v2/bot/message/multicast")
    suspend fun multicast(
        @Header("Authorization") bearer: String,
        @Body body: MulticastRequest,
    ): Response<Unit>
}

@Serializable
data class PushRequest(
    val to: String,
    val messages: List<LineMessage>,
)

@Serializable
data class MulticastRequest(
    val to: List<String>,
    val messages: List<LineMessage>,
)

@Serializable
data class LineMessage(
    val type: String = "text",
    val text: String,
)
