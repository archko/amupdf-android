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
import androidx.lifecycle.lifecycleScope
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentPdfOptBinding
import cn.archko.pdf.common.PDFCreaterHelper
import cn.archko.pdf.core.adapters.BaseViewHolder
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.entity.ResponseHandler
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.utils.Utils
import com.artifex.mupdf.fitz.Document
import com.google.android.material.slider.RangeSlider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * @author: archko 2023/3/8 :14:34
 */
class PdfOperationFragment : DialogFragment(R.layout.fragment_pdf_opt) {

    private lateinit var binding: FragmentPdfOptBinding
    protected lateinit var progressDialog: ProgressDialog

    private var mDataListener: DataListener? = null
    private var txtPath: String? = null

    private var type: Int = TYPE_MERGE

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = cn.archko.pdf.R.style.AppTheme
        setStyle(STYLE_NO_TITLE, themeId)

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
                    requireContext(),
                    result.data?.data
                )
                if (type == TYPE_EXTRACT_IMAGES) {
                    txtPath = path
                    binding.pdfPath.text = path
                    loadPdf(path)
                } else {
                    //adapter.data.add(adapter.itemCount, path)
                    //adapter.notifyDataSetChanged()
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
            Toast.makeText(requireActivity(), R.string.edit_cannot_load_pdf, Toast.LENGTH_LONG)
                .show()
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
            Toast.makeText(activity, R.string.edit_create_select_file, Toast.LENGTH_SHORT).show()
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
            start = binding.extract.rangeSlider.values[0].toInt() - 1
            end = binding.extract.rangeSlider.values[1].toInt()
            width = binding.extract.resolutionSlider.value.toInt()
        }

        lifecycleScope.launch {
            flow {
                try {
                    val result = PDFCreaterHelper.extractToImages(
                        requireActivity(),
                        width,
                        dir,
                        txtPath!!,
                        start,
                        end
                    )
                    emit(ResponseHandler.Success(result))
                } catch (e: Exception) {
                    emit(ResponseHandler.Failure())
                }
            }.flowOn(Dispatchers.IO)
                .collectLatest { response ->
                    progressDialog.dismiss()
                    if (response is ResponseHandler.Success<Int>) {
                        if (response.data == 0) {
                            Toast.makeText(
                                activity,
                                R.string.edit_extract_success,
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        } else if (response.data == -2) {
                            Toast.makeText(
                                activity,
                                R.string.edit_extract_error,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                activity,
                                String.format(
                                    getString(R.string.edit_extract_cancel_pages),
                                    response.data.toString()
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
        }
    }

    private fun extractHtml() {
        if (TextUtils.isEmpty(txtPath)) {
            Toast.makeText(activity, R.string.edit_create_select_file, Toast.LENGTH_SHORT).show()
            return
        }
        val name = FileUtils.getNameWithoutExt(txtPath)
        val dir = FileUtils.getStorageDir(name).absolutePath

        progressDialog.show()

        lifecycleScope.launch {
            flow {
                try {
                    var start: Int = 0
                    var end: Int = 0
                    if (binding.extract.extractLayout.visibility == View.VISIBLE) {
                        start = binding.extract.rangeSlider.values[0].toInt() - 1
                        end = binding.extract.rangeSlider.values[1].toInt()
                    }
                    val result = PDFCreaterHelper.extractToHtml(
                        start, end,
                        requireActivity(),
                        "$dir/$name.html",
                        txtPath!!
                    )

                    emit(ResponseHandler.Success(result))
                } catch (e: Exception) {
                    emit(ResponseHandler.Failure())
                }
            }.flowOn(Dispatchers.IO)
                .collectLatest { response ->
                    progressDialog.dismiss()
                    if (response is ResponseHandler.Success<Boolean>) {
                        if (response.data) {
                            Toast.makeText(
                                activity,
                                R.string.edit_extract_success,
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        } else {
                            Toast.makeText(
                                activity,
                                R.string.edit_extract_error,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("TAG", "onDestroy")
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        Log.d("TAG", "onDismiss")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentPdfOptBinding.inflate(layoutInflater)
        return binding.root
    }

    private fun updateUi() {
        /*if (type == TYPE_MERGE) {
            binding.layoutMerge.visibility = View.VISIBLE
            binding.layoutExtract.visibility = View.GONE
        } else {
            binding.layoutMerge.visibility = View.GONE
            binding.layoutExtract.visibility = View.VISIBLE
        }*/
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }

        binding.btnExtract.setOnClickListener {
            type = TYPE_EXTRACT_IMAGES
            updateUi()
        }

        binding.btnAddPdf.setOnClickListener { selectPdf() }
        binding.btnExtractImage.setOnClickListener { extractImages() }
        binding.btnExtractHtml.setOnClickListener { extractHtml() }
        /*adapter = object : BaseRecyclerAdapter<String>(activity) {

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): BaseViewHolder<String> {
                val root = inflater.inflate(R.layout.item_image, parent, false)
                return ViewHolder(root)
            }
        }
        binding.recyclerView.layoutManager = GridLayoutManager(activity, 2)
        binding.recyclerView.adapter = adapter*/

        updateUi()
        binding.extract.extractLayout.visibility = View.GONE
    }

    private fun setupRangeSlider(count: Int) {
        binding.extract.tvStart.text = String.format(getString(R.string.edit_from_page), "1")
        binding.extract.tvEnd.text =
            String.format(getString(R.string.edit_to_page), count.toString())

        binding.extract.rangeSlider.valueFrom = 1f
        if (count <= 1) {
            binding.extract.rangeSlider.valueTo = 2f
            binding.extract.rangeSlider.isEnabled = false
            binding.extract.rangeSlider.values = listOf(1f, 2f)
            return
        }
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
                    String.format(
                        getString(R.string.edit_from_page),
                        binding.extract.rangeSlider.values[0].toString()
                    )
                binding.extract.tvEnd.text =
                    String.format(
                        getString(R.string.edit_to_page),
                        binding.extract.rangeSlider.values[1].toString()
                    )
            }
        })
    }

    private fun merge() {
        Toast.makeText(activity, R.string.edit_extract_not_impl, Toast.LENGTH_SHORT).show()
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
        //adapter.data.remove(data)
        //adapter.notifyDataSetChanged()
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