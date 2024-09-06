package cn.archko.pdf.fragments

import android.app.ProgressDialog
import android.content.res.Configuration
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.awidget.LinearLayoutManager
import cn.archko.mupdf.R
import cn.archko.mupdf.databinding.FragmentPdfEditBinding
import cn.archko.pdf.common.PDFCreaterHelper
import cn.archko.pdf.core.cache.BitmapCache
import cn.archko.pdf.core.cache.BitmapPool
import cn.archko.pdf.core.common.Logcat
import cn.archko.pdf.core.listeners.ClickListener
import cn.archko.pdf.core.listeners.DataListener
import cn.archko.pdf.core.utils.ColorUtil.getColorMode
import cn.archko.pdf.core.utils.FileUtils
import cn.archko.pdf.core.utils.Utils
import cn.archko.pdf.decode.DocDecodeService
import cn.archko.pdf.decode.DocDecodeService.IView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vudroid.core.DecodeServiceBase
import org.vudroid.core.codec.CodecDocument

/**
 * @author: archko 2023/3/8 :14:34
 */
class PdfEditFragment : DialogFragment(R.layout.fragment_pdf_edit) {

    private lateinit var binding: FragmentPdfEditBinding
    protected lateinit var progressDialog: ProgressDialog
    private var pdfEditViewModel = PDFEditViewModel()
    private var decodeService: DocDecodeService? = null

    private var mDataListener: DataListener? = null
    private lateinit var pdfAdapter: MupdfGridAdapter

