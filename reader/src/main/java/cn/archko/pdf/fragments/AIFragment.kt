package cn.archko.pdf.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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
        adapter = AIAdapter(requireContext(), emptyList()) { pageIndex ->
            parentFragment?.let {
                if (it is OutlineTabFragment) {
                    it.onListItemClick(pageIndex)
                }
            }
        }
        binding.recyclerView.adapter = adapter
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

            // 按页面分组对话
            val conversationsByPage = conversations.groupBy { it.pageIndex }
            
            // 转换数据为列表形式
            val conversationList = conversationsByPage.entries.map { entry ->
                AIItem(entry.key, entry.value)
            }.sortedBy { it.pageIndex }

            adapter.updateData(conversationList)
        }
    }

    data class AIItem(
        val pageIndex: Int,
        val conversations: List<AIPageConversation>
    )

    inner class AIAdapter(
        private val context: Context,
        private var items: List<AIItem>,
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<AIAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvPage: TextView = view.findViewById(R.id.tv_page)
            val tvCount: TextView = view.findViewById(R.id.tv_count)
            val tvPreview: TextView = view.findViewById(R.id.tv_preview)
            val btnDelete: ImageButton = view.findViewById(R.id.btn_delete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_ai, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvPage.text = context.getString(R.string.page_label, item.pageIndex + 1)
            holder.tvCount.text = context.getString(R.string.ai_conversations_count, item.conversations.size)
            
            // 显示最后一条对话的问题预览
            item.conversations.firstOrNull()?.let { lastConv ->
                holder.tvPreview.text = lastConv.question
                holder.tvPreview.visibility = View.VISIBLE
            } ?: run {
                holder.tvPreview.visibility = View.GONE
            }

            holder.itemView.setOnClickListener {
                onItemClick(item.pageIndex)
            }

            holder.btnDelete.setOnClickListener {
                item.conversations.forEach { conversation ->
                    aiViewModel?.deleteConversation(conversation)
                }
            }
        }

        override fun getItemCount(): Int = items.size

        fun updateData(newItems: List<AIItem>) {
            items = newItems
            notifyDataSetChanged()
        }
    }
}