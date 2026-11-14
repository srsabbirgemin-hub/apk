package com.example.data.remote

import com.example.data.model.AppSettings
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessagePayload>,
    val temperature: Double = 0.7,
    @param:Json(name = "max_tokens") val maxTokens: Int = 140
)

@JsonClass(generateAdapter = true)
data class ChatMessagePayload(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class ChatCompletionResponse(
    val choices: List<ChatChoice>? = null,
    val error: ApiErrorDetail? = null
)

@JsonClass(generateAdapter = true)
data class ChatChoice(
    val message: ChatMessagePayload? = null
)

@JsonClass(generateAdapter = true)
data class ApiErrorDetail(
    val message: String? = null
)

sealed interface LlmResult {
    data class Success(val replyText: String) : LlmResult
    data class Failure(val reason: String) : LlmResult
}

class LlmRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build(),
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
) {
    private val requestAdapter = moshi.adapter(ChatCompletionRequest::class.java)
    private val responseAdapter = moshi.adapter(ChatCompletionResponse::class.java)

    suspend fun generateReply(
        settings: AppSettings,
        senderName: String,
        appName: String,
        incomingMessage: String
    ): LlmResult = withContext(Dispatchers.IO) {
        val apiKey = settings.aiApiKey.trim()
        if (apiKey.isEmpty()) {
            return@withContext LlmResult.Failure("API key is empty.")
        }

        val endpoint = normalizeChatEndpoint(settings.aiEndpointUrl, settings.aiProvider.defaultEndpoint)
        val model = settings.aiModel.trim().ifEmpty { settings.aiProvider.defaultModel }
        val systemPrompt = settings.aiSystemPrompt.trim().ifEmpty {
            "You are a polite, concise personal assistant replying on my behalf."
        }

        val payload = ChatCompletionRequest(
            model = model,
            messages = listOf(
                ChatMessagePayload(
                    role = "system",
                    content = "$systemPrompt\nConstraints: Keep reply concise (under 280 chars), plain text only, no markdown."
                ),
                ChatMessagePayload(
                    role = "user",
                    content = "Incoming message on $appName from $senderName:\n\"$incomingMessage\"\nWrite the auto-reply:"
                )
            )
        )

        val jsonBody = requestAdapter.toJson(payload)
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                val parsed = runCatching { responseAdapter.fromJson(rawBody) }.getOrNull()
                if (!response.isSuccessful) {
                    val errorMsg = parsed?.error?.message ?: "HTTP ${response.code} ${response.message}"
                    return@withContext LlmResult.Failure(errorMsg)
                }
                val content = parsed?.choices?.firstOrNull()?.message?.content?.trim().orEmpty()
                if (content.isEmpty()) {
                    LlmResult.Failure("Empty response from model.")
                } else {
                    LlmResult.Success(content)
                }
            }
        }.getOrElse { throwable ->
            LlmResult.Failure(throwable.localizedMessage ?: "Network request failed.")
        }
    }

    internal fun normalizeChatEndpoint(rawUrl: String, fallback: String): String {
        val trimmed = rawUrl.trim().ifEmpty { fallback }.trimEnd('/')
        return when {
            trimmed.endsWith("/chat/completions") -> trimmed
            trimmed.endsWith("/v1") -> "$trimmed/chat/completions"
            else -> "$trimmed/chat/completions"
        }
    }
}