    private var path: String? = null
    private var job: Job? = null
    private var crop: Boolean = false

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
        binding = FragmentPdfEditBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { dismiss() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            if (R.id.saveItem == item?.itemId) {
                //pdfEditViewModel.save()
            } else if (R.id.extractHtmlItem == item?.itemId) {
                val name = FileUtils.getNameWithoutExt(path)
                val dir = FileUtils.getStorageDir(name).absolutePath

                progressDialog.show()
                progressDialog.setCanceledOnTouchOutside(false)

                job = lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        decodeService?.pageCount?.let {
                            PDFCreaterHelper.extractToHtml(
                                0, it,
                                requireActivity(),
                                "$dir/$name.html",
                                path!!
                            )
                        }
                    }
                    if (result == true) {
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
                    progressDialog.dismiss()
                }
            }
            true
        }

        val colorMatrix = getColorMode(1)
        binding.autoCropButton.colorFilter = ColorMatrixColorFilter(ColorMatrix(colorMatrix))
        binding.autoCropButton.setOnClickListener { toggleCrop() }

        val iView = object : IView {
            override fun getWidth(): Int {
                return binding.recyclerView.width
            }

            override fun getHeight(): Int {
                return binding.recyclerView.height
            }
        }
        path?.let {
            val codecContext = DecodeServiceBase.openContext(it)
            if (null == codecContext) {
                Toast.makeText(requireActivity(), "open file error", Toast.LENGTH_SHORT).show()
                return@let
            }
            decodeService = DocDecodeService(codecContext)
            decodeService!!.setContainerView(iView)
            pdfAdapter = MupdfGridAdapter(
                decodeService!!,
                requireActivity(),
                binding.recyclerView,
                crop,
                object : ClickListener<View> {
                    override fun click(t: View?, pos: Int) {
                        if (binding.toolbarLayout.visibility == View.GONE) {
                            binding.toolbarLayout.visibility = View.VISIBLE
                        } else {
                            binding.toolbarLayout.visibility = View.GONE
                        }
                        Toast.makeText(
                            requireActivity(),
                            String.format(
                                getString(R.string.edit_page),
                                "${pos + 1}/" + decodeService!!.pageCount
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun longClick(t: View?, pos: Int, view: View) {
                        //showPopupMenu(view, pos)
                    }

                })
            binding.recyclerView.layoutManager = LinearLayoutManager(activity)
            binding.recyclerView.adapter = pdfAdapter
            binding.recyclerView.setHasFixedSize(true)

            val document: CodecDocument = decodeService!!.open(it, true)
            pdfAdapter.notifyDataSetChanged()
        }
    }

    private fun toggleCrop() {
        crop = !crop
        val resId =
            if (crop) cn.archko.pdf.R.drawable.ic_crop else cn.archko.pdf.R.drawable.ic_no_crop
        binding.autoCropButton.setImageResource(resId)
        binding.recyclerView.stopScroll()

        pdfAdapter.setCrop(crop)
    }

    private fun showPopupMenu(view: View, position: Int) {
        val popupMenu = PopupMenu(requireActivity(), view)
        popupMenu.menuInflater.inflate(R.menu.edit_menus, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            if (R.id.deleteItem == item.itemId) {
                pdfEditViewModel.deletePage(position)
                pdfAdapter.notifyItemRemoved(position)
            } else if (R.id.addItem == item.itemId) {
            } else if (R.id.extractImagesItem == item.itemId) {
                val width = pdfEditViewModel.aPageList[0].width.toInt()
                val dialog = ExtractDialog(requireActivity(),
                    width,
                    position,
                    pdfEditViewModel.countPages(),
                    object : ExtractDialog.ExtractListener {
                        override fun export(index: Int, width: Int) {
                            extract(index - 1, index, width)
                        }

                        override fun exportRange(start: Int, end: Int, width: Int) {
                            extract(start - 1, end, width)
                        }

                    })
                dialog.show()
            }
            true
        }
        popupMenu.show()
    }

    private fun extract(start: Int, end: Int, width: Int) {
        val name = FileUtils.getNameWithoutExt(path)
        val dir = FileUtils.getStorageDir(name).absolutePath

        progressDialog.show()
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.setOnDismissListener {
            PDFCreaterHelper.canExtract = false
        }

        PDFCreaterHelper.canExtract = true
        job = lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                PDFCreaterHelper.extractToImages(
                    requireActivity(),
                    //Utils.getScreenWidthPixelWithOrientation(requireActivity()),
                    width,
                    dir,
                    path!!,
                    start,
                    end
                )
            }
            if (result == 0) {
                Toast.makeText(activity, R.string.edit_extract_success, Toast.LENGTH_SHORT)
                    .show()
                //Toast.makeText(activity, "导出成功到$dir", Toast.LENGTH_SHORT).show()
            } else if (result == -2) {
                Toast.makeText(activity, R.string.edit_extract_error, Toast.LENGTH_SHORT).show()
                //Toast.makeText(activity, "导出错误!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "取消导出,已导出:${result}张", Toast.LENGTH_SHORT)
                    .show()
            }
            progressDialog.dismiss()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()

        decodeService?.recycle()

        BitmapCache.getInstance().clear()
        BitmapPool.getInstance().clear()
    }

    private fun deleteItem(data: String, position: Int) {
        pdfAdapter.notifyDataSetChanged()
    }

    fun setPath(path: String) {
        this.path = path
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.recyclerView.stopScroll()
        BitmapCache.getInstance().clear()

        val defaultWidth = Utils.dipToPixel(newConfig.screenWidthDp.toFloat())
        val defaultHeight = Utils.dipToPixel(newConfig.screenHeightDp.toFloat())
        Logcat.d(
            TAG, String.format(
                "newConfig:w-h:%s-%s, config:%s-%s, %s",
                defaultWidth,
                defaultHeight,
                newConfig.screenWidthDp,
                newConfig.screenHeightDp,
                newConfig.orientation
            )
        )

        val lm = (binding.recyclerView.layoutManager as LinearLayoutManager)
        var offset = 0
        val first = lm.findFirstVisibleItemPosition()
        if (first > 0) {
            val child = lm.findViewByPosition(first)
            child?.run {
                val r = Rect()
                child.getLocalVisibleRect(r)
                offset = r.top
            }
        }
        lm.scrollToPositionWithOffset(first, -offset)
        binding.recyclerView.adapter?.notifyDataSetChanged()
    }

    companion object {

        const val TAG = "PdfEditFragment"
        const val TYPE_MERGE = 0
        const val TYPE_EXTRACT_IMAGES = 1

        fun showCreateDialog(
            path: String,
            activity: FragmentActivity?,
            dataListener: DataListener?
        ) {
            val ft = activity?.supportFragmentManager?.beginTransaction()
            val prev = activity?.supportFragmentManager?.findFragmentByTag("create_dialog")
            if (prev != null) {
                ft?.remove(prev)
            }
            ft?.addToBackStack(null)

            val pdfFragment = PdfEditFragment()
            pdfFragment.setPath(path)
            pdfFragment.setListener(dataListener)
            pdfFragment.show(ft!!, "pdf_operation")
        }
    }
}
