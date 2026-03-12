package cn.archko.pdf.dialogs

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.R
import cn.archko.pdf.core.entity.AIPageConversation
import cn.archko.pdf.viewmodel.AIViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AI 页面对话框 - 基于页面内容的 AI 问答
 * @author: archko 2026/3/12
 */
class AIPageDialog : DialogFragment() {

    private var documentPath: String = ""
    private var pageIndex: Int = 0
    private var pageContent: String = ""
    private var aiViewModel: AIViewModel? = null

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvPageContent: TextView
    private lateinit var svPageContent: ScrollView
    private lateinit var tvConversationHistory: TextView
    private lateinit var rvConversations: RecyclerView
    private lateinit var etQuestion: EditText
    private lateinit var btnSend: Button
    private lateinit var pbLoading: ProgressBar
    private lateinit var llTokenUsage: LinearLayout
    private lateinit var tvPromptTokens: TextView
    private lateinit var tvCompletionTokens: TextView
    private lateinit var tvTotalTokens: TextView

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var conversationsJob: Job? = null
    private var isLoadingJob: Job? = null

    private lateinit var adapter: ConversationAdapter

    companion object {
        fun newInstance(
            documentPath: String,
            pageIndex: Int,
            pageContent: String,
            aiViewModel: AIViewModel
        ): AIPageDialog {
            return AIPageDialog().apply {
                this.documentPath = documentPath
                this.pageIndex = pageIndex
                this.pageContent = pageContent
                this.aiViewModel = aiViewModel
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.AppTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog?.apply {
            val lp: WindowManager.LayoutParams = window!!.attributes
            lp.dimAmount = 0f
            setCanceledOnTouchOutside(true)
            setCancelable(true)
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.9).toInt()
            window?.setLayout(width, height)
        }
        return inflater.inflate(R.layout.dialog_ai_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupListeners()
        loadPageContent()
        startObservingConversations()
        startObservingLoading()
    }

    private fun initViews(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        tvPageContent = view.findViewById(R.id.tvPageContent)
        svPageContent = view.findViewById(R.id.svPageContent)
        tvConversationHistory = view.findViewById(R.id.tvConversationHistory)
        rvConversations = view.findViewById(R.id.rvConversations)
        etQuestion = view.findViewById(R.id.etQuestion)
        btnSend = view.findViewById(R.id.btnSend)
        pbLoading = view.findViewById(R.id.pbLoading)
        llTokenUsage = view.findViewById(R.id.llTokenUsage)
        tvPromptTokens = view.findViewById(R.id.tvPromptTokens)
        tvCompletionTokens = view.findViewById(R.id.tvCompletionTokens)
        tvTotalTokens = view.findViewById(R.id.tvTotalTokens)

        // 设置标题
        toolbar.title = getString(R.string.ai_assistant_page).format(pageIndex + 1)
    }

    private fun setupRecyclerView() {
        rvConversations.layoutManager = LinearLayoutManager(requireContext())
        adapter = ConversationAdapter(requireContext())
        rvConversations.adapter = adapter
    }

    private fun setupListeners() {
        toolbar.setNavigationOnClickListener {
            dismiss()
        }

        btnSend.setOnClickListener {
            val question = etQuestion.text.toString().trim()
            if (question.isNotEmpty() && pageContent.isNotEmpty()) {
                sendQuestion(question)
            }
        }
    }

    private fun loadPageContent() {
        if (pageContent.isNotEmpty()) {
            tvPageContent.text = pageContent
        } else {
            tvPageContent.text = getString(R.string.unable_to_get_page_text)
        }

        // 加载对话历史
        aiViewModel?.loadConversations(documentPath, pageIndex)
    }

    private fun startObservingConversations() {
        conversationsJob?.cancel()
        conversationsJob = coroutineScope.launch {
            aiViewModel?.conversations?.collectLatest { conversations ->
                updateConversations(conversations)
            }
        }
    }

    private fun startObservingLoading() {
        isLoadingJob?.cancel()
        isLoadingJob = coroutineScope.launch {
            aiViewModel?.isLoading?.collectLatest { isLoading ->
                updateLoadingState(isLoading)
            }
        }
    }

    private fun updateConversations(conversations: List<AIPageConversation>) {
        if (conversations.isEmpty()) {
        } else {
            adapter.submitList(conversations)
            rvConversations.post {
                rvConversations.smoothScrollToPosition(conversations.size - 1)
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSend.isEnabled = !isLoading
        etQuestion.isEnabled = !isLoading
    }

    private fun sendQuestion(question: String) {
        val documentName = documentPath.substringAfterLast('/')
        
        aiViewModel?.askQuestion(
            documentPath = documentPath,
            documentName = documentName,
            pageIndex = pageIndex,
            question = question,
            pageContent = pageContent,
            onSuccess = { answer, promptTokens, completionTokens, totalTokens ->
                etQuestion.text.clear()
                
                // 显示 token 使用信息
                showTokenUsage(promptTokens, completionTokens, totalTokens)
            },
            onError = { error ->
                etQuestion.error = error
            }
        )
    }

    private fun showTokenUsage(promptTokens: Int, completionTokens: Int, totalTokens: Int) {
        llTokenUsage.visibility = View.VISIBLE
        tvPromptTokens.text = getString(R.string.prompt_tokens).format(promptTokens)
        tvCompletionTokens.text = getString(R.string.completion_tokens).format(completionTokens)
        tvTotalTokens.text = getString(R.string.total_tokens).format(totalTokens)
    }

    override fun onDestroyView() {
        conversationsJob?.cancel()
        isLoadingJob?.cancel()
        super.onDestroyView()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag ?: "aiPageDialog")
    }
}

/**
 * 对话列表适配器
 */
class ConversationAdapter(
    private val context: Context
) : ListAdapter<AIPageConversation, ConversationAdapter.ViewHolder>(DiffCallback()) {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestion: TextView = itemView.findViewById(R.id.tvQuestion)
        val tvAnswer: TextView = itemView.findViewById(R.id.tvAnswer)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ai_conversation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val conversation = getItem(position)
        holder.tvQuestion.text = conversation.question
        holder.tvAnswer.text = conversation.answer
        
        // 格式化时间
        val timeText = if (conversation.createdAt > 0) {
            val date = java.util.Date(conversation.createdAt)
            val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            formatter.format(date)
        } else {
            ""
        }
        holder.tvTime.text = timeText
    }

    class DiffCallback : DiffUtil.ItemCallback<AIPageConversation>() {
        override fun areItemsTheSame(oldItem: AIPageConversation, newItem: AIPageConversation): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AIPageConversation, newItem: AIPageConversation): Boolean {
            return oldItem.question == newItem.question &&
                    oldItem.answer == newItem.answer &&
                    oldItem.createdAt == newItem.createdAt
        }
    }
}