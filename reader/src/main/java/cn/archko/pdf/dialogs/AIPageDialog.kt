package cn.archko.pdf.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import cn.archko.pdf.R
import cn.archko.pdf.core.widgets.ColorItemDecoration
import cn.archko.pdf.core.adapters.BaseRecyclerListAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.entity.AIPageConversation
import cn.archko.pdf.databinding.DialogAiPageBinding
import cn.archko.pdf.databinding.ItemAiConversationBinding
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
class AIPageDialog : DialogFragment(R.layout.dialog_ai_page) {

    private lateinit var binding: DialogAiPageBinding
    private var documentPath: String = ""
    private var pageIndex: Int = 0
    private var pageContent: String = ""
    private var aiViewModel: AIViewModel? = null

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
        setStyle(STYLE_NO_FRAME, R.style.AppDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.apply {
            window!!.setBackgroundDrawable(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.dialog_background))
            window!!.decorView?.elevation = 16f // 16dp 的阴影深度，可根据需要调整
            val lp: WindowManager.LayoutParams = window!!.attributes
            lp.dimAmount = 0.5f 
            lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_DIM_BEHIND
            setCanceledOnTouchOutside(true)
            setCancelable(true)
            val width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.9).toInt()
            window?.setLayout(width, height)
        }
        binding = DialogAiPageBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        setupListeners()
        loadPageContent()
        startObservingConversations()
        startObservingLoading()
    }

    private fun initViews() {
        binding.toolbar.title = getString(R.string.ai_assistant_page).format(pageIndex + 1)
        binding.rvConversations.layoutManager = LinearLayoutManager(requireContext())
        val itemDecoration = ColorItemDecoration(requireContext())
        binding.rvConversations.addItemDecoration(itemDecoration)
        adapter = ConversationAdapter()
        binding.rvConversations.adapter = adapter
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }

        binding.btnSend.setOnClickListener {
            val question = binding.etQuestion.text.toString().trim()
            if (question.isNotEmpty() && pageContent.isNotEmpty()) {
                sendQuestion(question)
            }
        }
    }

    private fun loadPageContent() {
        if (pageContent.isNotEmpty()) {
            binding.tvPageContent.text = pageContent
        } else {
            binding.tvPageContent.text = getString(R.string.unable_to_get_page_text)
        }

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
        if (!conversations.isEmpty()) {
            adapter.submitList(conversations)
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSend.isEnabled = !isLoading
        binding.etQuestion.isEnabled = !isLoading
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
                binding.etQuestion.text?.clear()

                showTokenUsage(promptTokens, completionTokens, totalTokens)
            },
            onError = { error ->
                binding.etQuestion.error = error
            }
        )
    }

    private fun showTokenUsage(promptTokens: Int, completionTokens: Int, totalTokens: Int) {
        binding.llTokenUsage.visibility = View.VISIBLE
        binding.tvPromptTokens.text = getString(R.string.prompt_tokens).format(promptTokens)
        binding.tvCompletionTokens.text =
            getString(R.string.completion_tokens).format(completionTokens)
        binding.tvTotalTokens.text = getString(R.string.total_tokens).format(totalTokens)
    }

    override fun onDestroyView() {
        conversationsJob?.cancel()
        isLoadingJob?.cancel()
        super.onDestroyView()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        super.show(manager, tag ?: "aiPageDialog")
    }

    private inner class ConversationAdapter : BaseRecyclerListAdapter<AIPageConversation>(
        activity,
        object : DiffUtil.ItemCallback<AIPageConversation>() {
            override fun areItemsTheSame(
                oldItem: AIPageConversation,
                newItem: AIPageConversation
            ): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: AIPageConversation,
                newItem: AIPageConversation
            ): Boolean {
                return oldItem.question == newItem.question &&
                        oldItem.answer == newItem.answer &&
                        oldItem.createdAt == newItem.createdAt
            }
        }
    ) {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<AIPageConversation> {
            val binding = ItemAiConversationBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(binding)
        }
    }

    private class ViewHolder(
        private val binding: ItemAiConversationBinding
    ) : BaseViewHolder<AIPageConversation>(binding.root) {

        override fun onBind(conversation: AIPageConversation, position: Int) {
            binding.tvQuestion.text = conversation.question
            binding.tvAnswer.text = conversation.answer

            // 格式化时间
            val timeText = if (conversation.createdAt > 0) {
                val date = java.util.Date(conversation.createdAt)
                val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                formatter.format(date)
            } else {
                ""
            }
            binding.tvTime.text = timeText
        }
    }
}