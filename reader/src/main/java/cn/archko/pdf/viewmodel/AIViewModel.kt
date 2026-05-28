package cn.archko.pdf.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.core.App.Companion.instance
import cn.archko.pdf.core.common.AKDatabase
import cn.archko.pdf.core.common.Graph
import cn.archko.pdf.core.entity.AIPageConversation
import cn.archko.pdf.core.entity.AIProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * AI 功能 ViewModel
 * @author: archko 2026/3/1
 */
class AIViewModel : ViewModel() {

    private val database: AKDatabase = Graph.database
    var aiService: AIService = AIService()

    private val _providers = MutableStateFlow<List<AIProvider>>(emptyList())
    val providers: StateFlow<List<AIProvider>> = _providers

    private val _defaultProvider = MutableStateFlow<AIProvider?>(null)
    val defaultProvider: StateFlow<AIProvider?> = _defaultProvider

    // AI页面对话相关状态
    private val _conversations = MutableStateFlow<List<AIPageConversation>>(emptyList())
    val conversations: StateFlow<List<AIPageConversation>> = _conversations

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        initializeDefaultProviders()
        initializeAIService()
    }

    /**
     * 获取 AI 提示语配置（用于国际化）
     */
    fun getAIPromptConfig(): AIService.AIPromptConfig {
        val ctx = instance ?: throw IllegalStateException("Application not initialized")
        return AIService.AIPromptConfig(
            systemPrompt = ctx.getString(cn.archko.pdf.R.string.ai_system_prompt),
            userPromptFormat = ctx.getString(cn.archko.pdf.R.string.ai_user_prompt_format),
            unsupportedProvider = ctx.getString(cn.archko.pdf.R.string.ai_unsupported_provider),
            apiRequestFailed = ctx.getString(cn.archko.pdf.R.string.ai_api_request_failed),
            apiEmptyResponse = ctx.getString(cn.archko.pdf.R.string.ai_api_empty_response),
            emptyResponse = ctx.getString(cn.archko.pdf.R.string.ai_empty_response),
            apiCallFailed = ctx.getString(cn.archko.pdf.R.string.ai_api_call_failed)
        )
    }

    /**
     * 初始化 AI 服务配置（用于国际化）
     */
    private fun initializeAIService() {
        try {
            val config = getAIPromptConfig()
            aiService.setPromptConfig(config)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 初始化默认提供商
     */
    fun initializeDefaultProviders() {
        viewModelScope.launch {
            val existing = database.aiProviderDao().getAllProviders()
            val existingIds = existing.map { it.id }.toSet()

            // 所有默认提供商配置
            val allDefaults = listOf(
                // 国内提供商
                AIProvider(
                    id = "deepseek",
                    name = "DeepSeek",
                    apiKey = "",
                    baseUrl = "https://api.deepseek.com",
                    model = "deepseek-v4-flash",
                    maxTokens = 100000,
                    temperature = 0.7f,
                    isDefault = false
                ),
                AIProvider(
                    id = "qwen",
                    name = "通义千问",
                    apiKey = "",
                    baseUrl = "https://dashscope.aliyuncs.com",
                    model = "qwen-turbo",
                    maxTokens = 100000,
                    temperature = 0.7f,
                    isDefault = false
                ),
                AIProvider(
                    id = "glm",
                    name = "智谱清言",
                    apiKey = "",
                    baseUrl = "https://open.bigmodel.cn",
                    model = "glm-4-flash",
                    maxTokens = 100000,
                    temperature = 0.7f,
                    isDefault = false
                ),
                // 国外提供商
                AIProvider(
                    id = "openai",
                    name = "OpenAI GPT",
                    apiKey = "",
                    baseUrl = "https://api.openai.com",
                    model = "gpt-4o-mini",
                    maxTokens = 100000,
                    temperature = 0.7f,
                    isDefault = false
                ),
                AIProvider(
                    id = "gemini",
                    name = "Google Gemini",
                    apiKey = "",
                    baseUrl = "https://generativelanguage.googleapis.com",
                    model = "gemini-2.0-flash",
                    maxTokens = 100000,
                    temperature = 0.7f,
                    isDefault = false
                )
            )

            // 1. 插入缺失的提供商
            val toInsert = allDefaults.filter { it.id !in existingIds }
            if (toInsert.isNotEmpty()) {
                database.aiProviderDao().insertAllProviders(toInsert)
            }

            // 2. 更新已有提供商的配置（保留用户配置的 apiKey）
            existing.forEach { provider ->
                val defaultProvider = allDefaults.find { it.id == provider.id }
                if (defaultProvider != null) {
                    // 只更新基础配置，保留用户的 apiKey
                    val updated = provider.apply {
                        name = defaultProvider.name
                        baseUrl = defaultProvider.baseUrl
                        model = defaultProvider.model
                        maxTokens = defaultProvider.maxTokens
                        temperature = defaultProvider.temperature
                        updatedAt = System.currentTimeMillis()
                    }
                    database.aiProviderDao().updateProvider(updated)
                }
            }

            loadProviders()
        }
    }

    /**
     * 加载所有提供商
     */
    fun loadProviders() {
        viewModelScope.launch {
            val providers = database.aiProviderDao().getAllProviders()
            _providers.value = providers
            _defaultProvider.value = providers.find { it.isDefault }
        }
    }

    /**
     * 更新提供商
     */
    fun updateProvider(provider: AIProvider) {
        viewModelScope.launch {
            provider.updatedAt = System.currentTimeMillis()
            database.aiProviderDao().updateProvider(provider)
            loadProviders()
        }
    }

    /**
     * 设置默认提供商
     */
    fun setDefaultProvider(id: String) {
        viewModelScope.launch {
            database.aiProviderDao().clearAllDefaults()
            database.aiProviderDao().setDefault(id)
            loadProviders()
        }
    }

    /**
     * 获取当前可用的提供商
     */
    suspend fun getCurrentProvider(): AIProvider? {
        return database.aiProviderDao().getDefaultProvider()
    }

    // ========== AI页面对话功能 ==========

    /**
     * 加载指定页面的对话历史
     */
    fun loadConversations(path: String, pageIndex: Int) {
        viewModelScope.launch {
            val list = database.aiPageConversationDao().getConversationsByPage(path, pageIndex)
            _conversations.value = list
            println("AIViewModel.loadConversations: path=$path, page=$pageIndex, count=${list.size}")
        }
    }

    /**
     * 保存对话
     */
    fun saveConversation(
        documentPath: String,
        documentName: String,
        pageIndex: Int,
        question: String,
        answer: String,
        pageContent: String
    ) {
        viewModelScope.launch {
            val conversation = AIPageConversation(
                documentPath = documentPath,
                documentName = documentName,
                pageIndex = pageIndex,
                question = question,
                answer = answer,
                pageContent = pageContent
            )
            database.aiPageConversationDao().insertConversation(conversation)
            println("AIViewModel.saveConversation: $conversation")

            // 重新加载对话列表
            loadConversations(documentPath, pageIndex)
        }
    }

    /**
     * 删除对话
     */
    fun deleteConversation(conversation: AIPageConversation) {
        viewModelScope.launch {
            database.aiPageConversationDao().deleteConversation(conversation)
            println("AIViewModel.deleteConversation: $conversation")

            // 重新加载对话列表
            loadConversations(conversation.documentPath, conversation.pageIndex)
        }
    }

    /**
     * 设置加载状态
     */
    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    /**
     * 发送问题到 AI 并保存对话
     */
    fun askQuestion(
        documentPath: String,
        documentName: String,
        pageIndex: Int,
        question: String,
        pageContent: String,
        onSuccess: (answer: String, promptTokens: Int, completionTokens: Int, totalTokens: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                // 获取当前默认的 AI 提供商
                val provider = getCurrentProvider()
                if (provider == null) {
                    onError("请先配置 AI 提供商")
                    _isLoading.value = false
                    return@launch
                }

                if (provider.apiKey.isBlank()) {
                    onError("请先配置 ${provider.name} 的 API Key")
                    _isLoading.value = false
                    return@launch
                }

                // 调用 AI 服务
                val result = aiService.chat(provider, question, pageContent)

                result.fold(
                    onSuccess = { aiResponse ->
                        // 保存对话
                        saveConversation(
                            documentPath = documentPath,
                            documentName = documentName,
                            pageIndex = pageIndex,
                            question = question,
                            answer = aiResponse.answer,
                            pageContent = pageContent
                        )
                        onSuccess(
                            aiResponse.answer,
                            aiResponse.promptTokens,
                            aiResponse.completionTokens,
                            aiResponse.totalTokens
                        )
                    },
                    onFailure = { error ->
                        onError(error.message ?: "AI 调用失败")
                    }
                )
            } catch (e: Exception) {
                onError("发生错误: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 获取所有文档的对话记录（按页面分组）
     */
    fun loadAllConversations(documentPath: String) {
        viewModelScope.launch {
            val allConversations = database.aiPageConversationDao()
                .getConversationsByDocument(documentPath)
            _conversations.value = allConversations
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
