package cn.archko.pdf.viewmodel

import cn.archko.pdf.core.entity.AIProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * AI 服务 - 处理与 AI 提供商的通信
 * @author: archko 2026/3/3
 */
class AIService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(360, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * AI 响应结果，包含回答和 token 使用信息
     */
    data class AIResponse(
        val answer: String,
        val promptTokens: Int = 0,
        val completionTokens: Int = 0,
        val totalTokens: Int = 0
    )

    /**
     * 调用 AI 接口进行问答
     */
    suspend fun chat(
        provider: AIProvider,
        question: String,
        pageContent: String
    ): Result<AIResponse> {
        return try {
            when (provider.id) {
                "deepseek" -> chatWithDeepSeek(provider, question, pageContent)
                "qwen" -> chatWithQwen(provider, question, pageContent)
                "glm" -> chatWithGLM(provider, question, pageContent)
                else -> Result.failure(Exception("不支持的 AI 提供商: ${provider.id}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * DeepSeek API 调用
     */
    private suspend fun chatWithDeepSeek(
        provider: AIProvider,
        question: String,
        pageContent: String
    ): Result<AIResponse> {
        val url = "${provider.baseUrl}/v1/chat/completions"
        return makeOpenAIRequest(url, provider, question, pageContent, "DeepSeek")
    }

    /**
     * 通义千问 API 调用
     */
    private suspend fun chatWithQwen(
        provider: AIProvider,
        question: String,
        pageContent: String
    ): Result<AIResponse> {
        val url = "${provider.baseUrl}/compatible-mode/v1/chat/completions"
        return makeOpenAIRequest(url, provider, question, pageContent, "通义千问")
    }

    /**
     * 智谱清言 API 调用
     */
    private suspend fun chatWithGLM(
        provider: AIProvider,
        question: String,
        pageContent: String
    ): Result<AIResponse> {
        val url = "${provider.baseUrl}/api/paas/v4/chat/completions"
        return makeOpenAIRequest(url, provider, question, pageContent, "智谱清言")
    }

    /**
     * 通用的 OpenAI 兼容 API 请求
     */
    private suspend fun makeOpenAIRequest(
        url: String,
        provider: AIProvider,
        question: String,
        pageContent: String,
        providerName: String
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        try {
            // 构建请求 JSON
            val requestJson = JSONObject().apply {
                put("model", provider.model)
                put("max_tokens", provider.maxTokens)
                put("temperature", provider.temperature.toDouble())

                val messagesArray = JSONArray()
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put(
                        "content",
                        "你是一个专业的文档阅读助手。用户会提供文档页面的内容，并基于这些内容提问。请根据页面内容准确回答问题。"
                    )
                })
                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", "页面内容：\n$pageContent\n\n问题：$question")
                })
                put("messages", messagesArray)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${provider.apiKey}")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("$providerName API 请求失败: ${response.code} ${response.message}"))
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception("$providerName API 返回空响应体"))

            val responseJson = JSONObject(responseBody)

            // 解析响应
            val choicesArray = responseJson.optJSONArray("choices")
            val firstChoice = choicesArray?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            val answer = message?.optString("content")
                ?: return@withContext Result.failure(Exception("$providerName AI 返回空响应"))

            // 解析 token 使用信息
            val usage = responseJson.optJSONObject("usage")
            val promptTokens = usage?.optInt("prompt_tokens") ?: 0
            val completionTokens = usage?.optInt("completion_tokens") ?: 0
            val totalTokens = usage?.optInt("total_tokens") ?: 0

            Result.success(
                AIResponse(
                    answer = answer,
                    promptTokens = promptTokens,
                    completionTokens = completionTokens,
                    totalTokens = totalTokens
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("$providerName API 调用失败: ${e.message}", e))
        }
    }

    fun close() {
        client.dispatcher.executorService.shutdown()
    }
}
