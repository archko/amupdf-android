package cn.archko.pdf.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import android.widget.Toast
import cn.archko.pdf.R
import cn.archko.pdf.databinding.DialogSelectionBinding

/**
 * 文本选择对话框 - 显示选中的文本并提供复制功能
 * @author: archko 2026/3/12
 */
class SelectionDialog : DialogFragment(R.layout.dialog_selection) {

    private lateinit var binding: DialogSelectionBinding
    private var pageContent: String = ""

    companion object {
        fun newInstance(
            pageContent: String,
        ): SelectionDialog {
            return SelectionDialog().apply {
                this.pageContent = pageContent
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.AppTheme)
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
            val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
            val height = (resources.displayMetrics.heightPixels * 0.85).toInt()
            window?.setLayout(width, height)
        }
        binding = DialogSelectionBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        binding.selectedText.text = pageContent
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            dismiss()
        }
        
        binding.btnCopy.setOnClickListener {
            copyToClipboard(pageContent)
            dismiss()
        }
        
        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }
    
    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("selected_text", text)
        clipboard.setPrimaryClip(clip)

        Toast.makeText(requireContext(), R.string.copy_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}