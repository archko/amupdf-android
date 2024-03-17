package cn.archko.pdf.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
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
import androidx.recyclerview.widget.GridLayoutManager
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentPdfOptBinding
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.adapters.BaseRecyclerAdapter
import cn.archko.pdf.adapters.BaseViewHolder
import cn.archko.pdf.common.IntentFile
import cn.archko.pdf.common.PDFCreaterHelper
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.utils.FileUtils
import cn.archko.pdf.utils.Utils
import com.artifex.mupdf.fitz.Document
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.slider.RangeSlider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @author: archko 2023/3/8 :14:34
 */
class PdfOperationFragment : DialogFragment(R.layout.fragment_pdf_opt) {

    private lateinit var binding: FragmentPdfOptBinding
    protected lateinit var progressDialog: ProgressDialog

    private var mDataListener: DataListener? = null
    private lateinit var adapter: BaseRecyclerAdapter<String>
    private var txtPath: String? = null

    private var type: Int = TYPE_MERGE
    private var scope: CoroutineScope? = null
    private val customerDispatcher = AppExecutors.instance.diskIO().asCoroutineDispatcher()

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = android.R.style.Theme_Material_Light
        setStyle(DialogFragment.STYLE_NO_TITLE, themeId)

        progressDialog = ProgressDialog(activity)
        progressDialog.setMessage("Waiting...")
        progressDialog.setCanceledOnTouchOutside(false)
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
                if (type == TYPE_EXTRACT_IMAGES) {
                    txtPath = path
                    binding.pdfPath.text = path
                    loadPdf(path)
                } else {
                    adapter.data.add(adapter.itemCount, path)
                    adapter.notifyDataSetChanged()
                }
            }
        }

    private fun loadPdf(path: String?) {
        try {
            val doc = Document.openDocument(path)
            val count = doc.countPages()

            binding.extract.extractLayout.visibility = View.VISIBLE
            setupRangeSlider(count)
        } catch (e: Exception) {
            binding.extract.extractLayout.visibility = View.GONE
            Toast.makeText(requireActivity(), "无法加载pdf", Toast.LENGTH_LONG).show()
        }
    }

    private fun selectPdf() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/pdf")
        pickPdf.launch(intent)
    }

    private fun extractImages() {
        if (TextUtils.isEmpty(txtPath)) {
            Toast.makeText(activity, "请先选择pdf文件", Toast.LENGTH_SHORT).show()
            return
        }
        val name = FileUtils.getNameWithoutExt(txtPath)
        val dir = FileUtils.getStorageDir(name).absolutePath

        progressDialog.show()
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setOnDismissListener {
            PDFCreaterHelper.canExtract = false
        }

        PDFCreaterHelper.canExtract = true

        var start: Int = 0
        var end: Int = 0
        var width = Utils.getScreenWidthPixelWithOrientation(requireActivity())
        if (binding.extract.extractLayout.visibility == View.VISIBLE) {
            start = binding.extract.rangeSlider.values[0].toInt()
            end = binding.extract.rangeSlider.values[1].toInt()
            width = binding.extract.resolutionSlider.value.toInt()
        }

        if (scope?.isActive == true) {
            scope?.cancel()
        }
        scope = CoroutineScope(Job() + customerDispatcher)
        scope!!.launch {
            val result = PDFCreaterHelper.extractToImages(
                requireActivity(),
                width,
                dir,
                txtPath!!,
                start,
                end
            )
            withContext(Dispatchers.Main) {
                if (result == 0) {
                    Toast.makeText(activity, "导出成功", Toast.LENGTH_SHORT).show()
                } else if (result == -2) {
                    Toast.makeText(activity, "导出错误!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "取消导出,已导出${result}张", Toast.LENGTH_SHORT)
                        .show()
                }
                progressDialog.dismiss()
            }
        }
    }

    private fun extractHtml() {
        if (TextUtils.isEmpty(txtPath)) {
            Toast.makeText(activity, "请先选择pdf文件", Toast.LENGTH_SHORT).show()
            return
        }
        var name = FileUtils.getNameWithoutExt(txtPath)
        var dir = FileUtils.getStorageDir(name).absolutePath

        progressDialog.show()

        if (scope?.isActive == true) {
            scope?.cancel()
        }
        scope = CoroutineScope(Job() + customerDispatcher)
        scope!!.launch {
            val result = PDFCreaterHelper.extractToHtml(
                requireActivity(),
                dir + "/" + name + ".html",
                txtPath!!
            )
            withContext(Dispatchers.Main) {
                if (result) {
                    Toast.makeText(activity, "导出成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(activity, "导出错误!", Toast.LENGTH_SHORT).show()
                }
                progressDialog.dismiss()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TAG", "onDestroy")
        scope?.cancel()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        Log.d("TAG", "onDismiss")
        scope?.cancel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPdfOptBinding.inflate(layoutInflater)
        return binding.root
    }

    private fun updateUi() {
        if (type == TYPE_MERGE) {
            binding.layoutMerge.visibility = View.VISIBLE
            binding.layoutExtract.visibility = View.GONE
        } else {
            binding.layoutMerge.visibility = View.GONE
            binding.layoutExtract.visibility = View.VISIBLE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //dialog?.setTitle("创建pdf")
        //binding.back.setOnClickListener { dismiss() }
        binding.toolbar.setNavigationOnClickListener { dismiss() }

        binding.btnMerge.setOnClickListener {
            type = TYPE_MERGE
            updateUi()
        }
        binding.btnExtract.setOnClickListener {
            type = TYPE_EXTRACT_IMAGES
            updateUi()
        }

        binding.btnAddPdf.setOnClickListener { selectPdf() }
        binding.btnExtractImage.setOnClickListener { extractImages() }
        binding.btnExtractHtml.setOnClickListener { extractHtml() }
        binding.btnMergePdf.setOnClickListener { merge() }

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
        binding.extract.extractLayout.visibility = View.GONE
    }

    private fun setupRangeSlider(count: Int) {
        binding.extract.tvStart.text = "从1页"
        binding.extract.tvEnd.text = "到${count}页"

        binding.extract.rangeSlider.valueFrom = 1f
        binding.extract.rangeSlider.valueTo = count.toFloat()
        val values = mutableListOf<Float>()
        values.add(1f)
        values.add(count.toFloat())
        binding.extract.rangeSlider.values = values
        binding.extract.rangeSlider.setLabelFormatter { value ->
            value.toInt().toString()
        }

        binding.extract.rangeSlider.addOnSliderTouchListener(object :
            RangeSlider.OnSliderTouchListener {
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: RangeSlider) {
            }

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: RangeSlider) {
                binding.extract.tvStart.text =
                    "从${binding.extract.rangeSlider.values[0].toInt()}页"
                binding.extract.tvEnd.text = "到${binding.extract.rangeSlider.values[1].toInt()}页"
            }
        })
    }

    private fun merge() {
        Toast.makeText(activity, "not implemented", Toast.LENGTH_SHORT).show()
    }

    inner class ViewHolder(root: View) : BaseViewHolder<String>(root) {

        private var delete: View? = null
        private var tvName: TextView? = null

        init {
            delete = root.findViewById(R.id.delete)
            tvName = root.findViewById(R.id.tvName)
        }

        override fun onBind(data: String, position: Int) {
            delete?.setOnClickListener { deleteItem(data, position) }
            tvName?.text = data
        }
    }

    private fun deleteItem(data: String, position: Int) {
        adapter.data.remove(data)
        adapter.notifyDataSetChanged()
    }

    private fun setType(type: Int) {
        this.type = type
    }

    companion object {

        const val TAG = "PdfOperationFragment"
        const val TYPE_MERGE = 0
        const val TYPE_EXTRACT_IMAGES = 1

        fun showCreateDialog(
            type: Int,
            activity: FragmentActivity?,
            dataListener: DataListener?
        ) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("create_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val pdfFragment = PdfOperationFragment()
            pdfFragment.setType(type)
            pdfFragment.setListener(dataListener)
            pdfFragment.show(ft!!, "pdf_operation")
        }
    }
}
