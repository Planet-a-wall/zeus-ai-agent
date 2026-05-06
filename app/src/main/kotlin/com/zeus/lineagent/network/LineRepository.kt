package com.zeus.lineagent.network

class LineRepository(private val api: LineApi = NetworkModule.lineApi) {

    suspend fun push(channelToken: String, target: String, text: String): Result<Unit> = runCatching {
        require(channelToken.isNotBlank()) { "LINE channel access token is empty" }
        require(target.isNotBlank()) { "LINE target id is empty" }
        require(text.isNotBlank()) { "Message is empty" }
        val response = api.push(
            bearer = "Bearer $channelToken",
            body = PushRequest(to = target, messages = listOf(LineMessage(text = text.take(5000)))),
        )
        if (!response.isSuccessful) {
            error("LINE push failed: ${response.code()} ${response.errorBody()?.string().orEmpty()}")
        }
    }
}
