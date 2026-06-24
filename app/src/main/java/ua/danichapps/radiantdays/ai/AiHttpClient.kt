package ua.danichapps.radiantdays.ai

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

internal const val AI_HTTP_READ_TIMEOUT_SECONDS = 160L

fun createAiOkHttpClient(): OkHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(AI_HTTP_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(AI_HTTP_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
