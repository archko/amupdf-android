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
     * AI 提示语配置，用于国际化支持
     */
    data class AIPromptConfig(
        val systemPrompt: String,
        val userPromptFormat: String,
        val unsupportedProvider: String,
        val apiRequestFailed: String,
        val apiEmptyResponse: String,
        val emptyResponse: String,
        val apiCallFailed: String
    )

    /**
     * AI 响应结果，包含回答和 token 使用信息
     */
    data class AIResponse(
        val answer: String,
        val promptTokens: Int = 0,
        val completionTokens: Int = 0,
        val totalTokens: Int = 0
    )

    private var promptConfig: AIPromptConfig? = null

    /**
     * 设置提示语配置（用于国际化）
     */
    fun setPromptConfig(config: AIPromptConfig) {
        promptConfig = config
    }

    /**
     * 调用 AI 接口进行问答
     */
    suspend fun chat(
        provider: AIProvider,
        question: String,
        pageContent: String
    ): Result<AIResponse> {
        val config = promptConfig
        return try {
            when (provider.id) {
                "deepseek" -> chatWithDeepSeek(provider, question, pageContent, config)
                "qwen" -> chatWithQwen(provider, question, pageContent, config)
                "glm" -> chatWithGLM(provider, question, pageContent, config)
                "openai" -> chatWithOpenAI(provider, question, pageContent, config)
                "gemini" -> chatWithGemini(provider, question, pageContent, config)
                else -> Result.failure(Exception((config?.unsupportedProvider ?: "不支持的 AI 提供商: ${provider.id}").format(provider.id)))
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
        pageContent: String,
        config: AIPromptConfig?
    ): Result<AIResponse> {
        val url = "${provider.baseUrl}/v1/chat/completions"
        return makeOpenAIRequest(url, provider, question, pageContent, "DeepSeek", config)
    }

    /**
     * 通义千问 API 调用
     */
    private suspend fun chatWithQwen(
        provider: AIProvider,
        question: String,
        pageContent: String,
        config: AIPromptConfig?
    ): Result<AIResponse> {
        val url = "${provider.baseUrl}/compatible-mode/v1/chat/completions"
        return makeOpenAIRequest(url, provider, question, pageContent, "通义千问", config)
    }

    /**
     * 智谱清言 API 调用
     */
    private suspend fun chatWithGLM(
        provider: AIProvider,
        question: String,
        pageContent: String,
        config: AIPromptConfig?
    ): Result<AIResponse> {
        val url = "${provider.baseUrl}/api/paas/v4/chat/completions"
        return makeOpenAIRequest(url, provider, question, pageContent, "智谱清言", config)
    }

    /**
     * OpenAI GPT API 调用
     */
    private suspend fun chatWithOpenAI(
        provider: AIProvider,
        question: String,
        pageContent: String,
        config: AIPromptConfig?
    ): Result<AIResponse> {
        val url = "${provider.baseUrl}/v1/chat/completions"
        return makeOpenAIRequest(url, provider, question, pageContent, "OpenAI GPT", config)
    }

    /**
     * Google Gemini API 调用
     */
    private suspend fun chatWithGemini(
        provider: AIProvider,
        question: String,
        pageContent: String,
        config: AIPromptConfig?
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        val failedMsg = config?.apiRequestFailed ?: "API 请求失败: %d %s"
        val emptyBodyMsg = config?.apiEmptyResponse ?: "API 返回空响应体"
        val emptyRespMsg = config?.emptyResponse ?: "AI 返回空响应"
        val callFailedMsg = config?.apiCallFailed ?: "API 调用失败: %s"
        val userPrompt = config?.userPromptFormat?.format(pageContent, question)
            ?: "页面内容：\n$pageContent\n\n问题：$question"

        try {
            val url = "${provider.baseUrl}/v1beta/models/${provider.model}:generateContent?key=${provider.apiKey}"

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userPrompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", provider.maxTokens)
                    put("temperature", provider.temperature.toDouble())
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception(failedMsg.format(response.code, response.message)))
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception(emptyBodyMsg))

            val responseJson = JSONObject(responseBody)

            // 解析 Gemini 响应
            val candidates = responseJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val firstPart = parts?.optJSONObject(0)
            val answer = firstPart?.optString("text")
                ?: return@withContext Result.failure(Exception(emptyRespMsg))

            // Gemini API 不直接返回 token 使用信息，返回 0
            Result.success(
                AIResponse(
                    answer = answer,
                    promptTokens = 0,
                    completionTokens = 0,
                    totalTokens = 0
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception(callFailedMsg.format(e.message), e))
        }
    }

    /**
     * 通用的 OpenAI 兼容 API 请求
     */
    private suspend fun makeOpenAIRequest(
        url: String,
        provider: AIProvider,
        question: String,
        pageContent: String,
        providerName: String,
        config: AIPromptConfig?
    ): Result<AIResponse> = withContext(Dispatchers.IO) {
        val systemPrompt = config?.systemPrompt ?: "你是一个专业的文档阅读助手。用户会提供文档页面的内容，并基于这些内容提问。请根据页面内容准确回答问题。"
        val userPrompt = config?.userPromptFormat?.format(pageContent, question)
            ?: "页面内容：\n$pageContent\n\n问题：$question"
        val failedMsg = config?.apiRequestFailed ?: "API 请求失败: %d %s"
        val emptyBodyMsg = config?.apiEmptyResponse ?: "API 返回空响应体"
        val emptyRespMsg = config?.emptyResponse ?: "AI 返回空响应"
        val callFailedMsg = config?.apiCallFailed ?: "API 调用失败: %s"

        try {
            // 构建请求 JSON
            val requestJson = JSONObject().apply {
                put("model", provider.model)
                put("max_tokens", provider.maxTokens)
                put("temperature", provider.temperature.toDouble())

                val messagesArray = JSONArray()
                messagesArray.put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
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
                return@withContext Result.failure(Exception(failedMsg.format(response.code, response.message)))
            }

            val responseBody = response.body?.string()
                ?: return@withContext Result.failure(Exception(emptyBodyMsg))

            val responseJson = JSONObject(responseBody)

            // 解析响应
            val choicesArray = responseJson.optJSONArray("choices")
            val firstChoice = choicesArray?.optJSONObject(0)
            val message = firstChoice?.optJSONObject("message")
            val answer = message?.optString("content")
                ?: return@withContext Result.failure(Exception(emptyRespMsg))

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
            Result.failure(Exception(callFailedMsg.format(e.message), e))
        }
    }

    fun close() {
        client.dispatcher.executorService.shutdown()
    }
}
