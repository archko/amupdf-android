package cn.archko.pdf.fragments

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentEncryptDecryptBinding
import cn.archko.pdf.common.PDFCreaterHelper
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.listeners.DataListener
import com.artifex.mupdf.fitz.Document
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author: archko 2025/10/6 :14:34
 */
class EncryptOrDecryptFragment : DialogFragment(R.layout.fragment_encrypt_decrypt) {

    private lateinit var binding: FragmentEncryptDecryptBinding
    protected lateinit var progressDialog: ProgressDialog

    private var mDataListener: DataListener? = null
    private var bookPath: String? = null

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = cn.archko.pdf.R.style.AppTheme
        setStyle(STYLE_NO_TITLE, themeId)

        progressDialog = ProgressDialog(activity)
        progressDialog.setMessage(getString(R.string.waiting))
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
        binding = FragmentEncryptDecryptBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        binding.btnAddPdf.setOnClickListener { addPdf() }

        setupTabLayout()
        
        binding.btnEncrypt.setOnClickListener { encrypt() }
        binding.btnDecrypt.setOnClickListener { decrypt() }
    }

    private val pickPdf =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            addResult(result)
        }

    private fun addResult(result: ActivityResult?) {
        if (result?.resultCode == Activity.RESULT_OK) {
            try {
                val oneUri = result.data?.data
                if (oneUri != null) {
                    val parseParams = IntentFile.getPath(requireContext(), oneUri)
                    if (parseParams != null) {
                        bookPath = parseParams
                        showInfo()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "handlePickFileResult", e)
            }
        }
    }

    private fun showInfo() {
        if (bookPath != null) {
            val file = File(bookPath)
            val fileInfo = StringBuilder()
            fileInfo.append(getString(R.string.encrypt_decrypt_file_path, bookPath))
            fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_file_size, file.length().toString()))
            fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_file_name, file.name))
            
            try {
                val document = Document.openDocument(bookPath)
                try {
                    val pageCount = document.countPages()
                    fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_page_count, pageCount.toString()))
                    
                    // 获取元数据，有值才显示
                    document.getMetaData(Document.META_FORMAT)?.let { format ->
                        fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_format, format))
                    }
                    document.getMetaData(Document.META_INFO_TITLE)?.let { title ->
                        fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_title2, title))
                    }
                    document.getMetaData(Document.META_INFO_AUTHOR)?.let { author ->
                        fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_author, author))
                    }
                    document.getMetaData(Document.META_INFO_SUBJECT)?.let { subject ->
                        fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_subject, subject))
                    }
                    document.getMetaData(Document.META_INFO_KEYWORDS)?.let { keywords ->
                        fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_keywords, keywords))
                    }
                    document.getMetaData(Document.META_INFO_CREATOR)?.let { creator ->
                        fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_creator, creator))
                    }
                    document.getMetaData(Document.META_INFO_PRODUCER)?.let { producer ->
                        fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_producer, producer))
                    }
                    document.getMetaData(Document.META_INFO_CREATIONDATE)?.let { creationDate ->
                        fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_creation_date, creationDate))
                    }
                    document.getMetaData(Document.META_INFO_MODIFICATIONDATE)?.let { modificationDate ->
                        fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_modification_date, modificationDate))
                    }
                    
                    val encrypted = if (document.needsPassword()) "Yes" else "No"
                    fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_encrypted, encrypted))
                } finally {
                    document.destroy()
                }
            } catch (e: Exception) {
                fileInfo.append("\n").append(getString(R.string.encrypt_decrypt_error_read_pdf, e.message ?: "Unknown error"))
                Log.e(TAG, "Failed to load PDF metadata", e)
            }
            
            binding.infoText.text = fileInfo.toString()
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showEncryptTab()
                    1 -> showDecryptTab()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun showEncryptTab() {
        binding.encryptTab.visibility = View.VISIBLE
        binding.decryptTab.visibility = View.GONE
    }

    private fun showDecryptTab() {
        binding.encryptTab.visibility = View.GONE
        binding.decryptTab.visibility = View.VISIBLE
    }

    private fun addPdf() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        intent.setType("application/pdf")
        pickPdf.launch(intent)
    }

    private fun encrypt() {
        if (bookPath.isNullOrEmpty()) {
            Toast.makeText(activity, R.string.encrypt_decrypt_select_pdf_first, Toast.LENGTH_SHORT).show()
            return
        }
        val password = binding.etPassword.text.toString()
        if (password.isEmpty()) {
            Toast.makeText(activity, R.string.encrypt_decrypt_input_pwd, Toast.LENGTH_SHORT).show()
            return
        }
        
        // 生成加密后的文件路径，避免覆盖原文件
        val originalFile = File(bookPath)
        val encryptedFileName = originalFile.nameWithoutExtension + "_encrypted.pdf"
        val encryptedFilePath = File(originalFile.parentFile, encryptedFileName).absolutePath
        
        progressDialog.show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                PDFCreaterHelper.encryptPDF(bookPath, encryptedFilePath, password, password)
            }
            if (result){
                val successMsg = getString(R.string.encrypt_decrypt_encrypt_success, encryptedFilePath)
                Toast.makeText(activity, successMsg, Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(activity, R.string.encrypt_decrypt_encrypt_failed, Toast.LENGTH_LONG).show()
            }
            progressDialog.dismiss()
        }
    }

    private fun decrypt() {
        if (bookPath.isNullOrEmpty()) {
            Toast.makeText(activity, R.string.encrypt_decrypt_select_pdf_first, Toast.LENGTH_SHORT).show()
            return
        }
        val password = binding.etPassword.text.toString()
        if (password.isEmpty()) {
            Toast.makeText(activity, R.string.encrypt_decrypt_input_pwd, Toast.LENGTH_SHORT).show()
            return
        }
        
        // 生成解密后的文件路径，避免覆盖原文件
        val originalFile = File(bookPath)
        val decryptedFileName = originalFile.nameWithoutExtension + "_decrypted.pdf"
        val decryptedFilePath = File(originalFile.parentFile, decryptedFileName).absolutePath
        
        progressDialog.show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                PDFCreaterHelper.decryptPDF(bookPath, decryptedFilePath, password)
            }
            if (result) {
                val successMsg = getString(R.string.encrypt_decrypt_decrypt_success, decryptedFilePath)
                Toast.makeText(activity, successMsg, Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(activity, R.string.encrypt_decrypt_decrypt_failed, Toast.LENGTH_LONG).show()
            }
            progressDialog.dismiss()
        }
    }

    companion object {

        const val TAG = "EncryptOrDecryptFragment"

        fun showCreateDialog(
            activity: FragmentActivity?,
            dataListener: DataListener?
        ) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("encryptOrDecrypt_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val pdfFragment = EncryptOrDecryptFragment()
            pdfFragment.setListener(dataListener)
            pdfFragment.show(ft!!, "encryptOrDecrypt_dialog")
        }
    }
}