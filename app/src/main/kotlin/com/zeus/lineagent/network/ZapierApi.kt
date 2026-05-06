package com.zeus.lineagent.network

import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface ZapierApi {
    /** POSTs an arbitrary JSON payload to a Zapier "Catch Hook" webhook URL. */
    @POST
    suspend fun fire(
        @Url url: String,
        @Body payload: JsonElement,
    ): Response<Unit>
}

class ZapierRepository(private val api: ZapierApi = NetworkModule.zapierApi) {

    suspend fun fire(webhookUrl: String, payload: JsonElement): Result<Unit> = runCatching {
        require(webhookUrl.isNotBlank()) { "Zapier webhook URL is empty" }
        val response = api.fire(webhookUrl, payload)
        if (!response.isSuccessful) {
            error("Zapier webhook failed: ${response.code()} ${response.errorBody()?.string().orEmpty()}")
        }
    }
}
