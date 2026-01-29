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
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentSplitPdfBinding
import cn.archko.pdf.common.PDFCreaterHelper
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author: archko 2025/1/29 :17:23
 */
class SplitPdfFragment : DialogFragment(R.layout.fragment_split_pdf) {

    private lateinit var binding: FragmentSplitPdfBinding
    protected lateinit var progressDialog: ProgressDialog

    private var mDataListener: DataListener? = null
    private var selectedPdfPath: String? = null
    private var pageCount: Int = 0
    private var fileSize: Long = 0L

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = cn.archko.pdf.R.style.AppTheme
        setStyle(STYLE_NO_TITLE, themeId)

        progressDialog = ProgressDialog(activity)
        progressDialog.setMessage(getString(R.string.split_doing))
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
        binding = FragmentSplitPdfBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        binding.btnSelectPdf.setOnClickListener { selectPdf() }
        binding.btnSplit.setOnClickListener { splitPdf() }

        updateUI()
    }

    private val pickPdf =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            handlePickFileResult(result)
        }

    private fun handlePickFileResult(result: ActivityResult?) {
        if (result?.resultCode == Activity.RESULT_OK) {
            val path = IntentFile.getPath(requireContext(), result.data?.data)
            if (path != null) {
                selectedPdfPath = path
                loadPdfInfo(path)
            }
        }
    }

    private fun loadPdfInfo(path: String) {
        lifecycleScope.launch {
            try {
                val doc = com.artifex.mupdf.fitz.Document.openDocument(path)
                pageCount = doc.countPages()
                doc.destroy()

                val file = File(path)
                fileSize = file.length()

                withContext(Dispatchers.Main) {
                    updatePdfInfoUI(path, pageCount, fileSize)
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadPdfInfo", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        activity,
                        getString(R.string.encrypt_decrypt_error_read_pdf)
                            .format(e.message ?: ""),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @SuppressLint("StringFormatMatches")
    private fun updatePdfInfoUI(path: String, pageCount: Int, fileSize: Long) {
        binding.cardPdfInfo.visibility = View.VISIBLE
        binding.tvFileName.text = getString(R.string.encrypt_decrypt_file_name).format(File(path).name)
        binding.tvPageCount.text = getString(R.string.encrypt_decrypt_page_count).format(pageCount)
        binding.tvFileSize.text = getString(R.string.encrypt_decrypt_file_size).format(fileSize)
        
        binding.btnSplit.isEnabled = true
        binding.etRange.isEnabled = true
        binding.etFileName.isEnabled = true
        binding.tvStatus.text = getString(R.string.split_input_rule)
    }

    private fun selectPdf() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        pickPdf.launch(intent)
    }

    private fun splitPdf() {
        val pdfPath = selectedPdfPath
        if (pdfPath == null) {
            Toast.makeText(
                activity,
                getString(R.string.encrypt_decrypt_select_pdf_first),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val rangeInput = binding.etRange.text.toString().trim()
        if (rangeInput.isBlank()) {
            Toast.makeText(
                activity, getString(R.string.split_input_rule),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val outputFileName = binding.etFileName.text.toString().trim()
        if (outputFileName.isBlank()) {
            Toast.makeText(
                activity, getString(R.string.split_out_name_tip),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        progressDialog.show()
        lifecycleScope.launch {
            val userHome = FileUtils.getStorageDir("book")
            val result = withContext(Dispatchers.IO) {
                PDFCreaterHelper.splitPDF(
                    userHome,
                    pdfPath,
                    outputFileName,
                    rangeInput
                )
            }

            progressDialog.dismiss()

            if (result > 0) {
                Toast.makeText(
                    activity,
                    getString(R.string.split_success)
                        .format(userHome.absolutePath),
                    Toast.LENGTH_LONG
                ).show()
                dismiss()
            } else {
                Toast.makeText(
                    activity, getString(R.string.split_error),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateUI() {
        if (selectedPdfPath == null) {
            binding.cardPdfInfo.visibility = View.GONE
            binding.btnSplit.isEnabled = false
            binding.etRange.isEnabled = false
            binding.etFileName.isEnabled = false
            binding.tvStatus.text = getString(R.string.split_pdf_to_split)
        } else {
            binding.btnSplit.isEnabled = true
            binding.etRange.isEnabled = true
            binding.etFileName.isEnabled = true
        }
    }

    companion object {

        const val TAG = "SplitPdfFragment"

        fun showCreateDialog(
            activity: FragmentActivity?,
            dataListener: DataListener?
        ) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("splitpdf_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val pdfFragment = SplitPdfFragment()
            pdfFragment.setListener(dataListener)
            pdfFragment.show(ft!!, "splitpdf_dialog")
        }
    }
}
