package cn.archko.pdf.core.ui

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentReflowPdfBinding
import cn.archko.pdf.common.ReflowHelper
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.cache.BitmapPool
import cn.archko.pdf.core.common.IntentFile
import cn.archko.pdf.core.listeners.ClickListener
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.decode.DocDecodeService
import cn.archko.pdf.decode.DocDecodeService.IView
import cn.archko.pdf.fragments.MupdfGridAdapter
import cn.archko.pdf.fragments.PDFEditViewModel
import cn.archko.pdf.fragments.ReflowDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vudroid.core.DecodeServiceBase
import org.vudroid.core.codec.CodecDocument

/**
 * @author: archko 2023/7/28 :14:34
 */
class PdfReflowFragment : DialogFragment(R.layout.fragment_reflow_pdf) {

    private lateinit var binding: FragmentReflowPdfBinding
    protected lateinit var progressDialog: ProgressDialog
    private var pdfEditViewModel = PDFEditViewModel()
    private var pdfPath: String? = null

    private var mDataListener: DataListener? = null
    private lateinit var pdfAdapter: MupdfGridAdapter

    private var job: Job? = null

    fun setListener(dataListener: DataListener?) {
        mDataListener = dataListener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var themeId = cn.archko.pdf.R.style.AppTheme
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReflowPdfBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }

        val iView= object : IView {
            override fun getWidth(): Int {
                return binding.recyclerView.getWidth()
            }

            override fun getHeight(): Int {
                return binding.recyclerView.getHeight()
            }
        }

        //pdfPath = "/storage/emulated/0/book/3、医方真谛.pdf"
        pdfPath?.let {
            val codecContext = DecodeServiceBase.openContext(it)
            if (null == codecContext) {
                Toast.makeText(requireActivity(), "open file error", Toast.LENGTH_SHORT).show()
                return@let
            }
            val decodeService = DocDecodeService(codecContext)
            decodeService.setContainerView(iView)
            pdfAdapter = MupdfGridAdapter(
                decodeService,
                requireActivity(),
                binding.recyclerView,
                object : ClickListener<View> {
                    override fun click(t: View?, pos: Int) {
                        showReflowDialog(pos)
                    }

                    override fun longClick(t: View?, pos: Int, view: View) {
                    }
                })
            binding.recyclerView.layoutManager = LinearLayoutManager(activity)
            binding.recyclerView.adapter = pdfAdapter
            binding.recyclerView.setHasFixedSize(true)

            binding.btnAddPdf.setOnClickListener { selectPdf() }
            binding.btnReflow.setOnClickListener {
                if (null != pdfEditViewModel.getDocument()) {
                    showReflowDialog(0)
                }
            }

            val document: CodecDocument = decodeService.open(pdfPath, false, true)
            pdfAdapter.notifyDataSetChanged()
        }
    }

    private fun showReflowDialog(pos: Int) {
        val dialog = ReflowDialog(requireActivity(),
            Utils.getScreenWidthPixelWithOrientation(requireActivity()),
            Utils.getScreenHeightPixelWithOrientation(requireActivity()),
            pos,
            pdfEditViewModel,
            pdfPath!!,
            object : ReflowDialog.ReflowListener {

                override fun exportRange(start: Int, end: Int, width: Int) {
                    reflow(start - 1, end, width)
                }

            })
        dialog.show()
    }

    private val pickPdf =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult? ->
            if (result?.resultCode == Activity.RESULT_OK) {
                val path = IntentFile.getPath(
                    requireActivity(),
                    result.data?.data
                )
                pdfPath = path

                path?.let {
                    pdfEditViewModel.loadPdfDoc(requireActivity(), it, null)
                    pdfAdapter.notifyDataSetChanged()
                }
            }
        }

    private fun selectPdf() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("application/pdf")
        pickPdf.launch(intent)
    }

    private fun reflow(start: Int, end: Int, width: Int) {
        progressDialog.show()
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setCancelable(false)

        job = lifecycleScope.launch {
            val name = FileUtils.getNameWithoutExt(pdfPath)
            val dir = FileUtils.getStorageDir(name).absolutePath
            val result = withContext(Dispatchers.IO) {
                ReflowHelper.reflow(
                    pdfEditViewModel.mupdfDocument,
                    pdfEditViewModel.opt,
                    requireActivity(),
                    binding.layoutTmp,
                    start, end,
                    Utils.getScreenWidthPixelWithOrientation(requireActivity()),
                    Utils.getScreenHeightPixelWithOrientation(requireActivity()),
                    width,
                    dir,
                    name
                )
            }
            if (result > 0) {
                Toast.makeText(activity, "重排成功:$dir", Toast.LENGTH_SHORT).show()
            } else if (result == -2) {
                Toast.makeText(activity, "重排错误!", Toast.LENGTH_SHORT).show()
            }
            progressDialog.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()

        BitmapCache.getInstance().clear()
        BitmapPool.getInstance().clear()
    }

    companion object {

        const val TAG = "PdfReflowFragment"

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

            val pdfFragment = PdfReflowFragment()
            pdfFragment.setListener(dataListener)
            pdfFragment.show(ft!!, "pdf_reflow")
        }
    }
}
