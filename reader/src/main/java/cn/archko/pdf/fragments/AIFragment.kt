package cn.archko.pdf.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import cn.archko.pdf.R
import cn.archko.pdf.core.adapters.BaseRecyclerListAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.entity.AIPageConversation
import cn.archko.pdf.databinding.FragmentAiBinding
import cn.archko.pdf.databinding.ItemAiBinding
import cn.archko.pdf.viewmodel.AIViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * AI对话Fragment
 * @author: archko 2026/3/12
 */
class AIFragment(private val aiViewModel: AIViewModel?, private val documentPath: String) :
    Fragment(R.layout.fragment_ai) {

    private lateinit var binding: FragmentAiBinding
    private lateinit var adapter: AIAdapter

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var conversationsJob: Job? = null

    companion object {
        fun newInstance(aiViewModel: AIViewModel?, documentPath: String): AIFragment {
            return AIFragment(aiViewModel, documentPath).apply {
                arguments = Bundle().apply {
                    putString("document_path", documentPath)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAiBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = AIAdapter(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun onItemClick(pageIndex: Int) {
        parentFragment?.let {
            if (it is OutlineTabFragment) {
                it.onListItemClick(pageIndex)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        startObservingConversations()
    }

    override fun onPause() {
        super.onPause()
        stopObservingConversations()
    }

    private fun startObservingConversations() {
        conversationsJob?.cancel()
        conversationsJob = coroutineScope.launch {
            aiViewModel?.conversations?.collectLatest { conversations ->
                updateConversations(conversations)
            }
        }

        // 加载对话数据
        aiViewModel?.loadAllConversations(documentPath)
    }

    private fun stopObservingConversations() {
        conversationsJob?.cancel()
        conversationsJob = null
    }

    private fun updateConversations(conversations: List<AIPageConversation>) {
        if (conversations.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE

            val conversationsByPage = conversations.groupBy { it.pageIndex }

            val conversationList = conversationsByPage.entries.map { entry ->
                AIItem(entry.key, entry.value)
            }.sortedBy { it.pageIndex }

            adapter.submitList(conversationList)
        }
    }

    data class AIItem(
        val pageIndex: Int,
        val conversations: List<AIPageConversation>

    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AIItem

            if (pageIndex != other.pageIndex) return false
            if (conversations != other.conversations) return false

            return true
        }

        override fun hashCode(): Int {
            var result = pageIndex
            result = 31 * result + conversations.hashCode()
            return result
        }
    }

    inner class AIAdapter(
        context: Context,
    ) : BaseRecyclerListAdapter<AIItem>(
        context,
        object : DiffUtil.ItemCallback<AIItem>() {
            override fun areItemsTheSame(
                oldItem: AIItem,
                newItem: AIItem
            ): Boolean {
                return oldItem.pageIndex == newItem.pageIndex
            }

            override fun areContentsTheSame(
                oldItem: AIItem,
                newItem: AIItem
            ): Boolean {
                return oldItem.conversations == newItem.conversations
            }
        }) {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<AIItem> {
            val binding = ItemAiBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(binding)
        }
    }

    private inner class ViewHolder(
        private val binding: ItemAiBinding
    ) : BaseViewHolder<AIItem>(binding.root) {
        override fun onBind(item: AIItem, position: Int) {
            binding.tvPage.text = context!!.getString(R.string.page_label, item.pageIndex + 1)
            binding.tvCount.text =
                context!!.getString(R.string.ai_conversations_count, item.conversations.size)

            // 显示最后一条对话的问题预览
            item.conversations.firstOrNull()?.let { lastConv ->
                binding.tvPreview.text = lastConv.question
                binding.tvPreview.visibility = View.VISIBLE
            } ?: run {
                binding.tvPreview.visibility = View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick(item.pageIndex)
            }

            binding.btnDelete.setOnClickListener {
                item.conversations.forEach { conversation ->
                    aiViewModel?.deleteConversation(conversation)
                }
            }
        }
    }
}