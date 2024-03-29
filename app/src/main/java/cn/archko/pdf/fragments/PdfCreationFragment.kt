package cn.archko.pdf.fragments

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentCreatePdfBinding
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.BaseViewHolder
import cn.archko.pdf.common.IntentFile
import cn.archko.pdf.common.PDFCreaterHelper
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.utils.FileUtils
import coil.load
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * @author: archko 2023/3/8 :14:34
 */
class PdfCreationFragment : DialogFragment(R.layout.fragment_create_pdf) {

    private lateinit var binding: FragmentCreatePdfBinding
    protected lateinit var progressDialog: ProgressDialog

    private var mDataListener: DataListener? = null
    private lateinit var adapter: BaseRecyclerAdapter<String>
    private var oldPdfPath: String? = null
    private var txtPath: String? = null

    private var type: Int = TYPE_IMAGE

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = android.R.style.Theme_Material_Light
        setStyle(DialogFragment.STYLE_NO_TITLE, themeId)

        progressDialog = ProgressDialog(activity)
        progressDialog.setMessage("Waiting...")
    }

    override fun onResume() {
        super.onResume()
        //MobclickAgent.onPageStart(TAG)
    }

    override fun onPause() {
        super.onPause()
        //MobclickAgent.onPageEnd(TAG)
    }

    private val pickPdf =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            if (result?.resultCode == Activity.RESULT_OK) {
                val path = IntentFile.getPath(
                    requireActivity(),
                    result.data?.data
                )
                oldPdfPath = path
                binding.oldPdfPath.text = "Old pdf:$path"
            }
        }

    private fun selectPdf() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/pdf")
        pickPdf.launch(intent)
    }

    private fun createPdfFromImage() {
        val arr = arrayListOf<String>()
        arr.addAll(adapter.data)
        var path = oldPdfPath
        if (TextUtils.isEmpty(path)) {
            var name: String? = binding.pdfPath.editableText.toString()
            if (TextUtils.isEmpty(name)) {
                name = "new.pdf"
            }
            path = FileUtils.getStorageDir("book").absolutePath + File.separator + name
        }

        progressDialog.show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                PDFCreaterHelper.createPdfFromImages(path, arr)
            }
            if (result) {
                Toast.makeText(activity, "创建pdf成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "create pdf error!", Toast.LENGTH_SHORT).show()
            }
            progressDialog.dismiss()
        }
    }

    private fun createPdfFromTxt() {
        if (TextUtils.isEmpty(txtPath)) {
            Toast.makeText(activity, "请先选择文本文件", Toast.LENGTH_SHORT).show()
            return
        }
        var name: String? = binding.pdfName.editableText.toString()
        if (TextUtils.isEmpty(name)) {
            name = "new.pdf"
        }
        var path = FileUtils.getStorageDir("book").absolutePath + File.separator + name

        progressDialog.show()
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                PDFCreaterHelper.createPdfUseSystemFromTxt(
                    activity,
                    binding.layoutTmp,
                    txtPath,
                    path
                )
            }
            if (result) {
                Toast.makeText(activity, "创建pdf成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "创建pdf错误!", Toast.LENGTH_SHORT).show()
            }
            progressDialog.dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCreatePdfBinding.inflate(layoutInflater)
        return binding.root
    }

    private fun updateUi() {
        if (type == TYPE_IMAGE) {
            binding.layoutImage.visibility = View.VISIBLE
            binding.layoutTxt.visibility = View.GONE
        } else {
            binding.layoutImage.visibility = View.GONE
            binding.layoutTxt.visibility = View.VISIBLE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //dialog?.setTitle("创建pdf")
        //binding.back.setOnClickListener { dismiss() }
        binding.toolbar.setNavigationOnClickListener { dismiss() }

        binding.btnSelect.setOnClickListener { selectPdf() }
        binding.btnCreateFromImage.setOnClickListener { createPdfFromImage() }
        binding.btnAddImage.setOnClickListener { addImageItem() }

        binding.btnTxt.setOnClickListener {
            type = TYPE_TXT
            updateUi()
        }
        binding.btnImage.setOnClickListener {
            type = TYPE_IMAGE
            updateUi()
        }

        binding.btnAddTxt.setOnClickListener { addTxtItem() }
        binding.btnCreateFromTxt.setOnClickListener { createPdfFromTxt() }

        adapter = object : BaseRecyclerAdapter<String>(activity) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<String> {
                val root = inflater.inflate(R.layout.item_image, parent, false)
                return ViewHolder(root)
            }
        }
        binding.recyclerView.layoutManager = GridLayoutManager(activity, 2)
        binding.recyclerView.adapter = adapter

        updateUi()
    }

    inner class ViewHolder(root: View) : BaseViewHolder<String>(root) {

        var delete: View? = null
        var ivImage: ImageView? = null

        init {
            delete = root.findViewById(R.id.delete)
            ivImage = root.findViewById(R.id.ivImage)
        }

        override fun onBind(data: String, position: Int) {
            delete?.setOnClickListener { deleteItem(data, position) }
            ivImage?.load(File(data))
        }
    }

    private fun deleteItem(data: String, position: Int) {
        adapter.data.remove(data)
        adapter.notifyDataSetChanged()
    }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            //如果Uri为null，可能是多选了
            if (result?.resultCode == Activity.RESULT_OK) {
                val paths = mutableListOf<String>()
                try {
                    val oneUri = result.data?.data
                    if (oneUri != null) {
                        val parseParams =
                            IntentFile.getPath(requireActivity(), oneUri)
                        if (parseParams != null) {
                            paths.add(parseParams)
                        }
                    } else {
                        for (index in 0 until (result.data?.clipData?.itemCount ?: 0)) {
                            val uri = result.data?.clipData?.getItemAt(index)?.uri
                            if (uri != null) {
                                val parseParams = IntentFile.getPath(requireActivity(), uri)
                                if (parseParams != null) {
                                    paths.add(parseParams)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "handlePickFileResult", e)
                }
                adapter.data.addAll(adapter.itemCount, paths)
                adapter.notifyDataSetChanged()
            }
        }

    private fun addImageItem() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.setType("image/*")
        pickImage.launch(intent)
    }

    private val pickTxt =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            if (result?.resultCode == Activity.RESULT_OK) {
                val path = result.data?.data?.let { IntentFile.getPath(requireActivity(), it) }
                txtPath = path
                binding.txtPath.text = txtPath
            }
        }

    private fun addTxtItem() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("text/*")
        pickTxt.launch(intent)
    }

    private fun setType(type: Int) {
        this.type = type
    }

    companion object {

        const val TAG = "CreatePdfFragment"
        const val TYPE_TXT = 0
        const val TYPE_IMAGE = 1

        fun showCreateDialog(
            activity: FragmentActivity?,
            dataListener: DataListener?
        ) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("create_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val pdfFragment = PdfCreationFragment()
            pdfFragment.setListener(dataListener)
            pdfFragment.show(ft!!, "create_dialog")
        }
    }
}
