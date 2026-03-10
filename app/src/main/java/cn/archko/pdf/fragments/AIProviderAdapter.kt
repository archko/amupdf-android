package cn.archko.pdf.fragments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.databinding.ItemAiProviderBinding
import cn.archko.pdf.core.entity.AIProvider

/**
 * AI 提供商列表适配器
 */
class AIProviderAdapter(
    private val onEditClick: (AIProvider) -> Unit,
    private val onSetDefault: (AIProvider) -> Unit
) : ListAdapter<AIProvider, AIProviderAdapter.ViewHolder>(DiffCallback()) {

    private var defaultProviderId: String? = null

    fun setDefaultProviderId(id: String?) {
        defaultProviderId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemAiProviderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val provider = getItem(position)
        val isDefault = provider.id == defaultProviderId
        holder.bind(provider, isDefault)
        holder.binding.radioDefault.setOnClickListener {
            onSetDefault(provider)
        }
        holder.binding.btnEdit.setOnClickListener {
            onEditClick(provider)
        }
    }

    inner class ViewHolder(val binding: ItemAiProviderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(provider: AIProvider, isDefault: Boolean) {
            binding.tvProviderName.text = provider.name
            binding.tvProviderModel.text = provider.model
            binding.radioDefault.isChecked = isDefault
            if (provider.apiKey.isNotEmpty()) {
                binding.tvApiKeyHint.text = "API Key: ${provider.apiKey.take(8)}..."
            } else {
                binding.tvApiKeyHint.text = "API Key: 未设置"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AIProvider>() {
        override fun areItemsTheSame(oldItem: AIProvider, newItem: AIProvider): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AIProvider, newItem: AIProvider): Boolean {
            return oldItem.equals(newItem)
        }
    }
}