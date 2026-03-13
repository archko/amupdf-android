package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.DialogAiSettingBinding
import cn.archko.mupdf.databinding.ItemAiProviderBinding
import cn.archko.pdf.core.adapters.BaseRecyclerListAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.entity.AIProvider
import cn.archko.pdf.viewmodel.AIViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/**
 * AI 设置对话框
 * @author archko
 */
class AISettingDialogFragment : DialogFragment() {

    private lateinit var binding: DialogAiSettingBinding
    private lateinit var viewModel: AIViewModel
    private lateinit var adapter: AIProviderAdapter
    private var defaultProviderId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themeId = cn.archko.pdf.R.style.AppTheme
        setStyle(STYLE_NO_TITLE, themeId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAiSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(AIViewModel::class.java)

        binding.toolbar.setNavigationOnClickListener { dismiss() }

        adapter = AIProviderAdapter()
        binding.rvAiProviders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAiProviders.adapter = adapter

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            dismiss()
        }

        observeViewModel()
        viewModel.loadProviders()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.providers.collect { providers ->
                    adapter.submitList(providers)
                }
            }
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.defaultProvider.collect { defaultProvider ->
                    defaultProviderId = defaultProvider?.id
                }
            }
        }
    }

    private fun showEditDialog(provider: AIProvider) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ai_provider_edit, null)
        val toolbar = dialogView.findViewById<MaterialToolbar>(R.id.toolbar)
        val etApiKey = dialogView.findViewById<TextInputEditText>(R.id.etApiKey)
        val etBaseUrl = dialogView.findViewById<TextInputEditText>(R.id.etBaseUrl)
        val etModel = dialogView.findViewById<TextInputEditText>(R.id.etModel)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        etApiKey.setText(provider.apiKey)
        etBaseUrl.setText(provider.baseUrl)
        etModel.setText(provider.model)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        toolbar.setTitle(provider.name)
        toolbar.setNavigationOnClickListener { dialog.dismiss() }

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val apiKey = etApiKey.text.toString()
            val baseUrl = etBaseUrl.text.toString()
            val model = etModel.text.toString()
            val updatedProvider = AIProvider(
                id = provider.id,
                name = provider.name,
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model,
                maxTokens = provider.maxTokens,
                temperature = provider.temperature,
                enabled = provider.enabled,
                isDefault = provider.isDefault
            ).apply {
                createdAt = provider.createdAt
                updatedAt = System.currentTimeMillis()
            }
            viewModel.updateProvider(updatedProvider)
            dialog.dismiss()
        }

        dialog.show()
    }

    inner class AIProviderAdapter : BaseRecyclerListAdapter<AIProvider>(
        context,
        object : DiffUtil.ItemCallback<AIProvider>() {
            override fun areItemsTheSame(oldItem: AIProvider, newItem: AIProvider): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: AIProvider, newItem: AIProvider): Boolean {
                return oldItem == newItem
            }
        }) {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<AIProvider> {
            val binding = ItemAiProviderBinding.inflate(layoutInflater, parent, false)
            return ViewHolder(binding)
        }
    }

    inner class ViewHolder(
        private val binding: ItemAiProviderBinding
    ) : BaseViewHolder<AIProvider>(binding.root) {
        override fun onBind(provider: AIProvider, position: Int) {
            binding.tvProviderName.text = provider.name
            binding.tvProviderModel.text = provider.model
            binding.radioDefault.isChecked = provider.id == defaultProviderId
            if (provider.apiKey.isNotEmpty()) {
                binding.tvApiKeyHint.text =
                    String.format("API Key: %s", "${provider.apiKey.take(8)}...")
            } else {
                binding.tvApiKeyHint.text = "NO API Key"
            }

            binding.radioDefault.setOnClickListener {
                viewModel.setDefaultProvider(provider.id)
            }
            binding.btnEdit.setOnClickListener {
                showEditDialog(provider)
            }
        }
    }

    companion object {
        const val TAG = "AISettingDialogFragment"

        fun show(activity: FragmentActivity) {
            val fragment = AISettingDialogFragment()
            fragment.show(activity.supportFragmentManager, TAG)
        }
    }
}