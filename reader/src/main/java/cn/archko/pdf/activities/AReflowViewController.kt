package cn.archko.pdf.activities

import android.annotation.SuppressLint
import android.app.Activity.RESULT_FIRST_USER
import android.content.res.Configuration
import android.util.SparseArray
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import androidx.core.app.ComponentActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.archko.pdf.AppExecutors
import cn.archko.pdf.R
import cn.archko.pdf.adapters.MuPDFReflowAdapter
import cn.archko.pdf.colorpicker.ColorPickerDialog
import cn.archko.pdf.common.Logcat
import cn.archko.pdf.common.PdfOptionRepository
import cn.archko.pdf.common.StyleHelper
import cn.archko.pdf.databinding.TextStyleBinding
import cn.archko.pdf.entity.APage
import cn.archko.pdf.entity.FontBean
import cn.archko.pdf.fragments.FontsFragment
import cn.archko.pdf.listeners.AViewController
import cn.archko.pdf.listeners.DataListener
import cn.archko.pdf.listeners.OutlineListener
import cn.archko.pdf.mupdf.MupdfDocument
import cn.archko.pdf.viewmodel.PDFViewModel
import cn.archko.pdf.widgets.APageSeekBarControls
import cn.archko.pdf.widgets.ViewerDividerItemDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

/**
 * @author: archko 2020/5/15 :12:43
 */
