package cn.archko.pdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.DialogAiSettingBinding
import cn.archko.pdf.viewmodel.AIViewModel
import com.archko.reader.pdf.entity.AIProvider
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * AI 设置对话框
 * @author archko
 */
class AISettingDialogFragment : DialogFragment() {

    private lateinit var binding: DialogAiSettingBinding
    private lateinit var viewModel: AIViewModel
    private lateinit var adapter: AIProviderAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.AppTheme)
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

        adapter = AIProviderAdapter(
            onEditClick = { provider ->
                showEditDialog(provider)
            },
            onSetDefault = { provider ->
                viewModel.setDefaultProvider(provider.id)
            }
        )
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
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.providers.collect { providers ->
                adapter.submitList(providers)
            }
        }
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.defaultProvider.collect { defaultProvider ->
                adapter.setDefaultProviderId(defaultProvider?.id)
            }
        }
    }

    private fun showEditDialog(provider: AIProvider) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_ai_provider_edit, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val etApiKey = dialogView.findViewById<TextInputEditText>(R.id.etApiKey)
        val etBaseUrl = dialogView.findViewById<TextInputEditText>(R.id.etBaseUrl)
        val etModel = dialogView.findViewById<TextInputEditText>(R.id.etModel)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)

        tvTitle.text = "编辑 ${provider.name}"
        etApiKey.setText(provider.apiKey)
        etBaseUrl.setText(provider.baseUrl)
        etModel.setText(provider.model)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

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

    companion object {
        const val TAG = "AISettingDialogFragment"

        fun show(activity: FragmentActivity) {
            val fragment = AISettingDialogFragment()
            fragment.show(activity.supportFragmentManager, TAG)
        }
    }
}