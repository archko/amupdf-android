package cn.archko.pdf.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentMergePdfBinding
import cn.archko.mupdf.databinding.ItemMergePdfBinding
import cn.archko.pdf.common.PDFCreaterHelper
import cn.archko.pdf.core.adapters.BaseRecyclerListAdapter
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author: archko 2025/1/30 :8:19
 */
class MergePdfFragment : DialogFragment(R.layout.fragment_merge_pdf) {

    private lateinit var binding: FragmentMergePdfBinding
    protected lateinit var progressDialog: ProgressDialog

    private var mDataListener: DataListener? = null
    private val selectedFiles = mutableListOf<String>()
    private lateinit var adapter: FileListAdapter

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = cn.archko.pdf.R.style.AppTheme
        setStyle(STYLE_NO_TITLE, themeId)

        progressDialog = ProgressDialog(activity)
        progressDialog.setMessage("合并中...")
    }

    override fun onResume() {
        super.onResume()
        //MobclickAgent.onPageStart(TAG)
    }

    override fun onPause() {
        super.onPause()
        //MobclickAgent.onPageEnd(TAG)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentMergePdfBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        binding.btnSelectPdf.setOnClickListener { selectPdf() }
        binding.btnMerge.setOnClickListener { mergePdf() }

        adapter = FileListAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(activity)
        binding.recyclerView.adapter = adapter

        updateUI()
    }

    private val pickPdf =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            handlePickFileResult(result)
        }

    private fun handlePickFileResult(result: ActivityResult?) {
        if (result?.resultCode == Activity.RESULT_OK) {
            val files = mutableListOf<String>()
            try {
                val oneUri = result.data?.data
                if (oneUri != null) {
                    val path = IntentFile.getPath(requireContext(), oneUri)
                    if (path != null) {
                        files.add(path)
                    }
                } else {
                    // 多选文件
                    for (index in 0 until (result.data?.clipData?.itemCount ?: 0)) {
                        val uri = result.data?.clipData?.getItemAt(index)?.uri
                        if (uri != null) {
                            val path = IntentFile.getPath(requireContext(), uri)
                            if (path != null) {
                                files.add(path)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "handlePickFileResult", e)
            }
            
            selectedFiles.addAll(files)
            updateUI()
        }
    }

    private fun selectPdf() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            type = "application/pdf"
        }
        pickPdf.launch(intent)
    }

    private fun mergePdf() {
        if (selectedFiles.isEmpty() || selectedFiles.size < 2) {
            Toast.makeText(
                activity,
                R.string.merge_pdf_to_split,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val outputFileName = binding.etFileName.text.toString().trim()
        if (outputFileName.isBlank()) {
            Toast.makeText(
                activity, 
                R.string.split_out_name_tip,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        progressDialog.show()
        lifecycleScope.launch {
            val userHome = FileUtils.getStorageDir("book")
            var name = outputFileName
            if (!name.endsWith(".pdf")) {
                name = "$name.pdf"
            }
            val outputPath = File(userHome, name).absolutePath
            
            val result = withContext(Dispatchers.IO) {
                PDFCreaterHelper.mergePDF(outputPath, selectedFiles)
            }

            progressDialog.dismiss()

            if (result > 0) {
                Toast.makeText(
                    activity,
                    getString(R.string.merge_success)
                        .format(outputPath),
                    Toast.LENGTH_LONG
                ).show()
                dismiss()
            } else {
                Toast.makeText(
                    activity, getString(R.string.merge_error),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateUI() {
        if (selectedFiles.isEmpty()) {
            binding.tvFileCount.text = String.format(getString(R.string.merge_selected_count), "0")
            binding.btnMerge.isEnabled = false
            binding.etFileName.isEnabled = false
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.tvFileCount.text = String.format(getString(R.string.merge_selected_count), selectedFiles.size)
            binding.btnMerge.isEnabled = true
            binding.etFileName.isEnabled = true
            binding.recyclerView.visibility = View.VISIBLE
            adapter.submitList(selectedFiles.toList())
        }
    }

    private inner class FileListAdapter : BaseRecyclerListAdapter<String>(
        activity,
        object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        }
    ) {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<String> {
            val binding = ItemMergePdfBinding.inflate(layoutInflater, parent, false)
            return FileViewHolder(binding)
        }
    }

    private inner class FileViewHolder(
        private val binding: ItemMergePdfBinding
    ) : BaseViewHolder<String>(binding.root) {

        override fun onBind(data: String, position: Int) {
            binding.tvIndex.text = "${position + 1}."
            binding.tvName.text = File(data).name
            binding.btnDelete.setOnClickListener {
                selectedFiles.remove(data)
                updateUI()
            }
        }
    }

    companion object {

        const val TAG = "MergePdfFragment"

        fun showCreateDialog(
            activity: FragmentActivity?,
            dataListener: DataListener?
        ) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("mergepdf_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val pdfFragment = MergePdfFragment()
            pdfFragment.setListener(dataListener)
            pdfFragment.show(ft!!, "mergepdf_dialog")
        }
    }
}
