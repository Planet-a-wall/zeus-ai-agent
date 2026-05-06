package com.zeus.lineagent.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object NetworkModule {

    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val contentType = "application/json".toMediaType()

    private val sharedClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()
    }

    fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(sharedClient)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()

    val lineApi: LineApi by lazy { retrofit("https://api.line.me/").create(LineApi::class.java) }
    val asanaApi: AsanaApi by lazy { retrofit("https://app.asana.com/").create(AsanaApi::class.java) }
    val zapierApi: ZapierApi by lazy { retrofit("https://hooks.zapier.com/").create(ZapierApi::class.java) }
}