class AReflowViewController(
    private var context: ComponentActivity,
    private var contentView: View,
    private val mControllerLayout: RelativeLayout,
    private var pdfViewModel: PDFViewModel,
    private var mPath: String,
    private var mPageSeekBarControls: APageSeekBarControls?,
    private var gestureDetector: GestureDetector?,
    private var optionRepository: PdfOptionRepository
) :
    OutlineListener, AViewController {


    private var mStyleControls: View? = null

    private lateinit var binding: TextStyleBinding

    private var colorPickerDialog: ColorPickerDialog? = null

    private lateinit var mRecyclerView: RecyclerView
    private var mStyleHelper: StyleHelper? = null
    private var mMupdfDocument: MupdfDocument? = null
    private val START_PROGRESS = 15
    private lateinit var mPageSizes: SparseArray<APage>
    private var scope: CoroutineScope? = null

    init {
        initView()
    }

    private fun initView() {
        mRecyclerView = RecyclerView(context)//contentView.findViewById(R.id.recycler_view)
        with(mRecyclerView) {
            descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            isNestedScrollingEnabled = false
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            setItemViewCacheSize(0)

            addItemDecoration(ViewerDividerItemDecoration(context, LinearLayoutManager.VERTICAL))
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        updateProgress(getCurrentPos())
                    }
                }

                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                }
            })
        }
    }

    override fun init(pageSizes: SparseArray<APage>, mupdfDocument: MupdfDocument?, pos: Int) {
        try {
            if (scope == null || !scope!!.isActive) {
                scope =
                    CoroutineScope(Job() + AppExecutors.instance.diskIO().asCoroutineDispatcher())
            }
            Logcat.d("init:$this")
            if (null != mupdfDocument) {
                this.mPageSizes = pageSizes
                this.mMupdfDocument = mupdfDocument

                setReflowMode(pos)
            }
            addGesture()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    override fun doLoadDoc(pageSizes: SparseArray<APage>, mupdfDocument: MupdfDocument, pos: Int) {
        try {
            Logcat.d("doLoadDoc:$this")
            this.mPageSizes = pageSizes
            this.mMupdfDocument = mupdfDocument

            setReflowMode(pos)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
        }
    }

    override fun getDocumentView(): View {
        return mRecyclerView
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun addGesture() {
        mRecyclerView.setOnTouchListener { v, event ->
            gestureDetector?.onTouchEvent(event)
            false
        }
    }

    private fun setReflowMode(pos: Int) {
        if (null == mStyleHelper) {
            mStyleHelper = StyleHelper(context, optionRepository)
        }
        if (null == mRecyclerView.adapter) {

            mRecyclerView.adapter =
                MuPDFReflowAdapter(context, mMupdfDocument, mStyleHelper, scope, pdfViewModel)
        } else {
            (mRecyclerView.adapter as MuPDFReflowAdapter).setScope(scope)
        }

        if (pos > 0) {
            (mRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(pos, 0)
        }
    }

    override fun getCurrentPos(): Int {
        if (null == mRecyclerView.layoutManager) {
            return 0
        }
        var position =
            (mRecyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        if (position < 0) {
            position = 0
        }
        return position
    }

    override fun scrollToPosition(page: Int) {
        mRecyclerView.layoutManager?.run {
            val layoutManager: LinearLayoutManager = this as LinearLayoutManager
            layoutManager.scrollToPositionWithOffset(page, 0)
        }
    }

    override fun scrollPage(y: Int, top: Int, bottom: Int, margin: Int): Boolean {
        if (y < top) {
            var scrollY = mRecyclerView.scrollY
            scrollY -= mRecyclerView.height
            mRecyclerView.scrollBy(0, scrollY + margin)
            return true
        } else if (y > bottom) {
            var scrollY = mRecyclerView.scrollY
            scrollY += mRecyclerView.height
            mRecyclerView.scrollBy(0, scrollY - margin)
            return true
        }
        return false
    }

    override fun onSingleTap() {
        //if (mPageSeekBarControls?.visibility == View.VISIBLE) {
        //    mPageSeekBarControls?.hide()
        //    return
        //}
        showReflowConfigMenu()
    }

    override fun onDoubleTap() {
        //if (mMupdfDocument == null) {
        //    return
        //}
        //mPageSeekBarControls?.hide()
        mStyleControls?.visibility = View.GONE
        //if (!mDrawerLayout.isDrawerOpen(mLeftDrawer)) {
        //    mDrawerLayout.openDrawer(mLeftDrawer)
        //} else {
        //    mDrawerLayout.closeDrawer(mLeftDrawer)
        //}
        //showOutline()
    }

    override fun onSelectedOutline(index: Int) {
        mRecyclerView.layoutManager?.scrollToPosition(index - RESULT_FIRST_USER)
        updateProgress(index - RESULT_FIRST_USER)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        mRecyclerView.stopScroll()
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    fun updateProgress(index: Int) {
        if (mMupdfDocument != null && mPageSeekBarControls?.visibility == View.VISIBLE) {
            mPageSeekBarControls?.updatePageProgress(index)
        }
    }

    override fun notifyDataSetChanged() {
        mRecyclerView.adapter?.notifyDataSetChanged()
    }

    override fun notifyItemChanged(pos: Int) {
        mRecyclerView.adapter?.notifyItemChanged(pos)
    }

    //--------------------------------------

    override fun onResume() {
        mStyleControls?.visibility = View.GONE

        mRecyclerView.postDelayed({ mRecyclerView.adapter?.notifyDataSetChanged() }, 250L)
    }

    override fun onPause() {
        pdfViewModel.getBookProgress()?.reflow = 1
        val position = getCurrentPos()
        val zoomLevel = pdfViewModel.getBookProgress()!!.zoomLevel
        pdfViewModel.saveCurrentPage(
            mPath,
            mMupdfDocument!!.countPages(),
            position,
            zoomLevel,
            -1,
            0
        )
        if (null != mRecyclerView.adapter && mRecyclerView.adapter is MuPDFReflowAdapter) {
            (mRecyclerView.adapter as MuPDFReflowAdapter).clearCacheViews()
        }
    }

    override fun onDestroy() {
        scope?.let {
            scope!!.cancel()
        }
    }

    //===========================================
    override fun showController() {
        mStyleControls?.visibility = View.VISIBLE
    }

    private fun showReflowConfigMenu() {
        if (null == mStyleControls) {
            initStyleControls()
        } else {
            if (mStyleControls?.visibility == View.VISIBLE) {
                mStyleControls?.visibility = View.GONE
            } else {
                showStyleFragment()
            }
        }
    }

    private fun initStyleControls() {
        mPageSeekBarControls?.hide()
        if (null == mStyleControls) {
            binding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.text_style,
                null,
                false
            )
            mStyleControls = binding.root
            //LayoutInflater.from(context).inflate(R.layout.text_style, null, false)

            val lp = RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
            mControllerLayout.addView(mStyleControls, lp)
        }
        mStyleControls?.visibility = View.VISIBLE

        mStyleHelper?.let {
            val progress = (it.styleBean?.textSize!! - START_PROGRESS).toInt()
            binding.fontSeekBar.progress = progress
            binding.fontSizeLabel.text = String.format("%s", progress + START_PROGRESS)
            binding.fontSeekBar.max = 10
            binding.fontSeekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val index = (progress + START_PROGRESS)
                    binding.fontSizeLabel.text = String.format("%s", index)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    it.styleBean?.textSize = (seekBar?.progress!! + START_PROGRESS).toFloat()
                    it.saveStyleToSP(it.styleBean)
                    updateReflowAdapter()
                }
            })
            binding.fontFaceSelected.text = it.fontHelper?.fontBean?.fontName

            binding.lineSpaceLabel.text = String.format("%s倍", it.styleBean?.lineSpacingMult)
            binding.colorLabel.setBackgroundColor(it.styleBean?.bgColor!!)
            binding.colorLabel.setTextColor(it.styleBean?.fgColor!!)
        }

        binding.fontFaceChange.setOnClickListener {
            FontsFragment.showFontsDialog(
                context as FragmentActivity, mStyleHelper,
                object : DataListener {
                    override fun onSuccess(vararg args: Any?) {
                        updateReflowAdapter()
                        val fBean = args[0] as FontBean
                        binding.fontFaceSelected.text = fBean.fontName
                    }

                    override fun onFailed(vararg args: Any?) {
                    }
                })
        }

        binding.linespaceMinus.setOnClickListener {
            var old = mStyleHelper?.styleBean?.lineSpacingMult
            if (old!! < 0.8f) {
                return@setOnClickListener
            }
            old = old.minus(0.1f)
            applyLineSpace(old)
        }
        binding.linespacePlus.setOnClickListener {
            var old = mStyleHelper?.styleBean?.lineSpacingMult
            if (old!! > 2.2f) {
                return@setOnClickListener
            }
            old = old?.plus(0.1f)
            applyLineSpace(old)
        }
        binding.bgSetting.setOnClickListener {
            pickerColor(
                mStyleHelper?.styleBean?.bgColor!!
            ) { color ->
                binding.colorLabel.setBackgroundColor(color)
                mStyleHelper?.styleBean?.bgColor = color
                mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
                updateReflowAdapter()
            }
        }
        binding.fgSetting.setOnClickListener {
            pickerColor(
                mStyleHelper?.styleBean?.fgColor!!
            ) { color ->
                binding.colorLabel.setTextColor(color)
                mStyleHelper?.styleBean?.fgColor = color
                mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
                updateReflowAdapter()
            }
        }
    }

    private fun updateReflowAdapter() {
        mRecyclerView.adapter?.run {
            this.notifyDataSetChanged()
        }
    }

    private fun applyLineSpace(old: Float?) {
        binding.lineSpaceLabel.text = String.format("%s倍", old)
        mStyleHelper?.styleBean?.lineSpacingMult = old!!
        mStyleHelper?.saveStyleToSP(mStyleHelper?.styleBean)
        updateReflowAdapter()
    }

    private fun pickerColor(
        initialColor: Int,
        selectedListener: ColorPickerDialog.OnColorSelectedListener
    ) {
        if (null == colorPickerDialog) {
            colorPickerDialog = ColorPickerDialog(context, initialColor, selectedListener)
        } else {
            colorPickerDialog?.updateColor(initialColor)
            colorPickerDialog?.setOnColorSelectedListener(selectedListener)
        }
        colorPickerDialog?.show()
    }

    private fun showStyleFragment() {
        mStyleControls?.visibility = View.VISIBLE
    }

}
