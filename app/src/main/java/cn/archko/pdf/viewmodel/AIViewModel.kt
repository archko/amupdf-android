package cn.archko.pdf.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.archko.pdf.core.entity.AIPageConversation
import cn.archko.pdf.core.entity.AIProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * AI 功能 ViewModel
 * @author: archko 2026/3/1
 */
public class AIViewModel : ViewModel() {

    public var database: AkDatabase? = null

    private val _providers = MutableStateFlow<List<AIProvider>>(emptyList())
    public val providers: StateFlow<List<AIProvider>> = _providers

    private val _defaultProvider = MutableStateFlow<AIProvider?>(null)
    public val defaultProvider: StateFlow<AIProvider?> = _defaultProvider

    // AI页面对话相关状态
    private val _conversations = MutableStateFlow<List<AIPageConversation>>(emptyList())
    public val conversations: StateFlow<List<AIPageConversation>> = _conversations

    private val _isLoading = MutableStateFlow(false)
    public val isLoading: StateFlow<Boolean> = _isLoading

    /**
     * 初始化默认提供商
     */
    public fun initializeDefaultProviders() {
        viewModelScope.launch {
            val existing = database?.aiProviderDao()?.getAllProviders() ?: emptyList()
            if (existing.isEmpty()) {
                val defaults = listOf(
                    AIProvider(
                        id = "deepseek",
                        name = "DeepSeek",
                        apiKey = "",
                        baseUrl = "https://api.deepseek.com",
                        model = "deepseek-chat",
                        maxTokens = 2000,
                        temperature = 0.7f,
                        isDefault = true
                    ),
                    AIProvider(
                        id = "qwen",
                        name = "通义千问",
                        apiKey = "",
                        baseUrl = "https://dashscope.aliyuncs.com",
                        model = "qwen-turbo",
                        maxTokens = 2000,
                        temperature = 0.7f,
                        isDefault = false
                    ),
                    AIProvider(
                        id = "glm",
                        name = "智谱清言",
                        apiKey = "",
                        baseUrl = "https://open.bigmodel.cn",
                        model = "glm-4-flash",
                        maxTokens = 2000,
                        temperature = 0.7f,
                        isDefault = false
                    )
                )
                database?.aiProviderDao()?.insertAllProviders(defaults)
            }
            loadProviders()
        }
    }

    /**
     * 加载所有提供商
     */
    public fun loadProviders() {
        viewModelScope.launch {
            val providers = database?.aiProviderDao()?.getAllProviders() ?: emptyList()
            _providers.value = providers
            _defaultProvider.value = providers.find { it.isDefault }
        }
    }

    /**
     * 更新提供商
     */
    public fun updateProvider(provider: AIProvider) {
        viewModelScope.launch {
            provider.updatedAt = System.currentTimeMillis()
            database?.aiProviderDao()?.updateProvider(provider)
            loadProviders()
        }
    }

    /**
     * 设置默认提供商
     */
    public fun setDefaultProvider(id: String) {
        viewModelScope.launch {
            database?.aiProviderDao()?.clearAllDefaults()
            database?.aiProviderDao()?.setDefault(id)
            loadProviders()
        }
    }

    /**
     * 获取当前可用的提供商
     */
    public suspend fun getCurrentProvider(): AIProvider? {
        return database?.aiProviderDao()?.getDefaultProvider()
    }

    // ========== AI页面对话功能 ==========

    /**
     * 加载指定页面的对话历史
     */
    public fun loadConversations(path: String, pageIndex: Int) {
        viewModelScope.launch {
            val list = database?.aiPageConversationDao()?.getConversationsByPage(path, pageIndex)
                ?: emptyList()
            _conversations.value = list
            println("AIViewModel.loadConversations: path=$path, page=$pageIndex, count=${list.size}")
        }
    }

    /**
     * 保存对话
     */
    public fun saveConversation(
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
            database?.aiPageConversationDao()?.insertConversation(conversation)
            println("AIViewModel.saveConversation: $conversation")

            // 重新加载对话列表
            loadConversations(documentPath, pageIndex)
        }
    }

    /**
     * 删除对话
     */
    public fun deleteConversation(conversation: AIPageConversation) {
        viewModelScope.launch {
            database?.aiPageConversationDao()?.deleteConversation(conversation)
            println("AIViewModel.deleteConversation: $conversation")

            // 重新加载对话列表
            loadConversations(conversation.documentPath, conversation.pageIndex)
        }
    }

    /**
     * 设置加载状态
     */
    public fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    /**
     * 发送问题到 AI 并保存对话
     */
    public fun askQuestion(
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
    public fun loadAllConversations(documentPath: String) {
        viewModelScope.launch {
            val allConversations = database?.aiPageConversationDao()
                ?.getConversationsByDocument(documentPath) ?: emptyList()
            _conversations.value = allConversations
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
